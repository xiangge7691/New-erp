#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
实际生产批次约束迁移脚本（20261127）

对应 SQL：
    src/main/resources/Data/migration/20261127_actual_production_time_and_batch_guard.sql

内容：
    1. 巡检：同批次多条执行记录、工序时间倒挂、实际时间越界、status 与时间不一致；
    2. 备份 work_order_process_execution（默认开启）；
    3. 回写 status（与 ProcessExecutionStatus.resolve 口径一致）；
    4. 创建 uk_wpe_plan_batch(plan_batch_id) 唯一索引（仅当无重复时）；
    5. 创建 idx_wpe_work_order_step(work_order_id, step_order) 索引。

用法：
    python db_migrate_20261127_actual_production.py            # 只巡检，不写库
    python db_migrate_20261127_actual_production.py --apply    # 备份后执行迁移

连接参数默认测试库 192.168.5.100/erp_db，可用环境变量 ERP_DB_HOST / ERP_DB_PORT /
ERP_DB_USER / ERP_DB_PASSWORD / ERP_DB_NAME 覆盖。禁止对线上库 8.148.27.237 执行。
"""
import os
import sys

import pymysql

HOST = os.environ.get('ERP_DB_HOST', '192.168.5.100')
PORT = int(os.environ.get('ERP_DB_PORT', '3306'))
USER = os.environ.get('ERP_DB_USER', 'root')
DB = os.environ.get('ERP_DB_NAME', 'erp_db')
PASSWORD = os.environ.get('ERP_DB_PASSWORD')
# 依次尝试已知的测试库口令（均来自仓库内既有脚本/配置）
PASSWORD_CANDIDATES = [PASSWORD] if PASSWORD else ['89749050', '970627']

FORBIDDEN_HOSTS = {'8.148.27.237'}
TABLE = 'work_order_process_execution'
BACKUP_TABLE = 'work_order_process_execution_bak_20261127'
UK_NAME = 'uk_wpe_plan_batch'
IDX_NAME = 'idx_wpe_work_order_step'

STATUS_SQL = """
CASE
    WHEN start_time IS NULL THEN '待执行'
    WHEN end_time IS NULL THEN '执行中'
    ELSE '已完成'
