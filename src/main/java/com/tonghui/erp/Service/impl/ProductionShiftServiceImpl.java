package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.utils.SoftDeleteCleanHelper;
import com.tonghui.erp.Data.Entity.ProductionShift;
import com.tonghui.erp.Data.Entity.ProductionShiftTeam;
import com.tonghui.erp.Data.Entity.ProductionTeam;
import com.tonghui.erp.Data.mapper.ProductionShiftMapper;
import com.tonghui.erp.Data.mapper.ProductionShiftTeamMapper;
import com.tonghui.erp.Data.mapper.ProductionTeamMapper;
import com.tonghui.erp.Service.ProductionShiftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 生产班次服务实现类
 */
@Service
public class ProductionShiftServiceImpl extends ServiceImpl<ProductionShiftMapper, ProductionShift>
        implements ProductionShiftService {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 班次适用班组数据访问层 */
    @Autowired
    private ProductionShiftTeamMapper shiftTeamMapper;

    /** 生产班组数据访问层 */
    @Autowired
    private ProductionTeamMapper teamMapper;

    /** 软删除统一清理工具 */
    @Autowired
    private SoftDeleteCleanHelper softDeleteCleanHelper;

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 查询班次列表
     *
     * @param keyword 班次名称关键字
     * @param status  状态：0停用/1启用，为空查全部
     * @return 班次列表（含适用班组名称）
     */
    @Override
    public List<ProductionShift> listShifts(String keyword, Integer status) {
        LambdaQueryWrapper<ProductionShift> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ProductionShift::getShiftName, keyword);
        }
        if (status != null) {
            wrapper.eq(ProductionShift::getStatus, status);
        }
        wrapper.orderByAsc(ProductionShift::getStartTime);
        List<ProductionShift> shifts = this.list(wrapper);
        Map<Long, String> teamNames = teamMapper.selectList(null).stream()
                .collect(Collectors.toMap(ProductionTeam::getId, ProductionTeam::getTeamName));
        for (ProductionShift shift : shifts) {
            List<Long> teamIds = shiftTeamMapper.selectTeamIdsByShiftId(shift.getId());
            shift.setTeamIds(teamIds);
            shift.setTeamNames(teamIds.stream().map(teamNames::get)
                    .filter(StringUtils::hasText).collect(Collectors.joining("、")));
        }
        return shifts;
    }

    /**
     * 查询班次详情（含适用班组ID）
     *
     * @param id 班次ID
     * @return 班次详情
     */
    @Override
    public ProductionShift getShiftDetail(Long id) {
        ProductionShift shift = this.getById(id);
        if (shift == null) {
            return null;
        }
        shift.setTeamIds(shiftTeamMapper.selectTeamIdsByShiftId(id));
        return shift;
    }

    /**
     * 查询启用班次
     *
     * @return 启用班次列表
     */
    @Override
    public List<ProductionShift> listActive() {
        return this.lambdaQuery()
                .eq(ProductionShift::getStatus, 1)
                .orderByAsc(ProductionShift::getStartTime)
                .list();
    }

    /**
     * 查询班组适用的启用班次
     *
     * @param teamId 班组ID
     * @return 班次列表
     */
    @Override
    public List<ProductionShift> listShiftsForTeam(Long teamId) {
        return baseMapper.selectActiveByTeamId(teamId);
    }

    // endregion

    // region 新增与修改
    // ===================================
    // 新增与修改
    // ===================================

    /**
     * 新增班次
     *
     * @param shift 班次
     * @return 新增后的班次
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductionShift createShift(ProductionShift shift) {
        normalizeAndValidate(shift, null);
        if (shift.getStatus() == null) {
            shift.setStatus(1);
        }
        shift.setCrossDay(shift.getEndTime().isBefore(shift.getStartTime())
                || shift.getEndTime().equals(shift.getStartTime()) ? 1 : 0);
        softDeleteCleanHelper.cleanByUniqueField(baseMapper, "shift_name", shift.getShiftName());
        this.save(shift);
        saveTeams(shift.getId(), shift.getTeamIds());
        return getShiftDetail(shift.getId());
    }

    /**
     * 修改班次
     *
     * @param id    班次ID
     * @param shift 班次
     * @return 修改后的班次
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductionShift updateShift(Long id, ProductionShift shift) {
        ProductionShift existing = this.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("班次不存在");
        }
        shift.setId(id);
        normalizeAndValidate(shift, id);
        if (shift.getStatus() == null) {
            shift.setStatus(existing.getStatus());
        }
        shift.setCrossDay(shift.getEndTime().isBefore(shift.getStartTime())
                || shift.getEndTime().equals(shift.getStartTime()) ? 1 : 0);
        softDeleteCleanHelper.cleanByUniqueField(baseMapper, "shift_name", shift.getShiftName());
        this.updateById(shift);
        if (shift.getTeamIds() != null) {
            saveTeams(id, shift.getTeamIds());
        }
        return getShiftDetail(id);
    }

    /**
     * 启用/停用班次
     *
     * @param id     班次ID
     * @param status 状态：0停用/1启用
     * @return 修改后的班次
     */
    @Override
    public ProductionShift changeStatus(Long id, Integer status) {
        ProductionShift shift = this.getById(id);
        if (shift == null) {
            throw new IllegalArgumentException("班次不存在");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("状态只能为 0 或 1");
        }
        shift.setStatus(status);
        this.updateById(shift);
        return shift;
    }

    /**
     * 校验班次名称唯一与时间完整
     *
     * @param shift     班次
     * @param excludeId 排除的班次ID
     */
    private void normalizeAndValidate(ProductionShift shift, Long excludeId) {
        if (!StringUtils.hasText(shift.getShiftName())) {
            throw new IllegalArgumentException("班次名称不能为空");
        }
        if (shift.getStartTime() == null || shift.getEndTime() == null) {
            throw new IllegalArgumentException("班次开始与结束时间不能为空");
        }
        Long count = this.lambdaQuery()
                .eq(ProductionShift::getShiftName, shift.getShiftName())
                .ne(excludeId != null, ProductionShift::getId, excludeId)
                .count();
        if (count > 0) {
            throw new IllegalArgumentException("班次名称已存在");
        }
    }

    /**
     * 保存班次适用班组（全量覆盖）
     *
     * @param shiftId 班次ID
     * @param teamIds 班组ID列表
     */
    private void saveTeams(Long shiftId, List<Long> teamIds) {
        if (teamIds == null) {
            return;
        }
        if (CollectionUtils.isEmpty(teamIds)) {
            throw new IllegalArgumentException("班次至少需要选择一个适用班组");
        }
        shiftTeamMapper.physicalDeleteByShiftId(shiftId);
        for (Long teamId : teamIds.stream().distinct().toList()) {
            if (teamMapper.selectById(teamId) == null) {
                throw new IllegalArgumentException("班组不存在：" + teamId);
            }
            ProductionShiftTeam relation = new ProductionShiftTeam();
            relation.setShiftId(shiftId);
            relation.setTeamId(teamId);
            relation.setIsDeleted(0);
            relation.setVersion(0);
            shiftTeamMapper.insert(relation);
        }
    }

    // endregion
}
