package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.tonghui.erp.Data.mapper.PreparationFormulaMapper;
import com.tonghui.erp.Service.PreparationFormulaService;
import com.tonghui.erp.Common.utils.JwtHelper;
import com.tonghui.erp.Common.Config.JwtConfig;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PreparationFormulaServiceImpl extends ServiceImpl<PreparationFormulaMapper, PreparationFormula> implements PreparationFormulaService {

    /**
     * 获取当前用户ID
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

    /**
     * 新增处方明细
     *
     * @param formula 处方明细实体
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
     * 更新处方明细
     *
     * @param formula 处方明细实体
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
     * 删除处方明细
     *
     * @param formulaId 处方明细ID
     */
    @Override
    public void deleteFormula(Long formulaId) {
        this.baseMapper.deleteById(formulaId);
    }

    /**
     * 根据ID查询处方明细
     *
     * @param formulaId 处方明细ID
     * @return 处方明细实体
     */
    @Override
    public PreparationFormula getFormulaById(Long formulaId) {
        return this.baseMapper.selectById(formulaId);
    }

    /**
     * 根据制剂编码查询所有处方明细
     *
     * @param preparationCode 制剂编码
     * @return 处方明细集合
     */
    @Override
    public List<PreparationFormula> getFormulasByPreparationCode(String preparationCode) {
        QueryWrapper<PreparationFormula> wrapper = new QueryWrapper<>();
        wrapper.eq("preparation_code", preparationCode);
        return this.baseMapper.selectList(wrapper);
    }

    /**
     * 查询所有处方明细
     *
     * @return 处方明细集合
     */
    @Override
    public List<PreparationFormula> getAllFormulas() {
        return this.baseMapper.selectList(null);
    }
}
