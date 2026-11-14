package com.tonghui.erp.Service.impl;

import com.tonghui.erp.Common.Constants.BizFileType;
import com.tonghui.erp.Common.Constants.PreparationRenewalKind;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileDto;
import com.tonghui.erp.Common.Dto.PreparationRenewalDtos;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Service.BusinessFileService;
import com.tonghui.erp.Service.FileInfoService;
import com.tonghui.erp.Service.PreparationRenewalService;
import com.tonghui.erp.Service.PreparationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 制剂续展文件服务实现
 * <p>
 * 续展文件复用业务附件体系：bizType 固定为 PREPARATION_RENEWAL，bizId 为制剂ID，
 * refId 固定为 0，fileKind 为 {@link PreparationRenewalKind} 中的槽位编码。
 * 本类只负责分组查询、备注保存与 ZIP 打包，上传/删除仍走 BusinessFileService。
 * </p>
 */
@Service
public class PreparationRenewalServiceImpl implements PreparationRenewalService {

    // region 常量与依赖
    // ===================================
    // 常量与依赖
    // ===================================

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(PreparationRenewalServiceImpl.class);

    /** 续展文件业务类型编码 */
    private static final String BIZ_TYPE = BizFileType.PREPARATION_RENEWAL.getCode();

    /** 续展备注最大长度 */
    private static final int MAX_REMARK_LENGTH = 1000;

    /** 单次打包最大文件数 */
    private static final int MAX_EXPORT_FILES = 500;

    /** 单次打包最大字节数（500MB） */
    private static final long MAX_EXPORT_BYTES = 500L * 1024 * 1024;

    /** 制剂服务 */
    @Autowired
    private PreparationService preparationService;

    /** 业务附件服务 */
    @Autowired
    private BusinessFileService businessFileService;

    /** 文件信息服务 */
    @Autowired
    private FileInfoService fileInfoService;

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 查询制剂的续展文件（固定返回 10 个分组，空分组也返回）
     */
    @Override
    public PreparationRenewalDtos.RenewalFiles getRenewalFiles(Long preparationId) {
        Preparation preparation = requirePreparation(preparationId);
        Map<String, List<BusinessFileDto>> grouped = loadGroupedFiles(preparationId);

        List<PreparationRenewalDtos.RenewalFileGroup> groups = new ArrayList<>();
        for (PreparationRenewalKind kind : PreparationRenewalKind.ordered()) {
            PreparationRenewalDtos.RenewalFileGroup group = new PreparationRenewalDtos.RenewalFileGroup();
            group.setKind(kind.getCode());
            group.setLabel(kind.getLabel());
            group.setMaxCount(BizFileType.PREPARATION_RENEWAL.maxCount(kind.getCode()));
            group.setFiles(grouped.getOrDefault(kind.getCode(), new ArrayList<>()));
            groups.add(group);
        }

        PreparationRenewalDtos.RenewalFiles result = new PreparationRenewalDtos.RenewalFiles();
        result.setPreparationId(preparation.getPreparationId());
        result.setPreparationName(preparation.getPreparationName());
        result.setRenewalRemark(preparation.getRenewalRemark());
        result.setGroups(groups);
        return result;
    }

    // endregion

    // region 备注
    // ===================================
    // 备注
    // ===================================

