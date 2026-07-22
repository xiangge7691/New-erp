package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.tonghui.erp.Data.mapper.PreparationFormulaMapper;
import com.tonghui.erp.Data.mapper.PreparationMapper;
import com.tonghui.erp.Service.PreparationFormulaService;
import com.tonghui.erp.Common.utils.JwtHelper;
import com.tonghui.erp.Common.Config.JwtConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 制剂处方明细服务实现类
 * <p>
 * 实现PreparationFormulaService接口，提供制剂处方明细相关的业务逻辑处理，
 * 包括处方明细的增删改查等功能的具体实现
 * </p>
 *
 */
@Service
public class PreparationFormulaServiceImpl extends ServiceImpl<PreparationFormulaMapper, PreparationFormula> implements PreparationFormulaService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    @Autowired
    private PreparationMapper preparationMapper;

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 获取当前登录用户的用户ID
     * <p>从Spring Security上下文中提取认证信息，解析出当前用户的ID</p>
     *
     * @return 当前用户ID，如果无法获取则返回默认值1L
     */
    private Long getCurrentUserId() {
        try {
            // 从Security上下文中获取认证信息
            Object authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof UsernamePasswordAuthenticationToken) {
                UsernamePasswordAuthenticationToken authToken = (UsernamePasswordAuthenticationToken) authentication;
                String userIdStr = (String) authToken.getPrincipal();
                return Long.parseLong(userIdStr);
            }
        } catch (Exception e) {
            // 如果无法获取当前用户ID，使用默认值
            return 1L;
        }
        return 1L;
    }

    // endregion

    // region 基础CRUD操作
    // ===================================
    // 基础CRUD操作
    // ===================================

    /**
     * 新增制剂处方明细
     * <p>自动设置创建人、更新人、创建时间和更新时间</p>
     *
     * @param formula 制剂处方明细实体，包含处方关联的制剂编码、物料信息和用量等
     */
    @Override
    public void addFormula(PreparationFormula formula) {
        Long currentUserId = getCurrentUserId();

        if (formula.getCreatedBy() == null) {
            formula.setCreatedBy(currentUserId);
        }
        if (formula.getUpdatedBy() == null) {
            formula.setUpdatedBy(currentUserId);
        }
        if (formula.getCreatedTime() == null) {
            formula.setCreatedTime(LocalDateTime.now());
        }
        if (formula.getUpdatedTime() == null) {
            formula.setUpdatedTime(LocalDateTime.now());
        }
        this.baseMapper.insert(formula);
    }

    /**
     * 更新制剂处方明细
     * <p>自动更新更新时间和更新人信息</p>
     *
     * @param formula 制剂处方明细实体，包含要更新的字段信息
     */
    @Override
    public void updateFormula(PreparationFormula formula) {
        Long currentUserId = getCurrentUserId();

        formula.setUpdatedTime(LocalDateTime.now());
        if (formula.getUpdatedBy() == null) {
            formula.setUpdatedBy(currentUserId);
        }
        this.baseMapper.updateById(formula);
    }

    /**
     * 删除制剂处方明细
     *
     * @param formulaId 制剂处方明细ID
     */
    @Override
    public void deleteFormula(Long formulaId) {
        this.baseMapper.deleteById(formulaId);
    }

    /**
     * 根据ID查询制剂处方明细
     *
     * @param formulaId 制剂处方明细ID
     * @return 制剂处方明细实体，不存在则返回null
     */
    @Override
    public PreparationFormula getFormulaById(Long formulaId) {
        return this.baseMapper.selectById(formulaId);
    }

    // endregion

    // region 业务查询方法
    // ===================================
    // 业务查询方法
    // ===================================

    /**
     * 根据制剂编码查询所有关联的处方明细
     *
     * @param preparationCode 制剂编码
     * @return 该制剂编码下所有处方明细的集合
     */
    @Override
    public List<PreparationFormula> getFormulasByPreparationCode(String preparationCode) {
        QueryWrapper<PreparationFormula> wrapper = new QueryWrapper<>();
        wrapper.eq("preparation_code", preparationCode);
        return this.baseMapper.selectList(wrapper);
    }

    /**
     * 查询所有制剂处方明细
     *
     * @return 全部处方明细的集合
     */
    @Override
    public List<PreparationFormula> getAllFormulas() {
        return this.baseMapper.selectList(null);
    }

    // endregion

    // region 批量操作
    // ===================================
    // 批量操作
    // ===================================

    /**
     * 批量保存处方明细
     * <p>先删除该制剂原有的处方，再批量插入新处方</p>
     *
     * @param preparationId 制剂ID
     * @param formulas      处方明细列表
     */
    @Override
    @Transactional
    public void batchSave(Long preparationId, List<PreparationFormula> formulas) {
        QueryWrapper<PreparationFormula> wrapper = new QueryWrapper<>();
        wrapper.eq("preparation_id", preparationId);
        baseMapper.delete(wrapper);

        if (formulas != null && !formulas.isEmpty()) {
            Preparation preparation = preparationMapper.selectById(preparationId);
            String prepCode = preparation != null ? preparation.getPreparationCode() : null;
            String prepName = preparation != null ? preparation.getPreparationName() : null;

            formulas.forEach(f -> {
                f.setPreparationId(preparationId);
                f.setFormulaId(null);
                f.setPreparationCode(prepCode);
                f.setPreparationName(prepName);
            });
            saveBatch(formulas);
        }
    }

    // endregion
}
