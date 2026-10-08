package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.utils.CodeUniqueChecker;
import com.tonghui.erp.Data.Entity.Duty;
import com.tonghui.erp.Data.mapper.DutyMapper;
import com.tonghui.erp.Service.DutyService;
import org.springframework.stereotype.Service;

/**
 * 职务信息服务实现类
 * <p>
 * 实现DutyService接口，提供职务信息的编码唯一性校验及分页查询业务逻辑，
 * 职务编码通过对Mapper原生SQL查询绕过软删除过滤，保证编码不与已删除记录冲突
 * </p>
 */
@Service
public class DutyServiceImpl extends ServiceImpl<DutyMapper, Duty> implements DutyService {

    // region 编码校验
    // ===================================
    // 编码校验
    // ===================================

    /**
     * 校验职务编码是否唯一
     * <p>
     * 通过Mapper原生SQL统计包含软删除记录在内的全部记录，
     * 避免已软删除记录占用的编码被误判为可用导致唯一索引冲突
     * </p>
     *
     * @param code      职务编码
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 唯一返回true，否则返回false
     */
    @Override
    public boolean isCodeUnique(String code, Long excludeId) {
        return CodeUniqueChecker.isCodeUnique(code, excludeId, baseMapper::countByCodeIncludeDeleted);
    }

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 分页查询职务列表
     * <p>
     * 支持按关键字模糊匹配职务编码/名称/描述，支持状态筛选，按排序号升序排列
     * </p>
     *
     * @param keyword   关键字（模糊匹配职务编码/名称/描述，可选）
     * @param status    状态（1启用/0停用，可选）
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页数量
     * @return 职务分页结果
     */
    @Override
    public Page<Duty> queryDuties(String keyword, Integer status, int pageIndex, int pageSize) {
        QueryWrapper<Duty> wrapper = new QueryWrapper<>();
        // 关键字：职务编码/名称/描述 模糊匹配
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like("duty_code", keyword)
                    .or().like("duty_name", keyword)
                    .or().like("duty_desc", keyword));
        }
        // 状态筛选
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByAsc("sort_order");

        Page<Duty> page = new Page<>(pageIndex + 1, pageSize);
        return baseMapper.selectPage(page, wrapper);
    }

    // endregion
}