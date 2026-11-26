# 精创云温湿度同步脚本

从精创云平台（`new.e-elitech.cn`）读取温湿度检测仪的采集数据，按天汇总后写入 ERP 的
`temperature_humidity_record` 表，默认目标房间为「恒天2004办公室」（房间编码 `HT2004`）。

脚本独立运行，不修改后端代码，可由 Windows 计划任务每天定时调用。

## 文件说明

| 文件 | 说明 |
|---|---|
| `sync_room_th.py` | 主脚本，单文件，只依赖 Python 和 pymysql |
| `run_daily.bat` | 计划任务调用的批处理，默认同步昨天的数据并追加日志 |
| `token.txt` | 平台访问令牌（**不要提交到仓库**，已在 `.gitignore` 中排除） |
| `sync.log` | 运行日志，由批处理自动生成 |

## 平台接口说明

- 登录后的令牌存在浏览器 localStorage 的 `Access-Token`，请求头为 `Authorization: bearer <token>`，有效期 7 天。
- 设备列表：`POST /api/device/Device/get/deviceList`
- 历史数据：`POST /api/data/attribute/device/chartData?deviceId=&deviceTypeId=&startTime=&endTime=&showAllProbe=0`
  - 时间参数是秒级时间戳，按东八区解释。
  - 返回按 5 分钟一条，每条包含多个探头：探头1 光照（Lux）、探头2 温度（℃）、探头3 湿度（%RH）。
- 当前设备：`deviceId=1708729`、`deviceTypeId=37159`（温湿度检测仪 GSP-8G-C，GUID `90265662327170175293`）。

## 准备工作

```bash
pip install pymysql
```

更新令牌（令牌 7 天过期，接口返回 401 时必须更新）：

1. 浏览器登录 `https://new.e-elitech.cn`
2. F12 → Application → Local Storage → 选中站点 → 复制 `Access-Token` 的值
3. 覆盖写入本目录的 `token.txt`（只放令牌本身，不要加引号或 `bearer` 前缀）

## 使用

```bash
# 预览昨天的数据，不写库（默认行为）
python sync_room_th.py

# 预览指定日期
python sync_room_th.py --date 2026-11-23

# 写入昨天数据
python sync_room_th.py --commit

# 写入今天已经产生的数据（补数用）
python sync_room_th.py --date today --commit

# 按原始采集点逐条写入（一天多行，record_date 相同）
python sync_room_th.py --date 2026-11-23 --mode raw --commit

# 列出账号下的设备，核对 deviceId / deviceTypeId
python sync_room_th.py --list-devices

# 保存平台原始数据，便于排查字段问题
python sync_room_th.py --date 2026-11-23 --dump raw.json
```

## 配置项（环境变量）

不设置时使用括号内的默认值。

| 变量 | 说明 |
|---|---|
| `ELITECH_TOKEN` / `ELITECH_TOKEN_FILE` | 访问令牌，或令牌文件路径（`./token.txt`） |
| `ELITECH_BASE` | 平台地址（`https://new.e-elitech.cn`） |
| `ELITECH_DEVICE_ID` | 设备 ID（`1708729`） |
| `ELITECH_DEVICE_TYPE_ID` | 设备类型 ID（`37159`） |
| `ELITECH_DEVICE_NAME` / `ELITECH_DEVICE_MODEL` / `ELITECH_DEVICE_GUID` | 写入备注的设备信息 |
| `ERP_ROOM_CODE` | 目标房间编码（`HT2004`） |
| `ERP_RECORDER` | 记录人字段（`精创云自动采集`），也用于查重 |
| `ERP_DB_HOST` / `ERP_DB_PORT` / `ERP_DB_USER` / `ERP_DB_PASSWORD` / `ERP_DB_NAME` | 数据库连接（默认测试库 `192.168.5.100/erp_db`） |

## 写入语义

- **按天汇总（默认，`--mode daily`）**：同房间同一天只保留一条，温度/湿度取当天所有采集点的平均值（保留两位小数），
  极值、样本数、采集起止时间和设备信息写入 `remark`。重复执行会更新同一条记录。
- **原始逐条（`--mode raw`）**：每个采集点写一行，`record_date` 相同、时间写在 `remark`
  里，按备注查重避免重复插入。前端列表会看到同一天多行，除非业务明确要求，建议保持默认汇总模式。

## 计划任务

`run_daily.bat` 默认同步「昨天」的完整数据。注册 Windows 计划任务：

1. 打开「任务计划程序」→ 创建任务
2. 触发器：每天 07:10（可在触发器里调整）
3. 操作：启动程序，程序填 `run_daily.bat` 的完整路径
4. 勾选「不管用户是否登录都要运行」时，注意 `token.txt` 和 Python 的路径要使用绝对路径

也可以手动执行：

```bash
run_daily.bat
```

日志追加在 `sync.log`。

## 常见问题

- **401 / 令牌失效**：重新按上面的步骤更新 `token.txt`。
- **当天没有数据**：设备离线或当天尚未产生记录，脚本会正常退出，不写库。
- **提示未找到房间编码**：确认 `room_info` 中存在 `room_code = HT2004` 且未删除。
- **写入生产库**：通过环境变量把 `ERP_DB_*` 指向生产库即可，脚本默认连接测试库，不会自动切到生产。
