package com.tonghui.erp.Service.businesshistory;

import com.tonghui.erp.Common.Constants.BizFileType;
import com.tonghui.erp.Common.Dto.Log.BusinessHistoryScope;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import com.tonghui.erp.Data.mapper.PreparationFormulaMapper;
import com.tonghui.erp.Data.mapper.PreparationMapper;
import com.tonghui.erp.Data.mapper.PreparationProcessTemplateMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 制剂信息变更历史 Provider
 * <p>
 * 制剂历史包含主表 preparation、处方明细 preparation_formula 与工序模版
 * preparation_process_template；处方行、工序模版的变更日志分别以 formula_id、
 * template_id 作为 business_id，因此需要先解析该制剂下的全部明细行（包含已删除记录），
 * 避免历史记录缺少名称。工序模版的附件变更从 file_operation_log 聚合
 * </p>
 */
@Component
public class PreparationHistoryProvider implements BusinessHistoryProvider {

    // region 依赖
    // ===================================
    // 依赖
    // ===================================

    private final PreparationMapper preparationMapper;
    private final PreparationFormulaMapper preparationFormulaMapper;
    private final PreparationProcessTemplateMapper processTemplateMapper;

    /**
     * 构造制剂变更历史 Provider
     *
     * @param preparationMapper        制剂Mapper
     * @param preparationFormulaMapper 处方Mapper
     * @param processTemplateMapper    工序模版Mapper
     */
    public PreparationHistoryProvider(PreparationMapper preparationMapper,
                                      PreparationFormulaMapper preparationFormulaMapper,
                                      PreparationProcessTemplateMapper processTemplateMapper) {
        this.preparationMapper = preparationMapper;
        this.preparationFormulaMapper = preparationFormulaMapper;
        this.processTemplateMapper = processTemplateMapper;
    }

    // endregion

    // region Provider 实现
    // ===================================
    // Provider 实现
    // ===================================

    /**
     * 业务类型
     *
     * @return PREPARATION
     */
    @Override
    public String bizType() {
        return "PREPARATION";
    }

    /**
     * 解析制剂的查询范围
     *
     * @param bizId 制剂ID
     * @return 查询范围
     */
    @Override
    public BusinessHistoryScope resolve(String bizId) {
        Long id = parseId(bizId);
        Preparation preparation = preparationMapper.selectById(id);
        List<PreparationFormula> formulas = preparationFormulaMapper.selectByPreparationIdIncludeDeleted(id);
        List<PreparationProcessTemplate> templates = processTemplateMapper.selectByPreparationIdIncludeDeleted(id);

        BusinessHistoryScope scope = new BusinessHistoryScope();
        scope.setTitle(buildTitle(id, preparation));
        scope.addTable("preparation", List.of(String.valueOf(id)));
        scope.addFileScope(BizFileType.PREPARATION.getCode(), String.valueOf(id), "preparation");

        if (formulas != null && !formulas.isEmpty()) {
            List<String> formulaIds = new ArrayList<>(formulas.size());
            for (PreparationFormula formula : formulas) {
                if (formula.getFormulaId() == null) {
                    continue;
                }
                String formulaId = String.valueOf(formula.getFormulaId());
                formulaIds.add(formulaId);
                scope.addRecordLabel("preparation_formula", formulaId, formula.getMaterialName());
            }
            scope.addTable("preparation_formula", formulaIds);
        }

        if (templates != null && !templates.isEmpty()) {
            List<String> templateIds = new ArrayList<>(templates.size());
            for (PreparationProcessTemplate template : templates) {
                if (template.getTemplateId() == null) {
                    continue;
                }
                String templateId = String.valueOf(template.getTemplateId());
                templateIds.add(templateId);
                scope.addRecordLabel("preparation_process_template", templateId, processLabel(template));
            }
            scope.addTable("preparation_process_template", templateIds);
            scope.addFileScope(BizFileType.PREPARATION_PROCESS_TEMPLATE.getCode(), templateIds,
                    "preparation_process_template");
        }
        return scope;
    }

    // endregion

    // region 辅助方法
    // ===================================
    // 辅助方法
    // ===================================

    /**
     * 解析业务ID
     *
     * @param bizId 业务ID字符串
     * @return 业务ID
     */
    private Long parseId(String bizId) {
        try {
            return Long.valueOf(bizId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("业务ID不合法：" + bizId);
        }
    }

    /**
     * 构建工序展示名称
     *
     * @param template 工序模版
     * @return 工序名称
     */
    private String processLabel(PreparationProcessTemplate template) {
        String name = template.getProcessTypeName();
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        return "工序 #" + template.getTemplateId();
    }

    /**
     * 构建业务对象展示名称
     *
     * @param id          制剂ID
     * @param preparation 制剂
     * @return 展示名称
     */
    private String buildTitle(Long id, Preparation preparation) {
        if (preparation == null) {
            return "制剂 #" + id;
        }
        String code = preparation.getPreparationCode() == null ? "" : preparation.getPreparationCode();
        String name = preparation.getPreparationName() == null ? "" : preparation.getPreparationName();
        if (code.isEmpty()) {
            return name.isEmpty() ? "制剂 #" + id : name;
        }
        return name.isEmpty() ? code : code + " · " + name;
    }

    // endregion
}
