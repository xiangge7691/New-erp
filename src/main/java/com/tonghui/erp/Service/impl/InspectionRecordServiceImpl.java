package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.InspectionRecord;
import com.tonghui.erp.Data.Entity.InspectionRequest;
import com.tonghui.erp.Data.mapper.InspectionRecordMapper;
import com.tonghui.erp.Data.mapper.InspectionRequestMapper;
import com.tonghui.erp.Service.InspectionRecordService;
import com.tonghui.erp.Service.WorkOrderService;
import com.tonghui.erp.Common.utils.CodeUniqueChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 检验记录服务实现类
 * <p>
 * 实现InspectionRecordService接口，提供检验记录的编号生成、编号唯一性校验，
 * 以及成品检验完成后回填工单检验结束时间等业务逻辑，
 * 编号生成通过对Mapper原生SQL查询绕过软删除过滤，保证编号不与已删除记录冲突
 * </p>
 */
@Service
public class InspectionRecordServiceImpl extends ServiceImpl<InspectionRecordMapper, InspectionRecord> implements InspectionRecordService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 序列号生成服务
     */
    @Autowired
    private SequenceServiceImpl sequenceService;

    /**
     * 请检记录数据访问层（工单ID缺失时按请检编号回退查找）
     */
    @Autowired
    private InspectionRequestMapper inspectionRequestMapper;

    /**
     * 工单服务（用于回填检验结束时间并联动状态）
     */
    @Autowired
    private WorkOrderService workOrderService;

    // endregion

    // region 编号生成与校验
    // ===================================
    // 编号生成与校验
    // ===================================

    /**
     * 生成检验编号
     *
     * @return 生成的唯一检验编号
     */
    @Override
    public String generateCode() {
        return sequenceService.generateInspectionRecordCode();
    }

    /**
     * 校验检验编号是否唯一
     * <p>
     * 通过Mapper原生SQL统计包含软删除记录在内的全部记录，
     * 避免已软删除记录占用的编号被误判为可用导致唯一索引冲突
     * </p>
     *
     * @param code      检验编号
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 唯一返回true，否则返回false
     */
    @Override
    public boolean isCodeUnique(String code, Long excludeId) {
        return CodeUniqueChecker.isCodeUnique(code, excludeId, baseMapper::countByCodeIncludeDeleted);
    }

    // endregion

    // region 工单联动
    // ===================================
    // 工单联动
    // ===================================

    /**
     * 回填工单检验结束时间
     * <p>
     * 仅成品且结论合格的检验记录会推进工单检验状态；工单ID为空时通过关联请检单回退查找，
     * 与检验记录页面、检验任务看板两个入口保持同一口径
     * </p>
     *
     * @param record 检验记录（需包含 endTime、itemCategory、conclusion，workOrderId 可为空）
     */
    @Override
    public void syncWorkOrderInspectionEnd(InspectionRecord record) {
        if (record == null || record.getEndTime() == null) {
            return;
        }

        Long workOrderId = record.getWorkOrderId();
        String itemCategory = record.getItemCategory();

        // 工单ID缺失时通过关联请检单回退查找（兼容历史数据）
        if (workOrderId == null && StringUtils.hasText(record.getRelatedInspectionRequestCode())) {
            QueryWrapper<InspectionRequest> wrapper = new QueryWrapper<>();
            wrapper.eq("inspection_code", record.getRelatedInspectionRequestCode());
            wrapper.select("work_order_id", "item_category");
            wrapper.last("LIMIT 1");
            InspectionRequest request = inspectionRequestMapper.selectOne(wrapper);
            if (request != null) {
                workOrderId = request.getWorkOrderId();
                itemCategory = request.getItemCategory();
            }
        }

        // 仅成品且合格的检验记录才推进工单检验结束时间
        if (workOrderId != null && "成品".equals(itemCategory) && "合格".equals(record.getConclusion())) {
            workOrderService.syncWorkOrderTime(workOrderId, "inspectionEnd", record.getEndTime());
        }
    }

    // endregion
}