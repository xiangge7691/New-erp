# 制剂续展文件 —— 前端对接指南

> 适用项目：`C:\Work\Java\art-design-pro-main`
> 后端版本：`New-erp` 分支 `v1.0`（提交 `9109a53`）
> 核心代码：`PreparationRenewalController`、`PreparationRenewalServiceImpl`、`PreparationRenewalKind`、`BizFileType.PREPARATION_RENEWAL`
> 视觉稿：[docs/prototype/preparation-renewal/index.html](./prototype/preparation-renewal/index.html)
> 实现计划：[docs/preparation-renewal-implementation-plan.md](./preparation-renewal-implementation-plan.md)
> 测试库：`8.134.48.54/erp_db`；**禁止向 `8.148.27.237` 写入测试数据**

本文按「页面结构 → 常量 → API 模块 → 接口明细 → 交互实现 → 错误边界 → 验收」组织，可直接作为联调依据。

---

## 0. 结论先行

1. **不新增菜单和路由**。在制剂管理现有详情/编辑抽屉中新增第 6 个选项卡「续展文件」，与基本信息、配方、工序模板、文档、附件并列。
2. 页面数据只调用 **一个聚合接口** `GET /api/preparation/{id}/renewal-files`，一次返回 10 个分组和续展备注，不要逐类查询。
3. 上传、删除**复用业务附件接口**，业务类型固定为 `PREPARATION_RENEWAL`，不要传 `PREPARATION`（那是「附件」选项卡的数据，混用会导致两类文件串页）。
4. 续展文件绑定在 `preparation_id` 上，**新建制剂尚未保存（无 ID）时不能上传**，此时 tab 显示「请先保存制剂」的空态并禁用操作。
5. 打包下载返回 **ZIP 文件流**，包名固定为 `{制剂名}续展文件.zip`；没有任何文件时也会下载一个包含 10 个空文件夹的 ZIP。
6. 上传/删除成功后必须刷新聚合接口（重新拉取 10 组），不要只改本地数组，否则数量上限、排序和分组状态会与后端不一致。
7. 「续展备注」建议独立保存（失焦或点「保存备注」），避免与制剂主表单的整单保存相互覆盖。

---

## 1. 页面定位与布局

### 1.1 入口

- 制剂管理列表 → 行操作「续展文件」：打开详情抽屉并直接选中「续展文件」tab。
- 制剂管理详情/编辑抽屉 → 顶部 tab 切换到「续展文件」：首次进入时懒加载。
- 新建制剂：tab 可见但置灰或展示提示，保存主表拿到 `preparationId` 后再允许上传。

### 1.2 建议布局

```text
┌─────────────────────────────────────────────────────────────────────┐
│ 益智健脑口服液  Z04-012                       注册批件 2027-03-15  │
│ 委托配制批件 2026-12-31（剩余 47 天）                        [关闭] │
├─────────────────────────────────────────────────────────────────────┤
│ 基本信息 │ 配方 │ 工序模板 │ 文档 │ 附件 │ [续展文件]               │
├─────────────────────────────────────────────────────────────────────┤
│ 续展备注  [textarea 0/1000]                          [保存备注]     │
│ 已完成 8 个文件 / 10 类                              [打包下载]     │
├──────────────────┬──────────────────────────────────────────────────┤
│ 01 备案申请表 2  │  01 委托配制备案申请表                 2 / 10    │
│ 02 委托方证明 1  │  ┌ 文件行：名称 / 大小 / 时间 / 预览 下载 删除 ┐ │
│ 03 受托方证件 2  │  └──────────────────────────────────────────────┘ │
│ 04 前次备案   1  │  [+ 上传文件]                                    │
│ 05 质量总结   0  │  --------------------------------------------------│
│ 06 变化证明   0  │  02 委托方批准证明文件及许可证         1 / 10    │
│ 07 委托合同   1  │  ...                                             │
│ 08 真实性声明 0  │                                                  │
│ 09 授权委托书 0  │                                                  │
│ 10 其他文件   1  │                                                  │
└──────────────────┴──────────────────────────────────────────────────┘
```

- 左侧类型导航固定宽度约 200px，显示序号、简称、已传数量；点击滚动到对应分组。
- 右侧为可滚动分组列表；每个分组 = 标题（序号 + 全称 + 数量）+ 文件列表 + 上传按钮。
- 空分组保留标题和上传入口，不要隐藏，否则用户不知道要传哪些材料。

