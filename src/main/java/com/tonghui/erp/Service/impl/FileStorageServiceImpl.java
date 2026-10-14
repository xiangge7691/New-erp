package com.tonghui.erp.Service.impl;

import com.tonghui.erp.Common.Config.FileStorageConfig;
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
    
    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 文件存储配置（基础路径、大小限制、允许类型、目录映射等）
     */
    @Autowired
    private FileStorageConfig fileStorageConfig;

    // endregion

    // region 文件编码方法
    // ===================================
    // 文件编码方法
    // ===================================

    /**
     * 将上传文件编码为Base64字符串
     *
     * @param file 上传文件
     * @return Base64编码结果
     */
    @Override
    public String encodeFileToBase64(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            return java.util.Base64.getEncoder().encodeToString(fileBytes);
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + file.getOriginalFilename(), e);
        }
    }
    
    /**
     * 计算文件MD5摘要（32位小写十六进制）
     *
     * @param file 上传文件
     * @return MD5字符串
     */
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
    
    // endregion

    // region 文件存储方法
    // ===================================
    // 文件存储方法
    // ===================================

    /**
     * 通用文件上传（按分类分目录，不关联业务，委托给带业务参数的重载方法）
     *
     * @param file        上传文件
     * @param category    文件分类（image/document/archive，可为null）
     * @param description 文件描述（可为null）
     * @return 文件信息（未持久化，由FileInfoService负责入库）
     * @throws IOException 文件读写失败
     */
    @Override
    public FileInfo uploadFile(MultipartFile file, String category, String description) throws IOException {
        return uploadFileWithBusiness(file, category, description, null, null);
    }
    
    /**
     * 通用文件上传（按分类分目录，可选关联业务）
     * <p>
     * 流程：校验类型/大小 → 生成UUID存储名 → 按分类确定目录 → 落盘 → 组装FileInfo
     * </p>
     *
     * @param file         上传文件
     * @param category     文件分类（可为null，默认default）
     * @param description  文件描述（可为null）
     * @param businessId   业务ID（可为null）
     * @param businessType 业务类型（可为null）
     * @return 文件信息（未持久化）
     * @throws IOException 文件读写失败
     */
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
        
        // 生成访问URL
        fileInfo.setFileUrl("/api/files/" + fileInfo.getFileId());
        
        return fileInfo;
    }
    
    // endregion

    // region 文件删除方法
    // ===================================
    // 文件删除方法
    // ===================================

    /**
     * 获取文件输入流（占位实现，尚未接入数据库查询文件路径，始终返回null）
     *
     * @param fileId 文件ID
     * @return 暂未实现，返回null
     * @throws IOException 预留异常声明
     */
    @Override
    public InputStream getFileInputStream(Long fileId) throws IOException {
        // 这里应该从数据库查询文件路径，然后返回文件流
        // 暂时返回null，后续需要实现FileInfoService
        return null;
    }
    
    /**
     * 删除文件（占位实现，尚未实现磁盘文件与数据库记录的删除，始终返回false）
     *
     * @param fileId 文件ID
     * @return 暂未实现，返回false
     */
    @Override
    public boolean deleteFile(Long fileId) {
        // 这里应该实现文件删除逻辑
        // 包括删除磁盘文件和数据库记录
        return false;
    }
    
    // endregion

    // region 辅助方法
    // ===================================
    // 辅助方法
    // ===================================

    /**
     * 校验文件类型是否在允许列表内
     *
     * @param file 上传文件
     * @return 允许返回true，类型不支持返回false
     */
    @Override
    public boolean isAllowedFileType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && fileStorageConfig.getAllowedTypes().contains(contentType);
    }
    
    /**
     * 校验文件大小是否超出配置限制
     *
     * @param file 上传文件
     * @return 超出限制返回true
     */
    @Override
    public boolean isFileSizeExceeded(MultipartFile file) {
        return file.getSize() > fileStorageConfig.getMaxSize();
    }
    
    /**
     * 更新文件实体上的业务关联信息（仅覆盖非空值，同时刷新更新时间）
     *
     * @param fileInfo     文件实体
     * @param businessId   业务ID（null时不覆盖）
     * @param businessType 业务类型（空白时不覆盖）
     */
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
     * 获取文件扩展名（不含点号，统一小写；无扩展名时返回空串）
     *
     * @param filename 原始文件名（可为null）
     * @return 小写扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
    
    /**
     * 生成安全的存储文件名（32位UUID去掉横线 + 扩展名）
     *
     * @param extension 扩展名（可为空，为空时不带后缀）
     * @return 存储文件名
     */
    private String generateStoredName(String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid + (StringUtils.hasText(extension) ? "." + extension : "");
    }
    
    /**
     * 确定通用上传的文件存储路径（基础路径 + 分类子目录 + 存储文件名）
     *
     * @param category   文件分类
     * @param storedName 存储文件名
     * @return 文件完整存储路径
     */
    private Path determineStoragePath(String category, String storedName) {
        String basePath = resolveBasePath();
        String subDir = getCategorySubdirectory(category);
        return Paths.get(basePath, subDir, storedName);
    }
    
    /**
     * 解析基础存储路径（优先级：环境变量 ERP_FILE_STORAGE_PATH → 系统属性 erp.file.storage.path → 配置文件 file.base-path）
     *
     * @return 基础存储路径
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
     * 根据文件分类确定子目录名（image→images、document→documents、archive→archives，其他默认documents）
     *
     * @param category 文件分类（可为null）
     * @return 子目录名
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

    // endregion

    // region 按业务路径上传方法
    // ===================================
    // 按业务路径上传方法
    // ===================================

    /**
     * 按业务路径上传文件（不带自定义路径，委托给带customPath的重载方法）
     *
     * @param file         上传文件
     * @param businessType 业务类型（必填）
     * @param businessId   业务ID（必填）
     * @param entityName   实体名称（选填，用于目录名）
     * @param description  文件描述（可为null）
     * @return 文件信息（未持久化）
     * @throws IOException 文件读写失败
     */
    @Override
    public FileInfo uploadFileWithBusinessPath(MultipartFile file, String businessType,
                                                Long businessId, String entityName, String description) throws IOException {
        return uploadFileWithBusinessPath(file, businessType, businessId, entityName, description, null);
    }

    /**
     * 按业务路径上传文件（推荐方式）
     * <p>
     * 存储路径：{basePath}/{中文目录}/{customPath?}/{年}/{月}/{实体名}/{uuid.ext}
     * </p>
     *
     * @param file         上传文件
     * @param businessType 业务类型（必填，决定中文目录）
     * @param businessId   业务ID（必填）
     * @param entityName   实体名称（选填，为空时默认"未命名"）
     * @param description  文件描述（可为null）
     * @param customPath   自定义路径段（选填，插入在业务类型目录之后）
     * @return 文件信息（未持久化）
     * @throws IOException 文件读写失败
     */
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

        fileInfo.setFileUrl("/api/files/" + fileInfo.getFileId());

        return fileInfo;
    }

    // endregion

    // region 路径解析辅助方法
    // ===================================
    // 路径解析辅助方法
    // ===================================

    /**
     * 按业务类型+日期+实体名确定存储路径
     * 格式：{basePath}/{中文目录}/{customPath}/{年}/{月}/{实体名}/{uuid.ext}
     *
     * @param businessType 业务类型
     * @param entityName   实体名称（目录名）
     * @param storedName   存储文件名
     * @param customPath   自定义路径段（可为null）
     * @return 文件完整存储路径
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
     * 清理文件名中的非法字符（替换 \ / : * ? " < > | 为下划线，空白时返回"未命名"）
     *
     * @param name 原始名称（可为null）
     * @return 清理后的安全名称
     */
    private String sanitizeFileName(String name) {
        if (!StringUtils.hasText(name)) {
            return "未命名";
        }
        String sanitized = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return sanitized.isEmpty() ? "未命名" : sanitized;
    }

    // endregion
}