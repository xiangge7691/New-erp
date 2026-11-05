package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Constants.BizFileType;
import com.tonghui.erp.Common.Dto.BusinessFile.BizFileTypeDto;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileDto;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileSyncDto;
import com.tonghui.erp.Data.Entity.BusinessFile;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.mapper.BusinessFileMapper;
import com.tonghui.erp.Service.BusinessFileService;
import com.tonghui.erp.Service.FileInfoService;
import com.tonghui.erp.Service.FileOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 业务附件服务实现
 * <p>
 * 关系表为唯一查询来源；file_info.business_type / business_id 不再参与业务查询。
 * 关系行解绑使用物理删除，避免全局逻辑删除导致唯一键冲突。
 * </p>
 */
@Service
public class BusinessFileServiceImpl extends ServiceImpl<BusinessFileMapper, BusinessFile>
        implements BusinessFileService {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 文件信息服务 */
    @Autowired
    private FileInfoService fileInfoService;

    /** 文件操作日志服务 */
    @Autowired
    private FileOperationLogService fileOperationLogService;

    /** 用于校验父记录是否存在（表名/列名来自枚举，无注入风险） */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // endregion

    // region 上传
    // ===================================
    // 上传
    // ===================================

    /**
     * 上传文件并绑定业务记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessFileDto upload(MultipartFile file, String bizType, Long bizId, Long refId,
                                  String fileKind, String entityName, String description) throws IOException {
        BizFileType type = resolveType(bizType);
        String kind = type.normalizeKind(fileKind);
        long ref = refId == null ? 0L : refId;
        validateParent(type, bizId, ref);

        // 槽位数量校验
        long exists = count(new LambdaQueryWrapper<BusinessFile>()
                .eq(BusinessFile::getBizType, type.getCode())
                .eq(BusinessFile::getBizId, bizId)
                .eq(BusinessFile::getRefId, ref)
                .eq(BusinessFile::getFileKind, kind));
        int max = type.maxCount(kind);
        if (max >= 0 && exists >= max) {
            throw new IllegalArgumentException(
                    type.getDisplayName() + " 的 " + kind + " 槽位最多允许 " + max + " 个文件");
        }

        FileInfo fileInfo = null;
        try {
            // 物理文件入库（不写业务关联）
            fileInfo = fileInfoService.uploadFileToDir(file, type.getStorageDir(),
                    StringUtils.hasText(entityName) ? entityName : type.getDisplayName(), description);

            BusinessFile relation = new BusinessFile();
            relation.setBizType(type.getCode());
            relation.setBizId(bizId);
            relation.setRefId(ref);
            relation.setFileId(fileInfo.getFileId());
            relation.setFileKind(kind);
            relation.setSortOrder(baseMapper.nextSortOrder(type.getCode(), bizId, ref, kind));
            relation.setIsDeleted(0);
            save(relation);

            fileOperationLogService.log(fileInfo.getFileId(), fileInfo.getOriginalName(),
                    fileInfo.getFilePath(), "UPLOAD", "business",
                    "bizType=" + type.getCode() + ", bizId=" + bizId + ", refId=" + ref + ", kind=" + kind);
            return BusinessFileDto.from(relation, fileInfo);
        } catch (IOException | RuntimeException e) {
            // 关系写入失败时回收刚写入的文件记录，避免孤儿文件
            if (fileInfo != null && fileInfo.getFileId() != null) {
                fileInfoService.deleteFile(fileInfo.getFileId());
            }
            throw e;
        }
    }

    /**
     * 临时上传（不绑定业务）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo uploadTemp(MultipartFile file, String bizType, String entityName,
                               String description) throws IOException {
        BizFileType type = resolveType(bizType);
        FileInfo fileInfo = fileInfoService.uploadFileToDir(file, type.getStorageDir(),
                StringUtils.hasText(entityName) ? entityName : type.getDisplayName(), description);
        fileOperationLogService.log(fileInfo.getFileId(), fileInfo.getOriginalName(),
                fileInfo.getFilePath(), "UPLOAD", "business", "临时上传, bizType=" + type.getCode());
        return fileInfo;
    }

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 查询业务附件
     */
    @Override
    public List<BusinessFileDto> list(String bizType, Long bizId, Long refId, String fileKind) {
        BizFileType type = resolveType(bizType);
        if (bizId == null || bizId <= 0) {
            return Collections.emptyList();
        }
        long ref = refId == null ? 0L : refId;
        String kind = StringUtils.hasText(fileKind) ? type.normalizeKind(fileKind) : null;

        List<BusinessFile> relations = list(new LambdaQueryWrapper<BusinessFile>()
                .eq(BusinessFile::getBizType, type.getCode())
                .eq(BusinessFile::getBizId, bizId)
                .eq(ref > 0, BusinessFile::getRefId, ref)
                .eq(kind != null, BusinessFile::getFileKind, kind)
                .orderByAsc(BusinessFile::getSortOrder)
                .orderByAsc(BusinessFile::getId));
        return toDtoList(relations);
    }

    /**
     * 批量查询多个业务记录的附件
     */
    @Override
    public Map<Long, List<BusinessFileDto>> listBatch(String bizType, List<Long> bizIds, String fileKind) {
        if (bizIds == null || bizIds.isEmpty()) {
            return Collections.emptyMap();
        }
        BizFileType type = resolveType(bizType);
        String kind = StringUtils.hasText(fileKind) ? type.normalizeKind(fileKind) : null;

        Map<Long, List<BusinessFileDto>> result = new LinkedHashMap<>();
        bizIds.stream().filter(Objects::nonNull).distinct().forEach(id -> result.put(id, new ArrayList<>()));
        if (result.isEmpty()) {
            return result;
        }

        List<BusinessFile> relations = list(new LambdaQueryWrapper<BusinessFile>()
                .eq(BusinessFile::getBizType, type.getCode())
                .in(BusinessFile::getBizId, result.keySet())
                .eq(kind != null, BusinessFile::getFileKind, kind)
                .orderByAsc(BusinessFile::getSortOrder)
                .orderByAsc(BusinessFile::getId));
        for (BusinessFileDto dto : toDtoList(relations)) {
            result.computeIfAbsent(dto.getBizId(), key -> new ArrayList<>()).add(dto);
        }
        return result;
    }

    /**
     * 统计文件被多少条业务关系引用
     */
    @Override
    public long countByFileId(Long fileId) {
        if (fileId == null) {
            return 0;
        }
        return baseMapper.countByFileId(fileId);
    }

    // endregion

    // region 同步与删除
    // ===================================
    // 同步与删除
    // ===================================

    /**
     * 同步附件（保存单据时调用）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<BusinessFileDto> sync(BusinessFileSyncDto dto) {
        if (dto == null || dto.getBizId() == null) {
            throw new IllegalArgumentException("同步附件缺少业务ID");
        }
        BizFileType type = resolveType(dto.getBizType());
        String kind = type.normalizeKind(dto.getFileKind());
        long ref = dto.getRefId() == null ? 0L : dto.getRefId();
        validateParent(type, dto.getBizId(), ref);

        List<Long> fileIds = dto.getFileIds() == null ? Collections.emptyList()
                : dto.getFileIds().stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        int max = type.maxCount(kind);
        if (max >= 0 && fileIds.size() > max) {
            throw new IllegalArgumentException(
                    type.getDisplayName() + " 的 " + kind + " 槽位最多允许 " + max + " 个文件");
        }
        if (!fileIds.isEmpty()) {
            List<FileInfo> files = fileInfoService.listByIds(fileIds);
            if (files.size() != fileIds.size()) {
                throw new IllegalArgumentException("存在无效或已删除的文件ID");
            }
        }

        // 先全部解绑，再按目标顺序重建，保证顺序与前端一致
        baseMapper.physicalDelete(type.getCode(), dto.getBizId(), ref, kind);
        int sort = 0;
        for (Long fileId : fileIds) {
            BusinessFile relation = new BusinessFile();
            relation.setBizType(type.getCode());
            relation.setBizId(dto.getBizId());
            relation.setRefId(ref);
            relation.setFileId(fileId);
            relation.setFileKind(kind);
            relation.setSortOrder(sort++);
            relation.setIsDeleted(0);
            save(relation);
        }
        return list(type.getCode(), dto.getBizId(), ref, kind);
    }

    /**
     * 解绑并删除单个附件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long relationId) {
        BusinessFile relation = getById(relationId);
        if (relation == null) {
            throw new IllegalArgumentException("附件关联不存在");
        }
        FileInfo fileInfo = fileInfoService.getById(relation.getFileId());
        baseMapper.physicalDeleteById(relationId);

        // 文件不再被任何业务引用时，软删除文件记录（磁盘文件由文件管理模块回收）
        if (baseMapper.countByFileId(relation.getFileId()) == 0) {
            fileInfoService.deleteFile(relation.getFileId());
            if (fileInfo != null) {
                fileOperationLogService.log(fileInfo.getFileId(), fileInfo.getOriginalName(),
                        fileInfo.getFilePath(), "DELETE", "business",
                        "解绑附件, bizType=" + relation.getBizType() + ", bizId=" + relation.getBizId()
                                + ", refId=" + relation.getRefId());
            }
        }
    }

    /**
     * 解绑某业务对象的全部附件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int unbindAll(String bizType, Long bizId, Long refId, String fileKind) {
        BizFileType type = resolveType(bizType);
        if (bizId == null || bizId <= 0) {
            return 0;
        }
        String kind = StringUtils.hasText(fileKind) ? type.normalizeKind(fileKind) : null;

        List<BusinessFile> relations = list(new LambdaQueryWrapper<BusinessFile>()
                .eq(BusinessFile::getBizType, type.getCode())
                .eq(BusinessFile::getBizId, bizId)
                .eq(refId != null, BusinessFile::getRefId, refId)
                .eq(kind != null, BusinessFile::getFileKind, kind));
        if (relations.isEmpty()) {
            return 0;
        }
        baseMapper.physicalDelete(type.getCode(), bizId, refId, kind);
        for (BusinessFile relation : relations) {
            if (baseMapper.countByFileId(relation.getFileId()) == 0) {
                fileInfoService.deleteFile(relation.getFileId());
            }
        }
        return relations.size();
    }

    // endregion

    // region 注册表
    // ===================================
    // 注册表
    // ===================================

    /**
     * 获取全部业务类型定义
     */
    @Override
    public List<BizFileTypeDto> listTypes() {
        return Arrays.stream(BizFileType.values())
                .map(type -> new BizFileTypeDto(type.getCode(), type.getDisplayName(), type.getKindMaxCount()))
                .collect(Collectors.toList());
    }

    // endregion

    // region 私有方法
    // ===================================
    // 私有方法
    // ===================================

    /**
     * 解析并校验业务类型
     *
     * @param code 业务类型编码
     * @return 业务类型
     */
    private BizFileType resolveType(String code) {
        BizFileType type = StringUtils.hasText(code) ? BizFileType.resolve(code) : null;
        if (type == null) {
            throw new IllegalArgumentException("不支持的业务类型: " + code);
        }
        return type;
    }

    /**
     * 校验父记录存在且未删除
     *
     * @param type  业务类型
     * @param bizId 业务ID
     * @param refId 子对象ID
     */
    private void validateParent(BizFileType type, Long bizId, long refId) {
        if (bizId == null || bizId <= 0) {
            throw new IllegalArgumentException("业务ID不能为空");
        }
        if (type == BizFileType.APPROVAL_INSTANCE && refId > 0) {
            // 审批：节点必须存在，且节点所属工作流与实例一致
            String sql = "SELECT COUNT(*) FROM approval_node n "
                    + "JOIN approval_instance i ON i.workflow_id = n.workflow_id "
                    + "WHERE n.id = ? AND i.id = ? AND n.is_deleted = 0 AND i.is_deleted = 0";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, refId, bizId);
            if (count == null || count == 0) {
                throw new IllegalArgumentException("审批节点不存在或不属于该审批实例");
            }
            return;
        }
        String sql = "SELECT COUNT(*) FROM " + type.getTableName()
                + " WHERE " + type.getPkColumn() + " = ? AND is_deleted = 0";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, bizId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("业务记录不存在或已删除：" + type.getDisplayName());
        }
    }

    /**
     * 关系列表转返回对象
     *
     * @param relations 关系列表
     * @return 返回对象列表
     */
    private List<BusinessFileDto> toDtoList(List<BusinessFile> relations) {
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> fileIds = relations.stream().map(BusinessFile::getFileId).distinct().collect(Collectors.toList());
        Map<Long, FileInfo> fileMap = fileInfoService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(FileInfo::getFileId, file -> file));
        return relations.stream()
                .map(relation -> {
                    FileInfo file = fileMap.get(relation.getFileId());
                    return file == null ? null : BusinessFileDto.from(relation, file);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // endregion
}
