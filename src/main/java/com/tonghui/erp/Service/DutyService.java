package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.Duty;

/**
 * 职务信息服务接口
 * <p>
 * 提供职务信息的增删改查、职务编码唯一性校验及分页查询能力
 * </p>
 */
public interface DutyService extends IService<Duty> {

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
    boolean isCodeUnique(String code, Long excludeId);

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
    Page<Duty> queryDuties(String keyword, Integer status, int pageIndex, int pageSize);

    // endregion
}