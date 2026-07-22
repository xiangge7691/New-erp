package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.PreparationDocument;
import java.util.List;

/**
 * 制剂文档服务接口
 */
public interface PreparationDocumentService extends IService<PreparationDocument> {

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据制剂ID查询文档列表
     * @param preparationId 制剂ID
     * @return 文档列表
     */
    List<PreparationDocument> findByPreparationId(Long preparationId);

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据文档类型查询文档列表
     * @param docType 文档类型
     * @return 文档列表
     */
    List<PreparationDocument> findByDocType(String docType);

    // endregion

    // region 批量操作
    // ===================================
    // 批量操作
    // ===================================

    /**
     * 批量保存文档
     * <p>先删除该制剂原有的文档，再批量插入新文档</p>
     *
     * @param preparationId 制剂ID
     * @param documents     文档列表
     */
    void batchSave(Long preparationId, List<PreparationDocument> documents);

    // endregion
}