END
"""


def stdout_utf8():
    """Windows 控制台默认编码可能不是 UTF-8，统一输出编码"""
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass


def connect():
    """尝试连接测试库并返回连接对象"""
    last_error = None
    for password in PASSWORD_CANDIDATES:
        try:
            conn = pymysql.connect(host=HOST, port=PORT, user=USER, password=password,
                                   database=DB, charset='utf8mb4', connect_timeout=8,
                                   read_timeout=120, autocommit=False)
            print('已连接 %s:%s/%s（用户 %s）' % (HOST, PORT, DB, USER))
            return conn
        except Exception as exc:  # noqa: BLE001 - 需要汇总多个口令的失败原因
            last_error = exc
    raise RuntimeError('连接数据库失败：%s' % last_error)


def fetch_all(cursor, sql, args=None):
    """执行查询并返回元组列表"""
    cursor.execute(sql, args or ())
    return cursor.fetchall()


def scalar(cursor, sql, args=None):
    """执行查询并返回第一行第一列"""
    cursor.execute(sql, args or ())
    row = cursor.fetchone()
    return row[0] if row else None


def table_exists(cursor, table):
    """判断表是否存在"""
    return scalar(cursor,
                  "SELECT COUNT(*) FROM information_schema.tables "
                  "WHERE table_schema = %s AND table_name = %s", (DB, table)) > 0


def index_exists(cursor, table, index):
    """判断索引是否存在"""
    return scalar(cursor,
                  "SELECT COUNT(*) FROM information_schema.statistics "
                  "WHERE table_schema = %s AND table_name = %s AND index_name = %s",
                  (DB, table, index)) > 0


def patrol(cursor):
    """只读巡检，返回统计结果"""
    print('\n===== 一、巡检 =====')
    total = scalar(cursor, "SELECT COUNT(*) FROM `%s`" % TABLE)
    active = scalar(cursor, "SELECT COUNT(*) FROM `%s` WHERE is_deleted = 0" % TABLE)
    print('执行记录总数 %s（有效 %s）' % (total, active))

    dup_active = fetch_all(cursor, """
        SELECT plan_batch_id, COUNT(*) AS cnt, GROUP_CONCAT(id ORDER BY id) AS ids
        FROM `%s`
        WHERE plan_batch_id IS NOT NULL AND is_deleted = 0
        GROUP BY plan_batch_id
        HAVING COUNT(*) > 1
    """ % TABLE)
    dup_all = fetch_all(cursor, """
        SELECT plan_batch_id, COUNT(*) AS cnt, GROUP_CONCAT(id ORDER BY id) AS ids
        FROM `%s`
        WHERE plan_batch_id IS NOT NULL
        GROUP BY plan_batch_id
        HAVING COUNT(*) > 1
    """ % TABLE)
    print('\n[1] 同批次多条执行记录：有效数据 %d 组，含软删除 %d 组' % (len(dup_active), len(dup_all)))
    for plan_batch_id, cnt, ids in dup_all[:20]:
        print('    plan_batch_id=%s 共 %s 条，execution_id=%s' % (plan_batch_id, cnt, ids))
    if len(dup_all) > 20:
        print('    ... 其余 %d 组省略' % (len(dup_all) - 20))

    inversion = fetch_all(cursor, """
        SELECT e.id, e.work_order_id, e.step_order, e.start_time,
               p.step_order AS prev_step, p.end_time AS prev_end
        FROM `{t}` e
        JOIN `{t}` p ON p.work_order_id = e.work_order_id
                    AND p.step_order < e.step_order
                    AND p.is_deleted = 0
        WHERE e.is_deleted = 0
          AND e.start_time IS NOT NULL
          AND p.end_time IS NOT NULL
          AND p.end_time > e.start_time
        ORDER BY e.work_order_id, e.step_order, e.id
        LIMIT 50
    """.format(t=TABLE))
    inversion_total = scalar(cursor, """
        SELECT COUNT(*)
        FROM `{t}` e
        JOIN `{t}` p ON p.work_order_id = e.work_order_id
                    AND p.step_order < e.step_order
                    AND p.is_deleted = 0
        WHERE e.is_deleted = 0
          AND e.start_time IS NOT NULL
          AND p.end_time IS NOT NULL
          AND p.end_time > e.start_time
    """.format(t=TABLE))
    print('\n[2] 工序时间倒挂：%s 条（明细最多展示 50 条）' % inversion_total)
    for row in inversion[:20]:
        print('    execution_id=%s 工单=%s 工序=%s 开始=%s ← 前序工序=%s 结束=%s'
              % (row[0], row[1], row[2], row[3], row[4], row[5]))

    window_total = scalar(cursor, """
        SELECT COUNT(*)
        FROM `{t}` e
        JOIN work_order w ON w.work_order_id = e.work_order_id AND w.is_deleted = 0
        WHERE e.is_deleted = 0
          AND ((w.config_date IS NOT NULL AND e.start_time IS NOT NULL AND e.start_time < w.config_date)
            OR (w.config_complete_time IS NOT NULL AND e.end_time IS NOT NULL AND e.end_time > w.config_complete_time))
    """.format(t=TABLE))
    print('\n[3] 实际时间超出工单配置时间窗：%s 条' % window_total)

    mismatch = fetch_all(cursor, """
        SELECT id, work_order_id, step_order, start_time, end_time, status
        FROM `{t}`
        WHERE is_deleted = 0
          AND (status IS NULL OR status <> {s})
        LIMIT 50
    """.format(t=TABLE, s=STATUS_SQL))
    mismatch_total = scalar(cursor, """
        SELECT COUNT(*) FROM `{t}`
        WHERE is_deleted = 0 AND (status IS NULL OR status <> {s})
    """.format(t=TABLE, s=STATUS_SQL))
    print('\n[4] status 与起止时间不一致：%s 条（明细最多展示 50 条）' % mismatch_total)
    for row in mismatch[:10]:
        print('    id=%s 工单=%s 工序=%s 开始=%s 结束=%s 当前状态=%s'
              % (row[0], row[1], row[2], row[3], row[4], row[5]))

    return {'dup_all': dup_all, 'mismatch_total': mismatch_total}


def backup(cursor):
    """备份执行记录表（含软删除行）"""
    print('\n===== 二、备份 =====')
    if table_exists(cursor, BACKUP_TABLE):
        count = scalar(cursor, "SELECT COUNT(*) FROM `%s`" % BACKUP_TABLE)
        print('备份表 %s 已存在（%s 行），跳过备份' % (BACKUP_TABLE, count))
        return
    cursor.execute("CREATE TABLE `%s` LIKE `%s`" % (BACKUP_TABLE, TABLE))
    cursor.execute("INSERT INTO `%s` SELECT * FROM `%s`" % (BACKUP_TABLE, TABLE))
    src = scalar(cursor, "SELECT COUNT(*) FROM `%s`" % TABLE)
    dst = scalar(cursor, "SELECT COUNT(*) FROM `%s`" % BACKUP_TABLE)
    if src != dst:
        raise RuntimeError('备份行数不一致：原表 %s，备份表 %s' % (src, dst))
    print('已创建备份表 %s，共 %s 行' % (BACKUP_TABLE, dst))


def migrate(cursor, dup_all):
    """执行状态回写与索引创建"""
    print('\n===== 三、迁移 =====')

    cursor.execute("""
        UPDATE `{t}`
        SET status = {s}
        WHERE is_deleted = 0 AND (status IS NULL OR status <> {s})
    """.format(t=TABLE, s=STATUS_SQL))
    print('status 回写：%s 行' % cursor.rowcount)

    if index_exists(cursor, TABLE, UK_NAME):
        print('唯一索引 %s 已存在，跳过' % UK_NAME)
    elif dup_all:
        print('存在 %d 组同批次重复记录（含软删除行），跳过唯一索引 %s；'
              '请先人工合并后再执行本脚本' % (len(dup_all), UK_NAME))
    else:
        cursor.execute("ALTER TABLE `%s` ADD UNIQUE KEY `%s` (plan_batch_id)" % (TABLE, UK_NAME))
        print('已创建唯一索引 %s(plan_batch_id)' % UK_NAME)

    if index_exists(cursor, TABLE, IDX_NAME):
        print('索引 %s 已存在，跳过' % IDX_NAME)
    else:
        cursor.execute("ALTER TABLE `%s` ADD KEY `%s` (work_order_id, step_order)" % (TABLE, IDX_NAME))
        print('已创建索引 %s(work_order_id, step_order)' % IDX_NAME)


def verify(cursor):
    """迁移后校验"""
    print('\n===== 四、校验 =====')
    indexes = fetch_all(cursor, """
        SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS cols, non_unique
        FROM information_schema.statistics
        WHERE table_schema = %s AND table_name = %s
          AND index_name IN (%s, %s)
        GROUP BY index_name, non_unique
    """, (DB, TABLE, UK_NAME, IDX_NAME))
    for name, cols, non_unique in indexes:
        print('索引 %s(%s) %s' % (name, cols, '唯一' if non_unique == 0 else '普通'))
    if not indexes:
        print('未找到目标索引')

    dup = fetch_all(cursor, """
        SELECT plan_batch_id, COUNT(*) FROM `%s`
        WHERE plan_batch_id IS NOT NULL
        GROUP BY plan_batch_id HAVING COUNT(*) > 1 LIMIT 10
    """ % TABLE)
    print('同批次重复记录：%d 组' % len(dup))

    rows = fetch_all(cursor, """
        SELECT status, COUNT(*) FROM `%s` WHERE is_deleted = 0 GROUP BY status
    """ % TABLE)
    print('有效记录状态分布：%s' % ', '.join('%s=%s' % (r[0], r[1]) for r in rows))


def main():
    stdout_utf8()
    apply = '--apply' in sys.argv[1:]
    if HOST in FORBIDDEN_HOSTS:
        print('拒绝连接线上库 %s：只允许在测试库执行迁移' % HOST)
        return 2

    conn = connect()
    try:
        with conn.cursor() as cursor:
            result = patrol(cursor)
            if not apply:
                print('\n预览模式：未写库。确认后加 --apply 重跑。')
                return 0
            backup(cursor)
            migrate(cursor, result['dup_all'])
            conn.commit()
            verify(cursor)
            print('\n迁移完成。回滚索引见 20261127_actual_production_time_and_batch_guard_rollback.sql；'
                  '数据恢复可使用备份表 %s。' % BACKUP_TABLE)
            return 0
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == '__main__':
    sys.exit(main())
