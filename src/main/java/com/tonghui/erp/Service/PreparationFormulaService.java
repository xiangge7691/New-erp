package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Preparation.PreparationFormulaStockDto;
import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 制剂处方信息服务接口
 */
public interface PreparationFormulaService extends IService<PreparationFormula> {

    // region 基础操作
    // ===================================
    // 基础操作
    // ===================================

    /**
     * 新增处方明细
     *
     * @param formula 处方明细实体
     */
    void addFormula(PreparationFormula formula);

    /**
     * 更新处方明细
     *
     * @param formula 处方明细实体
     */
    void updateFormula(PreparationFormula formula);

    /**
     * 删除处方明细
     *
     * @param formulaId 处方明细ID
     */
    void deleteFormula(Long formulaId);

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据ID查询处方明细
     *
     * @param formulaId 处方明细ID
     * @return 处方明细实体
     */
    PreparationFormula getFormulaById(Long formulaId);

    /**
     * 根据制剂编码查询所有处方明细
     *
     * @param preparationCode 制剂编码
     * @return 处方明细集合
     */
    List<PreparationFormula> getFormulasByPreparationCode(String preparationCode);

    /**
     * 根据制剂查询处方明细（含库存汇总数量）
     * <p>
     * 通过制剂ID或制剂编码定位制剂（二选一），返回处方明细列表，
     * 每条明细附带该物料在库存中的汇总数量（按物料编码汇总所有批次与生产单位）
     * </p>
     *
     * @param preparationId   制剂ID（可为空）
     * @param preparationCode 制剂编码（可为空，与preparationId至少提供一个）
     * @return 处方明细（含库存汇总数量）列表
     */
    List<PreparationFormulaStockDto> getFormulasByPreparationWithStock(Long preparationId, String preparationCode);

    /**
     * 查询所有处方明细
     *
     * @return 全部处方明细的集合
     */
    List<PreparationFormula> getAllFormulas();

    // endregion

    // region 批量操作
    // ===================================
    // 批量操作
    // ===================================

    /**
     * 批量保存处方明细
     * <p>先删除该制剂原有的处方，再批量插入新处方</p>
     *
     * @param preparationId 制剂ID
     * @param formulas      处方明细列表
     */
    void batchSave(Long preparationId, List<PreparationFormula> formulas);

    // endregion
}
