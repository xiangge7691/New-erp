#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
精创云（new.e-elitech.cn）温湿度数据同步脚本

功能：
    从精创云平台按天读取温湿度记录仪的采集数据，汇总后写入 ERP 的
    temperature_humidity_record 表（房间温湿度记录表）。

设计说明：
    1. 不侵入 ERP 后端，独立运行，可由 Windows 计划任务定时调用。
    2. 平台按 5 分钟一条上报，而 ERP 表只有 record_date（无采集时间），
       默认按天汇总成一条：温度/湿度取日均值，极值和样本数写入 remark。
    3. 支持 --mode raw 按原始采集点逐条写入（一天多行，record_date 相同），
       仅在确认前端能接受重复日期时使用。
    4. 写库前按 房间+日期+记录人 查重，存在则更新，保证重复执行幂等。

依赖：
    Python 3.9+，pymysql（pip install pymysql）

示例：
    # 预览昨天数据，不写库（默认行为）
    python sync_room_th.py

    # 预览指定日期
    python sync_room_th.py --date 2026-11-23

    # 写入昨天数据到测试库
    python sync_room_th.py --commit

    # 写入今天已经产生的数据（手动补数时用）
    python sync_room_th.py --date today --commit

    # 列出账号下的设备，确认 deviceId / deviceTypeId
    python sync_room_th.py --list-devices
