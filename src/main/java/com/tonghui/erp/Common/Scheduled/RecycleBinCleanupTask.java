package com.tonghui.erp.Common.Scheduled;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.mapper.FileInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 回收站定时清理任务
 * <p>
 * 每天凌晨2点执行，清理超过30天的回收站文件
 * </p>
 */
@Component
public class RecycleBinCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(RecycleBinCleanupTask.class);

    /** 回收站文件保留天数 */
    private static final int RETENTION_DAYS = 30;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    /**
     * 每天凌晨2点执行清理
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanup() {
        log.info("开始清理回收站中超过{}天的文件...", RETENTION_DAYS);

        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);

        QueryWrapper<FileInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 1)
               .isNotNull("deleted_at")
               .le("deleted_at", threshold);

        List<FileInfo> expiredFiles = fileInfoMapper.selectList(wrapper);
        int count = 0;

        for (FileInfo fileInfo : expiredFiles) {
            try {
                // 物理删除磁盘文件
                Path filePath = Paths.get(fileInfo.getFilePath());
                if (Files.exists(filePath)) {
                    Files.deleteIfExists(filePath);
                }
                // 物理删除数据库记录
                fileInfoMapper.deleteById(fileInfo.getFileId());
                count++;
            } catch (IOException e) {
                log.error("清理文件失败: {}", fileInfo.getFilePath(), e);
            }
        }

        log.info("回收站清理完成，共清理{}个文件", count);
    }
}
