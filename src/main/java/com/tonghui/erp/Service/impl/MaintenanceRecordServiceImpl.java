package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceExportRowDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenancePendingDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenancePendingSummaryDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceRecordDetailSaveDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceRecordDetailVo;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceRecordListItemDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceRecordSaveDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceRecordWithDetailsDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceStockItemDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.MaintenanceRecord;
import com.tonghui.erp.Data.Entity.MaintenanceRecordDetail;
import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.mapper.FileInfoMapper;
import com.tonghui.erp.Data.mapper.MaintenanceRecordDetailMapper;
import com.tonghui.erp.Data.mapper.MaintenanceRecordMapper;
import com.tonghui.erp.Data.mapper.MaterialMapper;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.StockInMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Service.FileInfoService;
import com.tonghui.erp.Service.FileStorageService;
import com.tonghui.erp.Service.MaintenanceRecordService;
import com.tonghui.erp.Service.SystemConfigService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 养护记录服务实现类
 * <p>
 * 实现库存养护记录的业务规则：
 * 1. 编号按实际检查日期自动生成（YH-YYYYMMDD-NNN），不可修改，唯一索引冲突时自动重试（最多3次）
 * 2. 明细引用库存明细（stock）并快照物料信息、养护要求、批号、入库编号、检查时库存数量
 * 3. 登记养护本身不产生库存增减，仅记录检查过程与结果
 * 4. 检查项目四态（1未检查/2未见异常/3发现异常/4不适用），默认未见异常；实测含水量为空表示未测
 * 5. 下次养护日期=本次实际检查日期+养护周期快照（周期为空则不纳入提醒），按明细独立计算
 * 6. 更新时按明细ID原地更新（保留已有明细ID，仅删除被移除的行），主单附件与明细附件同步
 * </p>
 */
