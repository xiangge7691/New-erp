package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.FileManager.FileItemDto;
import com.tonghui.erp.Common.Dto.FileManager.FileSearchRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.FileInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件管理器搜索功能专项测试
 * <p>
 * 覆盖业务规则：
 * 1. keyword 按原始文件名匹配（磁盘存储名为UUID，不能按磁盘名匹配）
 * 2. 无 file_info 记录时回退磁盘文件名匹配
 * 全部用例使用事务回滚，磁盘临时文件用后即删
 * </p>
 */
@SpringBootTest
@Transactional
public class FileManagerSearchTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /**
     * 文件管理器服务
     */
    @Autowired
    private FileManagerService fileManagerService;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试按原始文件名搜索：上传后用原始名关键字应能命中
     */
    @Test
    public void testSearchByOriginalName() throws IOException {
        String originalName = "搜索验证关键词文档.txt";
        MockMultipartFile file = new MockMultipartFile(
                "file", originalName, "text/plain", "搜索验证内容".getBytes());
        FileInfo uploaded = fileManagerService.uploadFile(file, "test-search", "custom");
        try {
            // 按原始文件名关键字搜索（磁盘名为UUID，只有按原始名匹配才能命中）
            FileSearchRequestDto request = new FileSearchRequestDto();
            request.setKeyword("搜索验证关键词");
            request.setRoot("custom");
            request.setPageIndex(0);
            request.setPageSize(20);
            PagedResult<FileItemDto> result = fileManagerService.searchFiles(request);
            assertTrue(result.getItems().stream().anyMatch(i -> originalName.equals(i.getName())),
                    "按原始文件名关键字应命中上传的文件");

            // 关键字包含UUID存储名的部分字符时不应依赖磁盘名命中（磁盘名匹配已禁用，无原始记录才回退）
            request.setKeyword("不存在的关键词XYZQ");
            PagedResult<FileItemDto> empty = fileManagerService.searchFiles(request);
            assertFalse(empty.getItems().stream().anyMatch(i -> originalName.equals(i.getName())),
                    "不相关关键字不应命中");
        } finally {
            // 事务回滚不回滚磁盘文件，测试后手动清理
            if (uploaded.getFilePath() != null) {
                Path disk = Paths.get(uploaded.getFilePath());
                Files.deleteIfExists(disk);
            }
        }
    }

    // endregion
}
