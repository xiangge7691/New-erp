package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Quality.DestructionRecordDetailSaveDto;
import com.tonghui.erp.Common.Dto.Quality.DestructionRecordSaveDto;
import com.tonghui.erp.Common.Dto.Quality.DestructionRecordWithDetailsDto;
import com.tonghui.erp.Common.Dto.Quality.RetainedSampleOptionDto;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.DestructionRecord;
import com.tonghui.erp.Data.Entity.DestructionRecordDetail;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.RetainedSample;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.mapper.DestructionRecordDetailMapper;
import com.tonghui.erp.Data.mapper.DestructionRecordMapper;
import com.tonghui.erp.Data.mapper.FileInfoMapper;
import com.tonghui.erp.Data.mapper.RetainedSampleMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Service.DestructionRecordService;
import com.tonghui.erp.Service.FileInfoService;
import com.tonghui.erp.Service.FileStorageService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 销毁记录服务实现类
 * <p>
 * 实现销毁记录（不合格品销毁/留样及稳定性考察产品销毁）的业务规则：
 * 1. 销毁单号按记录类型选择前缀（BPXH/LYXH）自动生成，不可修改，唯一索引冲突时自动重试（最多3次）
 * 2. 合计数量由服务端按明细数量求和计算，前端不传
 * 3. 更新时明细整体重建（按destruction_id物理删除旧行再整单重插）
 * 4. 作废语义：不提供删除接口，作废后禁止修改；作废记录status=作废并记录作废时间与原因
 * 5. 留样反写联动：仅「留样及稳定性考察产品销毁」类型生效，明细retained_sample_id命中留样时
 *    置留样为已销毁并写入销毁日期；移除引用、类型变更或销毁单作废时回滚为留样中
 *    （仍被其他正常销毁单引用则不回滚，且保存时禁止同一留样被多张正常销毁单引用）
 * 6. 不合格品销毁类型仅保存retained_sample_id作追溯引用，不联动留样状态
 * </p>
 */