"""

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import date, datetime, timedelta, timezone

# ===================================
# 配置读取
# ===================================
# 所有配置均可用环境变量覆盖，未设置时使用下面的默认值。

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
TZ_CN = timezone(timedelta(hours=8))

ELITECH_BASE = os.environ.get("ELITECH_BASE", "https://new.e-elitech.cn").rstrip("/")
ELITECH_TOKEN_FILE = os.environ.get("ELITECH_TOKEN_FILE", os.path.join(SCRIPT_DIR, "token.txt"))
ELITECH_DEVICE_ID = int(os.environ.get("ELITECH_DEVICE_ID", "1708729"))
ELITECH_DEVICE_TYPE_ID = int(os.environ.get("ELITECH_DEVICE_TYPE_ID", "37159"))
ELITECH_DEVICE_NAME = os.environ.get("ELITECH_DEVICE_NAME", "温湿度检测仪")
ELITECH_DEVICE_GUID = os.environ.get("ELITECH_DEVICE_GUID", "90265662327170175293")
ELITECH_DEVICE_MODEL = os.environ.get("ELITECH_DEVICE_MODEL", "GSP-8G-C")

ROOM_CODE = os.environ.get("ERP_ROOM_CODE", "HT2004")
RECORDER = os.environ.get("ERP_RECORDER", "精创云自动采集")

DB_HOST = os.environ.get("ERP_DB_HOST", "192.168.5.100")
DB_PORT = int(os.environ.get("ERP_DB_PORT", "3306"))
DB_USER = os.environ.get("ERP_DB_USER", "root")
DB_PASSWORD = os.environ.get("ERP_DB_PASSWORD", "89749050")
DB_NAME = os.environ.get("ERP_DB_NAME", "erp_db")

# 温度、湿度探头的识别规则：优先按单位判断，单位缺失时回退到参数编码。
TEMP_UNITS = {"℃", "°C", "C"}
HUMI_UNITS = {"%RH", "%", "RH"}
TEMP_PARAM_CODES = {"0110"}
HUMI_PARAM_CODES = {"0120"}


# ===================================
# 平台接口调用
# ===================================

def load_token():
    """读取访问令牌：优先环境变量 ELITECH_TOKEN，其次读取 token.txt。"""
    token = os.environ.get("ELITECH_TOKEN", "").strip()
    if token:
        return token
    if os.path.exists(ELITECH_TOKEN_FILE):
        with open(ELITECH_TOKEN_FILE, "r", encoding="utf-8") as f:
            token = f.read().strip()
        if token:
            return token
    raise SystemExit(
        "缺少访问令牌：请设置环境变量 ELITECH_TOKEN，或把令牌写入 %s" % ELITECH_TOKEN_FILE
    )


def token_expire_hint(token):
    """解析 JWT 的 exp，返回可读的过期提示（不校验签名）。"""
    try:
        import base64
        payload = token.split(".")[1]
        payload += "=" * (-len(payload) % 4)
        data = json.loads(base64.urlsafe_b64decode(payload))
        exp = datetime.fromtimestamp(data["exp"], TZ_CN)
        return exp
    except Exception:
        return None


def api_request(token, method, path, body=None):
    """调用平台接口，返回解析后的 JSON；失败时抛出带说明的异常。"""
    url = ELITECH_BASE + path
    headers = {
        "Authorization": "bearer " + token,
        "localeCode": "zh-CN",
        "Time-Zone": "Asia/Shanghai",
        "Accept": "application/json, text/plain, */*",
    }
    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"

    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        text = e.read().decode("utf-8", "replace")
        if e.code == 401:
            raise SystemExit(
                "平台返回 401，访问令牌已失效。请在浏览器重新登录 new.e-elitech.cn，"
                "复制 localStorage 中的 Access-Token 到 %s。\n原始响应：%s"
                % (ELITECH_TOKEN_FILE, text[:300])
            )
        raise SystemExit("平台接口调用失败 HTTP %s：%s\n原始响应：%s" % (method, e.code, text[:300]))
    except urllib.error.URLError as e:
        raise SystemExit("无法连接精创云平台：%s" % e)


def list_devices(token):
    """列出账号下的设备，用于核对 deviceId / deviceTypeId。"""
    result = api_request(token, "POST", "/api/device/Device/get/deviceList",
                         {"pageNum": 1, "pageSize": 100})
    for dev in (result.get("data") or {}).get("list") or []:
        print("deviceId=%s deviceTypeId=%s 名称=%s 型号=%s GUID=%s 在线=%s"
              % (dev.get("id"), dev.get("typeId"), dev.get("deviceName"),
                 dev.get("deviceTypeName"), dev.get("deviceGuid"), dev.get("onlineFlag")))


def fetch_chart(token, start_dt, end_dt, page_size=1000):
    """
    拉取指定时间段的采集数据。

    平台接口：POST /api/data/attribute/device/chartData
    查询参数放 URL，请求体只放分页参数；deviceTypeId 必填。
    """
    query = urllib.parse.urlencode({
        "deviceId": ELITECH_DEVICE_ID,
        "deviceTypeId": ELITECH_DEVICE_TYPE_ID,
        "startTime": int(start_dt.timestamp()),
        "endTime": int(end_dt.timestamp()),
        "showAllProbe": 0,
    })
    result = api_request(token, "POST", "/api/data/attribute/device/chartData?" + query,
                         {"pageSize": page_size})
    if result.get("code") != 0:
        raise SystemExit("平台返回业务错误：%s" % json.dumps(result, ensure_ascii=False)[:400])
    return result.get("data") or {}


def fetch_day_records(token, target_date):
    """
    拉取某一天（00:00-24:00）的全部采集记录。

    按 6 小时分段请求，避免单次返回过大；按 rowKey 去重后按时间正序返回。
    """
    day_start = datetime(target_date.year, target_date.month, target_date.day, 0, 0, 0, tzinfo=TZ_CN)
    records = {}
    for hour in (0, 6, 12, 18):
        start = day_start + timedelta(hours=hour)
        end = min(start + timedelta(hours=6) - timedelta(seconds=1), day_start + timedelta(days=1) - timedelta(seconds=1))
        page = fetch_chart(token, start, end)
        for rec in page.get("data") or []:
            records[rec.get("rowKey")] = rec
    return sorted(records.values(), key=lambda r: r.get("monitorTime") or "")


# ===================================
# 数据解析
# ===================================

def parse_monitor_time(text):
    """把 "2026-11-23 16:31:30(+08:00)" 解析成本地时区的 datetime。"""
    if not text:
        return None
    try:
        return datetime.strptime(text[:19], "%Y-%m-%d %H:%M:%S").replace(tzinfo=TZ_CN)
    except ValueError:
        return None


def extract_point(rec):
    """
    从一条采集记录中提取 (温度, 湿度)。

    温度探头单位是 ℃，湿度探头单位是 %RH；另有光照探头（Lux）需要忽略。
    探头数值可能为 null 或故障，取不到时返回 None。
    """
    temperature = None
    humidity = None
    for param in rec.get("paramDataModelList") or []:
        value = param.get("paramValue")
        if value in (None, "") or param.get("fault"):
            continue
        try:
            number = float(value)
        except (TypeError, ValueError):
            continue
        unit = (param.get("unitCode") or "").strip()
        code = (param.get("paramCode") or "").strip()
        if temperature is None and (unit in TEMP_UNITS or code in TEMP_PARAM_CODES):
            temperature = number
        elif humidity is None and (unit in HUMI_UNITS or code in HUMI_PARAM_CODES):
            humidity = number
    return temperature, humidity


def build_daily_rows(records, target_date, room_name):
    """把一天的原始采集点汇总为一到两行温湿度记录（温度、湿度必须同时存在）。"""
    points = []
    for rec in records:
        when = parse_monitor_time(rec.get("monitorTime"))
        temperature, humidity = extract_point(rec)
        if when is None or temperature is None or humidity is None:
            continue
        points.append((when, temperature, humidity))

    if not points:
        return []

    temperatures = [p[1] for p in points]
    humidities = [p[2] for p in points]
    avg_t = round(sum(temperatures) / len(temperatures), 2)
    avg_h = round(sum(humidities) / len(humidities), 2)
    remark = (
        "精创云自动采集：采样 %d 条（%s - %s），"
        "温度 %.2f~%.2f℃，湿度 %.2f~%.2f%%RH，设备 %s（%s，GUID %s）"
        % (len(points),
           points[0][0].strftime("%H:%M"), points[-1][0].strftime("%H:%M"),
           min(temperatures), max(temperatures), min(humidities), max(humidities),
           ELITECH_DEVICE_NAME, ELITECH_DEVICE_MODEL, ELITECH_DEVICE_GUID)
    )
    return [{
        "room_id": None,
        "record_date": target_date,
        "inspection_area": room_name,
        "temperature": avg_t,
        "humidity": avg_h,
        "recorder": RECORDER,
        "remark": remark,
        "point_count": len(points),
    }]


def build_raw_rows(records, target_date, room_name):
    """按原始采集点逐条生成记录（record_date 全部是当天，时间写在 remark）。"""
    rows = []
    for rec in records:
        when = parse_monitor_time(rec.get("monitorTime"))
        temperature, humidity = extract_point(rec)
        if when is None or temperature is None or humidity is None:
            continue
        rows.append({
            "room_id": None,
            "record_date": target_date,
            "inspection_area": room_name,
            "temperature": round(temperature, 2),
            "humidity": round(humidity, 2),
            "recorder": RECORDER,
            "remark": "精创云自动采集：%s 采集，设备 %s（%s，GUID %s）"
                      % (when.strftime("%Y-%m-%d %H:%M:%S"),
                         ELITECH_DEVICE_NAME, ELITECH_DEVICE_MODEL, ELITECH_DEVICE_GUID),
            "point_count": 1,
        })
    return rows


# ===================================
# 数据库读写
# ===================================

def connect_db():
    """连接 ERP 数据库，缺少 pymysql 时给出安装提示。"""
    try:
        import pymysql
    except ImportError:
        raise SystemExit("缺少依赖 pymysql，请先执行：pip install pymysql")
    return pymysql.connect(
        host=DB_HOST, port=DB_PORT, user=DB_USER, password=DB_PASSWORD,
        database=DB_NAME, charset="utf8mb4", autocommit=False,
    )


def resolve_room(conn, room_code):
    """按房间编码查询房间，返回 (room_id, room_name)。"""
    with conn.cursor() as cur:
        cur.execute(
            "SELECT room_id, room_name FROM room_info WHERE room_code = %s AND is_deleted = 0 LIMIT 1",
            (room_code,),
        )
        row = cur.fetchone()
    if not row:
        raise SystemExit("未找到房间编码 %s，请先在系统中维护该房间" % room_code)
    return row[0], row[1]


def upsert_rows(conn, rows, match_remark=False):
    """
    查重后更新或插入，返回 (新增数, 更新数)。

    按天汇总模式（match_remark=False）：房间 + 日期 + 记录人唯一。
    原始逐条模式（match_remark=True）：再加上备注中的采集时间，避免一天多条互相覆盖。
    """
    inserted = updated = 0
    with conn.cursor() as cur:
        for row in rows:
            where = ("room_id = %s AND record_date = %s AND recorder = %s AND is_deleted = 0")
            params = [row["room_id"], row["record_date"], row["recorder"]]
            if match_remark:
                where += " AND remark = %s"
                params.append(row["remark"])
            cur.execute(
                "SELECT id FROM temperature_humidity_record WHERE " + where + " ORDER BY id DESC LIMIT 1",
                tuple(params),
            )
            exist = cur.fetchone()
            if exist:
                cur.execute(
                    "UPDATE temperature_humidity_record SET inspection_area = %s, temperature = %s, "
                    "humidity = %s, remark = %s, updated_time = NOW(), is_deleted = 0 WHERE id = %s",
                    (row["inspection_area"], row["temperature"], row["humidity"], row["remark"], exist[0]),
                )
                updated += 1
            else:
                cur.execute(
                    "INSERT INTO temperature_humidity_record "
                    "(room_id, record_date, inspection_area, temperature, humidity, recorder, remark, "
                    "created_time, is_deleted) VALUES (%s, %s, %s, %s, %s, %s, %s, NOW(), 0)",
                    (row["room_id"], row["record_date"], row["inspection_area"],
                     row["temperature"], row["humidity"], row["recorder"], row["remark"]),
                )
                inserted += 1
    return inserted, updated


# ===================================
# 命令行入口
# ===================================

def parse_args(argv):
    parser = argparse.ArgumentParser(description="同步精创云温湿度数据到 ERP 房间温湿度记录表")
    parser.add_argument("--date", default="yesterday",
                        help="目标日期：yyyy-MM-dd、yesterday（默认）或 today")
    parser.add_argument("--commit", action="store_true",
                        help="真正写入数据库；不加此参数时只预览")
    parser.add_argument("--mode", choices=["daily", "raw"], default="daily",
                        help="daily=按天汇总一行（默认）；raw=逐条写入原始采集点")
    parser.add_argument("--dump", metavar="FILE",
                        help="把当天拉取的原始数据保存为 JSON，便于排查")
    parser.add_argument("--list-devices", action="store_true", help="列出账号下的设备后退出")
    return parser.parse_args(argv)


def resolve_target_date(text):
    today = datetime.now(TZ_CN).date()
    if text == "today":
        return today
    if text == "yesterday":
        return today - timedelta(days=1)
    try:
        return date.fromisoformat(text)
    except ValueError:
        raise SystemExit("日期格式不正确：%s（应为 yyyy-MM-dd）" % text)


def main(argv=None):
    args = parse_args(argv)
    token = load_token()

    if args.list_devices:
        list_devices(token)
        return 0

    expire = token_expire_hint(token)
    if expire:
        days_left = (expire - datetime.now(TZ_CN)).days
        print("令牌到期时间：%s（剩余 %d 天）" % (expire.strftime("%Y-%m-%d %H:%M"), days_left))
        if days_left < 2:
            print("警告：令牌即将过期，请及时更新 token.txt", file=sys.stderr)

    target_date = resolve_target_date(args.date)
    records = fetch_day_records(token, target_date)
    print("平台返回原始采集记录 %d 条（%s）" % (len(records), target_date))

    if args.dump:
        with open(args.dump, "w", encoding="utf-8") as f:
            json.dump(records, f, ensure_ascii=False, indent=1)
        print("原始数据已保存到 %s" % args.dump)

    conn = connect_db()
    try:
        room_id, room_name = resolve_room(conn, ROOM_CODE)
        print("目标房间：%s（room_id=%s）" % (room_name, room_id))

        if args.mode == "raw":
            rows = build_raw_rows(records, target_date, room_name)
        else:
            rows = build_daily_rows(records, target_date, room_name)

        if not rows:
            print("当天没有可写入的温湿度数据（设备离线或未产生记录）")
            return 0

        for row in rows:
            row["room_id"] = room_id

        for index, row in enumerate(rows):
            if index >= 3 and len(rows) > 5:
                if index == len(rows) - 1:
                    print("  ...（其余 %d 条省略）" % (len(rows) - 4))
                else:
                    continue
            print("准备写入：日期=%s 温度=%s 湿度=%s 采样=%s"
                  % (row["record_date"], row["temperature"], row["humidity"], row["point_count"]))
            print("  备注：%s" % row["remark"])

        if not args.commit:
            print("\n当前为预览模式，未写库。确认无误后加 --commit 参数执行写入。")
            return 0

        inserted, updated = upsert_rows(conn, rows, match_remark=(args.mode == "raw"))
        conn.commit()
        print("写入完成：新增 %d 条，更新 %d 条" % (inserted, updated))
        return 0
    finally:
        conn.close()


if __name__ == "__main__":
    sys.exit(main())
