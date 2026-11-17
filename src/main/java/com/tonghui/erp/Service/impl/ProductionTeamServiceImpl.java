package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.utils.SoftDeleteCleanHelper;
import com.tonghui.erp.Data.Entity.PersonnelFile;
import com.tonghui.erp.Data.Entity.ProductionTeam;
import com.tonghui.erp.Data.Entity.ProductionTeamMember;
import com.tonghui.erp.Data.mapper.PersonnelFileMapper;
import com.tonghui.erp.Data.mapper.ProductionTeamMapper;
import com.tonghui.erp.Data.mapper.ProductionTeamMemberMapper;
import com.tonghui.erp.Service.ProductionTeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 生产班组服务实现类
 */
@Service
public class ProductionTeamServiceImpl extends ServiceImpl<ProductionTeamMapper, ProductionTeam>
        implements ProductionTeamService {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 班组成员数据访问层 */
    @Autowired
    private ProductionTeamMemberMapper teamMemberMapper;

    /** 人员档案数据访问层 */
    @Autowired
    private PersonnelFileMapper personnelFileMapper;

    /** 软删除统一清理工具 */
    @Autowired
    private SoftDeleteCleanHelper softDeleteCleanHelper;

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 查询班组列表
     *
     * @param keyword 班组编码/名称关键字
     * @param status  状态：0停用/1启用，为空查全部
     * @return 班组列表（含成员数量）
     */
    @Override
    public List<ProductionTeam> listTeams(String keyword, Integer status) {
        LambdaQueryWrapper<ProductionTeam> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(ProductionTeam::getTeamCode, keyword)
                    .or().like(ProductionTeam::getTeamName, keyword));
        }
        if (status != null) {
            wrapper.eq(ProductionTeam::getStatus, status);
        }
        wrapper.orderByAsc(ProductionTeam::getTeamCode);
        List<ProductionTeam> teams = this.list(wrapper);
        for (ProductionTeam team : teams) {
            team.setMemberCount(Math.toIntExact(teamMemberMapper.selectCount(
                    new LambdaQueryWrapper<ProductionTeamMember>()
                            .eq(ProductionTeamMember::getTeamId, team.getId()))));
        }
        return teams;
    }

    /**
     * 查询班组详情（含成员）
     *
     * @param id 班组ID
     * @return 班组详情
     */
    @Override
    public ProductionTeam getTeamDetail(Long id) {
        ProductionTeam team = this.getById(id);
        if (team == null) {
            return null;
        }
        List<ProductionTeamMember> members = teamMemberMapper.selectByTeamIdWithPersonnel(id);
        team.setMemberCount(members.size());
        team.setMemberIds(members.stream().map(ProductionTeamMember::getPersonnelId).toList());
        // 详情接口复用 memberIds 返回成员ID，成员明细由 /{id}/members 查询
        return team;
    }

    /**
     * 查询班组成员（含人员姓名、工号）
     *
     * @param id 班组ID
     * @return 成员列表
     */
    @Override
    public List<ProductionTeamMember> listMembers(Long id) {
        return teamMemberMapper.selectByTeamIdWithPersonnel(id);
    }

    /**
     * 查询启用班组
     *
     * @return 启用班组列表
     */
    @Override
    public List<ProductionTeam> listActive() {
        return this.lambdaQuery()
                .eq(ProductionTeam::getStatus, 1)
                .orderByAsc(ProductionTeam::getTeamCode)
                .list();
    }

    // endregion

    // region 新增与修改
    // ===================================
    // 新增与修改
    // ===================================

    /**
     * 新增班组
     *
     * @param team 班组
     * @return 新增后的班组
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductionTeam createTeam(ProductionTeam team) {
        normalizeAndValidate(team, null);
        if (!StringUtils.hasText(team.getTeamCode())) {
            team.setTeamCode("BZ" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        }
        if (team.getStatus() == null) {
            team.setStatus(1);
        }
        // 基础数据允许复用已软删的编码/名称，先物理清理冲突行
        softDeleteCleanHelper.cleanByUniqueField(baseMapper, "team_code", team.getTeamCode());
        softDeleteCleanHelper.cleanByUniqueField(baseMapper, "team_name", team.getTeamName());
        this.save(team);
        saveMembers(team.getId(), team.getMemberIds());
        return getTeamDetail(team.getId());
    }

    /**
     * 修改班组
     *
     * @param id   班组ID
     * @param team 班组
     * @return 修改后的班组
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductionTeam updateTeam(Long id, ProductionTeam team) {
        ProductionTeam existing = this.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("班组不存在");
        }
        team.setId(id);
        normalizeAndValidate(team, id);
        if (team.getStatus() == null) {
            team.setStatus(existing.getStatus());
        }
        softDeleteCleanHelper.cleanByUniqueField(baseMapper, "team_code", team.getTeamCode());
        softDeleteCleanHelper.cleanByUniqueField(baseMapper, "team_name", team.getTeamName());
        this.updateById(team);
        if (team.getMemberIds() != null) {
            saveMembers(id, team.getMemberIds());
        }
        return getTeamDetail(id);
    }

    /**
     * 启用/停用班组
     *
     * @param id     班组ID
     * @param status 状态：0停用/1启用
     * @return 修改后的班组
     */
    @Override
    public ProductionTeam changeStatus(Long id, Integer status) {
        ProductionTeam team = this.getById(id);
        if (team == null) {
            throw new IllegalArgumentException("班组不存在");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("状态只能为 0 或 1");
        }
        team.setStatus(status);
        this.updateById(team);
        return team;
    }

    /**
     * 校验班组编码/名称唯一
     *
     * @param team      班组
     * @param excludeId 排除的班组ID
     */
    private void normalizeAndValidate(ProductionTeam team, Long excludeId) {
        if (!StringUtils.hasText(team.getTeamName())) {
            throw new IllegalArgumentException("班组名称不能为空");
        }
        if (StringUtils.hasText(team.getTeamCode())) {
            Long codeCount = this.lambdaQuery()
                    .eq(ProductionTeam::getTeamCode, team.getTeamCode())
                    .ne(excludeId != null, ProductionTeam::getId, excludeId)
                    .count();
            if (codeCount > 0) {
                throw new IllegalArgumentException("班组编码已存在");
            }
        }
        Long nameCount = this.lambdaQuery()
                .eq(ProductionTeam::getTeamName, team.getTeamName())
                .ne(excludeId != null, ProductionTeam::getId, excludeId)
                .count();
        if (nameCount > 0) {
            throw new IllegalArgumentException("班组名称已存在");
        }
    }

    /**
     * 保存班组成员（全量覆盖）
     *
     * @param teamId    班组ID
     * @param memberIds 人员档案ID列表
     */
    private void saveMembers(Long teamId, List<Long> memberIds) {
        if (memberIds == null) {
            return;
        }
        if (CollectionUtils.isEmpty(memberIds)) {
            throw new IllegalArgumentException("班组至少需要一名成员");
        }
        // 物理删除旧成员，避免唯一键 (team_id, personnel_id) 与软删行冲突
        teamMemberMapper.physicalDeleteByTeamId(teamId);
        for (Long personnelId : memberIds.stream().distinct().toList()) {
            PersonnelFile personnel = personnelFileMapper.selectById(personnelId);
            if (personnel == null || Integer.valueOf(1).equals(personnel.getIsDeleted())) {
                throw new IllegalArgumentException("人员档案不存在：" + personnelId);
            }
            ProductionTeamMember member = new ProductionTeamMember();
            member.setTeamId(teamId);
            member.setPersonnelId(personnelId);
            member.setIsPrimary(0);
            member.setIsDeleted(0);
            member.setVersion(0);
            teamMemberMapper.insert(member);
        }
    }

    // endregion
}
