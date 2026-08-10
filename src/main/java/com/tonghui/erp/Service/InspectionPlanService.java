package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.InspectionPlan;

/**
 * 检验计划服务接口
 * <p>
 * 提供检验计划的基础CRUD、计划编号生成及编号唯一性校验等业务能力，
 * 用于质量检验模块的检验计划手动排程管理
 * </p>
 */
public interface InspectionPlanService extends IService<InspectionPlan> {

    /**
     * 生成计划编号
     * <p>
     * 编号格式：JH-YYYYMMDD-NNN，自动生成当天最大序号的下一个编号，
     * 若传入自定义编号则不重复生成
     * </p>
     *
     * @return 生成的唯一计划编号
     */
    String generateCode();

    /**
     * 校验计划编号是否唯一
     * <p>
     * 用于新增或修改时校验编号，排除指定ID自身的记录，避免修改时误判重复
     * </p>
     *
     * @param code     计划编号
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 唯一返回true，否则返回false
     */
    boolean isCodeUnique(String code, Long excludeId);
}