### 1.3 页面状态

| 状态 | 表现 |
|---|---|
| 加载中 | 抽屉内容区骨架屏，禁用下载 |
| 加载失败 | 展示错误信息 + 「重试」，不要清空上次数据 |
| 无制剂 ID | 提示「请先保存制剂信息，再维护续展文件」 |
| 无文件 | 10 个分组全部显示空态，下载按钮可点（下载空目录包） |
| 只读 | 上传、删除、保存备注、下载以外全部禁用；下载保留 |

---

## 2. 组件拆分建议

```text
src/api/preparationRenewal.ts                          # 接口与类型
src/views/preparation/components/
├── PreparationRenewalPanel.vue                        # tab 容器：加载、备注、工具栏、分组列表
├── RenewalFileGroup.vue                               # 单个文件类型分组
└── RenewalFileItem.vue                                # 文件行（可选，文件多时拆分）
```

> 现有制剂页面的目录如不是 `views/preparation`，按项目实际路径放置，接口模块独立即可。

---

## 3. 固定常量

10 类文件的编码、名称、数量上限由后端注册，**页面渲染以接口返回的 `groups[]` 为准**；下面的常量仅用于图标、排序兜底或离线调试。

| 序号 | kind | 展示名 | 上限 | ZIP 内文件夹 |
|---|---|---|---|---|
| 1 | `RENEWAL_APPLICATION_FORM` | 委托配制备案申请表 | 10 | `01_委托配制备案申请表` |
| 2 | `RENEWAL_COMMISSIONER_APPROVAL` | 委托方批准证明文件及许可证 | 10 | `02_委托方批准证明文件及许可证` |
| 3 | `RENEWAL_MANUFACTURER_LICENSE_GMP` | 受托方药品生产许可证及GMP证书 | 10 | `03_受托方药品生产许可证及GMP证书` |
| 4 | `RENEWAL_PREVIOUS_FILING` | 前次备案件或公告 | 10 | `04_前次备案件或公告` |
| 5 | `RENEWAL_PREVIOUS_PERIOD_SUMMARY` | 前次配制及质量情况总结 | 10 | `05_前次配制及质量情况总结` |
| 6 | `RENEWAL_CHANGE_PROOF` | 与前次备案变化的证明文件 | 10 | `06_与前次备案变化的证明文件` |
| 7 | `RENEWAL_COMMISSION_CONTRACT` | 委托配制合同 | 10 | `07_委托配制合同` |
| 8 | `RENEWAL_AUTHENTICITY_DECLARATION` | 申请材料真实性自我保证声明 | 10 | `08_申请材料真实性自我保证声明` |
| 9 | `RENEWAL_AUTHORIZATION_LETTER` | 授权委托书 | 10 | `09_授权委托书` |
| 10 | `RENEWAL_OTHER` | 其他文件 | 不限（`-1`） | `10_其他文件` |

```ts
export const RENEWAL_BIZ_TYPE = 'PREPARATION_RENEWAL'

export const RENEWAL_KINDS = [
  { code: 'RENEWAL_APPLICATION_FORM', label: '委托配制备案申请表' },
  { code: 'RENEWAL_COMMISSIONER_APPROVAL', label: '委托方批准证明文件及许可证' },
  { code: 'RENEWAL_MANUFACTURER_LICENSE_GMP', label: '受托方药品生产许可证及GMP证书' },
  { code: 'RENEWAL_PREVIOUS_FILING', label: '前次备案件或公告' },
  { code: 'RENEWAL_PREVIOUS_PERIOD_SUMMARY', label: '前次配制及质量情况总结' },
  { code: 'RENEWAL_CHANGE_PROOF', label: '与前次备案变化的证明文件' },
  { code: 'RENEWAL_COMMISSION_CONTRACT', label: '委托配制合同' },
  { code: 'RENEWAL_AUTHENTICITY_DECLARATION', label: '申请材料真实性自我保证声明' },
  { code: 'RENEWAL_AUTHORIZATION_LETTER', label: '授权委托书' },
  { code: 'RENEWAL_OTHER', label: '其他文件' }
] as const
```

---

## 4. API 模块

新建 `src/api/preparationRenewal.ts`。

