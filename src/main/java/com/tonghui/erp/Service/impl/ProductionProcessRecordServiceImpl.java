package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Data.Entity.ProductionProcessRecord;
import com.tonghui.erp.Service.ProductionProcessRecordService;
import com.tonghui.erp.Data.mapper.ProductionProcessRecordMapper;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产工序记录服务实现类
 * <p>
 * 针对表【production_process_record(生产工序记录表)】的数据库操作 Service 实现，提供生产工序记录的增删改查等业务逻辑的具体实现
 * </p>
 */
@Service
public class ProductionProcessRecordServiceImpl extends ServiceImpl<ProductionProcessRecordMapper, ProductionProcessRecord>
    implements ProductionProcessRecordService{

    //#region 生产工序记录查询实现方法
    // ===================================
    // 生产工序记录查询实现方法
    // ===================================

    /**
     * 根据生产计划 ID 查询工序记录列表
     *
     * @param planId 生产计划 ID
     * @return 工序记录列表，按工序顺序排序
     */
    
    @Override
    public List<ProductionProcessRecord> listByPlanId(Integer planId) {
        if (planId == null) {
            return List.of();
        }
        
        return this.lambdaQuery()
                .eq(ProductionProcessRecord::getPlanId, planId)
                .orderByAsc(ProductionProcessRecord::getStepOrder)
                .list();
    }

    /**
     * 根据生产计划 ID 分页查询工序记录
     *
     * @param planId 生产计划 ID
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    @Override
    public PagedResult<ProductionProcessRecord> listByPlanIdPaged(Integer planId, PageRequestDto pageRequest) {
        // 创建 Page 对象
        Page<ProductionProcessRecord> page;
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            page = new Page<>(1, 10000);
        } else {
            page = new Page<>(pageRequest.getPageIndex() + 1, pageRequest.getPageSize());
        }

        // 构建查询条件
        var query = this.lambdaQuery()
                .eq(ProductionProcessRecord::getPlanId, planId)
                .orderByAsc(ProductionProcessRecord::getStepOrder);

        Page<ProductionProcessRecord> resultPage = query.page(page);

        PagedResult<ProductionProcessRecord> pagedResult = new PagedResult<>();
        pagedResult.setItems(resultPage.getRecords());
        pagedResult.setTotalCount(resultPage.getTotal());

        // 处理分页信息
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            pagedResult.setPageIndex(0);
            if (resultPage.getTotal() > 0) {
                pagedResult.setPageSize((int) resultPage.getTotal());
            } else {
                pagedResult.setPageSize(0);
            }
        } else {
            pagedResult.setPageIndex((int) resultPage.getCurrent() - 1);
            pagedResult.setPageSize((int) resultPage.getSize());
        }

        return pagedResult;
    }

    /**
     * 根据记录状态查询工序记录（分页）
     *
     * @param recordStatus 记录状态（1-正常，0-作废）
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    @Override
    public PagedResult<ProductionProcessRecord> listByStatus(Integer recordStatus, PageRequestDto pageRequest) {
        // 创建 Page 对象
        Page<ProductionProcessRecord> page;
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            page = new Page<>(1, 10000);
        } else {
            page = new Page<>(pageRequest.getPageIndex() + 1, pageRequest.getPageSize());
        }

        // 构建查询条件
        var query = this.lambdaQuery();
        if (recordStatus != null) {
            query.eq(ProductionProcessRecord::getRecordStatus, recordStatus);
        }
        query.orderByDesc(ProductionProcessRecord::getCreatedTime);

        Page<ProductionProcessRecord> resultPage = query.page(page);

        PagedResult<ProductionProcessRecord> pagedResult = new PagedResult<>();
        pagedResult.setItems(resultPage.getRecords());
        pagedResult.setTotalCount(resultPage.getTotal());

        // 处理分页信息
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            pagedResult.setPageIndex(0);
            if (resultPage.getTotal() > 0) {
                pagedResult.setPageSize((int) resultPage.getTotal());
            } else {
                pagedResult.setPageSize(0);
            }
        } else {
            pagedResult.setPageIndex((int) resultPage.getCurrent() - 1);
            pagedResult.setPageSize((int) resultPage.getSize());
        }

        return pagedResult;
    }

    /**
     * 根据工序名称模糊查询（分页）
     *
     * @param processName 工序名称（模糊匹配）
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    @Override
    public PagedResult<ProductionProcessRecord> searchByProcessName(String processName, PageRequestDto pageRequest) {
        // 创建 Page 对象
        Page<ProductionProcessRecord> page;
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            page = new Page<>(1, 10000);
        } else {
            page = new Page<>(pageRequest.getPageIndex() + 1, pageRequest.getPageSize());
        }

        // 构建查询条件
        var query = this.lambdaQuery();
        
        if (processName != null && !processName.isEmpty()) {
            query.like(ProductionProcessRecord::getProcessName, processName);
        }
        query.orderByDesc(ProductionProcessRecord::getStartTime);

        Page<ProductionProcessRecord> resultPage = query.page(page);

        PagedResult<ProductionProcessRecord> pagedResult = new PagedResult<>();
        pagedResult.setItems(resultPage.getRecords());
        pagedResult.setTotalCount(resultPage.getTotal());

        // 处理分页信息
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            pagedResult.setPageIndex(0);
            if (resultPage.getTotal() > 0) {
                pagedResult.setPageSize((int) resultPage.getTotal());
            } else {
                pagedResult.setPageSize(0);
            }
        } else {
            pagedResult.setPageIndex((int) resultPage.getCurrent() - 1);
            pagedResult.setPageSize((int) resultPage.getSize());
        }

        return pagedResult;
    }

    /**
     * 根据操作人姓名模糊查询（分页）
     *
     * @param operatorName 操作人姓名（模糊匹配）
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    @Override
    public PagedResult<ProductionProcessRecord> searchByOperatorName(String operatorName, PageRequestDto pageRequest) {
        // 创建 Page 对象
        Page<ProductionProcessRecord> page;
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            page = new Page<>(1, 10000);
        } else {
            page = new Page<>(pageRequest.getPageIndex() + 1, pageRequest.getPageSize());
        }

        // 构建查询条件
        var query = this.lambdaQuery();
        
        if (operatorName != null && !operatorName.isEmpty()) {
            query.like(ProductionProcessRecord::getOperatorName, operatorName);
        }
        query.orderByDesc(ProductionProcessRecord::getStartTime);

        Page<ProductionProcessRecord> resultPage = query.page(page);

        PagedResult<ProductionProcessRecord> pagedResult = new PagedResult<>();
        pagedResult.setItems(resultPage.getRecords());
        pagedResult.setTotalCount(resultPage.getTotal());

        // 处理分页信息
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            pagedResult.setPageIndex(0);
            if (resultPage.getTotal() > 0) {
                pagedResult.setPageSize((int) resultPage.getTotal());
            } else {
                pagedResult.setPageSize(0);
            }
        } else {
            pagedResult.setPageIndex((int) resultPage.getCurrent() - 1);
            pagedResult.setPageSize((int) resultPage.getSize());
        }

        return pagedResult;
    }

    /**
     * 作废指定的工序记录
     *
     * @param recordId 记录 ID
     * @param updaterId 更新人 ID
     * @return 操作是否成功
     */
    @Override
    public boolean cancelRecord(Long recordId, Long updaterId) {
        if (recordId == null) {
            return false;
        }
        
        ProductionProcessRecord record = this.getById(recordId);
        if (record == null) {
            return false;
        }
        
        record.setRecordStatus(0);
        record.setUpdatedBy(updaterId);
        record.setUpdatedTime(LocalDateTime.now());
        
        return this.updateById(record);
    }

    //#endregion

    /**
     * 批量保存工序记录（先删后增）
     *
     * @param planId 生产计划ID
     * @param records 工序记录列表
     * @return 保存的记录列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ProductionProcessRecord> batchSaveByPlanId(Integer planId, List<ProductionProcessRecord> records) {
        // 删除该计划下已有的工序记录
        this.remove(new QueryWrapper<ProductionProcessRecord>().eq("plan_id", planId));

        // 设置公共字段
        Long currentUserId = EntityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        for (ProductionProcessRecord record : records) {
            record.setPlanId(planId);
            if (currentUserId != null) {
                record.setCreatedBy(currentUserId);
                record.setUpdatedBy(currentUserId);
            }
            record.setCreatedTime(now);
            record.setUpdatedTime(now);
        }

        // 批量插入
        this.saveBatch(records);
        return records;
    }
}




