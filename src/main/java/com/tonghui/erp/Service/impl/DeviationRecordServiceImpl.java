package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Quality.DeviationRecordSaveDto;
import com.tonghui.erp.Common.Dto.Quality.DeviationRecordWithAttachmentsDto;
import com.tonghui.erp.Common.Dto.Quality.WorkOrderOptionDto;
import com.tonghui.erp.Data.Entity.DeviationRecord;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.mapper.DeviationRecordMapper;
import com.tonghui.erp.Data.mapper.FileInfoMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Service.DeviationRecordService;
import com.tonghui.erp.Service.FileInfoService;
import com.tonghui.erp.Service.FileStorageService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 偏差记录服务实现类
 * <p>
 * 实现偏差记录的业务规则：
 * 1. 偏差编号PC-YYYYMMDD-NNN由服务端自动生成，不可修改，唯一索引冲突时自动重试（最多3次）
 * 2. 保存时从生产任务表快照任务编号/制剂名称/批号（仅作展示，不联动任务状态）
 * 3. 附件通过FileInfo的business_id/business_type建立业务关联
 * </p>
 */
@Service
public class DeviationRecordServiceImpl extends ServiceImpl<DeviationRecordMapper, DeviationRecord>
        implements DeviationRecordService {

    // region 常量定义
    // ===================================
    // 常量定义
    // ===================================

    /**
     * 偏差编号前缀
     */
    private static final String CODE_PREFIX = "PC";

    /**
     * 附件业务类型（父类型QUALITY + 子类型DEVIATION_RECORD）
     */
    private static final String BUSINESS_TYPE_DEVIATION_RECORD = "QUALITY_DEVIATION_RECORD";

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
     * 用户Mapper（查询创建人/更新人姓名）
     */
    @Autowired
    private UserMapper userMapper;

    /**
     * 生产任务Mapper（下拉查询与快照）
     */
    @Autowired
    private WorkOrderMapper workOrderMapper;

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

    // region 编号生成
    // ===================================
    // 编号生成
    // ===================================

    @Override
    public String generateCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String maxCode = baseMapper.selectMaxCodeByPrefix(CODE_PREFIX + "-" + dateStr);
        if (maxCode == null) {
            return String.format("%s-%s-%03d", CODE_PREFIX, dateStr, 1);
        }
        // 编号格式 前缀(2) + "-" + 日期(8) + "-"，序号位于最后一个"-"之后
        try {
            int seq = Integer.parseInt(maxCode.substring(maxCode.lastIndexOf('-') + 1));
            return String.format("%s-%s-%03d", CODE_PREFIX, dateStr, seq + 1);
        } catch (NumberFormatException e) {
            // 异常时从1开始（同日期下正常不会走到这里）
            return String.format("%s-%s-%03d", CODE_PREFIX, dateStr, 1);
        }
    }

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    @Override
    public PagedResult<DeviationRecordWithAttachmentsDto> search(String keyword, String workOrderCode,
                                               String startDate, String endDate, PageRequestDto pageRequest) {
        pageRequest.validateAndFix();
        LambdaQueryWrapper<DeviationRecord> wrapper = new LambdaQueryWrapper<>();
        // 关键字：模糊匹配偏差编号或偏差标题
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(DeviationRecord::getDeviationCode, keyword)
                    .or().like(DeviationRecord::getDeviationTitle, keyword));
        }
        // 生产任务编号：模糊匹配快照字段
        if (StringUtils.hasText(workOrderCode)) {
            wrapper.like(DeviationRecord::getWorkOrderCode, workOrderCode);
        }
        // 实际发生时间范围
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(DeviationRecord::getOccurTime, startDate + " 00:00:00");
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(DeviationRecord::getOccurTime, endDate + " 23:59:59");
        }
        wrapper.orderByDesc(DeviationRecord::getCreatedTime);
        Page<DeviationRecord> page = baseMapper.selectPage(PagedResult.toMybatisPage(pageRequest), wrapper);
        // 转DTO并批量回填创建人姓名（按创建人ID一次批量查用户，避免N+1）
        List<DeviationRecordWithAttachmentsDto> dtoList = new java.util.ArrayList<>(page.getRecords().size());
        java.util.Map<Long, User> userMap = loadUserMap(page.getRecords());
        for (DeviationRecord record : page.getRecords()) {
            DeviationRecordWithAttachmentsDto dto = new DeviationRecordWithAttachmentsDto();
            BeanUtils.copyProperties(record, dto);
            dto.setCreatedByName(resolveUserNameFromMap(record.getCreatedBy(), userMap));
            dtoList.add(dto);
        }
        Page<DeviationRecordWithAttachmentsDto> dtoPage =
                new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        dtoPage.setRecords(dtoList);
        return PagedResult.fromPage(dtoPage, pageRequest);
    }

    @Override
    public DeviationRecordWithAttachmentsDto getDetail(Long id) {
        DeviationRecord record = baseMapper.selectById(id);
        if (record == null) {
            return null;
        }
        DeviationRecordWithAttachmentsDto dto = new DeviationRecordWithAttachmentsDto();
        BeanUtils.copyProperties(record, dto);
        dto.setAttachments(fileInfoService.getFilesByBusiness(id, BUSINESS_TYPE_DEVIATION_RECORD, null));
        dto.setCreatedByName(resolveUserName(record.getCreatedBy()));
        dto.setUpdatedByName(resolveUserName(record.getUpdatedBy()));
        return dto;
    }

    @Override
    public List<WorkOrderOptionDto> searchWorkOrders(String keyword) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(WorkOrder::getWorkOrderCode, keyword)
                    .or().like(WorkOrder::getPreparationName, keyword));
        }
        wrapper.orderByDesc(WorkOrder::getCreatedTime);
        wrapper.last("LIMIT 20");
        return workOrderMapper.selectList(wrapper).stream().map(workOrder -> {
            WorkOrderOptionDto option = new WorkOrderOptionDto();
            option.setWorkOrderId(workOrder.getWorkOrderId());
            option.setWorkOrderCode(workOrder.getWorkOrderCode());
            option.setPreparationName(workOrder.getPreparationName());
            option.setBatchNumber(workOrder.getBatchNumber());
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
    public DeviationRecord create(DeviationRecordSaveDto dto) {
        validateRequired(dto);
        DeviationRecord record = new DeviationRecord();
        BeanUtils.copyProperties(dto, record);
        // 快照生产任务字段
        snapshotWorkOrder(record, dto.getWorkOrderId());
        // 自动编号 + 唯一索引冲突重试
        int attempts = 0;
        while (true) {
            record.setDeviationCode(generateCode());
            try {
                baseMapper.insert(record);
                break;
            } catch (DuplicateKeyException e) {
                if (++attempts >= MAX_CODE_RETRY) {
                    throw new RuntimeException("偏差编号生成冲突，请稍后重试", e);
                }
            }
        }
        // 绑定附件
        bindAttachments(record.getId(), dto.getAttachmentFileIds());
        return record;
    }

    @Override
    @Transactional
    public DeviationRecord update(Long id, DeviationRecordSaveDto dto) {
        DeviationRecord existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("偏差记录不存在");
        }
        validateRequired(dto);
        DeviationRecord record = new DeviationRecord();
        BeanUtils.copyProperties(dto, record);
        // 编号不可修改，保留原值
        record.setDeviationCode(existing.getDeviationCode());
        record.setId(id);
        // 重新快照生产任务字段
        snapshotWorkOrder(record, dto.getWorkOrderId());
        baseMapper.updateById(record);
        // 同步附件（解绑移除项、绑定新增项）
        syncAttachments(id, dto.getAttachmentFileIds());
        return baseMapper.selectById(id);
    }

    // endregion

    // region 附件
    // ===================================
    // 附件
    // ===================================

    @Override
    public List<FileInfo> getAttachments(Long id) {
        if (baseMapper.selectById(id) == null) {
            throw new RuntimeException("偏差记录不存在");
        }
        return fileInfoService.getFilesByBusiness(id, BUSINESS_TYPE_DEVIATION_RECORD, null);
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 校验新增/修改必填字段
     *
     * @param dto 保存请求体
     */
    private void validateRequired(DeviationRecordSaveDto dto) {
        if (dto.getWorkOrderId() == null) {
            throw new RuntimeException("关联生产任务必填");
        }
        if (!StringUtils.hasText(dto.getDeviationTitle())) {
            throw new RuntimeException("偏差标题必填");
        }
        if (dto.getDeviationTitle().length() > 200) {
            throw new RuntimeException("偏差标题不能超过200字符");
        }
        if (dto.getOccurTime() == null) {
            throw new RuntimeException("实际发生时间必填");
        }
        if (!StringUtils.hasText(dto.getDescription())) {
            throw new RuntimeException("偏差描述必填");
        }
        if (dto.getDescription().length() > 5000) {
            throw new RuntimeException("偏差描述不能超过5000字符");
        }
    }

    /**
     * 从生产任务表快照任务编号/制剂名称/批号到偏差记录
     *
     * @param record      偏差记录
     * @param workOrderId 生产任务ID
     */
    private void snapshotWorkOrder(DeviationRecord record, Long workOrderId) {
        WorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw new RuntimeException("关联生产任务不存在：" + workOrderId);
        }
        record.setWorkOrderId(workOrderId);
        record.setWorkOrderCode(workOrder.getWorkOrderCode());
        record.setPreparationName(workOrder.getPreparationName());
        record.setBatchNumber(workOrder.getBatchNumber());
    }

    /**
     * 新增时绑定附件（在FileInfo上打business_id/business_type标记）
     *
     * @param recordId    偏差记录ID
     * @param fileIds     附件文件ID列表（可为空）
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
            fileStorageService.updateBusinessInfo(fileInfo, recordId, BUSINESS_TYPE_DEVIATION_RECORD);
            fileInfoMapper.updateById(fileInfo);
        }
    }

    /**
     * 修改时同步附件：解绑已移除项、绑定新增项
     *
     * @param recordId 偏差记录ID
     * @param fileIds  最新的附件文件ID列表（null按空列表处理，即清空全部附件）
     */
    private void syncAttachments(Long recordId, List<Long> fileIds) {
        Set<Long> newIds = fileIds == null ? Set.of() : fileIds.stream().collect(Collectors.toSet());
        // 解绑当前已绑定但新列表中不存在的文件
        List<FileInfo> boundFiles = fileInfoService.getFilesByBusiness(recordId, BUSINESS_TYPE_DEVIATION_RECORD, null);
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
            fileStorageService.updateBusinessInfo(fileInfo, recordId, BUSINESS_TYPE_DEVIATION_RECORD);
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
     * @param records 偏差记录列表
     * @return 用户ID到用户实体的映射，无数据时返回空映射
     */
    private java.util.Map<Long, User> loadUserMap(List<DeviationRecord> records) {
        List<Long> userIds = records.stream()
                .map(DeviationRecord::getCreatedBy)
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

    // endregion
}