    /**
     * 保存续展备注（仅更新该字段）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRenewalRemark(Long preparationId, String renewalRemark) {
        requirePreparation(preparationId);
        String remark = renewalRemark == null ? "" : renewalRemark;
        if (remark.length() > MAX_REMARK_LENGTH) {
            throw new IllegalArgumentException("续展备注不能超过 " + MAX_REMARK_LENGTH + " 个字符");
        }
        // 仅更新续展备注，避免整表单覆盖；空串用于清空备注
        Preparation patch = new Preparation();
        patch.setPreparationId(preparationId);
        patch.setRenewalRemark(remark);
        preparationService.updateById(patch);
    }

    // endregion

    // region 打包下载
    // ===================================
    // 打包下载
    // ===================================

    /**
     * 打包下载续展文件
     */
    @Override
    public PreparationRenewalDtos.RenewalExport exportRenewalFiles(Long preparationId) {
        Preparation preparation = requirePreparation(preparationId);
        Map<String, List<BusinessFileDto>> grouped = loadGroupedFiles(preparationId);

        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            int fileCount = 0;
            long totalSize = 0L;

            // 按申报清单顺序写入 10 个文件夹，空文件夹也写目录项
            for (PreparationRenewalKind kind : PreparationRenewalKind.ordered()) {
                zip.putNextEntry(new ZipEntry(kind.getZipFolder() + "/"));
                zip.closeEntry();

                Set<String> usedNames = new HashSet<>();
                for (BusinessFileDto file : grouped.getOrDefault(kind.getCode(), new ArrayList<>())) {
                    FileInfo info = file.getFileId() == null ? null : fileInfoService.getById(file.getFileId());
                    if (info == null || !StringUtils.hasText(info.getFilePath())) {
                        log.warn("续展文件元数据缺失，跳过打包: preparationId={}, fileId={}", preparationId, file.getFileId());
                        continue;
                    }
                    Path path = Paths.get(info.getFilePath());
                    if (!Files.exists(path) || !Files.isRegularFile(path)) {
                        log.warn("续展文件磁盘文件不存在，跳过打包: preparationId={}, fileId={}, path={}",
                                preparationId, file.getFileId(), info.getFilePath());
                        continue;
                    }

                    fileCount++;
                    if (fileCount > MAX_EXPORT_FILES) {
                        throw new IllegalArgumentException("续展文件超过 " + MAX_EXPORT_FILES + " 个，请分批下载");
                    }
                    totalSize += Files.size(path);
                    if (totalSize > MAX_EXPORT_BYTES) {
                        throw new IllegalArgumentException("续展文件超过 500MB，请分批下载");
                    }

                    String entryName = uniqueEntryName(safeFileName(info.getOriginalName(), file.getFileId()), usedNames);
                    zip.putNextEntry(new ZipEntry(kind.getZipFolder() + "/" + entryName));
                    Files.copy(path, zip);
                    zip.closeEntry();
                }
            }

            zip.finish();
            PreparationRenewalDtos.RenewalExport export = new PreparationRenewalDtos.RenewalExport();
            export.setFileName(buildZipFileName(preparation));
            export.setData(bytes.toByteArray());
            return export;
        } catch (IOException e) {
            throw new IllegalStateException("导出续展文件失败: " + e.getMessage(), e);
        }
    }

    // endregion

    // region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================

    /**
     * 校验制剂存在并返回制剂信息
     *
     * @param preparationId 制剂ID
     * @return 制剂实体
     */
    private Preparation requirePreparation(Long preparationId) {
        if (preparationId == null || preparationId <= 0) {
            throw new IllegalArgumentException("制剂ID不能为空");
        }
        Preparation preparation = preparationService.getPreparationById(preparationId);
        if (preparation == null) {
            throw new IllegalArgumentException("制剂不存在");
        }
        return preparation;
    }

    /**
     * 查询续展文件并按槽位分组（保持附件原有的排序）
     *
     * @param preparationId 制剂ID
     * @return 槽位编码 -> 文件列表
     */
    private Map<String, List<BusinessFileDto>> loadGroupedFiles(Long preparationId) {
        List<BusinessFileDto> files = businessFileService.list(BIZ_TYPE, preparationId, 0L, null);
        return files.stream().collect(Collectors.groupingBy(
                BusinessFileDto::getFileKind, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * 构建压缩包文件名：{制剂名}续展文件.zip
     *
     * @param preparation 制剂
     * @return 安全文件名
     */
    private String buildZipFileName(Preparation preparation) {
        String rawName = preparation.getPreparationName();
        String base;
        if (!StringUtils.hasText(rawName)) {
            base = "制剂" + preparation.getPreparationId();
        } else {
            base = safeFileName(rawName, preparation.getPreparationId());
        }
        if (base.endsWith(".zip")) {
            base = base.substring(0, base.length() - 4);
        }
        // 控制文件名长度，避免超出文件系统限制
        if (base.length() > 100) {
            base = base.substring(0, 100);
        }
        return base + "续展文件.zip";
    }

    /**
     * 清理文件名中的非法字符，防止 ZIP 条目路径穿越
     *
     * @param name   原始文件名
     * @param fileId 文件ID（为空时用于兜底命名）
     * @return 安全文件名
     */
    private String safeFileName(String name, Long fileId) {
        String fallback = fileId == null ? "未命名文件" : "文件" + fileId;
        String safe = StringUtils.hasText(name)
                ? name.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_").trim()
                : fallback;
        if (!StringUtils.hasText(safe) || ".".equals(safe) || "..".equals(safe)) {
            return fallback;
        }
        return safe;
    }

    /**
     * 同目录内重名文件追加序号，避免 ZIP 条目覆盖
     *
     * @param name      文件名
     * @param usedNames 已使用的文件名集合
     * @return 唯一文件名
     */
    private String uniqueEntryName(String name, Set<String> usedNames) {
        if (usedNames.add(name)) {
            return name;
        }
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String extension = dot > 0 ? name.substring(dot) : "";
        for (int index = 1; ; index++) {
            String candidate = base + " (" + index + ")" + extension;
            if (usedNames.add(candidate)) {
                return candidate;
            }
        }
    }

    // endregion
}
