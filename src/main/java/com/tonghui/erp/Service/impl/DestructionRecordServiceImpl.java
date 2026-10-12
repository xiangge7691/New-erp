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
 * 5. 明细行retained_sample_id仅作追溯引用，保存销毁记录不联动留样状态
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
    public PagedResult<DestructionRecord> search(String recordType, String keyword, String batchNo,
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
        return PagedResult.fromPage(page, pageRequest);
    }

    @Override
    public DestructionRecordWithDetailsDto getDetail(Long id) {
        DestructionRecord record = baseMapper.selectById(id);
        if (record == null) {
            return null;
        }
        DestructionRecordWithDetailsDto dto = new DestructionRecordWithDetailsDto();
        BeanUtils.copyProperties(record, dto);
        dto.setDetails(destructionRecordDetailMapper.selectList(
                new LambdaQueryWrapper<DestructionRecordDetail>()
                        .eq(DestructionRecordDetail::getDestructionId, id)));
        dto.setAttachments(fileInfoService.getFilesByBusiness(id, BUSINESS_TYPE_DESTRUCTION_RECORD, null));
        dto.setCreatedByName(resolveUserName(record.getCreatedBy()));
        dto.setUpdatedByName(resolveUserName(record.getUpdatedBy()));
        return dto;
    }

    @Override
    public List<RetainedSampleOptionDto> searchRetainedSamples(String keyword) {
        LambdaQueryWrapper<RetainedSample> wrapper = new LambdaQueryWrapper<>();
        // 仅返回状态为留样中的记录
        wrapper.eq(RetainedSample::getStatus, "留样中");
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
        DestructionRecord record = new DestructionRecord();
        record.setId(id);
        record.setStatus(DestructionRecord.STATUS_VOID);
        record.setVoidTime(LocalDateTime.now());
        record.setVoidRemark(voidRemark);
        baseMapper.updateById(record);
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

    // endregion
}
