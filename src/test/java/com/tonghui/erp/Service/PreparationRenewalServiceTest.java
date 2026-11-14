package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Constants.PreparationRenewalKind;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileDto;
import com.tonghui.erp.Common.Dto.PreparationRenewalDtos;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Service.impl.PreparationRenewalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

/**
 * 制剂续展文件服务单元测试
 * <p>
 * 覆盖：10 个分组固定返回、ZIP 固定 10 个文件夹、重名文件自动加序号、
 * 磁盘文件缺失时跳过、续展备注长度校验。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class PreparationRenewalServiceTest {

    // region 测试依赖
    // ===================================
    // 测试依赖
    // ===================================

    /** 续展文件业务类型编码 */
    private static final String BIZ_TYPE = "PREPARATION_RENEWAL";

    /** 测试制剂ID */
    private static final Long PREPARATION_ID = 12L;

    @Mock
    private PreparationService preparationService;

    @Mock
    private BusinessFileService businessFileService;

    @Mock
    private FileInfoService fileInfoService;

    @InjectMocks
    private PreparationRenewalServiceImpl preparationRenewalService;

    /** 测试临时目录 */
    @TempDir
    Path tempDir;

    /** 测试制剂 */
    private Preparation preparation;

    // endregion

    // region 测试初始化
    // ===================================
    // 测试初始化
    // ===================================

    /**
     * 构造测试制剂
     */
    @BeforeEach
    void setUp() {
        preparation = new Preparation();
        preparation.setPreparationId(PREPARATION_ID);
        preparation.setPreparationName("感冒灵颗粒");
        preparation.setRenewalRemark("2027-03 到期，已启动续展");
    }

    // endregion

    // region 分组查询
    // ===================================
    // 分组查询
    // ===================================

    /**
     * 分组查询应固定返回 10 组，并按申报清单顺序排列
     */
    @Test
    void getRenewalFilesShouldReturnAllTenGroupsInOrder() {
        when(preparationService.getPreparationById(PREPARATION_ID)).thenReturn(preparation);
        when(businessFileService.list(eq(BIZ_TYPE), eq(PREPARATION_ID), eq(0L), isNull()))
                .thenReturn(List.of(businessFile(PreparationRenewalKind.APPLICATION_FORM, 1L, "备案申请表.pdf")));

        PreparationRenewalDtos.RenewalFiles result = preparationRenewalService.getRenewalFiles(PREPARATION_ID);

        assertEquals(PREPARATION_ID, result.getPreparationId());
        assertEquals("感冒灵颗粒", result.getPreparationName());
        assertEquals("2027-03 到期，已启动续展", result.getRenewalRemark());
        assertEquals(PreparationRenewalKind.values().length, result.getGroups().size());
        assertEquals(PreparationRenewalKind.APPLICATION_FORM.getCode(), result.getGroups().get(0).getKind());
        assertEquals(PreparationRenewalKind.APPLICATION_FORM.getLabel(), result.getGroups().get(0).getLabel());
        assertEquals(1, result.getGroups().get(0).getFiles().size());
        assertEquals(PreparationRenewalKind.OTHER.getCode(),
                result.getGroups().get(result.getGroups().size() - 1).getKind());
        assertTrue(result.getGroups().get(1).getFiles().isEmpty(), "无文件的类型应返回空数组");
    }

    /**
     * 制剂不存在时应抛出业务异常
     */
    @Test
    void getRenewalFilesShouldRejectMissingPreparation() {
        when(preparationService.getPreparationById(PREPARATION_ID)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> preparationRenewalService.getRenewalFiles(PREPARATION_ID));
        assertEquals("制剂不存在", exception.getMessage());
    }

    // endregion

    // region 打包下载
    // ===================================
    // 打包下载
    // ===================================

    /**
     * 压缩包应包含 10 个文件夹，重名文件自动加序号，磁盘缺失文件被跳过
     */
    @Test
    void exportShouldCreateTenFoldersAndDeduplicateNames() throws IOException {
        Path first = writeTempFile("材料.pdf", "第一份");
        Path second = writeTempFile("材料.pdf", "第二份");
        Path other = writeTempFile("说明.docx", "other");

        List<BusinessFileDto> files = new ArrayList<>();
        files.add(businessFile(PreparationRenewalKind.APPLICATION_FORM, 1L, "材料.pdf"));
        files.add(businessFile(PreparationRenewalKind.APPLICATION_FORM, 2L, "材料.pdf"));
        files.add(businessFile(PreparationRenewalKind.APPLICATION_FORM, 3L, "缺失.pdf"));
        files.add(businessFile(PreparationRenewalKind.OTHER, 4L, "说明.docx"));

        when(preparationService.getPreparationById(PREPARATION_ID)).thenReturn(preparation);
        when(businessFileService.list(eq(BIZ_TYPE), eq(PREPARATION_ID), eq(0L), isNull())).thenReturn(files);
        when(fileInfoService.getById(1L)).thenReturn(fileInfo(1L, first, "材料.pdf"));
        when(fileInfoService.getById(2L)).thenReturn(fileInfo(2L, second, "材料.pdf"));
        when(fileInfoService.getById(3L)).thenReturn(fileInfo(3L, tempDir.resolve("不存在.pdf"), "缺失.pdf"));
        when(fileInfoService.getById(4L)).thenReturn(fileInfo(4L, other, "说明.docx"));

        PreparationRenewalDtos.RenewalExport export = preparationRenewalService.exportRenewalFiles(PREPARATION_ID);

        assertEquals("感冒灵颗粒续展文件.zip", export.getFileName());
        assertNotNull(export.getData());
        assertTrue(export.getData().length > 0);

        List<String> entries = listZipEntries(export.getData());
        for (PreparationRenewalKind kind : PreparationRenewalKind.values()) {
            assertTrue(entries.contains(kind.getZipFolder() + "/"),
                    "压缩包应包含文件夹: " + kind.getZipFolder());
        }
        String formFolder = PreparationRenewalKind.APPLICATION_FORM.getZipFolder();
        assertTrue(entries.contains(formFolder + "/材料.pdf"));
        assertTrue(entries.contains(formFolder + "/材料 (1).pdf"), "重名文件应自动加序号");
        assertFalse(entries.contains(formFolder + "/缺失.pdf"), "磁盘文件缺失时应跳过");
        assertTrue(entries.contains(PreparationRenewalKind.OTHER.getZipFolder() + "/说明.docx"));
    }

    /**
     * 制剂不存在时打包应抛出业务异常
     */
    @Test
    void exportShouldRejectMissingPreparation() {
        when(preparationService.getPreparationById(PREPARATION_ID)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> preparationRenewalService.exportRenewalFiles(PREPARATION_ID));
        assertEquals("制剂不存在", exception.getMessage());
    }

    // endregion

    // region 续展备注
    // ===================================
    // 续展备注
    // ===================================

    /**
     * 备注超长时应拒绝保存
     */
    @Test
    void updateRenewalRemarkShouldRejectTooLongText() {
        when(preparationService.getPreparationById(PREPARATION_ID)).thenReturn(preparation);

        String longRemark = "备".repeat(1001);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> preparationRenewalService.updateRenewalRemark(PREPARATION_ID, longRemark));
        assertTrue(exception.getMessage().contains("1000"));
    }

    /**
     * 备注为空时应更新为空串（清空备注）
     */
    @Test
    void updateRenewalRemarkShouldAllowEmptyText() {
        when(preparationService.getPreparationById(PREPARATION_ID)).thenReturn(preparation);
        when(preparationService.updateById(any(Preparation.class))).thenReturn(true);

        preparationRenewalService.updateRenewalRemark(PREPARATION_ID, "");
        // 未抛异常即视为通过；字段更新由 MyBatis-Plus 部分更新完成
    }

    // endregion

    // region 测试辅助方法
    // ===================================
    // 测试辅助方法
    // ===================================

    /**
     * 构造业务附件返回对象
     */
    private BusinessFileDto businessFile(PreparationRenewalKind kind, Long fileId, String originalName) {
        BusinessFileDto dto = new BusinessFileDto();
        dto.setBizType(BIZ_TYPE);
        dto.setBizId(PREPARATION_ID);
        dto.setFileKind(kind.getCode());
        dto.setFileId(fileId);
        dto.setOriginalName(originalName);
        return dto;
    }

    /**
     * 构造文件元数据
     */
    private FileInfo fileInfo(Long fileId, Path path, String originalName) {
        FileInfo info = new FileInfo();
        info.setFileId(fileId);
        info.setFilePath(path.toString());
        info.setOriginalName(originalName);
        return info;
    }

    /**
     * 在临时目录写入测试文件
     */
    private Path writeTempFile(String fileName, String content) throws IOException {
        Path path = tempDir.resolve(fileName);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }

    /**
     * 读取 ZIP 内的全部条目名
     */
    private List<String> listZipEntries(byte[] data) throws IOException {
        List<String> entries = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.add(entry.getName());
            }
        }
        return entries;
    }

    // endregion
}
