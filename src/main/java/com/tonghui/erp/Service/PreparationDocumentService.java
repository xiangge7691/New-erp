package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.PreparationDocument;
import java.util.List;

/**
 * 制剂文档服务接口
 */
public interface PreparationDocumentService extends IService<PreparationDocument> {

    /**
     * 根据制剂ID查询文档列表
     * @param preparationId 制剂ID
     * @return 文档列表
     */
    List<PreparationDocument> findByPreparationId(Long preparationId);

    /**
     * 根据文档类型查询文档列表
     * @param docType 文档类型
     * @return 文档列表
     */
    List<PreparationDocument> findByDocType(String docType);
}
