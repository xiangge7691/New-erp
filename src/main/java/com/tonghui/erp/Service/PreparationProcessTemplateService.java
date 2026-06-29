package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import java.util.List;

/**
 * 制剂工序模版服务接口
 */
public interface PreparationProcessTemplateService extends IService<PreparationProcessTemplate> {

    /**
     * 根据制剂ID查询工序模版列表
     * @param preparationId 制剂ID
     * @return 工序模版列表（按工序顺序排序）
     */
    List<PreparationProcessTemplate> findByPreparationId(Long preparationId);

    /**
     * 批量保存工序模版
     * 先删除原有模版，再批量插入新模版
     * @param preparationId 制剂ID
     * @param templates 工序模版列表
     */
    void batchSave(Long preparationId, List<PreparationProcessTemplate> templates);
}
