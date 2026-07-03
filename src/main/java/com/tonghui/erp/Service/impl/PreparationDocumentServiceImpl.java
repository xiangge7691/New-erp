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
 * <p>
 * 实现PreparationDocumentService接口，提供制剂文档相关的业务逻辑处理，
 * 包括根据制剂ID和文档类型查询文档列表等功能的具体实现
 * </p>
 *
 */
@Service
public class PreparationDocumentServiceImpl extends ServiceImpl<PreparationDocumentMapper, PreparationDocument> implements PreparationDocumentService {

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据制剂ID查询文档列表
     * <p>按创建时间降序排列，最新的文档排在前面</p>
     *
     * @param preparationId 制剂ID
     * @return 该制剂关联的所有文档列表
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
     * <p>按创建时间降序排列，最新的文档排在前面</p>
     *
     * @param docType 文档类型
     * @return 该文档类型下的所有文档列表
     */
    @Override
    public List<PreparationDocument> findByDocType(String docType) {
        QueryWrapper<PreparationDocument> wrapper = new QueryWrapper<>();
        wrapper.eq("doc_type", docType)
               .orderByDesc("created_time");
        return list(wrapper);
    }

    // endregion
}