@Service
public class DestructionRecordServiceImpl extends ServiceImpl<DestructionRecordMapper, DestructionRecord>
        implements DestructionRecordService {

    // region 常量定义
    // ===================================
    // 常量定义
    // ===================================

    /**
     * 不合格品销毁单号前缀
     */
    private static final String PREFIX_UNQUALIFIED = "BPXH";

    /**
     * 留样及稳定性考察产品销毁单号前缀
     */
    private static final String PREFIX_RETAINED = "LYXH";

    /**
     * 附件业务类型（父类型QUALITY + 子类型DESTRUCTION_RECORD）
     */
    private static final String BUSINESS_TYPE_DESTRUCTION_RECORD = "QUALITY_DESTRUCTION_RECORD";

    /**
     * 编号生成重试上限
     */
    private static final int MAX_CODE_RETRY = 3;

    // endregion

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /**
     * 销毁记录明细Mapper
     */
    @Autowired
    private DestructionRecordDetailMapper destructionRecordDetailMapper;

    /**
     * 用户Mapper（查询创建人/更新人姓名）
     */
    @Autowired
    private UserMapper userMapper;

    /**
     * 留样记录Mapper（下拉查询）
     */
    @Autowired
    private RetainedSampleMapper retainedSampleMapper;

    /**
     * 文件信息Mapper（附件绑定/解绑）
     */
    @Autowired
    private FileInfoMapper fileInfoMapper;

    /**
     * 文件存储服务（设置文件业务信息）
     */
    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 文件信息服务（按业务查询附件列表）
     */
    @Autowired
    private FileInfoService fileInfoService;

    // endregion

    // region 单号生成
    // ===================================
    // 单号生成
    // ===================================

    @Override
    public String generateCode(String recordType) {
        String prefix = resolvePrefix(recordType);
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String maxCode = baseMapper.selectMaxCodeByPrefix(prefix + "-" + dateStr);
        if (maxCode == null) {
            return String.format("%s-%s-%03d", prefix, dateStr, 1);
        }
        // 单号格式 前缀 + "-" + 日期(8) + "-"，序号位于最后一个"-"之后
        try {
            int seq = Integer.parseInt(maxCode.substring(maxCode.lastIndexOf('-') + 1));
            return String.format("%s-%s-%03d", prefix, dateStr, seq + 1);
        } catch (NumberFormatException e) {
            // 异常时从1开始（同日期下正常不会走到这里）
            return String.format("%s-%s-%03d", prefix, dateStr, 1);
        }
    }

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    @Override
    public PagedResult<DestructionRecordWithDetailsDto> search(String recordType, String keyword, String batchNo,
                                                 String status, String startDate, String endDate,
                                                 PageRequestDto pageRequest) {
        pageRequest.validateAndFix();
        LambdaQueryWrapper<DestructionRecord> wrapper = new LambdaQueryWrapper<>();
        // 记录类型：精确匹配
        if (StringUtils.hasText(recordType)) {
            wrapper.eq(DestructionRecord::getRecordType, recordType);
        }
        // 关键字：模糊匹配销毁单号
        if (StringUtils.hasText(keyword)) {
            wrapper.like(DestructionRecord::getDestructionCode, keyword);
        }
        // 批号：存在该批号明细的主表命中（先查明细ID再in，避免子查询拼接）
        if (StringUtils.hasText(batchNo)) {
            List<Long> matchedIds = destructionRecordDetailMapper.selectList(
                            new LambdaQueryWrapper<DestructionRecordDetail>()
                                    .like(DestructionRecordDetail::getBatchNo, batchNo))
                    .stream()
                    .map(DestructionRecordDetail::getDestructionId)
                    .distinct()
                    .collect(Collectors.toList());
            if (matchedIds.isEmpty()) {
                return PagedResult.empty();
            }
            wrapper.in(DestructionRecord::getId, matchedIds);
        }
        // 状态：精确匹配
        if (StringUtils.hasText(status)) {
            wrapper.eq(DestructionRecord::getStatus, status);
        }
        // 登记时间范围（created_time）
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(DestructionRecord::getCreatedTime, startDate + " 00:00:00");
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(DestructionRecord::getCreatedTime, endDate + " 23:59:59");
        }
        wrapper.orderByDesc(DestructionRecord::getCreatedTime);
        Page<DestructionRecord> page = baseMapper.selectPage(PagedResult.toMybatisPage(pageRequest), wrapper);
        List<DestructionRecord> records = page.getRecords();
        if (records.isEmpty()) {
            return PagedResult.empty();
        }
        // 批量查询本页主表的全部明细（按主表ID分组），并回填留样编号与创建人姓名
        java.util.Map<Long, User> userMap = loadUserMap(records);
        java.util.Map<Long, List<DestructionRecordDetail>> detailMap = loadDetailMap(records);
        List<DestructionRecordWithDetailsDto> dtoList = new java.util.ArrayList<>(records.size());
        for (DestructionRecord record : records) {
            DestructionRecordWithDetailsDto dto = new DestructionRecordWithDetailsDto();
            BeanUtils.copyProperties(record, dto);
            dto.setCreatedByName(resolveUserNameFromMap(record.getCreatedBy(), userMap));
            List<DestructionRecordDetail> details = detailMap.getOrDefault(record.getId(), new java.util.ArrayList<>());
            fillRetainedCodes(details);
            dto.setDetails(details);
            dtoList.add(dto);
        }
        Page<DestructionRecordWithDetailsDto> dtoPage =
                new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        dtoPage.setRecords(dtoList);
        return PagedResult.fromPage(dtoPage, pageRequest);
    }

    @Override
    public DestructionRecordWithDetailsDto getDetail(Long id) {
        DestructionRecord record = baseMapper.selectById(id);
        if (record == null) {
            return null;
        }
        DestructionRecordWithDetailsDto dto = new DestructionRecordWithDetailsDto();
        BeanUtils.copyProperties(record, dto);
        List<DestructionRecordDetail> details = destructionRecordDetailMapper.selectList(
                new LambdaQueryWrapper<DestructionRecordDetail>()
                        .eq(DestructionRecordDetail::getDestructionId, id));
        // 回填明细行关联的留样编号
        fillRetainedCodes(details);
        dto.setDetails(details);
        dto.setAttachments(fileInfoService.getFilesByBusiness(id, BUSINESS_TYPE_DESTRUCTION_RECORD, null));
        dto.setCreatedByName(resolveUserName(record.getCreatedBy()));
        dto.setUpdatedByName(resolveUserName(record.getUpdatedBy()));
        return dto;
    }

    @Override
    public List<RetainedSampleOptionDto> searchRetainedSamples(String keyword) {
        LambdaQueryWrapper<RetainedSample> wrapper = new LambdaQueryWrapper<>();
        // 仅返回状态为留样中的记录（已销毁的留样由销毁单反写后自动排除）
        wrapper.eq(RetainedSample::getStatus, RetainedSample.STATUS_RETAINING);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(RetainedSample::getRetainedCode, keyword)
                    .or().like(RetainedSample::getMaterialName, keyword)
                    .or().like(RetainedSample::getBatchNo, keyword));
        }
        wrapper.orderByDesc(RetainedSample::getCreatedTime);
        wrapper.last("LIMIT 20");
        return retainedSampleMapper.selectList(wrapper).stream().map(sample -> {
            RetainedSampleOptionDto option = new RetainedSampleOptionDto();
            option.setId(sample.getId());
            option.setRetainedCode(sample.getRetainedCode());
            option.setMaterialName(sample.getMaterialName());
            option.setSpec(sample.getSpec());
            option.setBatchNo(sample.getBatchNo());
            option.setRetainedQuantity(sample.getRetainedQuantity());
            option.setStorageLocation(sample.getStorageLocation());
            return option;
        }).collect(Collectors.toList());
    }

    // endregion

    // region 新增与修改
    // ===================================
    // 新增与修改
    // ===================================

    @Override
    @Transactional
    public DestructionRecord create(DestructionRecordSaveDto dto) {
        validateRequired(dto);
        DestructionRecord record = new DestructionRecord();
        BeanUtils.copyProperties(dto, record);
        // 合计数量由服务端按明细求和计算
        record.setTotalQuantity(sumDetailQuantity(dto.getDetails()));
        // 状态默认正常
        record.setStatus(DestructionRecord.STATUS_NORMAL);
        // 自动单号 + 唯一索引冲突重试
        int attempts = 0;
        while (true) {
            record.setDestructionCode(generateCode(dto.getRecordType()));
            try {
                baseMapper.insert(record);
                break;
            } catch (DuplicateKeyException e) {
                if (++attempts >= MAX_CODE_RETRY) {
                    throw new RuntimeException("销毁单号生成冲突，请稍后重试", e);
                }
            }
        }
        // 保存明细
        saveDetails(record.getId(), dto.getDetails());
        // 留样销毁类型反写留样状态（不合格品销毁类型不联动）
        applyRetainedLinkage(record.getRecordType(), record.getId(),
                collectSampleIds(dto.getDetails()), record.getDestroyDate());
        // 绑定附件
        bindAttachments(record.getId(), dto.getAttachmentFileIds());
        return record;
    }

    @Override
    @Transactional
    public DestructionRecord update(Long id, DestructionRecordSaveDto dto) {
        DestructionRecord existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("销毁记录不存在");
        }
        if (DestructionRecord.STATUS_VOID.equals(existing.getStatus())) {
            throw new RuntimeException("已作废的销毁记录不能修改");
        }
        validateRequired(dto);
        // 明细重建前先收集旧引用留样，用于后续回滚判断
        Set<Long> oldSampleIds = collectReferencedSampleIds(id);
        DestructionRecord record = new DestructionRecord();
        BeanUtils.copyProperties(dto, record);
        // 单号与状态不可修改，保留原值
        record.setDestructionCode(existing.getDestructionCode());
        record.setStatus(existing.getStatus());
        record.setId(id);
        // 合计数量由服务端按明细求和计算
        record.setTotalQuantity(sumDetailQuantity(dto.getDetails()));
        baseMapper.updateById(record);
        // 明细整体重建：物理删除旧行再整单重插
        destructionRecordDetailMapper.physicalDeleteByDestructionId(id);
        saveDetails(id, dto.getDetails());
        // 留样反写联动：新引用置已销毁；被移除的旧引用回滚为留样中（类型改为不合格品销毁时全部回滚）
        Set<Long> newSampleIds = DestructionRecord.TYPE_RETAINED.equals(record.getRecordType())
                ? collectSampleIds(dto.getDetails()) : Set.of();
        applyRetainedLinkage(record.getRecordType(), id, newSampleIds, record.getDestroyDate());
        Set<Long> rollbackIds = new java.util.HashSet<>(oldSampleIds);
        rollbackIds.removeAll(newSampleIds);
        rollbackRetainedLinkage(id, rollbackIds);
        // 同步附件（解绑移除项、绑定新增项）
        syncAttachments(id, dto.getAttachmentFileIds());
        return baseMapper.selectById(id);
    }

    @Override
    @Transactional
    public void voidRecord(Long id, String voidRemark) {
        DestructionRecord existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("销毁记录不存在");
        }
        if (DestructionRecord.STATUS_VOID.equals(existing.getStatus())) {
            throw new RuntimeException("该销毁记录已作废");
        }
        if (StringUtils.hasText(voidRemark) && voidRemark.length() > 500) {
            throw new RuntimeException("作废原因不能超过500字符");
        }
        // 作废前收集本单引用的留样，作废后回滚其状态
        Set<Long> sampleIds = DestructionRecord.TYPE_RETAINED.equals(existing.getRecordType())
                ? collectReferencedSampleIds(id) : Set.of();
        DestructionRecord record = new DestructionRecord();
        record.setId(id);
        record.setStatus(DestructionRecord.STATUS_VOID);
        record.setVoidTime(LocalDateTime.now());
        record.setVoidRemark(voidRemark);
        baseMapper.updateById(record);
        // 留样销毁类型：作废即视为销毁未生效，回滚留样为留样中（仍被其他正常销毁单引用则不回滚）
        rollbackRetainedLinkage(id, sampleIds);
    }

    // endregion

    // region 附件
    // ===================================
    // 附件
    // ===================================

    @Override
    public List<FileInfo> getAttachments(Long id) {
        if (baseMapper.selectById(id) == null) {
            throw new RuntimeException("销毁记录不存在");
        }
        return fileInfoService.getFilesByBusiness(id, BUSINESS_TYPE_DESTRUCTION_RECORD, null);
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 根据记录类型解析单号前缀
     *
     * @param recordType 记录类型
     * @return 单号前缀
     */
    private String resolvePrefix(String recordType) {
        if (DestructionRecord.TYPE_UNQUALIFIED.equals(recordType)) {
            return PREFIX_UNQUALIFIED;
        }
        if (DestructionRecord.TYPE_RETAINED.equals(recordType)) {
            return PREFIX_RETAINED;
        }
        throw new RuntimeException("无效的记录类型：" + recordType);
    }

    /**
     * 校验新增/修改必填字段
     *
     * @param dto 保存请求体
     */
    private void validateRequired(DestructionRecordSaveDto dto) {
        // 记录类型必填且合法（resolvePrefix内部校验合法性）
        resolvePrefix(dto.getRecordType());
        // 明细必填且至少1行
        if (dto.getDetails() == null || dto.getDetails().isEmpty()) {
            throw new RuntimeException("销毁明细不能为空，至少需要1行");
        }
        for (DestructionRecordDetailSaveDto detail : dto.getDetails()) {
            if (!StringUtils.hasText(detail.getMaterialName())) {
                throw new RuntimeException("明细品名必填");
            }
            if (detail.getMaterialName().length() > 100) {
                throw new RuntimeException("明细品名不能超过100字符");
            }
        }
    }

    /**
     * 按明细数量列表求和计算合计数量
     *
     * @param details 明细列表
     * @return 合计数量（空明细返回0）
     */
    private BigDecimal sumDetailQuantity(List<DestructionRecordDetailSaveDto> details) {
        if (details == null || details.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return details.stream()
                .map(d -> d.getQuantity() == null ? BigDecimal.ZERO : d.getQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 保存明细行（绑定主表ID并逐行插入）
     *
     * @param destructionId 销毁记录主表ID
     * @param details       明细保存请求列表
     */
    private void saveDetails(Long destructionId, List<DestructionRecordDetailSaveDto> details) {
        for (DestructionRecordDetailSaveDto detailDto : details) {
            DestructionRecordDetail detail = new DestructionRecordDetail();
            BeanUtils.copyProperties(detailDto, detail);
            detail.setDestructionId(destructionId);
            destructionRecordDetailMapper.insert(detail);
        }
    }

    /**
     * 新增时绑定附件（在FileInfo上打business_id/business_type标记）
     *
     * @param recordId 销毁记录ID
     * @param fileIds  附件文件ID列表（可为空）
     */
    private void bindAttachments(Long recordId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        for (Long fileId : fileIds) {
            FileInfo fileInfo = fileInfoMapper.selectById(fileId);
            if (fileInfo == null) {
                throw new RuntimeException("附件文件不存在：" + fileId);
            }
            fileStorageService.updateBusinessInfo(fileInfo, recordId, BUSINESS_TYPE_DESTRUCTION_RECORD);
            fileInfoMapper.updateById(fileInfo);
        }
    }

    /**
     * 修改时同步附件：解绑已移除项、绑定新增项
     *
     * @param recordId 销毁记录ID
     * @param fileIds  最新的附件文件ID列表（null按空列表处理，即清空全部附件）
     */
    private void syncAttachments(Long recordId, List<Long> fileIds) {
        Set<Long> newIds = fileIds == null ? Set.of() : fileIds.stream().collect(Collectors.toSet());
        // 解绑当前已绑定但新列表中不存在的文件
        List<FileInfo> boundFiles = fileInfoService.getFilesByBusiness(recordId, BUSINESS_TYPE_DESTRUCTION_RECORD, null);
        for (FileInfo bound : boundFiles) {
            if (!newIds.contains(bound.getFileId())) {
                UpdateWrapper<FileInfo> wrapper = new UpdateWrapper<>();
                wrapper.eq("file_id", bound.getFileId())
                        .set("business_id", null)
                        .set("business_type", null);
                fileInfoMapper.update(null, wrapper);
            }
        }
        // 绑定新增文件（已绑定的跳过）
        Set<Long> boundIds = boundFiles.stream().map(FileInfo::getFileId).collect(Collectors.toSet());
        for (Long fileId : newIds) {
            if (boundIds.contains(fileId)) {
                continue;
            }
            FileInfo fileInfo = fileInfoMapper.selectById(fileId);
            if (fileInfo == null) {
                throw new RuntimeException("附件文件不存在：" + fileId);
            }
            fileStorageService.updateBusinessInfo(fileInfo, recordId, BUSINESS_TYPE_DESTRUCTION_RECORD);
            fileInfoMapper.updateById(fileInfo);
        }
    }

    /**
     * 根据用户ID解析用户显示名称（真实姓名，无则回退登录账号）
     *
     * @param userId 用户ID
     * @return 用户名称
     */
    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getUserName()) ? user.getUserName() : user.getUserAccount();
    }

    /**
     * 批量加载创建人用户映射（按用户ID索引），用于列表批量回填创建人姓名
     *
     * @param records 销毁记录列表
     * @return 用户ID到用户实体的映射，无数据时返回空映射
     */
    private java.util.Map<Long, User> loadUserMap(List<DestructionRecord> records) {
        List<Long> userIds = records.stream()
                .map(DestructionRecord::getCreatedBy)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return java.util.Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));
    }

    /**
     * 从用户映射中解析用户显示名称（真实姓名，无则回退登录账号）
     *
     * @param userId   用户ID
     * @param userMap  用户ID到用户实体的映射
     * @return 用户名称
     */
    private String resolveUserNameFromMap(Long userId, java.util.Map<Long, User> userMap) {
        if (userId == null) {
            return null;
        }
        User user = userMap.get(userId);
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getUserName()) ? user.getUserName() : user.getUserAccount();
    }

    /**
     * 批量查询本页主表的全部明细并按主表ID分组
     *
     * @param records 销毁记录列表
     * @return 主表ID到明细列表的映射，无数据时返回空映射
     */
    private java.util.Map<Long, List<DestructionRecordDetail>> loadDetailMap(List<DestructionRecord> records) {
        List<Long> ids = records.stream()
                .map(DestructionRecord::getId)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return java.util.Map.of();
        }
        return destructionRecordDetailMapper.selectList(
                        new LambdaQueryWrapper<DestructionRecordDetail>()
                                .in(DestructionRecordDetail::getDestructionId, ids))
                .stream()
                .collect(Collectors.groupingBy(DestructionRecordDetail::getDestructionId));
    }

    /**
     * 批量回填明细行关联留样编号（按retainedSampleId一次批量查留样，避免N+1）
     *
     * @param details 销毁明细列表
     */
    private void fillRetainedCodes(List<DestructionRecordDetail> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        List<Long> sampleIds = details.stream()
                .map(DestructionRecordDetail::getRetainedSampleId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (sampleIds.isEmpty()) {
            return;
        }
        java.util.Map<Long, String> codeMap = retainedSampleMapper.selectBatchIds(sampleIds).stream()
                .collect(Collectors.toMap(RetainedSample::getId, RetainedSample::getRetainedCode, (a, b) -> a));
        for (DestructionRecordDetail detail : details) {
            if (detail.getRetainedSampleId() != null) {
                detail.setRetainedCode(codeMap.get(detail.getRetainedSampleId()));
            }
        }
    }

    // endregion

    // region 留样反写联动
    // ===================================
    // 留样反写联动
    // ===================================

    /**
     * 从保存请求的明细列表中收集关联留样ID（去重，忽略空值）
     *
     * @param details 明细保存请求列表
     * @return 留样ID集合，空明细返回空集合
     */
    private Set<Long> collectSampleIds(List<DestructionRecordDetailSaveDto> details) {
        if (details == null || details.isEmpty()) {
            return new java.util.LinkedHashSet<>();
        }
        return details.stream()
                .map(DestructionRecordDetailSaveDto::getRetainedSampleId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    /**
     * 查询指定销毁单已落库明细所引用的留样ID集合
     *
     * @param destructionId 销毁记录主表ID
     * @return 留样ID集合，无引用时返回空集合
     */
    private Set<Long> collectReferencedSampleIds(Long destructionId) {
        return destructionRecordDetailMapper.selectList(
                        new LambdaQueryWrapper<DestructionRecordDetail>()
                                .eq(DestructionRecordDetail::getDestructionId, destructionId))
                .stream()
                .map(DestructionRecordDetail::getRetainedSampleId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    /**
     * 反写留样为已销毁（仅「留样及稳定性考察产品销毁」类型生效）
     * <p>
     * 销毁日期取销毁单的销毁日期，未填写时取登记当天（满足留样模块「已销毁必填销毁日期」的校验）。
     * 保存前校验留样存在且未被其他正常销毁单引用，避免同一留样被重复销毁。
     * </p>
     *
     * @param recordType 记录类型（不合格品销毁/留样及稳定性考察产品销毁）
     * @param recordId   当前销毁记录ID（用于排除自身引用）
     * @param sampleIds  本次需要反写的留样ID集合
     * @param destroyDate 销毁单上的销毁日期（可空）
     */
    private void applyRetainedLinkage(String recordType, Long recordId,
                                      Set<Long> sampleIds, LocalDate destroyDate) {
        if (!DestructionRecord.TYPE_RETAINED.equals(recordType) || sampleIds == null || sampleIds.isEmpty()) {
            return;
        }
        LocalDate linkageDate = destroyDate != null ? destroyDate : LocalDate.now();
        for (Long sampleId : sampleIds) {
            RetainedSample sample = retainedSampleMapper.selectById(sampleId);
            if (sample == null) {
                throw new RuntimeException("关联留样记录不存在：" + sampleId);
            }
            if (hasOtherActiveReference(sampleId, recordId)) {
                throw new RuntimeException("留样 " + sample.getRetainedCode()
                        + " 已被其他有效销毁单引用，不能重复销毁");
            }
            // 必须使用UpdateWrapper：updateById会忽略null值，无法清空/写入销毁日期
            UpdateWrapper<RetainedSample> wrapper = new UpdateWrapper<>();
            wrapper.eq("id", sampleId)
                    .set("status", RetainedSample.STATUS_DESTROYED)
                    .set("destroy_date", linkageDate)
                    .set("updated_by", EntityUtils.getCurrentUserId())
                    .set("updated_time", LocalDateTime.now());
            retainedSampleMapper.update(null, wrapper);
        }
    }

    /**
     * 回滚留样为留样中并清空销毁日期
     * <p>
     * 仅当该留样不再被其他正常销毁单引用时回滚；作废销毁单的明细仍保留，
     * 因此引用判断必须结合销毁单主表状态，不能只看明细表。
     * </p>
     *
     * @param excludeRecordId 需要排除的销毁记录ID（当前正在修改/作废的单据）
     * @param sampleIds       待回滚的留样ID集合
     */
    private void rollbackRetainedLinkage(Long excludeRecordId, Set<Long> sampleIds) {
        if (sampleIds == null || sampleIds.isEmpty()) {
            return;
        }
        for (Long sampleId : sampleIds) {
            if (hasOtherActiveReference(sampleId, excludeRecordId)) {
                continue;
            }
            UpdateWrapper<RetainedSample> wrapper = new UpdateWrapper<>();
            wrapper.eq("id", sampleId)
                    .set("status", RetainedSample.STATUS_RETAINING)
                    .set("destroy_date", null)
                    .set("updated_by", EntityUtils.getCurrentUserId())
                    .set("updated_time", LocalDateTime.now());
            retainedSampleMapper.update(null, wrapper);
        }
    }

    /**
     * 判断指定留样是否被其他正常状态的销毁单引用
     *
     * @param sampleId        留样记录ID
     * @param excludeRecordId 需要排除的销毁记录ID（可空）
     * @return 存在其他正常销毁单引用返回true
     */
    private boolean hasOtherActiveReference(Long sampleId, Long excludeRecordId) {
        List<Long> otherRecordIds = destructionRecordDetailMapper.selectList(
                        new LambdaQueryWrapper<DestructionRecordDetail>()
                                .eq(DestructionRecordDetail::getRetainedSampleId, sampleId)
                                .ne(excludeRecordId != null,
                                        DestructionRecordDetail::getDestructionId, excludeRecordId))
                .stream()
                .map(DestructionRecordDetail::getDestructionId)
                .distinct()
                .collect(Collectors.toList());
        if (otherRecordIds.isEmpty()) {
            return false;
        }
        return baseMapper.selectBatchIds(otherRecordIds).stream()
                .anyMatch(record -> DestructionRecord.STATUS_NORMAL.equals(record.getStatus()));
    }

    // endregion
}
