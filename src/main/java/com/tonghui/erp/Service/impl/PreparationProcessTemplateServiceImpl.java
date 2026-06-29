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
 */
@Service
public class PreparationProcessTemplateServiceImpl extends ServiceImpl<PreparationProcessTemplateMapper, PreparationProcessTemplate> implements PreparationProcessTemplateService {

    /**
     * 根据制剂ID查询工序模版列表
     * 按工序顺序升序排列
     */
    @Override
    public List<PreparationProcessTemplate> findByPreparationId(Long preparationId) {
        QueryWrapper<PreparationProcessTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq("preparation_id", preparationId)
               .orderByAsc("step_order");
        return list(wrapper);
    }

    /**
     * 批量保存工序模版
     * 使用事务保证数据一致性：先删除原有模版，再批量插入新模版
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
}
