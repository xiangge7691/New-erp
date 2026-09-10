package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.InspectionRequest;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.mapper.PreparationMapper;
import com.tonghui.erp.Service.InspectionRequestService;
import com.tonghui.erp.Service.RoomInfoService;
import com.tonghui.erp.Service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 请检记录控制器
 * <p>
 * 提供请检记录的CRUD操作、编号生成、生产任务关联查询、制剂信息查询、请检部门查询等功能
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────────────┬────────┬────────────────────────────────────┐
 * │ #  │ 接口                                                 │ 方法   │ 说明                               │
 * ├────┼──────────────────────────────────────────────────────┼────────┼────────────────────────────────────┤
 * │ 1  │ /api/inspection-request                              │ GET    │ 分页查询请检记录列表               │
 * │ 2  │ /api/inspection-request/{id}                         │ GET    │ 获取请检记录详情                   │
 * │ 3  │ /api/inspection-request                              │ POST   │ 新增请检记录                       │
 * │ 4  │ /api/inspection-request/{id}                         │ PUT    │ 更新请检记录                       │
 * │ 5  │ /api/inspection-request/{id}                         │ DELETE │ 删除请检记录（软删除）             │
 * │ 6  │ /api/inspection-request/generate-code                │ GET    │ 生成请检编号（QJ-YYYYMMDD-NNN）   │
 * │ 7  │ /api/inspection-request/work-orders/in-progress      │ GET    │ 查询生产中任务列表                 │
 * │ 8  │ /api/inspection-request/preparation/{code}           │ GET    │ 按制剂编码查询制剂信息             │
 * │ 9  │ /api/inspection-request/departments                  │ GET    │ 获取请检部门下拉选项               │
 * └────┴──────────────────────────────────────────────────────┴────────┴────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/inspection-request")