@Service
public class MaintenanceRecordServiceImpl extends ServiceImpl<MaintenanceRecordMapper, MaintenanceRecord>
        implements MaintenanceRecordService {

    // region 常量定义
    // ===================================
    // 常量定义
    // ===================================

    /**
     * 养护记录编号前缀
     */
    private static final String CODE_PREFIX = "YH";

    /**
     * 编号生成重试上限
     */
    private static final int MAX_CODE_RETRY = 3;

    /**
     * 主单附件业务类型（父类型STOCK + 子类型MAINTENANCE_RECORD）
     */
    private static final String BUSINESS_TYPE_MAINTENANCE_RECORD = "STOCK_MAINTENANCE_RECORD";

    /**
     * 明细附件业务类型（父类型STOCK + 子类型MAINTENANCE_DETAIL）
     */
    private static final String BUSINESS_TYPE_MAINTENANCE_DETAIL = "STOCK_MAINTENANCE_DETAIL";

    /**
     * 提前提醒天数系统配置键
     */
    private static final String CONFIG_REMIND_DAYS = "maintenance.remind.days";

    /**
     * 提前提醒天数默认值
     */
    private static final int DEFAULT_REMIND_DAYS = 3;

    /**
     * 待养护状态：已逾期
     */
    public static final String PENDING_OVERDUE = "OVERDUE";

    /**
     * 待养护状态：今日到期
     */
    public static final String PENDING_TODAY = "TODAY";

    /**
     * 待养护状态：即将到期
     */
    public static final String PENDING_SOON = "SOON";

    /**
     * 检查日期格式（编号日期段）
     */
    private static final DateTimeFormatter DATE_COMPACT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // endregion

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /**
     * 库存检查明细Mapper
     */
    @Autowired
    private MaintenanceRecordDetailMapper maintenanceRecordDetailMapper;

    /**
     * 库存Mapper（选择库存明细、待养护排除零库存）
     */
    @Autowired
    private StockMapper stockMapper;

    /**
     * 入库单Mapper（解析入库编号快照）
     */
    @Autowired
    private StockInMapper stockInMapper;

    /**
     * 生产单位Mapper（仓库名称解析与快照）
     */
    @Autowired
    private ProductionUnitMapper productionUnitMapper;

    /**
     * 物料Mapper（带出规格、养护注意事项、养护周期、是否重点养护）
     */
    @Autowired
    private MaterialMapper materialMapper;

    /**
     * 用户Mapper（检查人员/操作人员/创建人姓名解析）
     */
    @Autowired
    private UserMapper userMapper;

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

    /**
     * 系统配置服务（读取提前提醒天数）
     */
    @Autowired
    private SystemConfigService systemConfigService;

    // endregion

    // region 编号生成
    // ===================================
    // 编号生成
    // ===================================

    /**
     * 生成养护记录编号（YH-YYYYMMDD-NNN，按实际检查日期）
     *
     * @param checkDate 实际检查日期（yyyy-MM-dd），为空时取当天
     * @return 生成的唯一养护记录编号
     */
    @Override
    public String generateCode(String checkDate) {
        String dateStr = StringUtils.hasText(checkDate)
                ? checkDate.replace("-", "")
                : LocalDate.now().format(DATE_COMPACT);
        if (dateStr.length() != 8) {
            dateStr = LocalDate.now().format(DATE_COMPACT);
        }
        String prefix = CODE_PREFIX + "-" + dateStr + "-";
        String maxCode = baseMapper.selectMaxCodeByPrefix(prefix);
        if (maxCode == null) {
            return String.format("%s%03d", prefix, 1);
        }
        try {
            int seq = Integer.parseInt(maxCode.substring(maxCode.lastIndexOf('-') + 1));
            return String.format("%s%03d", prefix, seq + 1);
        } catch (NumberFormatException e) {
            // 编号异常时从1开始重试，由唯一索引兜底
            return String.format("%s%03d", prefix, 1);
        }
    }

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 条件分页查询养护记录列表（含明细数、异常明细数、创建人姓名）
     */
    @Override
    public PagedResult<MaintenanceRecordListItemDto> search(String keyword, Long warehouseId, String material,
                                                            String batchNumber, String inCode, String checkType,
                                                            String checkerName, Boolean hasAbnormal,
                                                            String startDate, String endDate,
                                                            PageRequestDto pageRequest) {
        pageRequest.validateAndFix();
        LambdaQueryWrapper<MaintenanceRecord> wrapper = new LambdaQueryWrapper<>();
        // 关键字：模糊匹配养护记录编号
        if (StringUtils.hasText(keyword)) {
            wrapper.like(MaintenanceRecord::getMaintenanceCode, keyword);
        }
        // 仓库：精确匹配
        if (warehouseId != null) {
            wrapper.eq(MaintenanceRecord::getWarehouseId, warehouseId);
        }
        // 检查类型：精确匹配
        if (StringUtils.hasText(checkType)) {
            wrapper.eq(MaintenanceRecord::getCheckType, checkType);
        }
        // 检查人员：模糊匹配姓名快照
        if (StringUtils.hasText(checkerName)) {
            wrapper.like(MaintenanceRecord::getCheckerName, checkerName);
        }
        // 检查时间范围
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(MaintenanceRecord::getCheckTime, LocalDateTime.parse(startDate + "T00:00:00"));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(MaintenanceRecord::getCheckTime, LocalDateTime.parse(endDate + "T23:59:59"));
        }
        // 明细维度筛选（物料/批号/入库编号/是否异常），无匹配直接返回空
        if (!applyDetailFilter(wrapper, material, batchNumber, inCode, hasAbnormal)) {
            return PagedResult.empty();
        }
        wrapper.orderByDesc(MaintenanceRecord::getCheckTime);
        Page<MaintenanceRecord> page = baseMapper.selectPage(PagedResult.toMybatisPage(pageRequest), wrapper);
        List<MaintenanceRecord> records = page.getRecords();
        if (records.isEmpty()) {
            return PagedResult.empty();
        }
        // 批量回填明细数、异常明细数与用户姓名（防N+1）
        Map<Long, User> userMap = loadUserMap(records);
        Map<Long, List<MaintenanceRecordDetail>> detailMap = loadDetailMap(records);
        List<MaintenanceRecordListItemDto> dtoList = new ArrayList<>(records.size());
        for (MaintenanceRecord record : records) {
            MaintenanceRecordListItemDto dto = new MaintenanceRecordListItemDto();
            dto.setId(record.getId());
            dto.setMaintenanceCode(record.getMaintenanceCode());
            dto.setCheckTime(record.getCheckTime());
            dto.setCheckType(record.getCheckType());
            dto.setWarehouseName(record.getWarehouseName());
            dto.setCheckerName(record.getCheckerName());
            dto.setRelatedMaintenanceCode(record.getRelatedMaintenanceCode());
            dto.setCreatedTime(record.getCreatedTime());
            dto.setUpdatedTime(record.getUpdatedTime());
            dto.setCreatedByName(resolveUserNameFromMap(record.getCreatedBy(), userMap));
            dto.setUpdatedByName(resolveUserNameFromMap(record.getUpdatedBy(), userMap));
            List<MaintenanceRecordDetail> details = detailMap.getOrDefault(record.getId(), new ArrayList<>());
            dto.setDetailCount(details.size());
            dto.setAbnormalCount((int) details.stream().filter(this::isDetailAbnormal).count());
            dtoList.add(dto);
        }
        Page<MaintenanceRecordListItemDto> dtoPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        dtoPage.setRecords(dtoList);
        return PagedResult.fromPage(dtoPage, pageRequest);
    }

    /**
     * 查询养护记录详情（含全部明细、明细附件、主单附件与姓名回填）
     *
     * @param id 养护记录ID
     * @return 详情，不存在时返回null
     */
    @Override
    public MaintenanceRecordWithDetailsDto getDetail(Long id) {
        MaintenanceRecord record = baseMapper.selectById(id);
        if (record == null) {
            return null;
        }
        MaintenanceRecordWithDetailsDto dto = new MaintenanceRecordWithDetailsDto();
        BeanUtils.copyProperties(record, dto);
        List<MaintenanceRecordDetail> details = maintenanceRecordDetailMapper.selectList(
                new LambdaQueryWrapper<MaintenanceRecordDetail>()
                        .eq(MaintenanceRecordDetail::getMaintenanceId, id)
                        .orderByAsc(MaintenanceRecordDetail::getId));
        // 转视图对象：补异常标记与明细附件
        List<MaintenanceRecordDetailVo> detailVos = new ArrayList<>(details.size());
        for (MaintenanceRecordDetail detail : details) {
            MaintenanceRecordDetailVo vo = new MaintenanceRecordDetailVo();
            BeanUtils.copyProperties(detail, vo);
            vo.setAbnormal(isDetailAbnormal(detail));
            vo.setAttachments(fileInfoService.getFilesByBusiness(
                    detail.getId(), BUSINESS_TYPE_MAINTENANCE_DETAIL, null));
            detailVos.add(vo);
        }
        dto.setDetails(detailVos);
        dto.setDetailCount(details.size());
        dto.setAbnormalCount((int) details.stream().filter(this::isDetailAbnormal).count());
        dto.setAttachments(fileInfoService.getFilesByBusiness(id, BUSINESS_TYPE_MAINTENANCE_RECORD, null));
        dto.setCreatedByName(resolveUserName(record.getCreatedBy()));
        dto.setUpdatedByName(resolveUserName(record.getUpdatedBy()));
        return dto;
    }

    /**
     * 查询养护记录主单附件列表
     *
     * @param id 养护记录ID
     * @return 附件文件列表
     */
    @Override
    public List<FileInfo> getAttachments(Long id) {
        if (baseMapper.selectById(id) == null) {
            throw new RuntimeException("养护记录不存在");
        }
        return fileInfoService.getFilesByBusiness(id, BUSINESS_TYPE_MAINTENANCE_RECORD, null);
    }

    // endregion

    // region 新增与修改
    // ===================================
    // 新增与修改
    // ===================================

    /**
     * 新增养护记录（编号自动生成 + 明细整体写入 + 附件绑定）
     *
     * @param dto 保存请求体
     * @return 新增的养护记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaintenanceRecord create(MaintenanceRecordSaveDto dto) {
        validateRequired(dto);
        MaintenanceRecord record = buildRecordFromDto(dto);
        // 编号生成 + 唯一冲突重试
        String dateStr = record.getCheckTime().toLocalDate().format(DATE_COMPACT);
        int retry = 0;
        while (true) {
            record.setMaintenanceCode(generateCode(dateStr));
            try {
                baseMapper.insert(record);
                break;
            } catch (DuplicateKeyException e) {
                if (++retry >= MAX_CODE_RETRY) {
                    throw new RuntimeException("生成养护记录编号失败，请稍后重试");
                }
            }
        }
        // 保存明细（快照物料/批号/入库编号/数量/养护要求，计算下次养护日期）
        saveDetails(record, dto.getDetails());
        // 主单附件绑定
        syncAttachments(record.getId(), BUSINESS_TYPE_MAINTENANCE_RECORD, dto.getAttachmentFileIds());
        return record;
    }

    /**
     * 修改养护记录（编号不可改；明细按ID原地更新保留ID，被移除行删除；附件同步）
     * <p>
     * 为支持明细附件按行定位（file_info.business_id = 明细ID），采用原地更新保留ID策略：
     * 入参携带 id 且属于本单的已有明细行执行更新（ID不变），无 id 的行新增，
     * 数据库中未被入参覆盖的既有明细行被物理删除。
     * </p>
     *
     * @param id  养护记录ID
     * @param dto 保存请求体
     * @return 修改后的养护记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaintenanceRecord update(Long id, MaintenanceRecordSaveDto dto) {
        validateRequired(dto);
        MaintenanceRecord existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("养护记录不存在");
        }
        MaintenanceRecord record = buildRecordFromDto(dto);
        record.setId(id);
        // 编号保持不变（系统生成，不可修改）
        record.setMaintenanceCode(existing.getMaintenanceCode());
        // DB加载的实体审计字段strictUpdateFill不覆盖非null值，需手动保留
        record.setCreatedBy(existing.getCreatedBy());
        record.setCreatedTime(existing.getCreatedTime());
        baseMapper.updateById(record);
        // 现有明细（用于保留ID与计算被移除行）
        Map<Long, MaintenanceRecordDetail> existingDetails = maintenanceRecordDetailMapper.selectList(
                        new LambdaQueryWrapper<MaintenanceRecordDetail>()
                                .eq(MaintenanceRecordDetail::getMaintenanceId, id))
                .stream()
                .collect(Collectors.toMap(MaintenanceRecordDetail::getId, d -> d, (a, b) -> a));
        // 按ID原地更新已有行、插入新增行（保留明细ID，附件可按ID继续定位）
        Set<Long> incomingIds = new HashSet<>();
        syncDetails(record, dto.getDetails(), existingDetails, incomingIds);
        // 被移除的既有行：解绑明细附件后物理删除
        List<Long> removedIds = existingDetails.keySet().stream()
                .filter(existingId -> !incomingIds.contains(existingId))
                .collect(Collectors.toList());
        if (!removedIds.isEmpty()) {
            for (Long removedId : removedIds) {
                unbindAttachments(removedId, BUSINESS_TYPE_MAINTENANCE_DETAIL);
            }
            maintenanceRecordDetailMapper.physicalDeleteByIds(removedIds);
        }
        // 主单附件同步
        syncAttachments(id, BUSINESS_TYPE_MAINTENANCE_RECORD, dto.getAttachmentFileIds());
        return baseMapper.selectById(id);
    }

    /**
     * 删除养护记录（主单软删除，明细物理删除并解绑明细附件）
     * <p>
     * 明细必须物理删除：残留明细会继续参与待养护提醒计算
     * </p>
     *
     * @param id 养护记录ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        MaintenanceRecord existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("养护记录不存在");
        }
        // 解绑明细附件与主单附件，避免残留业务关联
        List<MaintenanceRecordDetail> details = maintenanceRecordDetailMapper.selectList(
                new LambdaQueryWrapper<MaintenanceRecordDetail>()
                        .select(MaintenanceRecordDetail::getId)
                        .eq(MaintenanceRecordDetail::getMaintenanceId, id));
        for (MaintenanceRecordDetail detail : details) {
            unbindAttachments(detail.getId(), BUSINESS_TYPE_MAINTENANCE_DETAIL);
        }
        unbindAttachments(id, BUSINESS_TYPE_MAINTENANCE_RECORD);
        // 明细物理删除，主单走MP软删除（逻辑删除）
        maintenanceRecordDetailMapper.physicalDeleteByMaintenanceId(id);
        baseMapper.deleteById(id);
    }

    /**
     * 校验保存必填项
     *
     * @param dto 保存请求体
     */
    private void validateRequired(MaintenanceRecordSaveDto dto) {
        if (dto.getCheckTime() == null) {
            throw new RuntimeException("实际检查时间不能为空");
        }
        if (!StringUtils.hasText(dto.getCheckType())) {
            throw new RuntimeException("检查类型不能为空");
        }
        if (dto.getWarehouseId() == null) {
            throw new RuntimeException("仓库不能为空");
        }
        if (dto.getCheckerId() == null) {
            throw new RuntimeException("检查人员不能为空");
        }
        if (dto.getDetails() == null || dto.getDetails().isEmpty()) {
            throw new RuntimeException("至少录入一条库存明细");
        }
        for (MaintenanceRecordDetailSaveDto d : dto.getDetails()) {
            if (d.getStockId() == null) {
                throw new RuntimeException("库存明细不能为空");
            }
        }
        // 关联复查记录必须存在
        if (dto.getRelatedMaintenanceId() != null && baseMapper.selectById(dto.getRelatedMaintenanceId()) == null) {
            throw new RuntimeException("关联复查记录不存在");
        }
    }

    /**
     * 由保存Dto构建主单实体（含仓库与检查人员快照）
     *
     * @param dto 保存请求体
     * @return 主单实体
     */
    private MaintenanceRecord buildRecordFromDto(MaintenanceRecordSaveDto dto) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setCheckTime(dto.getCheckTime());
        record.setCheckType(dto.getCheckType());
        record.setWarehouseId(dto.getWarehouseId());
        // 仓库名称快照（按生产单位解析）
        record.setWarehouseName(resolveWarehouseName(dto.getWarehouseId()));
        record.setCheckerId(dto.getCheckerId());
        // 检查人员姓名快照
        record.setCheckerName(resolveUserName(dto.getCheckerId()));
        record.setEnvObserveInfo(dto.getEnvObserveInfo());
        record.setTemperature(dto.getTemperature());
        record.setHumidity(dto.getHumidity());
        record.setSummary(dto.getSummary());
        record.setRelatedMaintenanceId(dto.getRelatedMaintenanceId());
        if (dto.getRelatedMaintenanceId() != null) {
            MaintenanceRecord original = baseMapper.selectById(dto.getRelatedMaintenanceId());
            record.setRelatedMaintenanceCode(original == null ? null : original.getMaintenanceCode());
        }
        return record;
    }

    /**
     * 保存明细列表（新增记录：逐行插入并绑定明细附件）
     *
     * @param record     主单实体（已落库）
     * @param detailDtos 明细保存请求列表
     */
    private void saveDetails(MaintenanceRecord record, List<MaintenanceRecordDetailSaveDto> detailDtos) {
        DetailSnapshot snapshot = loadDetailSnapshot(detailDtos);
        for (MaintenanceRecordDetailSaveDto d : detailDtos) {
            MaintenanceRecordDetail detail = new MaintenanceRecordDetail();
            populateDetail(detail, record, d, snapshot);
            maintenanceRecordDetailMapper.insert(detail);
            // 明细附件绑定
            bindAttachments(detail.getId(), BUSINESS_TYPE_MAINTENANCE_DETAIL, d.getAttachmentFileIds());
        }
    }

    /**
     * 同步明细列表（更新记录：按ID原地更新保留ID、其余新增）
     * <p>
     * 入参携带 id 且属于本单的已有行执行原地更新（ID不变，附件可继续按ID定位）；
     * 无 id 的行新增；主单下未被入参覆盖的既有行由调用方负责物理删除
     * </p>
     *
     * @param record      主单实体
     * @param detailDtos  明细保存请求列表
     * @param existingMap 本单现有明细（key=明细ID）
     * @param incomingIds 出参：入参覆盖的已有明细ID集合（用于计算被移除行）
     */
    private void syncDetails(MaintenanceRecord record, List<MaintenanceRecordDetailSaveDto> detailDtos,
                             Map<Long, MaintenanceRecordDetail> existingMap, Set<Long> incomingIds) {
        DetailSnapshot snapshot = loadDetailSnapshot(detailDtos);
        for (MaintenanceRecordDetailSaveDto d : detailDtos) {
            boolean isExisting = d.getId() != null && existingMap.containsKey(d.getId());
            MaintenanceRecordDetail detail = new MaintenanceRecordDetail();
            if (isExisting) {
                // 已有行：原地更新，保留ID（附件可按ID继续定位）
                detail.setId(d.getId());
                incomingIds.add(d.getId());
            }
            populateDetail(detail, record, d, snapshot);
            if (isExisting) {
                maintenanceRecordDetailMapper.updateById(detail);
                // 同一明细ID，附件按最新列表同步（解绑移除项、绑定新增项）
                syncAttachments(detail.getId(), BUSINESS_TYPE_MAINTENANCE_DETAIL, d.getAttachmentFileIds());
            } else {
                maintenanceRecordDetailMapper.insert(detail);
                // 明细附件绑定
                bindAttachments(detail.getId(), BUSINESS_TYPE_MAINTENANCE_DETAIL, d.getAttachmentFileIds());
            }
        }
    }

    /**
     * 预加载明细所需的库存、物料、入库单信息（防N+1）
     *
     * @param detailDtos 明细保存请求列表
     * @return 明细预加载快照料
     */
    private DetailSnapshot loadDetailSnapshot(List<MaintenanceRecordDetailSaveDto> detailDtos) {
        Set<Long> stockIds = detailDtos.stream()
                .map(MaintenanceRecordDetailSaveDto::getStockId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Stock> stockMap = stockIds.isEmpty() ? Map.of() :
                stockMapper.selectBatchIds(stockIds).stream()
                        .collect(Collectors.toMap(Stock::getStockId, s -> s, (a, b) -> a));
        Map<Long, Material> materialMap = loadMaterialMapByStock(new ArrayList<>(stockMap.values()));
        Map<Long, String> inCodeMap = loadInCodeMap(stockMap.values());
        return new DetailSnapshot(stockMap, materialMap, inCodeMap);
    }

    /**
     * 填充明细实体（快照物料/批号/入库编号/数量/养护要求，计算下次养护日期）
     *
     * @param detail   待填充的明细实体（主键可为空或已有ID）
     * @param record   主单实体
     * @param d        明细保存请求
     * @param snapshot 预加载快照料
     */
    private void populateDetail(MaintenanceRecordDetail detail, MaintenanceRecord record,
                                MaintenanceRecordDetailSaveDto d, DetailSnapshot snapshot) {
        Stock stock = snapshot.stockMap().get(d.getStockId());
        if (stock == null) {
            throw new RuntimeException("库存明细不存在或已删除");
        }
        detail.setMaintenanceId(record.getId());
        detail.setStockId(stock.getStockId());
        // 物料信息快照
        detail.setMaterialCode(stock.getItemCode());
        detail.setMaterialName(stock.getItemName());
        Material material = snapshot.materialMap().get(stock.getStockId());
        if (material != null) {
            detail.setSpec(material.getSpec());
            detail.setMaintenanceNotes(material.getMaintenanceNotes());
            detail.setMaintenanceCycleDays(material.getMaintenanceCycleDays());
            detail.setKeyMaintenance(material.getKeyMaintenance());
        }
        detail.setCategoryName(stock.getCategoryName());
        detail.setUnitName(stock.getUnitName());
        // 批号、入库单、仓库与检查时库存数量快照
        detail.setBatchNumber(stock.getBatchNumber());
        detail.setStockInId(stock.getStockInId());
        detail.setInCode(snapshot.inCodeMap().get(stock.getStockInId()));
        detail.setWarehouseName(record.getWarehouseName());
        detail.setStockQuantity(stock.getQuantity());
        // 人工登记字段
        detail.setShelfLocation(d.getShelfLocation());
        detail.setCheckScope(d.getCheckScope());
        // 检查项目四态，默认未见异常
        detail.setPackagingStatus(defaultStatus(d.getPackagingStatus()));
        detail.setMoistureStatus(defaultStatus(d.getMoistureStatus()));
        detail.setPestStatus(defaultStatus(d.getPestStatus()));
        detail.setMoldStatus(defaultStatus(d.getMoldStatus()));
        detail.setOilStatus(defaultStatus(d.getOilStatus()));
        detail.setColorStatus(defaultStatus(d.getColorStatus()));
        detail.setOdorStatus(defaultStatus(d.getOdorStatus()));
        detail.setBreakageStatus(defaultStatus(d.getBreakageStatus()));
        detail.setStackingStatus(defaultStatus(d.getStackingStatus()));
        detail.setOtherStatus(defaultStatus(d.getOtherStatus()));
        detail.setOtherDescription(d.getOtherDescription());
        // 实测与异常（含水量NULL=未测）
        detail.setMoistureContent(d.getMoistureContent());
        detail.setStackTemperature(d.getStackTemperature());
        detail.setAbnormalDescription(d.getAbnormalDescription());
        // 养护措施与处理
        detail.setMeasures(d.getMeasures());
        detail.setMeasuresOther(d.getMeasuresOther());
        detail.setOperatorId(d.getOperatorId());
        detail.setOperatorName(resolveUserName(d.getOperatorId()));
        detail.setOperateTime(d.getOperateTime());
        detail.setHandleResult(d.getHandleResult());
        detail.setRemark(d.getRemark());
        // 周期提醒：最近养护日期=本次实际检查日期，下次=最近+周期快照（周期空则NULL）
        LocalDate lastDate = record.getCheckTime().toLocalDate();
        detail.setLastMaintenanceDate(lastDate);
        Integer cycle = detail.getMaintenanceCycleDays();
        detail.setNextMaintenanceDate(cycle == null ? null : lastDate.plusDays(cycle));
    }

    /**
     * 明细预加载快照料（库存/物料/入库编号），避免逐行查询
     *
     * @param stockMap    库存明细（key=stockId）
     * @param materialMap 物料（key=stockId）
     * @param inCodeMap   入库编号（key=stockInId）
     */
    private record DetailSnapshot(Map<Long, Stock> stockMap, Map<Long, Material> materialMap,
                                  Map<Long, String> inCodeMap) {
    }

    // endregion

    // region 库存选择
    // ===================================
    // 库存选择
    // ===================================

    /**
     * 库存明细分页查询（供登记页选择，带出入库编号与物料养护要求）
     *
     * @param warehouse 仓库名称（必填）
     * @param keyword   关键字（可空，模糊匹配物料编码/名称/批号/入库编号）
     * @param showZero  是否显示零库存（默认false）
     * @return 库存选择项列表
     */
    @Override
    public List<MaintenanceStockItemDto> getStockDetails(String warehouse, String keyword, Boolean showZero) {
        if (!StringUtils.hasText(warehouse)) {
            throw new RuntimeException("请选择仓库");
        }
        Long prodUnitId = resolveProdUnitId(warehouse);
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stock::getProdUnitId, prodUnitId);
        // 默认仅显示有库存的明细
        if (!Boolean.TRUE.equals(showZero)) {
            wrapper.gt(Stock::getQuantity, 0);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Stock::getItemCode, keyword)
                    .or().like(Stock::getItemName, keyword)
                    .or().like(Stock::getBatchNumber, keyword));
        }
        wrapper.orderByDesc(Stock::getStockId);
        List<Stock> stocks = stockMapper.selectList(wrapper);
        if (stocks.isEmpty()) {
            return new ArrayList<>();
        }
        // 批量带出物料养护要求与入库编号
        Map<Long, Material> materialMap = loadMaterialMapByStock(stocks);
        Map<Long, String> inCodeMap = loadInCodeMap(stocks);
        List<MaintenanceStockItemDto> dtoList = new ArrayList<>(stocks.size());
        for (Stock stock : stocks) {
            // 入库编号关键字过滤（入参要求匹配入库编号）
            String inCode = inCodeMap.get(stock.getStockInId());
            if (StringUtils.hasText(keyword) && !containsIgnoreCase(inCode, keyword)
                    && !containsIgnoreCase(stock.getItemCode(), keyword)
                    && !containsIgnoreCase(stock.getItemName(), keyword)
                    && !containsIgnoreCase(stock.getBatchNumber(), keyword)) {
                continue;
            }
            MaintenanceStockItemDto dto = new MaintenanceStockItemDto();
            dto.setStockId(stock.getStockId());
            dto.setMaterialCode(stock.getItemCode());
            dto.setMaterialName(stock.getItemName());
            dto.setCategoryName(stock.getCategoryName());
            dto.setBatchNumber(stock.getBatchNumber());
            dto.setStockInId(stock.getStockInId());
            dto.setInCode(inCode);
            dto.setWarehouseName(warehouse);
            dto.setWarehouseId(prodUnitId);
            dto.setStockQuantity(stock.getQuantity());
            dto.setUnitName(stock.getUnitName());
            dto.setStockStatus(stock.getStockStatus() == null ? null : String.valueOf(stock.getStockStatus()));
            Material material = materialMap.get(stock.getStockId());
            if (material != null) {
                dto.setSpec(material.getSpec());
                dto.setMaintenanceNotes(material.getMaintenanceNotes());
                dto.setMaintenanceCycleDays(material.getMaintenanceCycleDays());
                dto.setKeyMaintenance(material.getKeyMaintenance());
            }
            dtoList.add(dto);
        }
        return dtoList;
    }

    // endregion

    // region 到期提醒
    // ===================================
    // 到期提醒
    // ===================================

    /**
     * 分页查询待养护库存明细（排除零库存，逾期优先、下次养护日期升序）
     *
     * @param keyword       关键字（可空，模糊匹配物料编码/名称/批号/入库编号）
     * @param warehouseId   仓库ID（可空，精确匹配库存所在生产单位）
     * @param pendingStatus 提醒状态（可空：OVERDUE/TODAY/SOON）
     * @param pageIndex     页码（从0开始，-1全量）
     * @param pageSize      每页大小
     * @return 分页结果
     */
    @Override
    public PagedResult<MaintenancePendingDto> searchPending(String keyword, Long warehouseId,
                                                            String pendingStatus, int pageIndex, int pageSize) {
        // 1. 有库存的stock_id集合（待养护排除零库存，可按仓库过滤）
        Set<Long> activeStockIds = loadActiveStockIds(warehouseId);
        if (activeStockIds.isEmpty()) {
            return PagedResult.empty();
        }
        // 2. 取每个库存明细最近一次检查的明细（下次养护日期非空，按检查日期降序取首条）
        List<MaintenanceRecordDetail> details = maintenanceRecordDetailMapper.selectList(
                new LambdaQueryWrapper<MaintenanceRecordDetail>()
                        .in(MaintenanceRecordDetail::getStockId, activeStockIds)
                        .isNotNull(MaintenanceRecordDetail::getNextMaintenanceDate)
                        .orderByDesc(MaintenanceRecordDetail::getId));
        Map<Long, MaintenanceRecordDetail> latestByStock = new LinkedHashMap<>();
        for (MaintenanceRecordDetail d : details) {
            latestByStock.putIfAbsent(d.getStockId(), d);
        }
        if (latestByStock.isEmpty()) {
            return PagedResult.empty();
        }
        // 3. 主单信息回填（最近一次检查编号与时间）
        List<MaintenanceRecord> records = baseMapper.selectBatchIds(
                latestByStock.values().stream().map(MaintenanceRecordDetail::getMaintenanceId)
                        .collect(Collectors.toSet()));
        Map<Long, MaintenanceRecord> recordMap = records.stream()
                .collect(Collectors.toMap(MaintenanceRecord::getId, r -> r, (a, b) -> a));
        // 4. 计算提醒状态并过滤
        LocalDate today = LocalDate.now();
        int remindDays = systemConfigService.getInt(CONFIG_REMIND_DAYS, DEFAULT_REMIND_DAYS);
        List<MaintenancePendingDto> all = new ArrayList<>();
        for (MaintenanceRecordDetail d : latestByStock.values()) {
            String status = computePendingStatus(d.getNextMaintenanceDate(), today, remindDays);
            if (StringUtils.hasText(pendingStatus) && !pendingStatus.equals(status)) {
                continue;
            }
            MaintenancePendingDto dto = new MaintenancePendingDto();
            dto.setDetailId(d.getId());
            dto.setStockId(d.getStockId());
            dto.setMaterialCode(d.getMaterialCode());
            dto.setMaterialName(d.getMaterialName());
            dto.setSpec(d.getSpec());
            dto.setBatchNumber(d.getBatchNumber());
            dto.setInCode(d.getInCode());
            dto.setWarehouseName(d.getWarehouseName());
            dto.setUnitName(d.getUnitName());
            dto.setLastMaintenanceDate(d.getLastMaintenanceDate());
            dto.setNextMaintenanceDate(d.getNextMaintenanceDate());
            dto.setStockQuantity(d.getStockQuantity());
            dto.setKeyMaintenance(d.getKeyMaintenance());
            dto.setPendingStatus(status);
            // 逾期天数（已逾期>0，未逾期为0）
            long diff = java.time.temporal.ChronoUnit.DAYS.between(today, d.getNextMaintenanceDate());
            dto.setOverdueDays(diff < 0 ? -diff : 0);
            MaintenanceRecord r = recordMap.get(d.getMaintenanceId());
            if (r != null) {
                dto.setLastMaintenanceCode(r.getMaintenanceCode());
                dto.setLastCheckTime(r.getCheckTime());
            }
            // 关键字：物料编码/名称/批号/入库编号
            if (StringUtils.hasText(keyword)
                    && !containsIgnoreCase(d.getMaterialCode(), keyword)
                    && !containsIgnoreCase(d.getMaterialName(), keyword)
                    && !containsIgnoreCase(d.getBatchNumber(), keyword)
                    && !containsIgnoreCase(d.getInCode(), keyword)) {
                continue;
            }
            all.add(dto);
        }
        if (all.isEmpty()) {
            return PagedResult.empty();
        }
        // 5. 排序：逾期 → 今日 → 即将到期，再按下次养护日期升序
        all.sort((a, b) -> {
            int rank = pendingRank(a.getPendingStatus()) - pendingRank(b.getPendingStatus());
            if (rank != 0) {
                return rank;
            }
            if (a.getNextMaintenanceDate() == null && b.getNextMaintenanceDate() == null) {
                return 0;
            }
            if (a.getNextMaintenanceDate() == null) {
                return 1;
            }
            if (b.getNextMaintenanceDate() == null) {
                return -1;
            }
            return a.getNextMaintenanceDate().compareTo(b.getNextMaintenanceDate());
        });
        // 6. 手动分页（-1表示全量）
        int total = all.size();
        if (pageIndex < 0 || pageSize < 0) {
            return fromList(all, 0, total);
        }
        int from = Math.min(pageIndex * pageSize, total);
        int to = Math.min(from + pageSize, total);
        return fromList(all.subList(from, to), pageIndex, total);
    }

    /**
     * 查询待养护统计（首页展示）
     *
     * @return 已逾期/今日到期/即将到期数量与提前提醒天数
     */
    @Override
    public MaintenancePendingSummaryDto getPendingSummary() {
        MaintenancePendingSummaryDto summary = new MaintenancePendingSummaryDto();
        summary.setRemindDays(systemConfigService.getInt(CONFIG_REMIND_DAYS, DEFAULT_REMIND_DAYS));
        Set<Long> activeStockIds = loadActiveStockIds(null);
        if (activeStockIds.isEmpty()) {
            return summary;
        }
        List<MaintenanceRecordDetail> details = maintenanceRecordDetailMapper.selectList(
                new LambdaQueryWrapper<MaintenanceRecordDetail>()
                        .in(MaintenanceRecordDetail::getStockId, activeStockIds)
                        .isNotNull(MaintenanceRecordDetail::getNextMaintenanceDate)
                        .orderByDesc(MaintenanceRecordDetail::getId));
        Map<Long, MaintenanceRecordDetail> latestByStock = new LinkedHashMap<>();
        for (MaintenanceRecordDetail d : details) {
            latestByStock.putIfAbsent(d.getStockId(), d);
        }
        LocalDate today = LocalDate.now();
        long overdue = 0, todayDue = 0, soon = 0;
        for (MaintenanceRecordDetail d : latestByStock.values()) {
            String status = computePendingStatus(d.getNextMaintenanceDate(), today, summary.getRemindDays());
            if (status == null) {
                continue;
            }
            switch (status) {
                case PENDING_OVERDUE -> overdue++;
                case PENDING_TODAY -> todayDue++;
                default -> soon++;
            }
        }
        summary.setOverdueCount(overdue);
        summary.setTodayCount(todayDue);
        summary.setSoonCount(soon);
        return summary;
    }

    // endregion

    // region 导出
    // ===================================
    // 导出
    // ===================================

    /**
     * 按列表筛选条件导出养护台账Excel（主单+明细平铺，一行一物料）
     */
    @Override
    public void exportToExcel(String keyword, Long warehouseId, String material, String batchNumber,
                              String inCode, String checkType, String checkerName, Boolean hasAbnormal,
                              String startDate, String endDate, OutputStream out) {
        // 复用列表查询条件（-1全量）
        PagedResult<MaintenanceRecordListItemDto> page = search(keyword, warehouseId, material, batchNumber,
                inCode, checkType, checkerName, hasAbnormal, startDate, endDate, fullPageRequest());
        List<MaintenanceRecordListItemDto> records = page.getItems();
        // 构建导出行（明细平铺）
        List<MaintenanceExportRowDto> rows = new ArrayList<>();
        if (records != null && !records.isEmpty()) {
            for (MaintenanceRecordListItemDto r : records) {
                MaintenanceRecordWithDetailsDto detailDto = getDetail(r.getId());
                if (detailDto == null || detailDto.getDetails() == null) {
                    continue;
                }
                for (MaintenanceRecordDetailVo d : detailDto.getDetails()) {
                    rows.add(buildExportRow(detailDto, d));
                }
            }
        }
        try (Workbook workbook = new XSSFWorkbook()) {
            writeMaintenanceWorkbook(workbook, rows);
            workbook.write(out);
        } catch (IOException e) {
            throw new RuntimeException("导出养护台账失败", e);
        }
    }

    /**
     * 构建导出行（检查项目结果汇总为中文四态文本）
     *
     * @param record 主单详情（含主单字段）
     * @param d      明细视图对象
     * @return 导出行
     */
    private MaintenanceExportRowDto buildExportRow(MaintenanceRecordWithDetailsDto record,
                                                   MaintenanceRecordDetailVo d) {
        MaintenanceExportRowDto row = new MaintenanceExportRowDto();
        row.setMaintenanceCode(record.getMaintenanceCode());
        row.setCheckTime(record.getCheckTime());
        row.setCheckType(record.getCheckType());
        row.setWarehouseName(record.getWarehouseName());
        row.setCheckerName(record.getCheckerName());
        row.setEnvObserveInfo(record.getEnvObserveInfo());
        row.setTemperature(record.getTemperature());
        row.setHumidity(record.getHumidity());
        row.setSummary(record.getSummary());
        row.setMaterialCode(d.getMaterialCode());
        row.setMaterialName(d.getMaterialName());
        row.setSpec(d.getSpec());
        row.setCategoryName(d.getCategoryName());
        row.setBatchNumber(d.getBatchNumber());
        row.setInCode(d.getInCode());
        row.setUnitName(d.getUnitName());
        row.setStockQuantity(d.getStockQuantity());
        row.setShelfLocation(d.getShelfLocation());
        row.setCheckScope(d.getCheckScope());
        row.setCheckResultSummary(buildCheckResultSummary(d));
        row.setMoistureContent(d.getMoistureContent());
        row.setStackTemperature(d.getStackTemperature());
        row.setAbnormalDescription(d.getAbnormalDescription());
        row.setMeasures(d.getMeasures());
        row.setOperatorName(d.getOperatorName());
        row.setOperateTime(d.getOperateTime());
        row.setHandleResult(d.getHandleResult());
        row.setRemark(d.getRemark());
        row.setLastMaintenanceDate(d.getLastMaintenanceDate());
        row.setNextMaintenanceDate(d.getNextMaintenanceDate());
        return row;
    }

    /**
     * 检查项目结果汇总文本（项名：四态，分号分隔）
     *
     * @param d 明细视图对象
     * @return 汇总文本
     */
    private String buildCheckResultSummary(MaintenanceRecordDetailVo d) {
        LinkedHashMap<String, Integer> items = new LinkedHashMap<>();
        items.put("包装与标签", d.getPackagingStatus());
        items.put("受潮及干湿", d.getMoistureStatus());
        items.put("虫蛀情况", d.getPestStatus());
        items.put("霉变及腐败", d.getMoldStatus());
        items.put("泛油情况", d.getOilStatus());
        items.put("色泽及外观", d.getColorStatus());
        items.put("气味", d.getOdorStatus());
        items.put("破碎及杂质", d.getBreakageStatus());
        items.put("堆放及储存条件", d.getStackingStatus());
        items.put("其他异常", d.getOtherStatus());
        return items.entrySet().stream()
                .map(e -> e.getKey() + "：" + checkStatusText(e.getValue()))
                .collect(Collectors.joining("；"));
    }

    /**
     * 写入养护台账工作簿（表头灰底加粗，检查四态显示为中文）
     *
     * @param workbook 工作簿
     * @param rows     导出行列表
     */
    private void writeMaintenanceWorkbook(Workbook workbook, List<MaintenanceExportRowDto> rows) {
        String[] headers = {"养护记录编号", "实际检查时间", "检查类型", "仓库", "检查人员",
                "环境观测时间及位置", "温度(℃)", "相对湿度(%)", "总体说明",
                "物料编码", "物料名称", "规格", "分类", "批号", "入库编号", "计量单位", "检查时库存数量",
                "货架位置", "检查范围或数量", "检查项目结果汇总", "实测含水量(%)", "实测垛温(℃)",
                "异常描述", "养护措施", "操作人员", "操作时间", "处理结果", "备注",
                "最近养护日期", "下次养护日期"};
        Sheet sheet = workbook.createSheet("养护台账");
        // 表头样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        // 表头
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 16 * 256);
        }
        // 数据行
        int rowNum = 1;
        for (MaintenanceExportRowDto r : rows) {
            Row row = sheet.createRow(rowNum++);
            int col = 0;
            row.createCell(col++).setCellValue(nvl(r.getMaintenanceCode()));
            row.createCell(col++).setCellValue(r.getCheckTime() == null ? "" : r.getCheckTime().toString());
            row.createCell(col++).setCellValue(nvl(r.getCheckType()));
            row.createCell(col++).setCellValue(nvl(r.getWarehouseName()));
            row.createCell(col++).setCellValue(nvl(r.getCheckerName()));
            row.createCell(col++).setCellValue(nvl(r.getEnvObserveInfo()));
            setNumberCell(row.createCell(col++), r.getTemperature());
            setNumberCell(row.createCell(col++), r.getHumidity());
            row.createCell(col++).setCellValue(nvl(r.getSummary()));
            row.createCell(col++).setCellValue(nvl(r.getMaterialCode()));
            row.createCell(col++).setCellValue(nvl(r.getMaterialName()));
            row.createCell(col++).setCellValue(nvl(r.getSpec()));
            row.createCell(col++).setCellValue(nvl(r.getCategoryName()));
            row.createCell(col++).setCellValue(nvl(r.getBatchNumber()));
            row.createCell(col++).setCellValue(nvl(r.getInCode()));
            row.createCell(col++).setCellValue(nvl(r.getUnitName()));
            setNumberCell(row.createCell(col++), r.getStockQuantity());
            row.createCell(col++).setCellValue(nvl(r.getShelfLocation()));
            row.createCell(col++).setCellValue(nvl(r.getCheckScope()));
            row.createCell(col++).setCellValue(nvl(r.getCheckResultSummary()));
            // 实测含水量为空输出「未测」
            row.createCell(col++).setCellValue(r.getMoistureContent() == null ? "未测"
                    : r.getMoistureContent().toPlainString());
            setNumberCell(row.createCell(col++), r.getStackTemperature());
            row.createCell(col++).setCellValue(nvl(r.getAbnormalDescription()));
            row.createCell(col++).setCellValue(nvl(r.getMeasures()));
            row.createCell(col++).setCellValue(nvl(r.getOperatorName()));
            row.createCell(col++).setCellValue(r.getOperateTime() == null ? "" : r.getOperateTime().toString());
            row.createCell(col++).setCellValue(nvl(r.getHandleResult()));
            row.createCell(col++).setCellValue(nvl(r.getRemark()));
            row.createCell(col++).setCellValue(nvl(r.getLastMaintenanceDate()));
            row.createCell(col++).setCellValue(nvl(r.getNextMaintenanceDate()));
        }
    }

    // endregion

    // region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================

    /**
     * 追加明细维度筛选条件（物料/批号/入库编号/是否异常）
     *
     * @return false=无匹配记录（调用方应直接返回空结果）
     */
    private boolean applyDetailFilter(LambdaQueryWrapper<MaintenanceRecord> wrapper, String material,
                                      String batchNumber, String inCode, Boolean hasAbnormal) {
        boolean hasCondition = StringUtils.hasText(material) || StringUtils.hasText(batchNumber)
                || StringUtils.hasText(inCode) || hasAbnormal != null;
        if (!hasCondition) {
            return true;
        }
        // 是否异常：任一检查项目为发现异常或填写了异常描述
        List<Long> filteredIds = null;
        if (StringUtils.hasText(material) || StringUtils.hasText(batchNumber) || StringUtils.hasText(inCode)) {
            LambdaQueryWrapper<MaintenanceRecordDetail> dw = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(material)) {
                dw.and(w -> w.like(MaintenanceRecordDetail::getMaterialCode, material)
                        .or().like(MaintenanceRecordDetail::getMaterialName, material));
            }
            if (StringUtils.hasText(batchNumber)) {
                dw.like(MaintenanceRecordDetail::getBatchNumber, batchNumber);
            }
            if (StringUtils.hasText(inCode)) {
                dw.like(MaintenanceRecordDetail::getInCode, inCode);
            }
            filteredIds = maintenanceRecordDetailMapper.selectList(dw).stream()
                    .map(MaintenanceRecordDetail::getMaintenanceId)
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (filteredIds.isEmpty()) {
                return false;
            }
        }
        if (hasAbnormal != null) {
            List<Long> abnormalIds = selectAbnormalMaintenanceIds();
            if (hasAbnormal) {
                if (abnormalIds.isEmpty()) {
                    return false;
                }
                if (filteredIds != null) {
                    abnormalIds.retainAll(filteredIds);
                    if (abnormalIds.isEmpty()) {
                        return false;
                    }
                }
                wrapper.in(MaintenanceRecord::getId, abnormalIds);
            } else {
                if (!abnormalIds.isEmpty()) {
                    wrapper.notIn(MaintenanceRecord::getId, abnormalIds);
                }
                if (filteredIds != null) {
                    wrapper.in(MaintenanceRecord::getId, filteredIds);
                }
            }
        } else if (filteredIds != null) {
            wrapper.in(MaintenanceRecord::getId, filteredIds);
        }
        return true;
    }

    /**
     * 查询存在异常明细的养护记录ID集合（任一检查项目发现异常或填写了异常描述）
     *
     * @return 记录ID集合
     */
    private List<Long> selectAbnormalMaintenanceIds() {
        return maintenanceRecordDetailMapper.selectList(new LambdaQueryWrapper<MaintenanceRecordDetail>()
                        .select(MaintenanceRecordDetail::getMaintenanceId)
                        .and(w -> w.eq(MaintenanceRecordDetail::getPackagingStatus, MaintenanceRecordDetail.CHECK_ABNORMAL)
                                .or().eq(MaintenanceRecordDetail::getMoistureStatus, MaintenanceRecordDetail.CHECK_ABNORMAL)
                                .or().eq(MaintenanceRecordDetail::getPestStatus, MaintenanceRecordDetail.CHECK_ABNORMAL)
                                .or().eq(MaintenanceRecordDetail::getMoldStatus, MaintenanceRecordDetail.CHECK_ABNORMAL)
                                .or().eq(MaintenanceRecordDetail::getOilStatus, MaintenanceRecordDetail.CHECK_ABNORMAL)
                                .or().eq(MaintenanceRecordDetail::getColorStatus, MaintenanceRecordDetail.CHECK_ABNORMAL)
                                .or().eq(MaintenanceRecordDetail::getOdorStatus, MaintenanceRecordDetail.CHECK_ABNORMAL)
                                .or().eq(MaintenanceRecordDetail::getBreakageStatus, MaintenanceRecordDetail.CHECK_ABNORMAL)
                                .or().eq(MaintenanceRecordDetail::getStackingStatus, MaintenanceRecordDetail.CHECK_ABNORMAL)
                                .or().eq(MaintenanceRecordDetail::getOtherStatus, MaintenanceRecordDetail.CHECK_ABNORMAL)
                                .or().isNotNull(MaintenanceRecordDetail::getAbnormalDescription)
                                .and(w2 -> w2.ne(MaintenanceRecordDetail::getAbnormalDescription, ""))))
                .stream().map(MaintenanceRecordDetail::getMaintenanceId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    /**
     * 批量加载主单关联明细（防N+1）
     *
     * @param records 主单列表
     * @return 主单ID到明细列表的映射
     */
    private Map<Long, List<MaintenanceRecordDetail>> loadDetailMap(List<MaintenanceRecord> records) {
        List<Long> ids = records.stream().map(MaintenanceRecord::getId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return maintenanceRecordDetailMapper.selectList(
                        new LambdaQueryWrapper<MaintenanceRecordDetail>().in(MaintenanceRecordDetail::getMaintenanceId, ids))
                .stream().collect(Collectors.groupingBy(MaintenanceRecordDetail::getMaintenanceId));
    }

    /**
     * 批量加载创建人/更新人用户映射（防N+1）
     *
     * @param records 主单列表
     * @return 用户ID到用户的映射
     */
    private Map<Long, User> loadUserMap(List<MaintenanceRecord> records) {
        Set<Long> userIds = records.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getCreatedBy(), r.getUpdatedBy()))
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));
    }

    /**
     * 从用户映射中解析用户显示名称（真实姓名，无则回退登录账号）
     *
     * @param userId  用户ID
     * @param userMap 用户映射
     * @return 用户名称
     */
    private String resolveUserNameFromMap(Long userId, Map<Long, User> userMap) {
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
     * 按生产单位ID解析仓库名称
     *
     * @param warehouseId 生产单位ID
     * @return 仓库名称
     */
    private String resolveWarehouseName(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        ProductionUnit unit = productionUnitMapper.selectById(warehouseId);
        return unit == null ? null : unit.getProdUnitName();
    }

    /**
     * 按仓库名称解析生产单位ID（引用检验单的解析方式）
     *
     * @param warehouse 仓库名称
     * @return 生产单位ID
     */
    private Long resolveProdUnitId(String warehouse) {
        List<ProductionUnit> units = productionUnitMapper.selectList(
                new QueryWrapper<ProductionUnit>().eq("prod_unit_name", warehouse));
        if (units.isEmpty()) {
            throw new RuntimeException("仓库不存在: " + warehouse);
        }
        return units.get(0).getProdUnitId();
    }

    /**
     * 按物料编码批量加载物料（供快照带出规格/养护要求）
     *
     * @param stocks 库存明细列表
     * @return 库存明细ID到物料的映射
     */
    private Map<Long, Material> loadMaterialMapByStock(List<Stock> stocks) {
        Set<String> itemCodes = stocks.stream().map(Stock::getItemCode)
                .filter(StringUtils::hasText).collect(Collectors.toSet());
        if (itemCodes.isEmpty()) {
            return Map.of();
        }
        Map<String, Material> byCode = materialMapper.selectList(
                        new LambdaQueryWrapper<Material>().in(Material::getMaterialCode, itemCodes))
                .stream().collect(Collectors.toMap(Material::getMaterialCode, m -> m, (a, b) -> a));
        Map<Long, Material> result = new HashMap<>();
        for (Stock stock : stocks) {
            Material m = byCode.get(stock.getItemCode());
            if (m != null) {
                result.put(stock.getStockId(), m);
            }
        }
        return result;
    }

    /**
     * 加载入库编号映射（入库单ID → 入库编号）
     *
     * @param stocks 库存明细列表
     * @return 入库单ID到入库编号的映射
     */
    private Map<Long, String> loadInCodeMap(Iterable<Stock> stocks) {
        Set<Long> inIds = new java.util.HashSet<>();
        for (Stock s : stocks) {
            if (s.getStockInId() != null) {
                inIds.add(s.getStockInId());
            }
        }
        if (inIds.isEmpty()) {
            return Map.of();
        }
        return stockInMapper.selectBatchIds(inIds).stream()
                .filter(si -> StringUtils.hasText(si.getInCode()))
                .collect(Collectors.toMap(StockIn::getInId, StockIn::getInCode, (a, b) -> a));
    }

    /**
     * 检查项目四态默认值（空则默认未见异常）
     *
     * @param status 检查项目状态
     * @return 状态值
     */
    private Integer defaultStatus(Integer status) {
        return status == null ? MaintenanceRecordDetail.CHECK_NORMAL : status;
    }

    /**
     * 明细是否异常（任一检查项为发现异常或填写了异常描述）
     *
     * @param d 明细实体
     * @return true=异常明细
     */
    private boolean isDetailAbnormal(MaintenanceRecordDetail d) {
        return eqAbnormal(d.getPackagingStatus()) || eqAbnormal(d.getMoistureStatus())
                || eqAbnormal(d.getPestStatus()) || eqAbnormal(d.getMoldStatus())
                || eqAbnormal(d.getOilStatus()) || eqAbnormal(d.getColorStatus())
                || eqAbnormal(d.getOdorStatus()) || eqAbnormal(d.getBreakageStatus())
                || eqAbnormal(d.getStackingStatus()) || eqAbnormal(d.getOtherStatus())
                || StringUtils.hasText(d.getAbnormalDescription());
    }

    /**
     * 状态值是否等于"发现异常"
     *
     * @param status 状态值
     * @return true=发现异常
     */
    private boolean eqAbnormal(Integer status) {
        return status != null && status == MaintenanceRecordDetail.CHECK_ABNORMAL;
    }

    /**
     * 加载有库存的stock_id集合（待养护排除零库存）
     *
     * @param warehouseId 仓库ID（可空）
     * @return 库存明细ID集合
     */
    private Set<Long> loadActiveStockIds(Long warehouseId) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(Stock::getQuantity, 0);
        if (warehouseId != null) {
            wrapper.eq(Stock::getProdUnitId, warehouseId);
        }
        wrapper.select(Stock::getStockId);
        return stockMapper.selectList(wrapper).stream()
                .map(Stock::getStockId).collect(Collectors.toSet());
    }

    /**
     * 计算待养护状态
     *
     * @param nextDate   下次养护日期
     * @param today      今天
     * @param remindDays 提前提醒天数
     * @return OVERDUE/TODAY/SOON，不在提醒范围返回null
     */
    private String computePendingStatus(LocalDate nextDate, LocalDate today, int remindDays) {
        if (nextDate == null) {
            return null;
        }
        long diff = java.time.temporal.ChronoUnit.DAYS.between(today, nextDate);
        if (diff < 0) {
            return PENDING_OVERDUE;
        }
        if (diff == 0) {
            return PENDING_TODAY;
        }
        if (diff <= remindDays) {
            return PENDING_SOON;
        }
        return null;
    }

    /**
     * 待养护状态排序权重（逾期优先）
     *
     * @param status 提醒状态
     * @return 权重
     */
    private int pendingRank(String status) {
        if (PENDING_OVERDUE.equals(status)) {
            return 0;
        }
        if (PENDING_TODAY.equals(status)) {
            return 1;
        }
        if (PENDING_SOON.equals(status)) {
            return 2;
        }
        return 3;
    }

    /**
     * 构建全量分页请求（-1表示全量）
     *
     * @return 分页请求参数
     */
    private PageRequestDto fullPageRequest() {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(-1);
        pageRequest.setPageSize(-1);
        return pageRequest;
    }

    /**
     * 由列表直接构建分页结果
     *
     * @param items      当前页数据
     * @param pageIndex  页码
     * @param totalCount 总记录数
     * @return 分页结果
     */
    private <T> PagedResult<T> fromList(List<T> items, int pageIndex, int totalCount) {
        PagedResult<T> result = new PagedResult<>();
        result.setItems(items);
        result.setTotalCount(totalCount);
        result.setPageIndex(pageIndex);
        result.setPageSize(items.size());
        return result;
    }

    /**
     * 忽略大小写包含判断
     *
     * @param value   待检查值
     * @param keyword 关键字
     * @return true=包含
     */
    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && keyword != null
                && value.toLowerCase().contains(keyword.toLowerCase());
    }

    /**
     * 空值转空字符串
     *
     * @param value 值
     * @return 字符串
     */
    private String nvl(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 检查四态 → 中文
     *
     * @param status 状态值
     * @return 中文文本
     */
    private String checkStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case MaintenanceRecordDetail.CHECK_UNCHECKED -> "未检查";
            case MaintenanceRecordDetail.CHECK_NORMAL -> "未见异常";
            case MaintenanceRecordDetail.CHECK_ABNORMAL -> "发现异常";
            case MaintenanceRecordDetail.CHECK_NA -> "不适用";
            default -> "";
        };
    }

    /**
     * 数值单元格（BigDecimal统一显示）
     *
     * @param cell  单元格
     * @param value 数值
     */
    private void setNumberCell(Cell cell, Number value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof BigDecimal bd) {
            cell.setCellValue(bd.doubleValue());
        } else {
            cell.setCellValue(value.doubleValue());
        }
    }

    // endregion

    // region 附件管理
    // ===================================
    // 附件管理
    // ===================================

    /**
     * 新增/重建时绑定附件（在FileInfo上打business_id/business_type标记）
     *
     * @param businessId   业务ID
     * @param businessType 业务类型
     * @param fileIds      附件文件ID列表（可为空）
     */
    private void bindAttachments(Long businessId, String businessType, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        for (Long fileId : fileIds) {
            FileInfo fileInfo = fileInfoMapper.selectById(fileId);
            if (fileInfo == null) {
                throw new RuntimeException("附件文件不存在：" + fileId);
            }
            fileStorageService.updateBusinessInfo(fileInfo, businessId, businessType);
            fileInfoMapper.updateById(fileInfo);
        }
    }

    /**
     * 解绑业务下全部附件（重建明细前调用）
     *
     * @param businessId   业务ID
     * @param businessType 业务类型
     */
    private void unbindAttachments(Long businessId, String businessType) {
        List<FileInfo> bound = fileInfoService.getFilesByBusiness(businessId, businessType, null);
        for (FileInfo fileInfo : bound) {
            UpdateWrapper<FileInfo> wrapper = new UpdateWrapper<>();
            wrapper.eq("file_id", fileInfo.getFileId())
                    .set("business_id", null)
                    .set("business_type", null);
            fileInfoMapper.update(null, wrapper);
        }
    }

    /**
     * 同步附件：解绑已移除项、绑定新增项
     *
     * @param businessId   业务ID
     * @param businessType 业务类型
     * @param fileIds      最新的附件文件ID列表（null按空列表处理，即清空全部附件）
     */
    private void syncAttachments(Long businessId, String businessType, List<Long> fileIds) {
        Set<Long> newIds = fileIds == null ? Set.of() : fileIds.stream().collect(Collectors.toSet());
        // 解绑当前已绑定但新列表中不存在的文件
        List<FileInfo> boundFiles = fileInfoService.getFilesByBusiness(businessId, businessType, null);
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
            fileStorageService.updateBusinessInfo(fileInfo, businessId, businessType);
            fileInfoMapper.updateById(fileInfo);
        }
    }

    // endregion
}
