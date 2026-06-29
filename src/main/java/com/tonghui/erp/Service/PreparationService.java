package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Data.Entity.Preparation;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;

import java.util.List;

/**
 * 制剂信息业务接口
 */
public interface PreparationService extends IService<Preparation> {

    // #region 基础操作

    /**
     * 新增制剂
     *
     * @param preparation 制剂实体
     */
    void addPreparation(Preparation preparation);

    /**
     * 更新制剂
     *
     * @param preparation 制剂实体
     */
    void updatePreparation(Preparation preparation);

    /**
     * 删除制剂
     *
     * @param preparationId 制剂ID
     */
    void deletePreparation(Long preparationId);

    // #endregion

    // #region 查询操作

    /**
     * 根据ID查询制剂
     *
     * @param preparationId 制剂ID
     * @return 制剂实体
     */
    Preparation getPreparationById(Long preparationId);

    /**
     * 根据编码查询制剂
     *
     * @param preparationCode 制剂编码
     * @return 制剂实体
     */
    Preparation getPreparationByCode(String preparationCode);

    /**
     * 查询所有启用状态的制剂
     *
     * @return 制剂集合
     */
    List<Preparation> getEnabledPreparations();

    /**
     * 查询所有制剂
     *
     * @return 制剂集合
     */
    List<Preparation> getAllPreparations();

    // #endregion

    // #region 高级查询

    /**
     * 高级查询制剂（支持分页）
     *
     * @param preparation 查询条件
     * @param pageNum     页码
     * @param pageSize    每页大小
     * @return 分页结果
     */
    Page<Preparation> queryPreparations(Preparation preparation, int pageNum, int pageSize);

    // #endregion

    // #region 分页查询

    /**
     * 获取制剂列表（分页）
     *
     * @param pageRequestDto 分页请求参数
     * @return 分页结果
     */
    PagedResult<Preparation> getPreparationList(PageRequestDto pageRequestDto);

    // #endregion
}