public class InspectionRequestController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 请检记录服务 */
    @Autowired
    private InspectionRequestService inspectionRequestService;

    /** 工单服务（查询生产中任务） */
    @Autowired
    private WorkOrderService workOrderService;

    /** 制剂主数据Mapper（按制剂编码查询规格） */
    @Autowired
    private PreparationMapper preparationMapper;

    /** 房间信息服务（请检部门下拉） */
    @Autowired
    private RoomInfoService roomInfoService;

    // endregion

    // region 分页查询
    // ===================================
    // 分页查询
    // ===================================

    /**
     * 分页查询请检记录列表
     *
     * 示例请求：
     * GET /api/inspection-request?pageIndex=0&pageSize=20&keyword=QJ&processName=配液&requestDepartment=制剂室提取浓缩车间
     *
     * @param keyword           关键字（对请检编号/任务编号/被检物名称/批号/请检人进行模糊匹配，可选）
     * @param processName       工序筛选（可选）
     * @param requestDepartment 请检部门筛选（可选）
     * @param requestTimeStart  请检时间起始（可选）
     * @param requestTimeEnd    请检时间结束（可选）
     * @param pageIndex         页码（从0开始）
     * @param pageSize          每页大小
     * @return 请检记录分页结果
     */
    @GetMapping
    public ApiResponse<PagedResult<InspectionRequest>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String processName,
            @RequestParam(required = false) String requestDepartment,
            @RequestParam(required = false) String requestTimeStart,
            @RequestParam(required = false) String requestTimeEnd,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            Page<InspectionRequest> pageResult = inspectionRequestService.queryRequests(
                    new InspectionRequest(), keyword, processName, requestDepartment,
                    requestTimeStart, requestTimeEnd, safePageIndex, safePageSize);

            PagedResult<InspectionRequest> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(safePageIndex);
            pagedResult.setPageSize((int) pageResult.getSize());
            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "查询请检记录列表");
        }
    }

    // endregion

    // region 请检记录详情
    // ===================================
    // 请检记录详情
    // ===================================

    /**
     * 根据ID获取请检记录详情
     *
     * 示例请求：
     * GET /api/inspection-request/1
     *
     * @param id 请检记录ID
     * @return 请检记录实体
     */
    @GetMapping("/{id}")
    public ApiResponse<InspectionRequest> getById(@PathVariable Long id) {
        try {
            InspectionRequest request = inspectionRequestService.getById(id);
            if (request == null) {
                return error("请检记录不存在");
            }
            return success(request);
        } catch (Exception ex) {
            return exception(ex, "获取请检记录详情");
        }
    }

    // endregion

    // region 请检记录创建与更新
    // ===================================
    // 请检记录创建与更新
    // ===================================

    /**
     * 新增请检记录（含DuplicateKeyException重试机制）
     *
     * 示例请求：
     * POST /api/inspection-request
     * Content-Type: application/json
     * {
     *   "inspectionCode": "",
     *   "workOrderId": 1,
     *   "workOrderCode": "SC-20260725-001",
     *   "inspectionPlanCode": "JH-20260901-001",
     *   "preparationCode": "Z00347",
     *   "preparationName": "益肾壮骨丸",
     *   "inspectionItemName": "益肾壮骨丸",
     *   "batchNumber": "HG20260715",
     *   "spec": "250ml/瓶",
     *   "processName": "配液",
     *   "configQuantity": "2500",
     *   "requestDepartment": "制剂室提取浓缩车间",
     *   "requester": "宋帅",
     *   "requestTime": "2026-09-08T10:20:00",
     *   "inspectionItems": "性状、水分、浸出物、含量测定",
     *   "remark": "过程请检"
     * }
     *
     * @param request 请检记录实体
     * @return 创建后的请检记录
     */
    @PostMapping
    public ApiResponse<InspectionRequest> create(@RequestBody InspectionRequest request) {
        try {
            if (request == null) {
                return error("请求参数不能为空");
            }
            if (!StringUtils.hasText(request.getInspectionItemName())) {
                return error("被检物名称不能为空");
            }
            if (!StringUtils.hasText(request.getProcessName())) {
                return error("工序不能为空");
            }
            if (!StringUtils.hasText(request.getConfigQuantity())) {
                return error("配置量不能为空");
            }
            if (!StringUtils.hasText(request.getRequestDepartment())) {
                return error("请检部门不能为空");
            }
            if (!StringUtils.hasText(request.getRequester())) {
                return error("请检人不能为空");
            }
            if (request.getRequestTime() == null) {
                return error("请检时间不能为空");
            }
            if (!StringUtils.hasText(request.getInspectionItems())) {
                return error("请检项目不能为空");
            }

            // 自动生成请检编号（如果未提供）
            boolean autoGenerated = false;
            if (!StringUtils.hasText(request.getInspectionCode())) {
                request.setInspectionCode(inspectionRequestService.generateCode());
                autoGenerated = true;
            }

            // 手动传入编号时校验唯一性
            if (!autoGenerated && !inspectionRequestService.isCodeUnique(request.getInspectionCode(), null)) {
                return error("请检编号已存在");
            }

            // 含DuplicateKeyException重试机制（最多3次）
            int attempts = 0;
            while (true) {
                try {
                    inspectionRequestService.save(request);
                    return success(request, "请检记录创建成功");
                } catch (DuplicateKeyException e) {
                    if (!autoGenerated || ++attempts >= 3) {
                        return error("请检编号生成冲突，请稍后重试");
                    }
                    request.setInspectionCode(inspectionRequestService.generateCode());
                }
            }
        } catch (Exception ex) {
            return exception(ex, "创建请检记录");
        }
    }

    /**
     * 更新请检记录
     *
     * 示例请求：
     * PUT /api/inspection-request/1
     * Content-Type: application/json
     * {
     *   "inspectionCode": "QJ-20260908-001",
     *   "processName": "灭菌",
     *   "requester": "李生产",
     *   "inspectionItems": "性状、微生物限度"
     * }
     *
     * @param id      请检记录ID
     * @param request 请检记录实体
     * @return 更新后的请检记录
     */
    @PutMapping("/{id}")
    public ApiResponse<InspectionRequest> update(@PathVariable Long id, @RequestBody InspectionRequest request) {
        try {
            if (request == null) {
                return error("请求参数不能为空");
            }

            InspectionRequest existing = inspectionRequestService.getById(id);
            if (existing == null) {
                return error("请检记录不存在");
            }

            // 修改编号时校验唯一性
            if (StringUtils.hasText(request.getInspectionCode())
                    && !request.getInspectionCode().equals(existing.getInspectionCode())) {
                if (!inspectionRequestService.isCodeUnique(request.getInspectionCode(), id)) {
                    return error("请检编号已存在");
                }
            }

            request.setId(id);
            inspectionRequestService.updateById(request);
            return success(request, "请检记录更新成功");
        } catch (Exception ex) {
            return exception(ex, "更新请检记录");
        }
    }

    /**
     * 删除请检记录（软删除）
     *
     * 示例请求：
     * DELETE /api/inspection-request/1
     *
     * @param id 请检记录ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            InspectionRequest existing = inspectionRequestService.getById(id);
            if (existing == null) {
                return error("请检记录不存在");
            }
            inspectionRequestService.removeById(id);
            return success(null, "删除成功");
        } catch (Exception ex) {
            return exception(ex, "删除请检记录");
        }
    }

    // endregion

    // region 辅助查询接口
    // ===================================
    // 辅助查询接口
    // ===================================

    /**
     * 生成请检编号（格式 QJ-YYYYMMDD-NNN）
     *
     * 示例请求：
     * GET /api/inspection-request/generate-code
     *
     * @return 请检编号
     */
    @GetMapping("/generate-code")
    public ApiResponse<String> generateCode() {
        try {
            String code = inspectionRequestService.generateCode();
            return success(code, "生成请检编号成功");
        } catch (Exception ex) {
            return exception(ex, "生成请检编号");
        }
    }

    /**
     * 查询生产中任务列表（供请检记录下拉选择）
     * <p>
     * 仅列出状态为"生产中"的工单（configDate有值且configCompleteTime为空），
     * 返回工单编号、制剂编码、制剂名称、批号、计划数量等信息
     * </p>
     *
     * 示例请求：
     * GET /api/inspection-request/work-orders/in-progress?keyword=益肾&pageIndex=0&pageSize=50
     *
     * @param keyword   关键字（可选，模糊匹配工单编号/制剂编码/制剂名称）
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页大小
     * @return 生产中任务分页结果
     */
    @GetMapping("/work-orders/in-progress")
    public ApiResponse<PagedResult<WorkOrder>> getInProgressWorkOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "50") int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 50 : Math.max(1, pageSize);

            Page<WorkOrder> pageResult = workOrderService.getInProgressWorkOrders(keyword, safePageIndex, safePageSize);

            PagedResult<WorkOrder> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(safePageIndex);
            pagedResult.setPageSize((int) pageResult.getSize());
            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "查询生产中任务");
        }
    }

    /**
     * 按制剂编码查询制剂信息（带出规格等字段）
     *
     * 示例请求：
     * GET /api/inspection-request/preparation/Z00347
     *
     * @param code 制剂编码
     * @return 制剂实体（含规格）
     */
    @GetMapping("/preparation/{code}")
    public ApiResponse<Preparation> getPreparationByCode(@PathVariable String code) {
        try {
            QueryWrapper<Preparation> wrapper = new QueryWrapper<>();
            wrapper.eq("preparation_code", code);
            wrapper.eq("is_deleted", 0);
            Preparation preparation = preparationMapper.selectOne(wrapper);
            if (preparation == null) {
                return error("制剂信息不存在");
            }
            return success(preparation);
        } catch (Exception ex) {
            return exception(ex, "查询制剂信息");
        }
    }

    /**
     * 获取请检部门下拉选项
     * <p>
     * 优先从RoomInfo表中查询启用状态的房间名称（去重），
     * 若结果为空则返回兜底选项列表
     * </p>
     *
     * 示例请求：
     * GET /api/inspection-request/departments
     *
     * @return 请检部门名称列表
     */
    @GetMapping("/departments")
    public ApiResponse<List<String>> getDepartments() {
        try {
            // 查询启用状态的房间名称，去重
            List<RoomInfo> activeRooms = roomInfoService.listActive();
            Set<String> departmentSet = activeRooms.stream()
                    .map(RoomInfo::getRoomName)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            List<String> departments;
            if (!departmentSet.isEmpty()) {
                departments = new ArrayList<>(departmentSet);
            } else {
                // 兜底选项
                departments = Arrays.asList(
                        "制剂室提取浓缩车间",
                        "制剂室固体制剂车间",
                        "干燥粉碎间",
                        "提取浓缩醇沉间",
                        "灭菌间",
                        "口服液配液间",
                        "口服液灌装间",
                        "外包间"
                );
            }
            return success(departments);
        } catch (Exception ex) {
            return exception(ex, "获取请检部门列表");
        }
    }

    // endregion
}