> `request.get<T>()` 等项目封装已经解包 `ApiResponse<T>`，业务代码只拿 `data`，不要再取一次 `.data`。
> 分页无关；本组接口没有分页参数。

```ts
import request from '@/utils/http'

/** 业务附件（复用业务附件接口返回结构） */
export interface BusinessFileItem {
  relationId: number
  bizType: string
  bizId: number
  refId: number
  fileId: number
  fileKind: string
  originalName: string
  fileSize: number
  contentType: string
  fileExtension: string
  description?: string | null
  fileUrl: string
  previewUrl: string
  createdTime?: string | null
}

/** 单个续展文件类型分组 */
export interface RenewalFileGroup {
  kind: string
  label: string
  /** -1 表示不限 */
  maxCount: number
  files: BusinessFileItem[]
}

/** 续展文件页数据 */
export interface PreparationRenewalFiles {
  preparationId: number
  preparationName: string
  renewalRemark: string | null
  groups: RenewalFileGroup[]
}

/** 上传参数 */
export interface UploadRenewalFileParams {
  file: File
  preparationId: number
  fileKind: string
  /** 用于存储目录，建议传制剂品名 */
  entityName?: string
}

/** 查询 10 组续展文件 + 续展备注 */
export function fetchPreparationRenewalFiles(preparationId: number) {
  return request.get<PreparationRenewalFiles>({
    url: `/api/preparation/${preparationId}/renewal-files`
  })
}

/** 保存续展备注（空串表示清空） */
export function savePreparationRenewalRemark(preparationId: number, renewalRemark: string) {
  return request.put<null>({
    url: `/api/preparation/${preparationId}/renewal-remark`,
    data: { renewalRemark }
  })
}

/** 上传并绑定续展文件 */
export function uploadRenewalFile(params: UploadRenewalFileParams) {
  const fd = new FormData()
  fd.append('file', params.file)
  fd.append('bizType', 'PREPARATION_RENEWAL')
  fd.append('bizId', String(params.preparationId))
  fd.append('fileKind', params.fileKind)
  if (params.entityName) fd.append('entityName', params.entityName)
  return request.post<BusinessFileItem>({
    url: '/api/business-files/upload',
    data: fd
  })
}

/** 删除续展文件（解绑关系并回收无引用文件） */
export function deleteBusinessFile(relationId: number) {
  return request.del<boolean>({
    url: `/api/business-files/${relationId}`
  })
}

/** 打包下载，返回 Blob */
export function exportPreparationRenewalFiles(preparationId: number) {
  return request.request<Blob>({
    url: `/api/preparation/${preparationId}/renewal-files/export`,
    method: 'GET',
    responseType: 'blob'
  })
}

/** 单文件预览/下载，返回 Blob */
export function fetchFileBlob(fileId: number, preview = false) {
  return request.request<Blob>({
    url: preview ? `/api/files/preview/${fileId}` : `/api/files/${fileId}`,
    method: 'GET',
    responseType: 'blob'
  })
}
```

---

## 5. 接口明细

### 5.1 查询续展文件

```http
GET /api/preparation/56/renewal-files
Authorization: Bearer <token>
```

响应（`groups` 固定 10 项，顺序与申报清单一致）：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "preparationId": 56,
    "preparationName": "益智健脑口服液",
    "renewalRemark": "注册批件 2027-03-15 到期，已启动续展",
    "groups": [
      {
        "kind": "RENEWAL_APPLICATION_FORM",
        "label": "委托配制备案申请表",
        "maxCount": 10,
        "files": [
          {
            "relationId": 317,
            "bizType": "PREPARATION_RENEWAL",
            "bizId": 56,
            "refId": 0,
            "fileId": 851,
            "fileKind": "RENEWAL_APPLICATION_FORM",
            "originalName": "备案申请表.pdf",
            "fileSize": 204800,
            "contentType": "application/pdf",
            "fileExtension": "pdf",
            "description": null,
            "fileUrl": "/api/files/851",
            "previewUrl": "/api/files/preview/851",
            "createdTime": "2026-11-14 10:58:53"
          }
        ]
      }
    ]
  }
}
```

字段说明：

| 字段 | 说明 |
|---|---|
| `relationId` | 附件关系 ID，**删除时用它**，不是 `fileId` |
| `fileId` | 文件 ID，预览/下载用 |
| `fileKind` | 续展类型编码，与分组 `kind` 一致 |
| `originalName` | 原始文件名，直接展示 |
| `fileSize` | 字节数，前端格式化 |
| `createdTime` | `yyyy-MM-dd HH:mm:ss`，可能为空 |
| `renewalRemark` | 可能为 `null`，前端按空串处理 |

### 5.2 上传并绑定

```http
POST /api/business-files/upload
Content-Type: multipart/form-data

