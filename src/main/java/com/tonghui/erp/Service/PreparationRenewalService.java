package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.PreparationRenewalDtos;

/**
 * 制剂续展文件服务
 * <p>
 * 续展文件通过业务附件体系存储（bizType = PREPARATION_RENEWAL），
 * 本服务负责按 10 类文件分组查询、续展备注保存与 ZIP 打包下载。
 * </p>
 */
public interface PreparationRenewalService {

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 查询制剂的续展文件（固定返回 10 个分组，空分组也返回）
     *
     * @param preparationId 制剂ID
     * @return 续展文件分组与续展备注
     * @throws IllegalArgumentException 制剂不存在
     */
    PreparationRenewalDtos.RenewalFiles getRenewalFiles(Long preparationId);

    // endregion

    // region 备注
    // ===================================
    // 备注
    // ===================================

    /**
     * 保存续展备注（仅更新该字段）
     *
     * @param preparationId 制剂ID
     * @param renewalRemark 续展备注（空串表示清空，最长 1000 字符）
     * @throws IllegalArgumentException 制剂不存在或备注超长
     */
    void updateRenewalRemark(Long preparationId, String renewalRemark);

    // endregion

    // region 打包下载
    // ===================================
    // 打包下载
    // ===================================

    /**
     * 打包下载续展文件
     * <p>
     * 压缩包名为 {制剂名}续展文件.zip，包内固定包含 10 个文件类型文件夹（空类型也建目录）。
     * </p>
     *
     * @param preparationId 制剂ID
     * @return 文件名与 ZIP 内容
     * @throws IllegalArgumentException 制剂不存在、文件数量或大小超限
     */
    PreparationRenewalDtos.RenewalExport exportRenewalFiles(Long preparationId);

    // endregion
}
