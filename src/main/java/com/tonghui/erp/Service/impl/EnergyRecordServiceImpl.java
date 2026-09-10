package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.Energy.EnergyRecordPageResult;
import com.tonghui.erp.Common.Dto.Energy.EnergySummaryDto;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.EnergyRecord;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.mapper.EnergyRecordMapper;
import com.tonghui.erp.Data.mapper.FileInfoMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Service.EnergyRecordService;
import com.tonghui.erp.Service.FileInfoService;
import com.tonghui.erp.Service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 能耗记录服务实现类
 * <p>
 * 实现能耗记录的业务规则：
 * 1. 计量单位按能耗类型自动映射（电→度，自来水/燃气→立方米）
 * 2. 实用量未提供时自动计算（本月表底 - 上月表底），负数报错
 * 3. 总价未提供时自动计算（实用量 × 单价），保留2位小数
 * 4. 操作人取当前登录用户，冗余姓名便于列表展示
 * </p>
 */
@Service
public class EnergyRecordServiceImpl implements EnergyRecordService {

    // region 常量定义
    // ===================================
    // 常量定义
    // ===================================

    /**
     * 凭证附件的业务类型（对应 FileStorageConfig 中的 ENERGY_RECORD 枚举）
     */
    private static final String BUSINESS_TYPE_ENERGY_RECORD = "ENERGY_RECORD";

    /**
     * 月份格式校验正则（YYYY-MM）
     */
    private static final String MONTH_PATTERN = "^\\d{4}-(0[1-9]|1[0-2])$";

    /**
     * 合法能耗类型列表
     */
    private static final Set<String> ENERGY_TYPES = Set.of("自来水", "电", "燃气");

    // endregion

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 能耗记录Mapper
     */
    @Autowired
    private EnergyRecordMapper energyRecordMapper;

    /**
     * 文件信息Mapper
     */
    @Autowired
    private FileInfoMapper fileInfoMapper;

    /**
     * 用户Mapper（查询操作人姓名）
     */
    @Autowired
    private UserMapper userMapper;

    /**
     * 文件存储服务（绑定凭证附件业务信息）
     */
    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 文件信息服务（查询凭证附件列表）
     */
    @Autowired
    private FileInfoService fileInfoService;

    // endregion

    // region 查询与汇总
    // ===================================
    // 查询与汇总
    // ===================================

    @Override
    public EnergyRecordPageResult pageQuery(String month, String energyType, int pageIndex, int pageSize) {
        int safePageIndex = Math.max(0, pageIndex);
        int safePageSize = pageSize <= 0 ? 20 : Math.min(100, pageSize);

        QueryWrapper<EnergyRecord> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(month)) {
            wrapper.eq("month", month);
        }
        if (StringUtils.hasText(energyType)) {
            wrapper.eq("energy_type", energyType);
        }
        wrapper.orderByDesc("month");

        Page<EnergyRecord> page = new Page<>(safePageIndex + 1, safePageSize);
        Page<EnergyRecord> result = energyRecordMapper.selectPage(page, wrapper);

        // 批量查询凭证附件关联，组装 hasAttachment 标记（避免逐条查询）
        Map<Long, List<FileInfo>> attachmentMap = loadAttachmentMap(result.getRecords());

        EnergyRecordPageResult pageResult = new EnergyRecordPageResult();
        pageResult.setItems(result.getRecords());
        pageResult.setTotalCount(result.getTotal());
        pageResult.setPageIndex(safePageIndex);
        pageResult.setPageSize(safePageSize);
        pageResult.setSummary(buildSummary(month, energyType));

        // 为列表项设置附件标记
        for (EnergyRecord record : result.getRecords()) {
            List<FileInfo> attachments = attachmentMap.get(record.getRecordId());
            record.setHasAttachment(attachments != null && !attachments.isEmpty());
        }

