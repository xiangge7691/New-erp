package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonghui.erp.Common.Dto.Log.DataChangeLogDetailDto;
import com.tonghui.erp.Common.Dto.Log.DataChangeLogDto;
import com.tonghui.erp.Common.Dto.Log.FieldChange;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.SysDataChangeLog;
import com.tonghui.erp.Data.mapper.SysDataChangeLogMapper;
import com.tonghui.erp.Service.DataChangeLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据变更日志服务实现类
 * <p>
 * 负责变更日志的分页查询、详情查询与过期清理
 * </p>
 */
@Service
public class DataChangeLogServiceImpl implements DataChangeLogService {

    // region 常量与依赖
    // ===================================
    // 常量与依赖
    // ===================================

    private static final Logger log = LoggerFactory.getLogger(DataChangeLogServiceImpl.class);

    /** 单次清理最大循环次数 */
    private static final int MAX_CLEANUP_ROUNDS = 1000;

    /** 字段差异JSON类型 */
    private static final TypeReference<List<FieldChange>> FIELD_CHANGE_TYPE = new TypeReference<>() {
    };

    private final SysDataChangeLogMapper sysDataChangeLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 构造数据变更日志服务
     *
     * @param sysDataChangeLogMapper 数据变更日志Mapper
     * @param objectMapper           Jackson对象映射器
     */
    public DataChangeLogServiceImpl(SysDataChangeLogMapper sysDataChangeLogMapper,
                                    ObjectMapper objectMapper) {
        this.sysDataChangeLogMapper = sysDataChangeLogMapper;
        this.objectMapper = objectMapper;
    }

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 查询指定操作日志下的全部数据变更
     *
     * @param operationLogId 操作日志ID
     * @return 变更列表（按执行顺序）
     */
    @Override
    public List<DataChangeLogDto> listByOperationLogId(Long operationLogId) {
        if (operationLogId == null) {
            return List.of();
        }
        QueryWrapper<SysDataChangeLog> wrapper = new QueryWrapper<>();
        wrapper.eq("operation_log_id", operationLogId).orderByAsc("id");
        return sysDataChangeLogMapper.selectList(wrapper).stream().map(this::toDto).toList();
    }

    /**
     * 分页查询数据变更日志
     *
     * @param operationLogId 操作日志ID
     * @param traceId        变更TraceID
     * @param tableName      表名
     * @param businessId     业务记录主键
     * @param changeType     变更类型
     * @param userId         操作人ID
     * @param userAccount    操作人账号
     * @param startTime      起始时间
     * @param endTime        截止时间
     * @param pageRequest    分页参数
     * @return 分页变更日志
     */
    @Override
    public PagedResult<DataChangeLogDto> search(Long operationLogId, String traceId, String tableName,
                                                String businessId, String changeType, Long userId,
                                                String userAccount, LocalDateTime startTime,
                                                LocalDateTime endTime, PageRequestDto pageRequest) {
        QueryWrapper<SysDataChangeLog> wrapper = new QueryWrapper<>();
        wrapper.eq(operationLogId != null, "operation_log_id", operationLogId);
        wrapper.eq(hasText(traceId), "trace_id", traceId);
        wrapper.eq(hasText(tableName), "table_name", tableName);
        wrapper.eq(hasText(businessId), "business_id", businessId);
        wrapper.eq(hasText(changeType), "change_type", changeType);
        wrapper.eq(userId != null, "user_id", userId);
        wrapper.eq(hasText(userAccount), "user_account", userAccount);
        wrapper.ge(startTime != null, "created_time", startTime);
        wrapper.le(endTime != null, "created_time", endTime);
        wrapper.orderByDesc("created_time").orderByDesc("id");

        Page<SysDataChangeLog> page = PagedResult.toMybatisPage(pageRequest);
        page = sysDataChangeLogMapper.selectPage(page, wrapper);
        PagedResult<SysDataChangeLog> entityResult = PagedResult.fromPage(page, pageRequest);

        PagedResult<DataChangeLogDto> result = new PagedResult<>();
        result.setItems(entityResult.getItems().stream().map(this::toDto).toList());
        result.setTotalCount(entityResult.getTotalCount());
        result.setPageIndex(entityResult.getPageIndex());
        result.setPageSize(entityResult.getPageSize());
        return result;
    }

    /**
     * 查询数据变更详情
     *
     * @param id 变更日志ID
     * @return 变更详情；不存在返回 null
     */
    @Override
    public DataChangeLogDetailDto detail(Long id) {
        if (id == null) {
            return null;
        }
        SysDataChangeLog entity = sysDataChangeLogMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        DataChangeLogDetailDto dto = new DataChangeLogDetailDto();
        BeanUtils.copyProperties(entity, dto);
        dto.setChangedFields(parseChangedFields(entity.getChangedFields()));
        dto.setBeforeJson(entity.getBeforeJson());
        dto.setAfterJson(entity.getAfterJson());
        return dto;
    }

    // endregion

    // region 清理
    // ===================================
    // 清理
    // ===================================

    /**
     * 物理清理指定天数之前的变更日志
     *
     * @param retentionDays 保留天数
     * @param batchSize     单批删除条数
     * @return 实际删除条数
     */
    @Override
    public int cleanup(int retentionDays, int batchSize) {
        if (retentionDays <= 0 || batchSize <= 0) {
            return 0;
        }
        LocalDateTime before = LocalDateTime.now().minusDays(retentionDays);
        int total = 0;
        for (int round = 0; round < MAX_CLEANUP_ROUNDS; round++) {
            int deleted = sysDataChangeLogMapper.deleteBefore(before, batchSize);
            total += deleted;
            if (deleted < batchSize) {
                break;
            }
        }
        return total;
    }

    // endregion

    // region 辅助方法
    // ===================================
    // 辅助方法
    // ===================================

    /**
     * 实体转换为列表项
     *
     * @param entity 变更日志实体
     * @return 列表项
     */
    private DataChangeLogDto toDto(SysDataChangeLog entity) {
        DataChangeLogDto dto = new DataChangeLogDto();
        BeanUtils.copyProperties(entity, dto);
        dto.setChangedFields(parseChangedFields(entity.getChangedFields()));
        return dto;
    }

    /**
     * 解析字段差异 JSON
     *
     * @param json 字段差异JSON
     * @return 字段差异列表
     */
    private List<FieldChange> parseChangedFields(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, FIELD_CHANGE_TYPE);
        } catch (Exception e) {
            log.debug("解析字段差异失败：{}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 判断字符串是否有内容
     *
     * @param text 字符串
     * @return 是否有内容
     */
    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    // endregion
}
