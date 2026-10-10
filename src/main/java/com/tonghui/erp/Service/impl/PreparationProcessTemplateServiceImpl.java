package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import com.tonghui.erp.Data.mapper.PreparationProcessTemplateMapper;
import com.tonghui.erp.Service.PreparationProcessTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 制剂工序模版服务实现类
 * <p>
 * 实现PreparationProcessTemplateService接口，提供制剂工序模版相关的业务逻辑处理，
 * 包括根据制剂ID查询模版列表、批量保存模版等功能的具体实现
 * </p>
 *
 */
@Service
public class PreparationProcessTemplateServiceImpl extends ServiceImpl<PreparationProcessTemplateMapper, PreparationProcessTemplate> implements PreparationProcessTemplateService {

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据制剂ID查询工序模版列表
     * <p>按工序顺序升序排列</p>
     *
     * @param preparationId 制剂ID
     * @return 该制剂关联的所有工序模版列表
     */
    @Override
    public List<PreparationProcessTemplate> findByPreparationId(Long preparationId) {
        QueryWrapper<PreparationProcessTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq("preparation_id", preparationId)
               .orderByAsc("step_order");
        return list(wrapper);
    }

    // endregion

    // region 批量操作
    // ===================================
    // 批量操作
    // ===================================

    /**
     * 批量保存工序模版
     * <p>
     * 使用事务保证数据一致性。为支持按行附件（file_info.business_id = template_id），
     * 采用原地更新保留ID的策略：入参中携带 templateId 且属于该制剂的已有行执行更新（ID不变），
     * 无 templateId 的行新增，数据库中未被入参覆盖的既有行被删除。
     * </p>
     *
     * @param preparationId 制剂ID
     * @param templates     工序模版列表（已有行须携带原 templateId，可为null或空列表将清空所有模版）
     */
    @Override
    @Transactional
    public void batchSave(Long preparationId, List<PreparationProcessTemplate> templates) {
        // 查询该制剂现有模版（用于保留ID与计算被移除行）
        Map<Long, PreparationProcessTemplate> existingMap = findByPreparationId(preparationId).stream()
                .collect(Collectors.toMap(PreparationProcessTemplate::getTemplateId, t -> t, (a, b) -> a));
        Set<Long> incomingIds = new HashSet<>();
        List<PreparationProcessTemplate> toInsert = new ArrayList<>();

        if (templates != null && !templates.isEmpty()) {
            for (PreparationProcessTemplate template : templates) {
                template.setPreparationId(preparationId);
                // 已有行：按 ID 原地更新，保留 template_id（附件可继续按行定位）
                if (template.getTemplateId() != null && existingMap.containsKey(template.getTemplateId())) {
                    incomingIds.add(template.getTemplateId());
                    updateById(template);
                } else {
                    // 新增行：清空 ID 交由自增生成
                    template.setTemplateId(null);
                    toInsert.add(template);
                }
            }
            if (!toInsert.isEmpty()) {
                saveBatch(toInsert);
            }
        }

        // 被移除的既有行物理删除（入参中不存在的行）
        List<Long> removedIds = existingMap.keySet().stream()
                .filter(id -> !incomingIds.contains(id))
                .collect(Collectors.toList());
        if (!removedIds.isEmpty()) {
            baseMapper.physicalDeleteByIds(removedIds);
        }
    }

    // endregion
}
