package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.BusinessFile.BizFileTypeDto;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileDto;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileSyncDto;
import com.tonghui.erp.Data.Entity.BusinessFile;
import com.tonghui.erp.Data.Entity.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 业务附件服务
 * <p>
 * 关系表 business_file 是业务附件的唯一查询来源；file_info.business_type / business_id
 * 不再参与业务查询。
 * </p>
 */
public interface BusinessFileService extends IService<BusinessFile> {

    // region 上传
    // ===================================
    // 上传
    // ===================================

    /**
     * 上传文件并绑定业务记录
     *
     * @param file        文件
     * @param bizType     业务类型编码
     * @param bizId       业务ID
     * @param refId       子对象ID（可为空）
     * @param fileKind    文件槽位（可为空，默认 DEFAULT）
     * @param entityName  实体名称（用于磁盘目录，可为空）
     * @param description 文件描述（可为空）
     * @return 附件信息
     * @throws IOException 文件读写失败
     */
    BusinessFileDto upload(MultipartFile file, String bizType, Long bizId, Long refId,
                           String fileKind, String entityName, String description) throws IOException;

    /**
     * 临时上传（只写物理文件，不绑定业务；保存单据时通过 sync 绑定）
     *
     * @param file        文件
     * @param bizType     业务类型编码（用于目录和后续校验）
     * @param entityName  实体名称（可为空）
     * @param description 文件描述（可为空）
     * @return 文件元数据
     * @throws IOException 文件读写失败
     */
    FileInfo uploadTemp(MultipartFile file, String bizType, String entityName, String description) throws IOException;

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 查询业务附件
     *
     * @param bizType  业务类型编码
     * @param bizId    业务ID
     * @param refId    子对象ID（为空或0时不按子对象过滤）
     * @param fileKind 文件槽位（可为空）
     * @return 附件列表
     */
    List<BusinessFileDto> list(String bizType, Long bizId, Long refId, String fileKind);

    /**
     * 批量查询多个业务记录的附件
     *
     * @param bizType  业务类型编码
     * @param bizIds   业务ID列表
     * @param fileKind 文件槽位（可为空）
     * @return 业务ID -> 附件列表
     */
    Map<Long, List<BusinessFileDto>> listBatch(String bizType, List<Long> bizIds, String fileKind);

    /**
     * 统计文件被多少条业务关系引用
     *
     * @param fileId 文件ID
     * @return 引用数
     */
    long countByFileId(Long fileId);

    // endregion

    // region 同步与删除
    // ===================================
    // 同步与删除
    // ===================================

    /**
     * 同步附件（保存单据时调用，差量解绑/绑定）
     *
     * @param dto 同步请求
     * @return 同步后的附件列表
     */
    List<BusinessFileDto> sync(BusinessFileSyncDto dto);

    /**
     * 解绑并删除单个附件
     *
     * @param relationId 关系ID
     */
    void delete(Long relationId);

    /**
     * 解绑某业务对象的全部附件（业务记录删除时调用）
     *
     * @param bizType  业务类型编码
     * @param bizId    业务ID
     * @param refId    子对象ID；null 表示该业务对象下的所有子对象（审批实例删除时传 null），
     *                 0 或具体值表示只解绑该子对象
     * @param fileKind 文件槽位（可为空，为空时解绑全部槽位）
     * @return 解绑数量
     */
    int unbindAll(String bizType, Long bizId, Long refId, String fileKind);

    // endregion

    // region 注册表
    // ===================================
    // 注册表
    // ===================================

    /**
     * 获取全部业务类型定义
     *
     * @return 业务类型列表
     */
    List<BizFileTypeDto> listTypes();

    // endregion
}
