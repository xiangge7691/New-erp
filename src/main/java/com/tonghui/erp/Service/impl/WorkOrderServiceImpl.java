package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Service.EquipmentService;
import com.tonghui.erp.Service.PreparationProcessTemplateService;
import com.tonghui.erp.Service.PreparationService;
import com.tonghui.erp.Service.ProductionPlanService;
import com.tonghui.erp.Service.RoomInfoService;
import com.tonghui.erp.Service.WorkOrderProcessExecutionService;
import com.tonghui.erp.Service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工单服务实现类
 * <p>
 * 实现WorkOrderService接口，提供工单相关的业务逻辑处理，包括工单的增删改查、
 * 工单编号自动生成、高级查询等功能的具体实现
 * </p>
 *
 */
@Service
public class WorkOrderServiceImpl extends ServiceImpl<WorkOrderMapper, WorkOrder>
        implements WorkOrderService {
    
    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 制剂服务，用于获取制剂信息 */
    @Autowired
    private PreparationService preparationService;

    /** 制剂工序模板服务，用于拉取工序模板 */
    @Autowired
    private PreparationProcessTemplateService preparationProcessTemplateService;

    /** 工单工序执行记录服务，用于自动绑定工序记录 */
    @Autowired
    private WorkOrderProcessExecutionService workOrderProcessExecutionService;

    /** 配置室服务，用于根据描述匹配配置室ID */
    @Autowired
    private RoomInfoService roomInfoService;

    /** 设备服务，用于根据描述匹配设备ID */
    @Autowired
    private EquipmentService equipmentService;

    /** 生产计划服务，用于工单变更后联动刷新关联计划的状态 */
    @Autowired
    private ProductionPlanService productionPlanService;

    // endregion

    // region 分页查询方法
    // ===================================
    // 分页查询方法
    // ===================================

    /**
     * 分页查询工单列表
     *
     * @param pageRequestDto 分页请求参数，包含页码和每页数量
     * @return 工单分页结果
     */
    @Override
    public PagedResult<WorkOrder> getWorkOrderList(PageRequestDto pageRequestDto) {
        Page<WorkOrder> page = new Page<>(pageRequestDto.getPageIndex(), pageRequestDto.getPageSize());
        Page<WorkOrder> workOrderPage = this.page(page);

        PagedResult<WorkOrder> pagedResult = new PagedResult<>();
        pagedResult.setItems(workOrderPage.getRecords());
        pagedResult.setTotalCount(workOrderPage.getTotal());
        pagedResult.setPageIndex(pageRequestDto.getPageIndex());
        pagedResult.setPageSize(pageRequestDto.getPageSize());

        return pagedResult;
    }

    // endregion

    // region 基础CRUD操作
    // ===================================
    // 基础CRUD操作
    // ===================================

    /**
     * 新增工单
     * <p>
     * 自动根据preparationId填充制剂编码和名称，设置创建时间和更新时间，
     * 以及当前操作用户作为创建人和更新人
     * </p>
     *
     * @param workOrder 工单实体
     * @return 操作是否成功
     */
    /** 工单编号生成最大重试次数 */
    private static final int MAX_RETRY = 3;

    @Override
    @Transactional
    public boolean addWorkOrder(WorkOrder workOrder) {
        // 如果提供了preparationId但没有提供preparationCode和preparationName，则从Preparation表中获取
        if (workOrder.getPreparationId() != null &&
            (workOrder.getPreparationCode() == null || workOrder.getPreparationCode().isEmpty()) &&
            (workOrder.getPreparationName() == null || workOrder.getPreparationName().isEmpty())) {

            Preparation preparation = preparationService.getPreparationById(workOrder.getPreparationId());
            if (preparation != null) {
                workOrder.setPreparationCode(preparation.getPreparationCode());
                workOrder.setPreparationName(preparation.getPreparationName());
            }
        }

        // 如果preparationCode仍然为空，则抛出异常
        if (workOrder.getPreparationCode() == null || workOrder.getPreparationCode().isEmpty()) {
            throw new RuntimeException("创建失败: preparation_code不能为空，请提供制剂信息");
        }

        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        workOrder.setCreatedTime(now);
        workOrder.setUpdatedTime(now);

        // 根据日期字段自动计算工单状态
        workOrder.setCurrentStatus(resolveStatus(workOrder.getConfigDate(),
                workOrder.getConfigCompleteTime(), workOrder.getArchiveTime(),
                workOrder.getInspectionStart(), workOrder.getInspectionEnd()));

        // 获取当前用户ID
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            workOrder.setCreatedBy(currentUserId);
            workOrder.setUpdatedBy(currentUserId);
        }

        // 重试机制：处理工单编号并发冲突
        for (int i = 0; i < MAX_RETRY; i++) {
            // 首次或重试时生成编号
            if (workOrder.getWorkOrderCode() == null || workOrder.getWorkOrderCode().isEmpty()) {
                workOrder.setWorkOrderCode(generateWorkOrderCode());
            }

            try {
                boolean saved = this.save(workOrder);

                // 工单保存成功后，自动根据制剂拉取工序模板并绑定工序执行记录
                if (saved && workOrder.getPreparationId() != null) {
                    bindProcessTemplates(workOrder);
                }

                // 工单保存成功后，联动刷新关联生产计划的状态（待生产→生产中）
                if (saved && workOrder.getPlanId() != null) {
                    productionPlanService.refreshPlanStatus(workOrder.getPlanId().intValue());
                }

                return saved;
            } catch (DuplicateKeyException e) {
                // 编号冲突，清空编号后重试
                workOrder.setWorkOrderCode(null);
                if (i == MAX_RETRY - 1) {
                    throw new RuntimeException("创建失败: 工单编号生成冲突，请稍后重试", e);
                }
            }
        }

        return false;
    }

    /**
     * 更新工单
     * <p>自动更新更新时间和更新人信息，根据日期字段重新计算工单状态，更新后联动刷新关联生产计划的状态</p>
     *
     * @param workOrder 工单实体，包含要更新的字段信息
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean updateWorkOrder(WorkOrder workOrder) {
        // 设置更新时间
        workOrder.setUpdatedTime(LocalDateTime.now());

        // 获取当前用户ID
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            workOrder.setUpdatedBy(currentUserId);
        }

        // 根据日期字段自动计算工单状态（未提交的日期字段沿用数据库现有值）
        WorkOrder existing = workOrder.getWorkOrderId() != null ? this.getById(workOrder.getWorkOrderId()) : null;
        LocalDateTime configDate = workOrder.getConfigDate() != null ? workOrder.getConfigDate()
                : (existing != null ? existing.getConfigDate() : null);
        LocalDateTime configCompleteTime = workOrder.getConfigCompleteTime() != null ? workOrder.getConfigCompleteTime()
                : (existing != null ? existing.getConfigCompleteTime() : null);
        LocalDateTime archiveTime = workOrder.getArchiveTime() != null ? workOrder.getArchiveTime()
                : (existing != null ? existing.getArchiveTime() : null);
        LocalDateTime inspectionStart = workOrder.getInspectionStart() != null ? workOrder.getInspectionStart()
                : (existing != null ? existing.getInspectionStart() : null);
        LocalDateTime inspectionEnd = workOrder.getInspectionEnd() != null ? workOrder.getInspectionEnd()
                : (existing != null ? existing.getInspectionEnd() : null);
        workOrder.setCurrentStatus(resolveStatus(configDate, configCompleteTime, archiveTime,
                inspectionStart, inspectionEnd));

        boolean updated = this.updateById(workOrder);

        // 更新成功后，联动刷新关联生产计划的状态（如工单出库后计划变为已完成）
        if (updated && workOrder.getPlanId() != null) {
            productionPlanService.refreshPlanStatus(workOrder.getPlanId().intValue());
        }

        return updated;
    }

    /**
     * 删除工单
     * <p>删除后联动刷新关联生产计划的状态</p>
     *
     * @param workOrderId 工单ID
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean deleteWorkOrder(Long workOrderId) {
        // 删除前查询工单，获取关联计划ID用于状态刷新
        WorkOrder workOrder = this.getById(workOrderId);
        boolean removed = this.removeById(workOrderId);

        // 删除成功后，联动刷新关联生产计划的状态
        if (removed && workOrder != null && workOrder.getPlanId() != null) {
            productionPlanService.refreshPlanStatus(workOrder.getPlanId().intValue());
        }

        return removed;
    }

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据ID查询工单
     *
     * @param workOrderId 工单ID
     * @return 工单实体，不存在则返回null
     */
    @Override
    public WorkOrder getWorkOrderById(Long workOrderId) {
        return this.getById(workOrderId);
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询工单（支持多条件组合查询和时间范围筛选）
     * <p>支持按工单ID、工单编号、工单名称、制剂、关联计划ID、关联计划名称、关联计划编号、当前状态等条件精确或模糊查询</p>
     *
     * @param workOrder 查询条件实体，非null字段将作为等值或模糊查询条件
     * @param keyword   关键字（对工单编号、工单名称、制剂编码、制剂名称、关联计划名称、关联计划编号进行模糊匹配，可选）
     * @param createdTimeStart 创建时间起始值（含）
     * @param createdTimeEnd   创建时间结束值（含）
     * @param updatedTimeStart 更新时间起始值（含）
     * @param updatedTimeEnd   更新时间结束值（含）
     * @param pageNum          页码，从0开始
     * @param pageSize         每页数量
     * @return 工单分页结果
     */
    @Override
    public Page<WorkOrder> queryWorkOrders(WorkOrder workOrder,
                                           String keyword,
                                           LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                           LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                           int pageNum, int pageSize) {
        int actualPageNum = pageNum + 1;

        Page<WorkOrder> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            // 关键字对工单编号、工单名称、制剂编码、制剂名称、关联计划名称、关联计划编号进行模糊匹配
            List<Long> planIdsByNumber = findPlanIdsByNumber(keyword);
            wrapper.and(w -> {
                w.like("work_order_code", keyword).or().like("work_order_name", keyword)
                        .or().like("preparation_code", keyword).or().like("preparation_name", keyword)
                        .or().like("plan_name", keyword);
                // 关联计划编号不在工单表，需先在生产计划表中模糊匹配出计划ID，再按 plan_id 过滤
                if (!planIdsByNumber.isEmpty()) {
                    w.or().in("plan_id", planIdsByNumber);
                }
            });
        }
        if (workOrder.getWorkOrderId() != null) {
            wrapper.eq("work_order_id", workOrder.getWorkOrderId());
        }
        if (StringUtils.hasText(workOrder.getWorkOrderCode())) {
            wrapper.like("work_order_code", workOrder.getWorkOrderCode());
        }
        if (StringUtils.hasText(workOrder.getWorkOrderName())) {
            wrapper.like("work_order_name", workOrder.getWorkOrderName());
        }
        if (workOrder.getPreparationId() != null) {
            wrapper.eq("preparation_id", workOrder.getPreparationId());
        }
        if (workOrder.getPlanId() != null) {
            wrapper.eq("plan_id", workOrder.getPlanId());
        }
        if (StringUtils.hasText(workOrder.getPlanName())) {
            wrapper.like("plan_name", workOrder.getPlanName());
        }
        // 按关联生产计划编号筛选：工单表无编号字段，先在生产计划表模糊匹配出计划ID，再按 plan_id 过滤
        if (StringUtils.hasText(workOrder.getPlanNumber())) {
            List<Long> planIdsByNumber = findPlanIdsByNumber(workOrder.getPlanNumber());
            if (planIdsByNumber.isEmpty()) {
                // 无匹配的计划编号，返回空结果
                wrapper.eq("plan_id", -1L);
            } else {
                wrapper.in("plan_id", planIdsByNumber);
            }
        }
        if (StringUtils.hasText(workOrder.getCurrentStatus())) {
            wrapper.eq("current_status", workOrder.getCurrentStatus());
        }
        if (StringUtils.hasText(workOrder.getPreparationCode())) {
            wrapper.like("preparation_code", workOrder.getPreparationCode());
        }
        if (StringUtils.hasText(workOrder.getPreparationName())) {
            wrapper.like("preparation_name", workOrder.getPreparationName());
        }
        if (workOrder.getBatchQty() != null) {
            wrapper.eq("batch_qty", workOrder.getBatchQty());
        }
        if (StringUtils.hasText(workOrder.getProducer())) {
            wrapper.like("producer", workOrder.getProducer());
        }
        if (StringUtils.hasText(workOrder.getReceiver())) {
            wrapper.like("receiver", workOrder.getReceiver());
        }
        if (workOrder.getDeliveryTime() != null) {
            wrapper.eq("delivery_time", workOrder.getDeliveryTime());
        }
        if (workOrder.getInvoicePrice() != null) {
            wrapper.eq("invoice_price", workOrder.getInvoicePrice());
        }
        if (workOrder.getInsurancePrice() != null) {
            wrapper.eq("insurance_price", workOrder.getInsurancePrice());
        }
        if (workOrder.getSettlementPrice() != null) {
            wrapper.eq("settlement_price", workOrder.getSettlementPrice());
        }
        if (StringUtils.hasText(workOrder.getBatchNumber())) {
            wrapper.like("batch_number", workOrder.getBatchNumber());
        }
        if (workOrder.getOutboundQty() != null) {
            wrapper.eq("outbound_qty", workOrder.getOutboundQty());
        }
        if (workOrder.getReceiptAmount() != null) {
            wrapper.eq("receipt_amount", workOrder.getReceiptAmount());
        }
        if (workOrder.getActualReceiptAmount() != null) {
            wrapper.eq("actual_receipt_amount", workOrder.getActualReceiptAmount());
        }
        if (workOrder.getInvoiceAmount() != null) {
            wrapper.eq("invoice_amount", workOrder.getInvoiceAmount());
        }
        if (workOrder.getSettlementAmount() != null) {
            wrapper.eq("settlement_amount", workOrder.getSettlementAmount());
        }
        if (workOrder.getReturnAmount() != null) {
            wrapper.eq("return_amount", workOrder.getReturnAmount());
        }
        if (workOrder.getCreatedBy() != null) {
            wrapper.eq("created_by", workOrder.getCreatedBy());
        }
        if (workOrder.getUpdatedBy() != null) {
            wrapper.eq("updated_by", workOrder.getUpdatedBy());
        }

        // 时间范围查询
        if (createdTimeStart != null) {
            wrapper.ge("created_time", createdTimeStart);
        }
        if (createdTimeEnd != null) {
            wrapper.le("created_time", createdTimeEnd);
        }
        if (updatedTimeStart != null) {
            wrapper.ge("updated_time", updatedTimeStart);
        }
        if (updatedTimeEnd != null) {
            wrapper.le("updated_time", updatedTimeEnd);
        }

        // 按创建时间倒序排列，新创建的显示在最前
        wrapper.orderByDesc("created_time");

        return this.page(page, wrapper);
    }

    /**
     * 根据关键字在生产计划表的生产计划编号（plan_number）中模糊匹配，返回匹配的计划ID列表
     * <p>
     * 工单表未冗余生产计划编号字段，需通过 plan_id 关联 production_plan 表查询
     * </p>
     *
     * @param keyword 关联计划编号关键字（可为空）
     * @return 匹配的生产计划ID列表（无匹配返回空列表）
     */
    private List<Long> findPlanIdsByNumber(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return new ArrayList<>();
        }
        List<ProductionPlan> plans = productionPlanService.list(
                new QueryWrapper<ProductionPlan>().like("plan_number", keyword));
        return plans.stream().map(Plan -> Plan.getId().longValue()).collect(Collectors.toList());
    }

    // endregion

    // region 状态管理
    // ===================================
    // 状态管理
    // ===================================

    /**
     * 根据日期字段计算工单当前状态
     * <p>
     * 状态流转规则（依据配置日期、配置完成日期、归档时间三个字段）：
     * <ul>
     *   <li>待生产 - 配置日期为空</li>
     *   <li>生产中 - 配置日期有值 且 配置完成日期为空</li>
     *   <li>已生产 - 配置完成日期有值</li>
     *   <li>已归档 - 归档时间有值</li>
     * </ul>
     * 在工单新增、更新时调用，保证 current_status 列与日期字段实时一致
     * </p>
     *
     * @param configDate        配置日期
     * @param configCompleteTime 配置完成日期
     * @param archiveTime       归档时间
     * @return 计算后的工单状态
     */
    private String resolveStatus(LocalDateTime configDate, LocalDateTime configCompleteTime,
            LocalDateTime archiveTime, LocalDateTime inspectionStart, LocalDateTime inspectionEnd) {
        if (archiveTime != null) {
            return "已归档";
        }
        if (inspectionStart != null && inspectionEnd != null) {
            return "已检验";
        }
        if (configCompleteTime != null) {
            return "已生产";
        }
        if (configDate != null) {
            return "生产中";
        }
        return "待生产";
    }

    // endregion

    // region 工序模板绑定
    // ===================================
    // 工序模板绑定
    // ===================================

    /**
     * 根据制剂ID拉取工序模板，转换为工单工序执行记录并批量保存
     * <p>
     * 在新增工单时自动调用，将制剂的工序模板绑定到工单的工序执行记录中
     * </p>
     *
     * @param workOrder 工单实体（需已保存，包含workOrderId）
     */
    private void bindProcessTemplates(WorkOrder workOrder) {
        List<PreparationProcessTemplate> templates =
                preparationProcessTemplateService.findByPreparationId(workOrder.getPreparationId());

        if (templates == null || templates.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = EntityUtils.getCurrentUserId();

        List<WorkOrderProcessExecution> executions = new ArrayList<>();
        for (PreparationProcessTemplate template : templates) {
            WorkOrderProcessExecution exec = new WorkOrderProcessExecution();
            exec.setWorkOrderId(workOrder.getWorkOrderId());
            exec.setProcessTypeId(template.getProcessTypeId());
            exec.setStepOrder(template.getStepOrder());
            exec.setProcessQty(template.getStandardQty());
            exec.setKeyProcessParams(template.getKeyProcessParams());
            exec.setRemark(template.getRemark());

            // 根据模板中的描述文本匹配配置室ID
            if (StringUtils.hasText(template.getRoomDesc())) {
                QueryWrapper<RoomInfo> roomWrapper = new QueryWrapper<>();
                roomWrapper.eq("room_name", template.getRoomDesc());
                roomWrapper.last("LIMIT 1");
                RoomInfo room = roomInfoService.getOne(roomWrapper);
                if (room != null) {
                    exec.setRoomId(room.getRoomId());
                }
            }

            // 根据模板中的描述文本匹配设备ID
            if (StringUtils.hasText(template.getEquipmentDesc())) {
                QueryWrapper<Equipment> eqWrapper = new QueryWrapper<>();
                eqWrapper.eq("equipment_name", template.getEquipmentDesc());
                eqWrapper.last("LIMIT 1");
                Equipment equipment = equipmentService.getOne(eqWrapper);
                if (equipment != null) {
                    exec.setEquipmentId(equipment.getEquipmentId());
                }
            }

            exec.setStatus("待执行");
            exec.setIsDeleted(0);
            exec.setVersion(1);
            exec.setCreatedBy(currentUserId);
            exec.setUpdatedBy(currentUserId);
            exec.setCreatedTime(now);
            exec.setUpdatedTime(now);
            executions.add(exec);
        }

        workOrderProcessExecutionService.batchSave(workOrder.getWorkOrderId(), executions);
    }

    // endregion

    // region 工单编号生成
    // ===================================
    // 工单编号生成
    // ===================================

    /**
     * 自动生成工单编号
     * <p>
     * 编号格式：GD + 年月日(8位) + 序号(4位)，例如：GD202512010001
     * 序号根据当天已有的最大编号自动递增
     * </p>
     *
     * @return 生成的唯一工单编号
     */
    @Override
    public String generateWorkOrderCode() {
        // 生成工单编号格式: GD + 年月日 + 4位序号，例如: GD202512010001
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "GD" + dateStr;
        
        // 查询当天最大的工单编号（绕过软删除，避免删除后编号重复）
        String lastCode = baseMapper.selectMaxCodeByPrefix(prefix);
        
        int nextSeq = 1;
        if (lastCode != null) {
            try {
                String seqPart = lastCode.substring(prefix.length());
                nextSeq = Integer.parseInt(seqPart) + 1;
            } catch (Exception e) {
                // 解析失败，使用默认序号1
                nextSeq = 1;
            }
        }
        
        return String.format("%s%04d", prefix, nextSeq);
    }
    
    // endregion
}
