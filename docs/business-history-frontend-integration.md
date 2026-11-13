# 生产任务 / 制剂详情变更历史 前端实现参考

> 对应原型：`_analysis/prototypes/业务变更历史原型.html`
> 后端接口：`GET /api/business-history/{bizType}/{bizId}`
> 适用页面：生产任务（工单）详情、制剂详情

---

## 1. 页面数据来源

详情页需要两类数据，建议并行请求：

| 数据 | 接口 | 用途 |
|------|------|------|
| 业务详情 | `GET /api/work-orders/{id}` 或 `GET /api/preparation/{id}/details` | 左侧基本信息、处方明细 |
| 变更历史 | `GET /api/business-history/{bizType}/{bizId}` | 右侧时间线、字段角标、处方行角标 |

如果页面只需要变更历史，可以只请求第二个接口；`BusinessHistoryDto.title` 会返回业务对象展示名称。

```ts
const [detail, history] = await Promise.all([
  fetchDetail(bizType, bizId),
  fetchHistory(bizType, bizId, { pageIndex: 0, pageSize: 20 })
]);
```

> 生产任务详情使用 `GET /api/work-orders/{id}`。
> 制剂详情使用 `GET /api/preparation/{id}/details`，返回主表 + `formulas` + `processTemplates` + `documents`。

---

## 2. 聚合接口

### 2.1 请求

```http
GET /api/business-history/{bizType}/{bizId}?pageIndex=0&pageSize=20
```

| 参数 | 必填 | 说明 |
|------|------|------|
| `bizType` | 是 | `WORK_ORDER`（生产任务）/ `PREPARATION`（制剂） |
| `bizId` | 是 | 业务主键 |
| `pageIndex` | 否 | 按“操作事件”分页，从 0 开始，默认 0 |
| `pageSize` | 否 | 每页事件数，默认 20，最大 200 |
| `field` | 否 | 只看某个字段，传数据库列名，如 `spec`、`batch_qty`、`dosage` |
| `source` | 否 | `PRIMARY` 主表变更 / `RELATED` 关联明细变更 |
| `startTime` | 否 | 支持 `yyyy-MM-dd` 或 `yyyy-MM-dd HH:mm:ss` |
| `endTime` | 否 | 同上 |

示例：

```http
# 生产任务：只看批量变更
GET /api/business-history/WORK_ORDER/880?pageIndex=0&pageSize=20&field=batch_qty

# 制剂：只看处方明细里的处方量变更
GET /api/business-history/PREPARATION/42?pageIndex=0&pageSize=20&field=dosage&source=RELATED
```

