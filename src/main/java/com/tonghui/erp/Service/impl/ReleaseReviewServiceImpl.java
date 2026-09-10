package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.ReleaseReview;
import com.tonghui.erp.Data.mapper.ReleaseReviewMapper;
import com.tonghui.erp.Service.ReleaseReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 审核放行服务实现类
 * <p>
 * 实现ReleaseReviewService接口，提供审核放行的编号生成与唯一性校验业务逻辑，
 * 编号生成通过对Mapper原生SQL查询绕过软删除过滤，保证编号不与已删除记录冲突
 * </p>
 */
@Service
public class ReleaseReviewServiceImpl extends ServiceImpl<ReleaseReviewMapper, ReleaseReview> implements ReleaseReviewService {

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
     * 生成放行编号
     *
     * @return 生成的唯一放行编号
     */
    @Override
    public String generateCode() {
        return sequenceService.generateReleaseCode();
    }

    /**
     * 校验放行编号是否唯一
     * <p>
     * 通过Mapper原生SQL统计包含软删除记录在内的全部记录，
     * 避免已软删除记录占用的编号被误判为可用导致唯一索引冲突
     * </p>
     *
     * @param code      放行编号
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 唯一返回true，否则返回false
     */
    @Override
    public boolean isCodeUnique(String code, Long excludeId) {
        return baseMapper.countByCodeIncludeDeleted(code, excludeId) == 0;
    }

    // endregion
}