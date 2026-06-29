package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 制剂处方信息服务接口
 */
public interface PreparationFormulaService extends IService<PreparationFormula> {

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
     * 查询所有处方明细
     *
     * @return 处方明细集合
     */
    List<PreparationFormula> getAllFormulas();
}
