package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.FileRequirementConfig;

import java.util.List;

/**
 * 文件必传要求服务
 * <p>
 * 产品信息汇总的缺件统计和审核放行的文件完整性共用。
 * 规则优先级：品种级 > 被检品属性级 > 全局。
 * </p>
 */
public interface FileRequirementService extends IService<FileRequirementConfig> {

    /**
     * 查询全部启用的文件要求
     *
     * @return 文件要求列表
     */
    List<FileRequirementConfig> listAll();

    /**
     * 解析某个槽位的要求
     *
     * @param bizType         业务类型
     * @param fileKind        文件槽位
     * @param preparationCode 品种编码，可为空
     * @param itemCategory    被检品属性，可为空
     * @return REQUIRED/CONDITIONAL/OPTIONAL，未配置时返回 OPTIONAL
     */
    String resolveRequirement(String bizType, String fileKind, String preparationCode, String itemCategory);
}
