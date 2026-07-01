package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.PreparationDocument;
import com.tonghui.erp.Data.mapper.PreparationDocumentMapper;
import com.tonghui.erp.Service.PreparationDocumentService;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 制剂文档服务实现类
 */
@Service
public class PreparationDocumentServiceImpl extends ServiceImpl<PreparationDocumentMapper, PreparationDocument> implements PreparationDocumentService {

    /**
     * 根据制剂ID查询文档列表
     * 按创建时间降序排列
     */
    @Override
    public List<PreparationDocument> findByPreparationId(Long preparationId) {
        QueryWrapper<PreparationDocument> wrapper = new QueryWrapper<>();
        wrapper.eq("preparation_id", preparationId)
               .orderByDesc("created_time");
        return list(wrapper);
    }

    /**
     * 根据文档类型查询文档列表
     * 按创建时间降序排列
     */
    @Override
    public List<PreparationDocument> findByDocType(String docType) {
        QueryWrapper<PreparationDocument> wrapper = new QueryWrapper<>();
        wrapper.eq("doc_type", docType)
               .orderByDesc("created_time");
        return list(wrapper);
    }
}
