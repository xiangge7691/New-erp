package com.tonghui.erp.Common.Dto;

import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileDto;
import lombok.Data;

import java.util.List;

/**
 * 制剂续展文件相关传输对象
 * <p>
 * 用于「续展文件」选项卡的分组查询、续展备注保存与 ZIP 打包下载。
 * </p>
 */
public class PreparationRenewalDtos {

    // region 分组查询
    // ===================================
    // 分组查询
    // ===================================

    /**
     * 单个续展文件类型分组
     */
    @Data
    public static class RenewalFileGroup {

        /** 槽位编码 */
        private String kind;

        /** 文件类型展示名称 */
        private String label;

        /** 该类型最大文件数（-1 表示不限） */
        private Integer maxCount;

        /** 该类型下的文件列表，无文件时为空数组 */
        private List<BusinessFileDto> files;
    }

    /**
     * 续展文件页数据（10 个分组 + 续展备注）
     */
    @Data
    public static class RenewalFiles {

        /** 制剂ID */
        private Long preparationId;

        /** 制剂品名 */
        private String preparationName;

        /** 续展备注 */
        private String renewalRemark;

        /** 10 个文件类型分组，按申报清单顺序排列 */
        private List<RenewalFileGroup> groups;
    }

    // endregion

    // region 备注保存
    // ===================================
    // 备注保存
    // ===================================

    /**
     * 续展备注保存请求
     */
    @Data
    public static class RenewalRemarkRequest {

        /** 续展备注（传空串表示清空） */
        private String renewalRemark;
    }

    // endregion

    // region 打包下载
    // ===================================
    // 打包下载
    // ===================================

    /**
     * 续展文件打包结果
     */
    @Data
    public static class RenewalExport {

        /** 下载文件名（{制剂名}续展文件.zip） */
        private String fileName;

        /** ZIP 字节内容 */
        private byte[] data;
    }

    // endregion
}
