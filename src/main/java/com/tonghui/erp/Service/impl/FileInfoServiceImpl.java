package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PagedResult;
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
 * <p>
 * 负责文件元数据的持久化管理：上传入库、按分类/业务条件查询、删除等
 * 文件物理存储由 FileStorageService 完成，本类只负责数据库记录
 * </p>
 */
@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> 
    implements FileInfoService {
    
    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 文件存储服务（负责磁盘存储、类型/大小校验、MD5计算）
     */
    @Autowired
    private FileStorageService fileStorageService;

    // endregion

    // region 文件上传方法
    // ===================================
    // 文件上传方法
    // ===================================

    /**
     * 通用文件上传（不关联业务，委托给带业务参数的重载方法）
     *
     * @param file        上传文件
     * @param category    文件分类（image/document/archive，可为null）
     * @param description 文件描述（可为null）
     * @return 保存后的文件信息（含fileId、fileUrl）
     * @throws IOException 文件读写失败
     */
    @Override
    @Transactional
    public FileInfo uploadFile(MultipartFile file, String category, String description) throws IOException {
        return uploadFileWithBusiness(file, category, description, null, null);
    }
    
    /**
     * 通用文件上传（可选关联业务ID与业务类型）
     * <p>
     * 流程：校验类型/大小 → 计算MD5 → 磁盘保存 → 写库 → 回填fileUrl
     * </p>
     *
     * @param file         上传文件
     * @param category     文件分类（可为null，默认default）
     * @param description  文件描述（可为null）
     * @param businessId   业务ID（可为null）
     * @param businessType 业务类型（可为null）
     * @return 保存后的文件信息
     * @throws IOException 文件读写失败
     */
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
        
        // 保存到数据库
        save(fileInfo);

        // 更新fileUrl（保存后才有fileId）
        fileInfo.setFileUrl("/api/files/" + fileInfo.getFileId());
        updateById(fileInfo);

        return fileInfo;
    }
    
    // endregion

    // region 文件操作方法
    // ===================================
    // 文件操作方法
    // ===================================

    /**
     * 获取文件输入流（下载/预览用）
     *
     * @param fileId 文件ID
     * @return 文件输入流
     * @throws IOException 文件不存在或读取失败
     */
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
    
    /**
     * 删除文件（仅逻辑删除数据库记录，磁盘文件不在此方法内删除）
     *
     * @param fileId 文件ID
     * @return 是否删除成功（记录不存在时返回false）
     */
    @Override
    @Transactional
    public boolean deleteFile(Long fileId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            return false;
        }
        
        // 软删除数据库记录
        removeById(fileId);
        return true;
    }
    
    // endregion

    // region 文件查询方法
    // ===================================
    // 文件查询方法
    // ===================================

    /**
     * 按文件分类分页查询文件列表（按创建时间倒序）
     *
     * @param category  文件分类（可为null，为null时查询全部）
     * @param pageIndex 页码，从0开始
     * @param pageSize  每页数量
     * @return 分页结果
     */
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
    
    /**
     * 按业务ID与业务类型查询关联文件列表（按创建时间倒序）
     *
     * @param businessId   业务ID
     * @param businessType 业务类型
     * @param customPath   自定义路径（选填，精确匹配custom_path字段）
     * @return 关联文件列表
     */
    @Override
    public List<FileInfo> getFilesByBusiness(Long businessId, String businessType, String customPath) {
        QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("business_id", businessId)
                   .eq("business_type", businessType)
                   .eq(StringUtils.hasText(customPath), "custom_path", customPath)
                   .orderByDesc("created_time");
        return list(queryWrapper);
    }
    
    /**
     * 按文件MD5查询文件列表（可用于查重复文件）
     *
     * @param fileMd5 文件MD5值
     * @return 具有相同MD5的文件列表
     */
    @Override
    public List<FileInfo> getFilesByMd5(String fileMd5) {
        QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("file_md5", fileMd5);
        return list(queryWrapper);
    }

    /**
     * 按业务路径上传文件（不带自定义路径，委托给带customPath的重载方法）
     *
     * @param file         上传文件
     * @param businessType 业务类型（必填，见FileController业务类型枚举）
     * @param businessId   业务ID（必填）
     * @param entityName   实体名称（选填，用于目录名，为空时默认"未命名"）
     * @param description  文件描述（可为null）
     * @return 保存后的文件信息
     * @throws IOException 文件读写失败
     */
    @Override
    @Transactional
    public FileInfo uploadFileWithBusinessPath(MultipartFile file, String businessType,
                                                Long businessId, String entityName, String description) throws IOException {
        return uploadFileWithBusinessPath(file, businessType, businessId, entityName, description, null);
    }

    /**
     * 按业务路径上传文件（推荐方式）
     * <p>
     * 存储路径：{basePath}/{中文目录}/{customPath?}/{年}/{月}/{实体名}/{uuid.ext}
     * 流程：校验类型/大小 → 计算MD5 → 磁盘保存 → 写库 → 回填fileUrl
     * </p>
     *
     * @param file         上传文件
     * @param businessType 业务类型（必填）
     * @param businessId   业务ID（必填）
     * @param entityName   实体名称（选填，为空时默认"未命名"）
     * @param description  文件描述（可为null）
     * @param customPath   自定义路径段（选填，插入在业务类型目录之后）
     * @return 保存后的文件信息
     * @throws IOException 文件读写失败
     */
    @Override
    @Transactional
    public FileInfo uploadFileWithBusinessPath(MultipartFile file, String businessType,
                                                Long businessId, String entityName, String description, String customPath) throws IOException {
        if (!fileStorageService.isAllowedFileType(file)) {
            throw new IllegalArgumentException("不支持的文件类型: " + file.getContentType());
        }
        if (fileStorageService.isFileSizeExceeded(file)) {
            throw new IllegalArgumentException("文件大小超出限制: " + file.getSize() + " bytes");
        }

        String fileMd5 = fileStorageService.calculateMD5(file);
        FileInfo fileInfo = fileStorageService.uploadFileWithBusinessPath(file, businessType, businessId, entityName, description, customPath);
        fileInfo.setFileMd5(fileMd5);
        fileInfo.setDescription(description);

        save(fileInfo);

        fileInfo.setFileUrl("/api/files/" + fileInfo.getFileId());
        updateById(fileInfo);

        return fileInfo;
    }

    // endregion
}
