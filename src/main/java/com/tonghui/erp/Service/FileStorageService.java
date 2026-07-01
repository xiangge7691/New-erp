package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件存储服务接口
 */
public interface FileStorageService {
    
    /**
     * 将文件转换为Base64字符串
     *
     * @param file 文件对象
     * @return Base64编码的文件内容
     */
    String encodeFileToBase64(MultipartFile file);
    
    /**
     * 计算文件的MD5值
     *
     * @param file 文件
     * @return MD5值
     */
    String calculateMD5(MultipartFile file);
    
    /**
     * 上传文件到磁盘并保存元数据
     *
     * @param file 文件对象
     * @param category 文件分类
     * @param description 文件描述
     * @return 文件信息实体
     * @throws IOException IO异常
     */
    FileInfo uploadFile(MultipartFile file, String category, String description) throws IOException;
    
    /**
     * 上传文件到磁盘并保存元数据（带业务信息）
     *
     * @param file 文件对象
     * @param category 文件分类
     * @param description 文件描述
     * @param businessId 业务ID
     * @param businessType 业务类型
     * @return 文件信息实体
     * @throws IOException IO异常
     */
    FileInfo uploadFileWithBusiness(MultipartFile file, String category, String description, 
                                  Long businessId, String businessType) throws IOException;
    
    /**
     * 根据文件ID获取文件输入流
     *
     * @param fileId 文件ID
     * @return 文件输入流
     */
    InputStream getFileInputStream(Long fileId) throws IOException;
    
    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @return 是否删除成功
     */
    boolean deleteFile(Long fileId);
    
    /**
     * 验证文件类型是否允许
     *
     * @param file 文件对象
     * @return 是否允许
     */
    boolean isAllowedFileType(MultipartFile file);
    
    /**
     * 验证文件大小是否超出限制
     *
     * @param file 文件对象
     * @return 是否超出限制
     */
    boolean isFileSizeExceeded(MultipartFile file);
    
    /**
     * 更新文件的业务关联信息
     *
     * @param fileInfo 文件信息实体
     * @param businessId 业务ID
     * @param businessType 业务类型
     */
    void updateBusinessInfo(FileInfo fileInfo, Long businessId, String businessType);

    /**
     * 按业务路径上传文件
     * 路径格式：{basePath}/{业务中文目录}/{年}/{月}/{实体名}/{uuid.ext}
     *
     * @param file 文件对象
     * @param businessType 业务类型（如 EQUIPMENT_MAINTENANCE）
     * @param businessId 业务ID
     * @param entityName 实体名称（如设备名称，用于目录名）
     * @param description 文件描述
     * @return 文件信息实体
     * @throws IOException IO异常
     */
    FileInfo uploadFileWithBusinessPath(MultipartFile file, String businessType,
                                        Long businessId, String entityName, String description) throws IOException;

    FileInfo uploadFileWithBusinessPath(MultipartFile file, String businessType,
                                        Long businessId, String entityName, String description, String customPath) throws IOException;
}