file=@备案申请表.pdf
bizType=PREPARATION_RENEWAL
bizId=56
fileKind=RENEWAL_APPLICATION_FORM
entityName=益智健脑口服液
```

| 参数 | 必填 | 说明 |
|---|---|---|
| `file` | 是 | 单文件 |
| `bizType` | 是 | 固定 `PREPARATION_RENEWAL` |
| `bizId` | 是 | 制剂 ID |
| `fileKind` | 是 | 10 类之一，按上传分组传 |
| `refId` | 否 | 续展文件固定 0，可不传 |
| `entityName` | 否 | 存储目录名，建议传制剂品名 |
| `description` | 否 | 文件描述，当前页面不要求 |

成功返回：

```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": { "relationId": 317, "fileId": 851, "fileKind": "RENEWAL_APPLICATION_FORM", "originalName": "备案申请表.pdf", "...": "..." }
}
```

限制（后端校验，前端提前拦截更友好）：

- 单个文件 ≤ **100MB**（`file.max-size`）。
- 允许类型：`jpeg/png/gif/bmp/pdf/doc/docx/xls/xlsx/txt/zip/rar`。
- 1~9 类每类最多 **10 个**，第 10 类「其他文件」不限。
- 制剂必须存在且未删除。
- 超限返回 `code = 400`，例如：`制剂续展 的 RENEWAL_APPLICATION_FORM 槽位最多允许 10 个文件`。

### 5.3 删除

```http
DELETE /api/business-files/317
```

```json
{ "code": 200, "message": "附件删除成功", "data": true }
```

删除的是 `relationId`。文件不再被其他业务引用时，后端会软删除文件记录；磁盘文件由文件管理模块回收。

### 5.4 保存续展备注

```http
PUT /api/preparation/56/renewal-remark
Content-Type: application/json

{ "renewalRemark": "注册批件 2027-03-15 到期，已启动续展" }
```

```json
{ "code": 200, "message": "续展备注保存成功", "data": null }
```

- `renewalRemark` 最长 **1000** 字符，超长返回 `code = 400`。
- 传 `""` 表示清空；传 `null` 同样按清空处理。
- 该接口只更新备注字段，不影响制剂其他字段。

### 5.5 打包下载

```http
GET /api/preparation/56/renewal-files/export
```

成功响应：

```text
HTTP/1.1 200
Content-Type: application/zip
Content-Disposition: attachment; filename*=UTF-8''%E7%9B%8A%E6%99%BA%E5%81%A5%E8%84%91%E5%8F%A3%E6%9C%8D%E6%B6%B2%E7%BB%AD%E5%B1%95%E6%96%87%E4%BB%B6.zip
```

压缩包结构（固定 10 个文件夹，空类型也保留）：

```text
益智健脑口服液续展文件.zip
├── 01_委托配制备案申请表/
│   ├── 备案申请表.pdf
│   └── 备案申请表 (1).pdf
├── 02_委托方批准证明文件及许可证/
├── 03_受托方药品生产许可证及GMP证书/
├── 04_前次备案件或公告/
├── 05_前次配制及质量情况总结/
├── 06_与前次备案变化的证明文件/
├── 07_委托配制合同/
├── 08_申请材料真实性自我保证声明/
├── 09_授权委托书/
└── 10_其他文件/
```

失败时返回 JSON（HTTP 400/500）：

```json
{ "code": 500, "message": "下载续展文件失败: ...", "data": null }
```

> 由于请求设置了 `responseType: 'blob'`，失败响应也会被包装成 Blob。前端需要判断 `blob.type` 是否包含 `application/json`，再读取文本解析 `message`。

### 5.6 单文件预览 / 下载

| 用途 | 接口 |
|---|---|
| 预览 | `GET /api/files/preview/{fileId}` |
| 下载 | `GET /api/files/{fileId}` |

这两个接口返回文件流。建议用 `responseType: 'blob'` + `URL.createObjectURL`，不要直接 `window.open`（可能丢失 `Authorization` 头，且新窗口会被拦截）。

---

## 6. 交互实现要点

### 6.1 加载与刷新

- 首次切到「续展文件」tab 时调用聚合接口；切换回其他 tab 不必重复请求。
- 上传、删除成功后**重新调用聚合接口**，用返回值覆盖本地 `groups`、`renewalRemark`；不要在前端手工维护计数。
- 详情抽屉关闭时清空缓存；同一制剂再次打开时重新拉取。

### 6.2 上传

- 每个分组一个「上传文件」按钮，隐藏的 `<input type="file" multiple>`，`accept` 与后端允许类型一致。
- 建议**串行上传**选中的多个文件：一是便于逐文件提示失败，二是避免并发触发槽位上限校验（后端计数校验非原子）。
- 上传期间禁用该分组的上传按钮和删除按钮，显示进度或 `loading`。
- 成功提示 `已上传 {name}`；失败直接展示后端 `message`，保留已成功的文件。
- 单文件大小、数量上限在前端先判断，减少一次往返。

### 6.3 数量与禁用规则

```ts
const reachedLimit = (group: RenewalFileGroup) =>
  group.maxCount >= 0 && group.files.length >= group.maxCount