### 2.2 返回结构

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "bizType": "PREPARATION",
    "bizId": "42",
    "title": "Z04-W37-009 · 理伤续断丸",
    "fieldStats": [
      { "field": "spec", "label": "规格", "count": 1 },
      { "field": "dosage", "label": "处方量", "count": 2 },
      { "field": "batch_qty", "label": "批量", "count": 1 }
    ],
    "recordStats": [
      {
        "tableName": "preparation",
        "recordId": "42",
        "label": "42",
        "count": 2
      },
      {
        "tableName": "preparation_formula",
        "recordId": "32046",
        "label": "当归（检）",
        "count": 1
      }
    ],
    "events": [
      {
        "operationLogId": 9105,
        "traceId": "tr-prep-05",
        "operationBusinessType": "PREPARATION_FORMULA",
        "module": "制剂配方",
        "operationType": "UPDATE",
        "operationDesc": "批量保存制剂配方",
        "userName": "系统管理员",
        "userAccount": "root",
        "clientIp": "218.94.12.34",
        "ipLocation": "江苏省南京市",
        "macAddress": "3C-52-82-1A-4F-09",
        "createdTime": "2026-11-10 10:02:11",
        "changes": [
          {
            "dataChangeId": 501,
            "tableName": "preparation_formula",
            "businessId": "32046",
            "recordLabel": "当归（检）",
            "changeType": "UPDATE",
            "changeSource": "RELATED",
            "field": "dosage",
            "label": "处方量",
            "before": "0.070",
            "after": "0.074"
          }
        ]
      }
    ],
    "totalCount": 6,
    "pageIndex": 0,
    "pageSize": 20,
    "truncated": false
  }
}
```

### 2.3 字段说明

| 字段 | 说明 |
|------|------|
| `fieldStats` | 字段级变更次数，用于详情页字段旁的数字角标；不受 `field/source` 筛选影响 |
| `recordStats` | 记录级变更次数，用于处方明细行角标；`tableName=preparation_formula` 时 `recordId` 是处方行 ID |
| `events` | 按操作事件归组，时间倒序；一次操作的多张表、多个字段变更放在同一个事件里 |
| `changes` | 扁平化的字段变更，一个字段一条；前端直接渲染“字段 / 改前 / 改后” |
| `changeSource` | `PRIMARY` 主表、`RELATED` 关联明细，用于区分“主表变更 / 处方明细” |
| `recordLabel` | 关联记录名称，如处方行的物料名称；可能为空，为空时回退展示 `businessId` |
| `totalCount` | 筛选后的事件总数，不是字段变更条数 |
| `truncated` | 变更日志加载达到单次上限时为 `true`，前端可提示“历史记录较多，仅展示最近部分” |

注意：

- `before` / `after` 是 `Object`，可能是字符串、数字或 `null`；`null` 展示为“空”。
- 新增记录的事件可能 `changes` 为空（INSERT 没有字段差异），前端显示“创建记录，无字段差异”。
- `pageIndex` / `pageSize` 按事件分页，不是按字段变更行分页。

---

## 3. 页面渲染映射

### 3.1 左侧详情

| 原型元素 | 数据来源 |
|----------|----------|
| 生产任务基本信息 | `GET /api/work-orders/{id}` |
| 制剂基本信息 | `GET /api/preparation/{id}/details` |
| 制剂处方信息 | 详情接口的 `formulas` |
| 字段旁数字角标 | `fieldStats` 中 `count > 0` 的字段 |
| 处方行旁数字角标 | `recordStats` 中 `tableName=preparation_formula` 的记录 |

字段角标建议：

```ts
const fieldCountMap = new Map(
  history.fieldStats.map(item => [item.field, item.count])
);
```

数据库列名与页面字段的对应关系按现有详情页字段名即可，例如：

| 页面字段 | `field` |
|----------|---------|
| 规格 | `spec` |
| 批量 | `batch_qty` |
| 预计成本价 | `estimated_cost_price` |
| 销售单价 | `sales_price` |
| 功能主治 | `function_main` |
| 制法 | `method` |
| 批号 | `batch_number` |
| 当前状态 | `current_status` |
| 处方量 | `dosage` |

### 3.2 右侧时间线

每个 `event` 渲染为一张卡片：

- 标题：`operationDesc`，为空时根据 `operationType` 回退“新增记录 / 修改记录 / 删除记录”；
- 标签：`operationType` 映射新增 / 修改 / 删除 / 审批 / 系统；
- 时间：`createdTime`；
- 操作人：`userName`（`userAccount`）；
- 终端：`clientIp`、`macAddress`；
- 字段差异：遍历 `changes`，展示 `label`（`field`）、`before`、`after`；
- 页脚：`traceId` + “操作日志 #operationLogId”。

`operationLogId` 可点击跳转：

```http
GET /api/system/operation-log/{id}
```

### 3.3 筛选交互

| 用户操作 | 请求变化 |
|----------|----------|
| 点击字段角标（如“规格 1”） | `field=spec`，重新请求 |
| 选择“全部字段” | 移除 `field` |
| 点击处方行角标 | `field=dosage&source=RELATED` |
| 来源选择“主表变更” | `source=PRIMARY` |
| 来源选择“处方明细” | `source=RELATED` |
| 清空筛选 | 移除 `field`、`source`，回到第 0 页 |
| 翻页 | 修改 `pageIndex`，保留当前 `field/source/time` |

字段下拉选项直接来自 `fieldStats`，不需要额外接口：

```ts
const fieldOptions = history.fieldStats.map(item => ({
  label: `${item.label}（${item.count}）`,
  value: item.field
}));
```

### 3.4 长文本

`before` / `after` 超过约 70 字符时，列表里截断展示，点击“完整”打开左右对照弹窗。

如果需要整行前后快照（`beforeJson` / `afterJson`），调用：

```http
GET /api/system/data-change-log/{dataChangeId}
```

字段差异接口返回的是变更行，不会返回整行快照，避免列表加载大字段。

---

## 4. TypeScript 类型参考

```ts
export type BusinessHistoryBizType = 'WORK_ORDER' | 'PREPARATION';
export type BusinessHistorySource = 'PRIMARY' | 'RELATED';

