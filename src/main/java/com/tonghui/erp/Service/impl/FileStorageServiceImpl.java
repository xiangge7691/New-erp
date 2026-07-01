package com.tonghui.erp.Service.impl;

import com.tonghui.erp.Common.Config.FileStorageConfig;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件存储服务实现（支持磁盘存储和Base64）
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {
    
    @Autowired
    private FileStorageConfig fileStorageConfig;
    
    @Override
    public String encodeFileToBase64(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            return java.util.Base64.getEncoder().encodeToString(fileBytes);
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + file.getOriginalFilename(), e);
        }
    }
    
    @Override
    public String calculateMD5(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            InputStream inputStream = file.getInputStream();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] digest = md.digest();
            
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("计算文件MD5失败", e);
        }
    }
    
    @Override
    public FileInfo uploadFile(MultipartFile file, String category, String description) throws IOException {
        return uploadFileWithBusiness(file, category, description, null, null);
    }
    
    @Override
    public FileInfo uploadFileWithBusiness(MultipartFile file, String category, String description,
                                         Long businessId, String businessType) throws IOException {
        // 验证文件
        if (!isAllowedFileType(file)) {
            throw new IllegalArgumentException("不支持的文件类型: " + file.getContentType());
        }
        
        if (isFileSizeExceeded(file)) {
            throw new IllegalArgumentException("文件大小超出限制: " + file.getSize() + " bytes");
        }
        
        // 生成安全文件名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String storedName = generateStoredName(extension);
        
        // 确定存储路径
        Path storagePath = determineStoragePath(category, storedName);
        
        // 确保目录存在
        Files.createDirectories(storagePath.getParent());
        
        // 保存文件到磁盘
        Files.copy(file.getInputStream(), storagePath, StandardCopyOption.REPLACE_EXISTING);
        
        // 创建文件信息实体
        FileInfo fileInfo = new FileInfo();
        fileInfo.setOriginalName(originalFilename);
        fileInfo.setStoredName(storedName);
        fileInfo.setFilePath(storagePath.toString());
        fileInfo.setFileSize(file.getSize());
        fileInfo.setContentType(file.getContentType());
        fileInfo.setFileExtension(extension);
        fileInfo.setCategory(StringUtils.hasText(category) ? category : "default");
        fileInfo.setDescription(description);
        fileInfo.setStorageType("LOCAL");
        
        // 设置业务关联信息
        if (businessId != null) {
            fileInfo.setBusinessId(businessId);
        }
        if (StringUtils.hasText(businessType)) {
            fileInfo.setBusinessType(businessType);
        }
        
        // 计算MD5
        if (fileStorageConfig.isEnableMd5Check()) {
            fileInfo.setFileMd5(calculateMD5(file));
        }
        
        // 设置创建信息
        Long currentUserId = EntityUtils.getCurrentUserId();
        fileInfo.setCreatedBy(currentUserId);
        fileInfo.setCreatedTime(LocalDateTime.now());
        fileInfo.setUpdatedTime(LocalDateTime.now());
        
        // 生成访问URL
        fileInfo.setFileUrl("/api/files/" + fileInfo.getFileId());
        
        return fileInfo;
    }
    
    @Override
    public InputStream getFileInputStream(Long fileId) throws IOException {
        // 这里应该从数据库查询文件路径，然后返回文件流
        // 暂时返回null，后续需要实现FileInfoService
        return null;
    }
    
    @Override
    public boolean deleteFile(Long fileId) {
        // 这里应该实现文件删除逻辑
        // 包括删除磁盘文件和数据库记录
        return false;
    }
    
    @Override
    public boolean isAllowedFileType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && fileStorageConfig.getAllowedTypes().contains(contentType);
    }
    
    @Override
    public boolean isFileSizeExceeded(MultipartFile file) {
        return file.getSize() > fileStorageConfig.getMaxSize();
    }
    
    @Override
    public void updateBusinessInfo(FileInfo fileInfo, Long businessId, String businessType) {
        if (fileInfo != null) {
            if (businessId != null) {
                fileInfo.setBusinessId(businessId);
            }
            if (StringUtils.hasText(businessType)) {
                fileInfo.setBusinessType(businessType);
            }
            fileInfo.setUpdatedTime(LocalDateTime.now());
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
    
    /**
     * 生成安全的存储文件名
     */
    private String generateStoredName(String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid + (StringUtils.hasText(extension) ? "." + extension : "");
    }
    
    /**
     * 确定文件存储路径
     */
    private Path determineStoragePath(String category, String storedName) {
        String basePath = resolveBasePath();
        String subDir = getCategorySubdirectory(category);
        return Paths.get(basePath, subDir, storedName);
    }
    
    /**
     * 解析基础存储路径（支持环境变量覆盖）
     */
    private String resolveBasePath() {
        // 优先使用环境变量
        String envPath = System.getenv("ERP_FILE_STORAGE_PATH");
        if (StringUtils.hasText(envPath)) {
            return envPath;
        }
        
        // 使用系统属性
        String sysPath = System.getProperty("erp.file.storage.path");
        if (StringUtils.hasText(sysPath)) {
            return sysPath;
        }
        
        // 使用配置文件中的路径
        return fileStorageConfig.getBasePath();
    }
    
    /**
     * 根据文件分类确定子目录
     */
    private String getCategorySubdirectory(String category) {
        if (!StringUtils.hasText(category) || "default".equals(category)) {
            return fileStorageConfig.getDocumentDir();
        }
        
        switch (category.toLowerCase()) {
            case "image":
            case "images":
            case "img":
                return fileStorageConfig.getImageDir();
            case "document":
            case "documents":
            case "doc":
                return fileStorageConfig.getDocumentDir();
            case "archive":
            case "archives":
            case "zip":
            case "rar":
                return fileStorageConfig.getArchiveDir();
            default:
                return fileStorageConfig.getDocumentDir();
        }
    }

    @Override
    public FileInfo uploadFileWithBusinessPath(MultipartFile file, String businessType,
                                                Long businessId, String entityName, String description) throws IOException {
        return uploadFileWithBusinessPath(file, businessType, businessId, entityName, description, null);
    }

    @Override
    public FileInfo uploadFileWithBusinessPath(MultipartFile file, String businessType,
                                                Long businessId, String entityName, String description, String customPath) throws IOException {
        if (!isAllowedFileType(file)) {
            throw new IllegalArgumentException("不支持的文件类型: " + file.getContentType());
        }
        if (isFileSizeExceeded(file)) {
            throw new IllegalArgumentException("文件大小超出限制: " + file.getSize() + " bytes");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String storedName = generateStoredName(extension);

        Path storagePath = determineBusinessPath(businessType, entityName, storedName, customPath);
        Files.createDirectories(storagePath.getParent());
        Files.copy(file.getInputStream(), storagePath, StandardCopyOption.REPLACE_EXISTING);

        FileInfo fileInfo = new FileInfo();
        fileInfo.setOriginalName(originalFilename);
        fileInfo.setStoredName(storedName);
        fileInfo.setFilePath(storagePath.toString());
        fileInfo.setFileSize(file.getSize());
        fileInfo.setContentType(file.getContentType());
        fileInfo.setFileExtension(extension);
        fileInfo.setCategory(fileStorageConfig.getBusinessTypeDir(businessType));
        fileInfo.setDescription(description);
        fileInfo.setStorageType("LOCAL");

        if (businessId != null) {
            fileInfo.setBusinessId(businessId);
        }
        if (StringUtils.hasText(businessType)) {
            fileInfo.setBusinessType(businessType);
        }
        if (StringUtils.hasText(customPath)) {
            fileInfo.setCustomPath(customPath);
        }

        if (fileStorageConfig.isEnableMd5Check()) {
            fileInfo.setFileMd5(calculateMD5(file));
        }

        Long currentUserId = EntityUtils.getCurrentUserId();
        fileInfo.setCreatedBy(currentUserId);
        fileInfo.setCreatedTime(LocalDateTime.now());
        fileInfo.setUpdatedTime(LocalDateTime.now());
        fileInfo.setFileUrl("/api/files/" + fileInfo.getFileId());

        return fileInfo;
    }

    /**
     * 按业务类型+日期+实体名确定存储路径
     * 格式：{basePath}/{中文目录}/{customPath}/{年}/{月}/{实体名}/{uuid.ext}
     */
    private Path determineBusinessPath(String businessType, String entityName, String storedName, String customPath) {
        String basePath = resolveBasePath();
        String dir = fileStorageConfig.getBusinessTypeDir(businessType);
        String year = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String month = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM"));
        String safeName = sanitizeFileName(entityName);
        if (StringUtils.hasText(customPath)) {
            String safeCustomPath = sanitizeFileName(customPath);
            return Paths.get(basePath, dir, safeCustomPath, year, month, safeName, storedName);
        }
        return Paths.get(basePath, dir, year, month, safeName, storedName);
    }

    /**
     * 清理文件名中的非法字符
     */
    private String sanitizeFileName(String name) {
        if (!StringUtils.hasText(name)) {
            return "未命名";
        }
        String sanitized = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return sanitized.isEmpty() ? "未命名" : sanitized;
    }
}