```

- 显示 `已传 / 上限`，上限为 `-1` 时显示「不限」。
- 达到上限时禁用上传按钮，并给出 tooltip「该类最多上传 10 个文件」。

### 6.4 续展备注

- `textarea` 设 `maxlength="1000"`，右下角显示 `当前长度 / 1000`。
- 推荐「保存备注」按钮显式保存；如果用失焦自动保存，至少做 500ms 防抖，并在请求中禁用重复提交。
- 保存成功更新本地 `renewalRemark`，同时**同步给父级制剂表单模型**（见 6.7）。
- 卸载或切 tab 时若有未保存内容，给一次离开确认（可选）。

### 6.5 打包下载

```ts
import { exportPreparationRenewalFiles } from '@/api/preparationRenewal'

/** 与服务端一致的文件名清理 */
function safeName(name: string) {
  const cleaned = (name || '').replace(/[\\/:*?"<>|\r\n]/g, '_').trim()
  return cleaned || '制剂'
}

async function downloadRenewalZip(preparationId: number, preparationName: string) {
  const blob = await exportPreparationRenewalFiles(preparationId)

  // 失败时后端返回 JSON，被包装成了 Blob
  if (blob.type.includes('application/json')) {
    const payload = JSON.parse(await blob.text())
    throw new Error(payload.message || '下载失败')
  }

  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${safeName(preparationName)}续展文件.zip`
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}
```

- 包名与服务端一致，优先用本地拼接；如果项目的 `request` 封装能拿到响应头，可优先解析 `Content-Disposition` 的 `filename*=UTF-8''...`。
- 下载期间按钮 loading，禁止重复点击。
- 没有文件也可下载（10 个空文件夹），不要在前端拦截。

### 6.6 预览与单文件下载

```ts
import { fetchFileBlob } from '@/api/preparationRenewal'

async function previewFile(fileId: number) {
  const blob = await fetchFileBlob(fileId, true)
  const url = URL.createObjectURL(blob)
  window.open(url, '_blank')
  // 给浏览器留出加载时间再释放
  setTimeout(() => URL.revokeObjectURL(url), 60_000)
}

async function downloadFile(fileId: number, fileName: string) {
  const blob = await fetchFileBlob(fileId, false)
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  a.click()
  URL.revokeObjectURL(url)
}
```

- PDF、图片可直接预览；Word/Excel 浏览器不能内联渲染时，预览按钮退化为下载或提示使用下载。
- 文件行展示图标时按 `fileExtension` 区分：pdf 红、doc/docx 蓝、xls/xlsx 绿、图片紫、zip/rar 橙、其他灰。

### 6.7 与制剂主表单保存的关系（重要）

`renewalRemark` 是 `preparation` 表字段，`GET /api/preparation/{id}` 与 `/details` 都会返回它。存在两种保存方式：

1. **推荐**：tab 内用 `PUT /api/preparation/{id}/renewal-remark` 独立保存，同时在保存成功后把值写回父级表单模型。
2. 随制剂整单保存：把 `renewalRemark` 放进主表单的 `form` 对象，`PUT /api/preparation/{id}` 一并提交。

**风险**：如果主表单在打开时加载了旧的 `renewalRemark`，用户在 tab 内改完并单独保存后，又点主表单「保存」，旧值会把新备注覆盖。二选一即可，不要两种方式混用；若必须混用，保存备注后要同步父级 `form.renewalRemark` 或重新拉取详情。

### 6.8 只读与权限

- 查看权限只读：隐藏上传/删除/保存备注按钮，保留下载、预览。
- 写操作按钮按项目现有权限指令控制（如 `v-auth`），并在后端返回 400 时兜底提示。
- 后端已记录操作日志：备注保存为 `UPDATE`，打包下载为 `EXPORT`，模块为「制剂管理」。

---

## 7. 错误与边界

| 场景 | 后端表现 | 前端处理 |
|---|---|---|
| 制剂不存在/已删除 | `code = 400`，`制剂不存在` | 关闭 tab，提示并刷新列表 |
| 上传超过该类型上限 | `code = 400`，`...槽位最多允许 10 个文件` | 提示并刷新分组 |
| 文件类型不支持 | `code = 400`，`不支持的文件类型` | 上传前按 `accept` 拦截 |
| 文件超过 100MB | `code = 400`，`文件大小超出限制` | 上传前校验 |
| 备注超过 1000 字 | `code = 400` | `maxlength` + 计数 |
| 导出超过 500 个文件或 500MB | HTTP 400/500 + JSON | 解析 Blob 中的 `message` 并提示 |
| 磁盘文件缺失 | 后端跳过该文件，ZIP 仍成功 | 不提示失败；可在日志中排查 |
| 同目录重名文件 | 自动追加 ` (1)`、` (2)` | 无需处理，展示后端文件名即可 |
| 未登录/令牌失效 | 401 或统一错误 | 走项目登录失效逻辑 |

通用约定：

- 业务错误仍是 **HTTP 200 + `code != 200`**，不能只看 HTTP 状态码；项目 `request` 封装通常已统一处理。
- 日期时间格式为 `yyyy-MM-dd HH:mm:ss`，日期为 `yyyy-MM-dd`。

---

## 8. 联调验收清单

- [ ] 进入已有制剂详情，切到「续展文件」，10 个分组全部显示，顺序与申报清单一致。
- [ ] 每组上传 1 个文件后刷新，文件出现在正确分组，计数变为 `1 / 10`。
- [ ] 第 11 个文件上传被拦截，提示包含「最多允许 10 个文件」。
- [ ] 「其他文件」可超过 10 个。
- [ ] 两个同名文件在同一分组，ZIP 内分别为 `xxx.pdf` 与 `xxx (1).pdf`。
- [ ] 空分组也保留上传入口；全部为空时仍能下载 ZIP，解压有 10 个空文件夹。
- [ ] 删除文件后关系消失、计数减少；刷新页面后仍为空。
- [ ] 备注保存后重新打开详情可回显；清空备注后回显为空。
- [ ] 主表单保存不会覆盖 tab 内刚保存的备注（按 6.7 的方案验证）。
- [ ] 新建未保存制剂时，续展文件 tab 不允许上传并给出提示。
- [ ] 预览 PDF/图片正常，Word/Excel 走下载。
- [ ] 下载 ZIP 中文名不乱码，包名与制剂名一致。
- [ ] 只读账号看不到上传、删除、保存备注按钮。

---

## 9. 后端文件索引

| 内容 | 路径 |
|---|---|
| 控制器 | `src/main/java/com/tonghui/erp/Controller/PreparationRenewalController.java` |
| 服务实现 | `src/main/java/com/tonghui/erp/Service/impl/PreparationRenewalServiceImpl.java` |
| 续展类型枚举 | `src/main/java/com/tonghui/erp/Common/Constants/PreparationRenewalKind.java` |
| 业务类型注册 | `src/main/java/com/tonghui/erp/Common/Constants/BizFileType.java`（`PREPARATION_RENEWAL`） |
| 返回对象 | `src/main/java/com/tonghui/erp/Common/Dto/PreparationRenewalDtos.java` |
| 数据库迁移 | `src/main/resources/Data/migration/add_preparation_renewal_remark.sql` |
| 单元测试 | `src/test/java/com/tonghui/erp/Service/PreparationRenewalServiceTest.java` |
