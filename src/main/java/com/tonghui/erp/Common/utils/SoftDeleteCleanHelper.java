package com.tonghui.erp.Common.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Component;

/**
 * 软删除统一清理工具类
 * <p>
 * 解决软删除带来的两类问题：
 * 1. 唯一约束冲突 - 新增时唯一字段与已软删除记录冲突
 * 2. 外键约束冲突 - 物理删除父表时被子表外键阻塞
 * </p>
 */
@Component
public class SoftDeleteCleanHelper {

    /**
     * 按唯一字段清理已软删除的记录
     * <p>物理删除指定表中 is_deleted=1 且唯一字段匹配的记录</p>
     *
     * @param mapper      Mapper实例
     * @param uniqueField 唯一字段名
     * @param uniqueValue 唯一字段值
     * @return 删除的记录数
     */
    @SuppressWarnings("unchecked")
    public <T> int cleanByUniqueField(BaseMapper<T> mapper, String uniqueField, Object uniqueValue) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        wrapper.eq(uniqueField, uniqueValue);
        wrapper.eq("is_deleted", 1);
        return mapper.delete(wrapper);
    }

    /**
     * 按外键字段清理子表记录
     * <p>物理删除子表中所有引用指定父表ID的记录（不区分软删除状态）</p>
     *
     * @param mapper    子表Mapper实例
     * @param foreignKey 外键字段名
     * @param parentId   父表ID
     * @return 删除的记录数
     */
    @SuppressWarnings("unchecked")
    public <T> int cleanChildRecords(BaseMapper<T> mapper, String foreignKey, Object parentId) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        wrapper.eq(foreignKey, parentId);
        return mapper.delete(wrapper);
    }
}
