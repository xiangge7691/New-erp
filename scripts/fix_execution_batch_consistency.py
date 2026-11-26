#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
修复排产批次状态与执行记录批次归属的一致性。

背景：
    批次状态由关联的实际执行记录推导（无执行=计划中、已开始未完工=执行中、全部完成=已完工）。
    历史版本在更新执行记录时会把它换到别的批次，原批次状态没有重算，导致
    「批次没有任何执行记录却显示已完工」。本脚本按当前执行记录重算全部批次状态。

用法：
    python fix_execution_batch_consistency.py            # 只预览差异，不写库
    python fix_execution_batch_consistency.py --apply    # 实际写回批次状态

连接参数默认取测试库 192.168.5.100/erp_db，可用环境变量 ERP_DB_HOST / ERP_DB_PORT /
ERP_DB_USER / ERP_DB_PASSWORD / ERP_DB_NAME 覆盖。只更新 work_order_process_plan_batch.status。
"""
import os
import sys

import pymysql

HOST = os.environ.get('ERP_DB_HOST', '192.168.5.100')
PORT = int(os.environ.get('ERP_DB_PORT', '3306'))
USER = os.environ.get('ERP_DB_USER', 'root')
PASSWORD = os.environ.get('ERP_DB_PASSWORD', '89749050')
DB = os.environ.get('ERP_DB_NAME', 'erp_db')

PLANNED = '计划中'
IN_PROGRESS = '执行中'
COMPLETED = '已完工'


def fetch(conn):
    """读取批次与执行记录（均排除软删除行）"""
    with conn.cursor() as cur:
        cur.execute("""
            SELECT b.id, b.batch_no, b.status, p.work_order_id, p.step_order
            FROM work_order_process_plan_batch b
            JOIN work_order_process_plan p ON p.id = b.plan_id AND p.is_deleted = 0
            WHERE b.is_deleted = 0
        """)
        batches = cur.fetchall()
        cur.execute("""
            SELECT id, work_order_id, plan_batch_id, batch_no, step_order,
                   start_time, end_time, status
            FROM work_order_process_execution
            WHERE is_deleted = 0
        """)
        executions = cur.fetchall()
    return batches, executions


def belongs(batch, execution):
    """执行记录是否归属该批次：优先批次ID，历史行未写批次ID时按批次序号兜底"""
    if execution[2] is not None:
        return execution[2] == batch[0]
    return (execution[3] is not None and batch[1] is not None and execution[3] == batch[1]
            and execution[1] == batch[3] and execution[4] == batch[4])


def is_completed(execution):
    """已完成：状态为已完成，或已填写实际结束时间"""
    return execution[7] == '已完成' or execution[6] is not None


def resolve_status(rows):
    """按关联执行记录推导批次状态"""
    if not rows:
        return PLANNED
    if all(is_completed(row) for row in rows):
        return COMPLETED
    if any(row[5] is not None for row in rows):
        return IN_PROGRESS
    return PLANNED


def main():
    apply = '--apply' in sys.argv[1:]
    conn = pymysql.connect(host=HOST, port=PORT, user=USER, password=PASSWORD, database=DB,
                           charset='utf8mb4', connect_timeout=10, read_timeout=120)
    try:
        batches, executions = fetch(conn)
    finally:
        conn.close()

    changes = []
    for batch in batches:
        matched = [e for e in executions if belongs(batch, e)]
        target = resolve_status(matched)
        if (batch[2] or '') != target:
            changes.append((batch[0], batch[3], batch[4], batch[2], target, len(matched)))

    print('批次总数: %d，需要修正: %d' % (len(batches), len(changes)))
    for batch_id, work_order_id, step_order, old, new, count in changes[:50]:
        print('  批次 %s（工单 %s / 工序 %s，执行记录 %d 条）: %s -> %s'
              % (batch_id, work_order_id, step_order, count, old or '空', new))
    if len(changes) > 50:
        print('  ... 其余 %d 条省略' % (len(changes) - 50))

    if not apply:
        print('预览模式：未写库。确认后加 --apply 重跑。')
        return
    if not changes:
        print('无需写库。')
        return
    conn = pymysql.connect(host=HOST, port=PORT, user=USER, password=PASSWORD, database=DB,
                           charset='utf8mb4', connect_timeout=10, read_timeout=120)
    try:
        with conn.cursor() as cur:
            for batch_id, _, _, _, new, _ in changes:
                cur.execute("UPDATE work_order_process_plan_batch SET status = %s WHERE id = %s",
                            (new, batch_id))
        conn.commit()
    finally:
        conn.close()
    print('已写回 %d 条批次状态。' % len(changes))


if __name__ == '__main__':
    main()