        return pageResult;
    }

    /**
     * 按筛选条件构建费用汇总
     *
     * @param month      月份（可为空）
     * @param energyType 能耗类型（可为空）
     * @return 汇总DTO（总金额 + 分类金额）
     */
    private EnergySummaryDto buildSummary(String month, String energyType) {
        EnergyRecordMapper.EnergySummary summary = energyRecordMapper.selectSummary(
                StringUtils.hasText(month) ? month : null,
                StringUtils.hasText(energyType) ? energyType : null);

        EnergySummaryDto dto = new EnergySummaryDto();
        dto.setTotalAmount(summary != null ? summary.getTotalAmount() : BigDecimal.ZERO);
        dto.addByType("自来水", summary != null ? summary.getWaterAmount() : BigDecimal.ZERO);
        dto.addByType("电", summary != null ? summary.getElectricityAmount() : BigDecimal.ZERO);
        dto.addByType("燃气", summary != null ? summary.getGasAmount() : BigDecimal.ZERO);
        return dto;
    }

    /**
     * 批量查询记录ID对应的凭证附件（business_type + business_id 关联）
     *
     * @param records 能耗记录列表
     * @return recordId -> 附件列表 的映射
     */
    private Map<Long, List<FileInfo>> loadAttachmentMap(List<EnergyRecord> records) {
        if (records == null || records.isEmpty()) {
            return Map.of();
        }
        Set<Long> recordIds = records.stream()
                .map(EnergyRecord::getRecordId)
                .collect(Collectors.toSet());

        QueryWrapper<FileInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("business_type", BUSINESS_TYPE_ENERGY_RECORD)
               .in("business_id", recordIds);
        List<FileInfo> attachments = fileInfoMapper.selectList(wrapper);

        return attachments.stream().collect(Collectors.groupingBy(FileInfo::getBusinessId));
    }

    // endregion

    // region 新增与编辑
    // ===================================
    // 新增与编辑
    // ===================================

    @Override
    @Transactional
    public EnergyRecord create(EnergyRecord record, Long attachmentFileId) {
        validateAndFill(record, true);
        energyRecordMapper.insert(record);
        bindAttachment(record.getRecordId(), attachmentFileId);
        return record;
    }

    @Override
    @Transactional
    public EnergyRecord update(Long id, EnergyRecord record, Long attachmentFileId) {
        EnergyRecord exist = energyRecordMapper.selectById(id);
        if (exist == null) {
            throw new RuntimeException("能耗记录不存在");
        }
        record.setRecordId(id);
        // 部分字段更新：用数据库现有值补齐未提供的字段（actualUsage/totalAmount 留空触发联动重算）
        mergeDefaults(record, exist);
        validateAndFill(record, false);
        energyRecordMapper.updateById(record);
        bindAttachment(id, attachmentFileId);
        return record;
    }

    /**
     * 用数据库现有值补齐请求体中未提供的字段
     * <p>
     * 月份/类型/表底/单价/备注为空时沿用现有值；
     * 实用量与总价不补齐（保持为空触发自动重算，实现改表底→用量联动、改单价→总价联动）
     * </p>
     *
     * @param target 请求体记录（待补齐）
     * @param source 数据库现有记录
     */
    private void mergeDefaults(EnergyRecord target, EnergyRecord source) {
        if (!StringUtils.hasText(target.getMonth())) {
            target.setMonth(source.getMonth());
        }
        if (!StringUtils.hasText(target.getEnergyType())) {
            target.setEnergyType(source.getEnergyType());
        }
        if (target.getLastMeterReading() == null) {
            target.setLastMeterReading(source.getLastMeterReading());
        }
        if (target.getCurrentMeterReading() == null) {
            target.setCurrentMeterReading(source.getCurrentMeterReading());
        }
        if (target.getUnitPrice() == null) {
            target.setUnitPrice(source.getUnitPrice());
        }
        if (target.getRemark() == null) {
            target.setRemark(source.getRemark());
        }
        // 操作人沿用原记录（编辑不改变操作人）
        target.setOperatorId(source.getOperatorId());
        target.setOperatorName(source.getOperatorName());
    }

    @Override
    public void delete(Long id) {
        EnergyRecord exist = energyRecordMapper.selectById(id);
        if (exist == null) {
            throw new RuntimeException("能耗记录不存在");
        }
        // 全局逻辑删除：自动转为 UPDATE is_deleted=1（凭证附件文件保留）
        energyRecordMapper.deleteById(id);
    }

    // endregion

    // region 业务规则处理
    // ===================================
    // 业务规则处理
    // ===================================

    /**
     * 校验并补齐能耗记录的业务字段
     * <p>
     * 1. 校验月份格式、能耗类型合法性
     * 2. 自动映射计量单位
     * 3. 实用量未提供时自动计算（本月表底 - 上月表底），负数报错
     * 4. 总价未提供时自动计算（实用量 × 单价），保留2位小数
     * 5. 新增时记录操作人；编辑时不覆盖操作人
     * </p>
     *
     * @param record   能耗记录
     * @param isCreate 是否为新增（true-新增，false-编辑）
     */
    private void validateAndFill(EnergyRecord record, boolean isCreate) {
        if (!StringUtils.hasText(record.getMonth())) {
            throw new RuntimeException("月份不能为空");
        }
        if (!record.getMonth().matches(MONTH_PATTERN)) {
            throw new RuntimeException("月份格式不正确，应为 YYYY-MM");
        }
        if (!StringUtils.hasText(record.getEnergyType())) {
            throw new RuntimeException("能耗类型不能为空");
        }
        if (!ENERGY_TYPES.contains(record.getEnergyType())) {
            throw new RuntimeException("能耗类型不合法，仅支持：自来水/电/燃气");
        }

        // 计量单位自动映射
        record.setUnit(getUnitByType(record.getEnergyType()));

        // 实用量：未提供且表底读数齐全时自动计算
        if (record.getActualUsage() == null
                && record.getLastMeterReading() != null
                && record.getCurrentMeterReading() != null) {
            record.setActualUsage(record.getCurrentMeterReading().subtract(record.getLastMeterReading()));
        }
        if (record.getActualUsage() != null && record.getActualUsage().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("本月实用量不能为负数");
        }

        // 总价：未提供且实用量与单价齐全时自动计算，保留2位小数
        if (record.getTotalAmount() == null
                && record.getActualUsage() != null
                && record.getUnitPrice() != null) {
            record.setTotalAmount(record.getActualUsage().multiply(record.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP));
        }
        if (record.getTotalAmount() != null && record.getTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("总价不能为负数");
        }

        // 新增时记录操作人
        if (isCreate) {
            Long currentUserId = EntityUtils.getCurrentUserId();
            record.setOperatorId(currentUserId);
            record.setOperatorName(resolveUserName(currentUserId));
        }
    }

    /**
     * 根据能耗类型获取计量单位
     *
     * @param energyType 能耗类型
     * @return 计量单位（电→度，自来水/燃气→立方米）
     */
    private String getUnitByType(String energyType) {
        if ("电".equals(energyType)) {
            return "度";
        }
        if ("自来水".equals(energyType) || "燃气".equals(energyType)) {
            return "立方米";
        }
        throw new RuntimeException("未知能耗类型: " + energyType);
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
     * 绑定凭证附件到能耗记录（在 FileInfo 上打 business_type/business_id 标记）
     * <p>
     * 文件本体已由前端调用文件管理上传接口存储，此处仅建立业务关联
     * </p>
     *
     * @param recordId        能耗记录ID
     * @param attachmentFileId 附件文件ID（可为空，空则跳过）
     */
    private void bindAttachment(Long recordId, Long attachmentFileId) {
        if (attachmentFileId == null) {
            return;
        }
        FileInfo fileInfo = fileInfoMapper.selectById(attachmentFileId);
        if (fileInfo == null) {
            throw new RuntimeException("附件文件不存在");
        }
        fileStorageService.updateBusinessInfo(fileInfo, recordId, BUSINESS_TYPE_ENERGY_RECORD);
        fileInfoMapper.updateById(fileInfo);
    }

    // endregion

    // region 附件查询
    // ===================================
    // 附件查询
    // ===================================

    @Override
    public List<FileInfo> getAttachments(Long id) {
        EnergyRecord exist = energyRecordMapper.selectById(id);
        if (exist == null) {
            throw new RuntimeException("能耗记录不存在");
        }
        return fileInfoService.getFilesByBusiness(id, BUSINESS_TYPE_ENERGY_RECORD, null);
    }

    // endregion
}