export interface BusinessHistoryFieldStat {
  field: string;
  label: string;
  count: number;
}

export interface BusinessHistoryRecordStat {
  tableName: string;
  recordId: string;
  label: string;
  count: number;
}

export interface BusinessHistoryChange {
  dataChangeId: number;
  tableName: string;
  businessId: string;
  recordLabel: string | null;
  changeType: 'INSERT' | 'UPDATE' | 'DELETE';
  changeSource: BusinessHistorySource;
  field: string;
  label: string;
  before: unknown;
  after: unknown;
}

export interface BusinessHistoryEvent {
  operationLogId: number | null;
  traceId: string | null;
  operationBusinessType: string | null;
  module: string | null;
  operationType: string;
  operationDesc: string | null;
  userName: string | null;
  userAccount: string | null;
  clientIp: string | null;
  ipLocation: string | null;
  macAddress: string | null;
  createdTime: string;
  changes: BusinessHistoryChange[];
}

export interface BusinessHistoryResult {
  bizType: BusinessHistoryBizType;
  bizId: string;
  title: string;
  fieldStats: BusinessHistoryFieldStat[];
  recordStats: BusinessHistoryRecordStat[];
  events: BusinessHistoryEvent[];
  totalCount: number;
  pageIndex: number;
  pageSize: number;
  truncated: boolean;
}
```

---

## 5. 请求封装示例

```ts
interface HistoryQuery {
  pageIndex?: number;
  pageSize?: number;
  field?: string;
  source?: BusinessHistorySource;
  startTime?: string;
  endTime?: string;
}

