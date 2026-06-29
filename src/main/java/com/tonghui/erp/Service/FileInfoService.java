package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Common.Dto.PagedResult;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 文件信息服务接口
 */
public interface FileInfoService extends IService<FileInfo> {

    /**
     * 上传文件
     *
     * @param file 文件对象
     * @param category 文件分类
     * @param description 文件描述
     * @return 文件信息实体
     * @throws IOException IO异常
     */
    FileInfo uploadFile(MultipartFile file, String category, String description) throws IOException;
    
    /**
     * 上传文件（带业务信息）
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
     * 根据ID获取文件输入流
     *
     * @param fileId 文件ID
     * @return 文件输入流
     * @throws IOException IO异常
     */
    InputStream getFileInputStream(Long fileId) throws IOException;

    /**
     * 删除文件（包括磁盘文件和数据库记录）
     *
     * @param fileId 文件ID
     * @return 是否删除成功
     */
    boolean deleteFile(Long fileId);

    /**
     * 根据分类查询文件列表（分页）
     *
     * @param category 文件分类
     * @param pageIndex 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PagedResult<FileInfo> getFilesByCategory(String category, int pageIndex, int pageSize);

    /**
     * 根据业务ID和业务类型查询文件
     *
     * @param businessId 业务ID
     * @param businessType 业务类型
     * @return 文件列表
     */
    List<FileInfo> getFilesByBusiness(Long businessId, String businessType);

    /**
     * 根据文件MD5查找是否存在相同文件
     *
     * @param fileMd5 文件MD5
     * @return 文件信息列表
     */
    List<FileInfo> getFilesByMd5(String fileMd5);
}
