package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Service.PreparationService;
import com.tonghui.erp.Service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

        // 获取当前用户ID
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            workOrder.setCreatedBy(currentUserId);
            workOrder.setUpdatedBy(currentUserId);
        }

        return this.save(workOrder);
    }

    /**
     * 更新工单
     * <p>自动更新更新时间和更新人信息</p>
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

        return this.updateById(workOrder);
    }

    /**
     * 删除工单
     *
     * @param workOrderId 工单ID
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean deleteWorkOrder(Long workOrderId) {
        return this.removeById(workOrderId);
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
     *
     * @param workOrder 查询条件实体，非null字段将作为等值或模糊查询条件
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
                                           LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                           LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                           int pageNum, int pageSize) {
        int actualPageNum = pageNum + 1;

        Page<WorkOrder> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<>();

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

        return this.page(page, wrapper);
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
        
        // 查询当天最大的工单编号
        QueryWrapper<WorkOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.likeRight("work_order_code", prefix);
        queryWrapper.orderByDesc("work_order_code");
        queryWrapper.last("LIMIT 1");
        
        WorkOrder lastWorkOrder = this.getOne(queryWrapper);
        
        int nextSeq = 1;
        if (lastWorkOrder != null && lastWorkOrder.getWorkOrderCode() != null) {
            try {
                String lastCode = lastWorkOrder.getWorkOrderCode();
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