async function getBusinessHistory(
  bizType: BusinessHistoryBizType,
  bizId: string | number,
  query: HistoryQuery = {}
): Promise<BusinessHistoryResult> {
  const params = new URLSearchParams();
  params.set('pageIndex', String(query.pageIndex ?? 0));
  params.set('pageSize', String(query.pageSize ?? 20));
  if (query.field) params.set('field', query.field);
  if (query.source) params.set('source', query.source);
  if (query.startTime) params.set('startTime', query.startTime);
  if (query.endTime) params.set('endTime', query.endTime);

  const response = await fetch(
    `/api/business-history/${bizType}/${bizId}?${params.toString()}`,
    {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token') ?? ''}`
      }
    }
  );
  const body = await response.json();
  if (body.code !== 200) {
    throw new Error(body.message || '查询变更历史失败');
  }
  return body.data as BusinessHistoryResult;
}
```

---

## 6. 与原型页面的对应关系

| 原型 | 接入方式 |
|------|----------|
| 顶部“生产任务详情 / 制剂详情”切换 | 真实页面中通常由路由决定，不需要切换 |
| 左侧字段角标 | `fieldStats` |
| 处方行“有调整 / 数字” | `recordStats` |
| 右侧时间线 | `events` |
| 字段筛选下拉 | `fieldStats` 生成选项，变化时传 `field` |
| 主表 / 处方明细筛选 | 传 `source` |
| “完整”按钮 | 展示 `before` / `after` 全文；需要整行快照时查 `data-change-log/{id}` |
| “操作日志 #xxx” | 跳转或抽屉展示 `GET /api/system/operation-log/{id}` |
| “清空筛选” | 清空 `field/source`，重新请求第一页 |
| “历史记录较多”提示 | `truncated=true` 时展示 |

---

## 7. 边界与注意事项

1. **事件分页**：`totalCount` 是事件数，不是字段变更数；前端分页控件按事件分页。
2. **字段统计不受筛选影响**：即使用户选了 `field=spec`，`fieldStats` 仍返回全部字段的次数，方便随时切换。
3. **创建事件没有字段差异**：`changes` 为空时展示“创建记录，无字段差异”，不要隐藏整条事件。
4. **处方行被删除**：后端会通过 `preparation_formula` 历史行补全 `recordLabel`，前端直接用；为空时回退显示 `businessId`。
5. **历史截断**：`truncated=true` 表示单次加载达到上限，前端提示“仅展示最近部分变更”，如有需要再按时间范围缩小查询。
6. **MAC 为空**：旧日志或未上报 MAC 的客户端可能为空，展示“—”，不要报错。
7. **操作日志缺失**：极少数日志被清理后，事件仍会用数据变更里的操作人/时间兜底，`operationLogId` 可能不可点击。
8. **权限**：接口仍需要 JWT；页面入口建议由菜单/按钮权限控制，后端后续可按角色补方法级权限。
9. **刷新时机**：详情页保存成功后重新请求变更历史；筛选变化时只请求变更历史，不重新请求详情。
10. **不要再用多次 data-change-log 查询拼时间线**：生产任务、制剂详情统一使用聚合接口，避免主表 + 子表多次请求和前端合并。

---

## 8. 已复用的现有接口

以下接口不需要为新页面重新开发，按需使用：

| 接口 | 用途 |
|------|------|
| `GET /api/work-orders/{id}` | 生产任务详情 |
| `GET /api/preparation/{id}/details` | 制剂详情（含处方、工艺模板、附件） |
| `GET /api/system/operation-log/{id}` | 操作日志详情 |
| `GET /api/system/operation-log/{id}/changes` | 某次操作的全部变更（调试/兜底） |
| `GET /api/system/data-change-log/{id}` | 单条变更详情（整行快照） |

---

## 9. 部署与迁移

后端本次新增/启用的接口需要先执行迁移脚本：

```bash
# 已执行过 MAC 字段迁移的可跳过
src/main/resources/Data/migration/add_log_mac_address.sql

# 业务变更历史查询索引（幂等）
src/main/resources/Data/migration/add_business_history_indexes.sql
```

说明：

- `add_business_history_indexes.sql` 为 `sys_operation_log`、`sys_data_change_log`、`preparation_formula` 补查询索引；
- 脚本已在测试库 `8.134.48.54/erp_db` 随集成测试执行；
- 生产库 `8.148.27.237/erp_db1` 需要手动执行后再重启服务；
- 前端不需要额外配置，接口仍在 `/api` 下，JWT 认证方式不变。

---

## 10. 测试建议

前端联调至少覆盖：

1. 生产任务有多个模块间接修改（检验、入库、审核放行）时，时间线完整。
2. 制剂主表字段（规格、制法、功能主治）与处方明细（处方量）同时出现。
3. 点击“规格”“批量”“处方量”角标，列表只剩对应字段。
4. 处方行角标只显示该行记录。
5. 清空筛选后恢复全部事件。
6. 翻页保留筛选条件。
7. 长文本“完整”弹窗展示改前/改后全文。
8. 无历史记录时显示空状态。
9. `truncated=true` 时显示提示。
10. 操作日志跳转正常，MAC 为空时展示“—”。
