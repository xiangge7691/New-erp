package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import com.tonghui.erp.Data.mapper.PreparationProcessTemplateMapper;
import com.tonghui.erp.Service.PreparationProcessTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

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
     * <p>使用事务保证数据一致性：先删除该制剂原有的工序模版，再批量插入新模版</p>
     *
     * @param preparationId 制剂ID
     * @param templates     工序模版列表，可为null或空列表（将清空所有模版）
     */
    @Override
    @Transactional
    public void batchSave(Long preparationId, List<PreparationProcessTemplate> templates) {
        // 删除该制剂原有的工序模版
        QueryWrapper<PreparationProcessTemplate> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("preparation_id", preparationId);
        remove(deleteWrapper);
        
        // 设置制剂ID并批量保存
        if (templates != null && !templates.isEmpty()) {
            for (PreparationProcessTemplate template : templates) {
                template.setPreparationId(preparationId);
            }
            saveBatch(templates);
        }
    }

    // endregion
}
