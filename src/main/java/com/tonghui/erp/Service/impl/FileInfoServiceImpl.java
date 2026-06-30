package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.mapper.FileInfoMapper;
import com.tonghui.erp.Service.FileInfoService;
import com.tonghui.erp.Service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件信息服务实现类
 */
@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> 
    implements FileInfoService {
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Override
    @Transactional
    public FileInfo uploadFile(MultipartFile file, String category, String description) throws IOException {
        return uploadFileWithBusiness(file, category, description, null, null);
    }
    
    @Override
    @Transactional
    public FileInfo uploadFileWithBusiness(MultipartFile file, String category, String description,
                                         Long businessId, String businessType) throws IOException {
        // 验证文件
        if (!fileStorageService.isAllowedFileType(file)) {
            throw new IllegalArgumentException("不支持的文件类型: " + file.getContentType());
        }
        
        if (fileStorageService.isFileSizeExceeded(file)) {
            throw new IllegalArgumentException("文件大小超出限制: " + file.getSize() + " bytes");
        }
        
        // 计算文件 MD5（用于后续可能的查询，但不阻止重复上传）
        String fileMd5 = fileStorageService.calculateMD5(file);
        
        // 直接使用FileStorageService保存文件并获取FileInfo（包含业务信息）
        FileInfo fileInfo = fileStorageService.uploadFileWithBusiness(file, category, description, businessId, businessType);
        
        // 设置额外的业务信息
        fileInfo.setFileMd5(fileMd5);
        fileInfo.setCategory(StringUtils.hasText(category) ? category : "default");
        fileInfo.setDescription(description);
        
        // 设置创建信息
        Long currentUserId = EntityUtils.getCurrentUserId();
        fileInfo.setCreatedBy(currentUserId);
        fileInfo.setCreatedTime(LocalDateTime.now());
        fileInfo.setUpdatedTime(LocalDateTime.now());
        
        // 保存到数据库
        save(fileInfo);

        // 更新fileUrl（保存后才有fileId）
        fileInfo.setFileUrl("/api/files/" + fileInfo.getFileId());
        updateById(fileInfo);

        return fileInfo;
    }
    
    @Override
    public InputStream getFileInputStream(Long fileId) throws IOException {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            throw new IllegalArgumentException("文件不存在: " + fileId);
        }
        
        Path filePath = Paths.get(fileInfo.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IOException("文件不存在于磁盘: " + fileInfo.getFilePath());
        }
        
        return Files.newInputStream(filePath);
    }
    
    @Override
    @Transactional
    public boolean deleteFile(Long fileId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            return false;
        }
        
        try {
            // 删除磁盘文件
            Path filePath = Paths.get(fileInfo.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            
            // 删除数据库记录
            removeById(fileId);
            return true;
        } catch (IOException e) {
            throw new RuntimeException("删除文件失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public PagedResult<FileInfo> getFilesByCategory(String category, int pageIndex, int pageSize) {
        QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(category)) {
            queryWrapper.eq("category", category);
        }
        queryWrapper.orderByDesc("created_time");
        
        Page<FileInfo> page = new Page<>(pageIndex + 1, pageSize);
        Page<FileInfo> result = page(page, queryWrapper);
        
        PagedResult<FileInfo> pagedResult = new PagedResult<>();
        pagedResult.setItems(result.getRecords());
        pagedResult.setTotalCount(result.getTotal());
        pagedResult.setPageIndex(pageIndex);
        pagedResult.setPageSize(pageSize);
        
        return pagedResult;
    }
    
    @Override
    public List<FileInfo> getFilesByBusiness(Long businessId, String businessType) {
        QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("business_id", businessId)
                   .eq("business_type", businessType)
                   .orderByDesc("created_time");
        return list(queryWrapper);
    }
    
    @Override
    public List<FileInfo> getFilesByMd5(String fileMd5) {
        QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("file_md5", fileMd5);
        return list(queryWrapper);
    }

    @Override
    @Transactional
    public FileInfo uploadFileWithBusinessPath(MultipartFile file, String businessType,
                                                Long businessId, String entityName, String description) throws IOException {
        if (!fileStorageService.isAllowedFileType(file)) {
            throw new IllegalArgumentException("不支持的文件类型: " + file.getContentType());
        }
        if (fileStorageService.isFileSizeExceeded(file)) {
            throw new IllegalArgumentException("文件大小超出限制: " + file.getSize() + " bytes");
        }

        String fileMd5 = fileStorageService.calculateMD5(file);
        FileInfo fileInfo = fileStorageService.uploadFileWithBusinessPath(file, businessType, businessId, entityName, description);
        fileInfo.setFileMd5(fileMd5);
        fileInfo.setDescription(description);

        Long currentUserId = EntityUtils.getCurrentUserId();
        fileInfo.setCreatedBy(currentUserId);
        fileInfo.setCreatedTime(LocalDateTime.now());
        fileInfo.setUpdatedTime(LocalDateTime.now());

        save(fileInfo);

        fileInfo.setFileUrl("/api/files/" + fileInfo.getFileId());
        updateById(fileInfo);

        return fileInfo;
    }
}
