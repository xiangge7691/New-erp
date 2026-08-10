package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.InspectionPlan;
import com.tonghui.erp.Data.mapper.InspectionPlanMapper;
import com.tonghui.erp.Service.InspectionPlanService;
import com.tonghui.erp.Service.impl.SequenceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 检验计划服务实现类
 * <p>
 * 实现InspectionPlanService接口，提供检验计划的编号生成与唯一性校验业务逻辑，
 * 编号生成通过对Mapper原生SQL查询绕过软删除过滤，保证编号不与已删除记录冲突
 * </p>
 */
@Service
public class InspectionPlanServiceImpl extends ServiceImpl<InspectionPlanMapper, InspectionPlan> implements InspectionPlanService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 序列号生成服务
     */
    @Autowired
    private SequenceServiceImpl sequenceService;

    // endregion

    // region 编号生成与校验
    // ===================================
    // 编号生成与校验
    // ===================================

    /**
     * 生成计划编号
     *
     * @return 生成的唯一计划编号
     */
    @Override
    public String generateCode() {
        return sequenceService.generateInspectionPlanCode();
    }

    /**
     * 校验计划编号是否唯一
     *
     * @param code      计划编号
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 唯一返回true，否则返回false
     */
    @Override
    public boolean isCodeUnique(String code, Long excludeId) {
        QueryWrapper<InspectionPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("plan_code", code)
                .eq("is_deleted", 0);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        return count(wrapper) == 0;
    }

    // endregion
}