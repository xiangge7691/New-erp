package com.tonghui.erp.Common.Dto.BusinessFile;

import com.tonghui.erp.Data.Entity.BusinessFile;
import com.tonghui.erp.Data.Entity.FileInfo;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务附件返回对象
 */
@Data
public class BusinessFileDto {

    // region 关联信息
    // ===================================
    // 关联信息
    // ===================================

    /** 关系ID（删除附件时使用） */
    private Long relationId;

    /** 业务类型编码 */
    private String bizType;

    /** 业务ID */
    private Long bizId;

    /** 子对象ID（审批节点ID等，无子对象时为0） */
    private Long refId;

    /** 文件ID */
    private Long fileId;

    /** 文件槽位 */
    private String fileKind;

    // endregion

    // region 文件信息
    // ===================================
    // 文件信息
    // ===================================

    /** 原始文件名 */
    private String originalName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 内容类型 */
    private String contentType;

    /** 扩展名 */
    private String fileExtension;

    /** 文件描述 */
    private String description;

    /** 下载地址 */
    private String fileUrl;

    /** 预览地址 */
    private String previewUrl;

    /** 上传时间 */
    private LocalDateTime createdTime;

    // endregion

    /**
     * 由关系与文件元数据构建返回对象
     *
     * @param relation 关系
     * @param file     文件元数据
     * @return 返回对象
     */
    public static BusinessFileDto from(BusinessFile relation, FileInfo file) {
        BusinessFileDto dto = new BusinessFileDto();
        dto.setRelationId(relation.getId());
        dto.setBizType(relation.getBizType());
        dto.setBizId(relation.getBizId());
        dto.setRefId(relation.getRefId());
        dto.setFileId(file.getFileId());
        dto.setFileKind(relation.getFileKind());
        dto.setOriginalName(file.getOriginalName());
        dto.setFileSize(file.getFileSize());
        dto.setContentType(file.getContentType());
        dto.setFileExtension(file.getFileExtension());
        dto.setDescription(file.getDescription());
        dto.setFileUrl(file.getFileUrl() != null ? file.getFileUrl() : "/api/files/" + file.getFileId());
        dto.setPreviewUrl("/api/files/preview/" + file.getFileId());
        dto.setCreatedTime(relation.getCreatedTime());
        return dto;
    }
}
