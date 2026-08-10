package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.ReleaseReview;

/**
 * 审核放行服务接口
 * <p>
 * 提供审核放行的基础CRUD、放行编号生成及编号唯一性校验等业务能力，
 * 用于质量检验模块的放行决策记录管理
 * </p>
 */
public interface ReleaseReviewService extends IService<ReleaseReview> {

    /**
     * 生成放行编号
     * <p>
     * 编号格式：FX-YYYYMMDD-NNN，自动生成当天最大序号的下一个编号
     * </p>
     *
     * @return 生成的唯一放行编号
     */
    String generateCode();

    /**
     * 校验放行编号是否唯一
     * <p>
     * 用于新增或修改时校验编号，排除指定ID自身的记录，避免修改时误判重复
     * </p>
     *
     * @param code     放行编号
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 唯一返回true，否则返回false
     */
    boolean isCodeUnique(String code, Long excludeId);
}