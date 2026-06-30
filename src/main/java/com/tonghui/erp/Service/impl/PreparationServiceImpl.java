package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Config.JwtConfig;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.PreparationWithDetailsDto;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.PreparationDocument;
import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import com.tonghui.erp.Data.mapper.PreparationDocumentMapper;
import com.tonghui.erp.Data.mapper.PreparationFormulaMapper;
import com.tonghui.erp.Data.mapper.PreparationMapper;
import com.tonghui.erp.Data.mapper.PreparationProcessTemplateMapper;
import com.tonghui.erp.Service.PreparationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Common.utils.JwtHelper;

@Service
public class PreparationServiceImpl extends ServiceImpl<PreparationMapper, Preparation> implements PreparationService {

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private PreparationFormulaMapper preparationFormulaMapper;

    @Autowired
    private PreparationDocumentMapper preparationDocumentMapper;

    @Autowired
    private PreparationProcessTemplateMapper preparationProcessTemplateMapper;

    @Override
    public PagedResult<Preparation> getPreparationList(PageRequestDto pageRequestDto) {
        Page<Preparation> page = new Page<>(pageRequestDto.getPageIndex(), pageRequestDto.getPageSize());
        Page<Preparation> preparationPage = this.baseMapper.selectPage(page, null);

        PagedResult<Preparation> pagedResult = new PagedResult<>();
        pagedResult.setItems(preparationPage.getRecords());
        pagedResult.setTotalCount(preparationPage.getTotal());
        pagedResult.setPageIndex(pageRequestDto.getPageIndex());
        pagedResult.setPageSize(pageRequestDto.getPageSize());
        
        return pagedResult;
    }

    // #region 基础操作

    /**
     * 新增制剂
     *
     * @param preparation 制剂实体
     */
    @Override
    public void addPreparation(Preparation preparation) {
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        preparation.setCreatedTime(now);
        preparation.setUpdatedTime(now);
        
        // 获取当前用户ID
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            preparation.setCreatedBy(currentUserId);
            preparation.setUpdatedBy(currentUserId);
        }
        
        this.baseMapper.insert(preparation);
    }

    /**
     * 更新制剂
     *
     * @param preparation 制剂实体
     */
    @Override
    public void updatePreparation(Preparation preparation) {
        // 设置更新时间
        preparation.setUpdatedTime(LocalDateTime.now());
        
        // 获取当前用户ID
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            preparation.setUpdatedBy(currentUserId);
        }
        
        this.baseMapper.updateById(preparation);
    }

    /**
     * 删除制剂
     *
     * @param preparationId 制剂ID
     */
    @Override
    public void deletePreparation(Long preparationId) {
        this.baseMapper.deleteById(preparationId);
    }

    // #endregion

    // #region 查询操作

    /**
     * 根据ID查询制剂
     *
     * @param preparationId 制剂ID
     * @return 制剂实体
     */
    @Override
    public Preparation getPreparationById(Long preparationId) {
        return this.baseMapper.selectById(preparationId);
    }

    /**
     * 根据编码查询制剂
     *
     * @param preparationCode 制剂编码
     * @return 制剂实体
     */
    @Override
    public Preparation getPreparationByCode(String preparationCode) {
        QueryWrapper<Preparation> wrapper = new QueryWrapper<>();
        wrapper.like("preparation_code", preparationCode);
        return this.baseMapper.selectOne(wrapper);
    }

    /**
     * 查询所有启用状态的制剂
     *
     * @return 制剂集合
     */
    @Override
    public List<Preparation> getEnabledPreparations() {
        QueryWrapper<Preparation> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        return this.baseMapper.selectList(wrapper);
    }

    /**
     * 查询所有制剂
     *
     * @return 制剂集合
     */
    @Override
    public List<Preparation> getAllPreparations() {
        return this.baseMapper.selectList(null);
    }

    // #endregion

    //#region 高级查询

    /**
     * 高级查询制剂（支持分页）
     *
     * @param preparation 查询条件
     * @param pageNum     页码
     * @param pageSize    每页大小
     * @return 分页结果
     */
    @Override
    public Page<Preparation> queryPreparations(Preparation preparation, int pageNum, int pageSize) {
        // 如果pageNum和pageSize都为-1，则返回所有数据
        boolean returnAll = (pageNum == -1 && pageSize == -1);
        
        QueryWrapper<Preparation> wrapper = new QueryWrapper<>();

        if (preparation.getPreparationId() != null) {
            wrapper.eq("preparation_id", preparation.getPreparationId());
        }
        if (StringUtils.hasText(preparation.getPreparationCode())) {
            // 构造不连续子串匹配模式，在每个字符之间插入%
            StringBuilder patternBuilder = new StringBuilder();
            for (char c : preparation.getPreparationCode().toCharArray()) {
                patternBuilder.append(c).append("%");
            }
            String pattern = patternBuilder.toString();
            wrapper.like("preparation_code", pattern);
        }

        if (StringUtils.hasText(preparation.getPreparationName())) {
            wrapper.like("preparation_name", preparation.getPreparationName().trim());
        }
        if (StringUtils.hasText(preparation.getSpec())) {
            wrapper.like("spec", preparation.getSpec().trim());
        }
        if (StringUtils.hasText(preparation.getProcessAttr())) {
            wrapper.eq("process_attr", preparation.getProcessAttr().trim());
        }
        if (StringUtils.hasText(preparation.getPackageSpec())) {
            wrapper.eq("package_spec", preparation.getPackageSpec().trim());
        }
        if (StringUtils.hasText(preparation.getDosageForm())) {
            wrapper.eq("dosage_form", preparation.getDosageForm().trim());
        }
        if (preparation.getStatus() != null) {
            wrapper.eq("status", preparation.getStatus());
        }
        if (preparation.getCreatedTime() != null) {
            wrapper.ge("created_time", preparation.getCreatedTime());
        }
        if (preparation.getUpdatedTime() != null) {
            wrapper.le("updated_time", preparation.getUpdatedTime());
        }
        
        // 添加单位名称过滤条件
        if (StringUtils.hasText(preparation.getUnitName())) {
            wrapper.like("unit_name", preparation.getUnitName());
        }
        
        // 添加生产商过滤条件
        if (StringUtils.hasText(preparation.getProducer())) {
            wrapper.like("producer", preparation.getProducer());
        }
        
        // 添加制剂备案过滤条件
        if (StringUtils.hasText(preparation.getRecordInfo())) {
            wrapper.like("record_info", preparation.getRecordInfo());
        }
        
        // 添加功能主治过滤条件
        if (StringUtils.hasText(preparation.getFunctionMain())) {
            wrapper.like("function_main", preparation.getFunctionMain());
        }
        
        // 添加制法过滤条件
        if (StringUtils.hasText(preparation.getMethod())) {
            wrapper.like("method", preparation.getMethod());
        }

        // 默认按照制剂编码倒序排列
        wrapper.orderByDesc("preparation_code");

        // 如果需要返回所有数据
        if (returnAll) {
            List<Preparation> allResults = this.baseMapper.selectList(wrapper);
            Page<Preparation> page = new Page<>();
            page.setRecords(allResults);
            page.setTotal(allResults.size());
            page.setCurrent(1);
            page.setSize(allResults.size());
            return page;
        } else {
            // 处理正常的分页情况
            // 将页码从0开始转换为1开始
            int actualPageNum = pageNum + 1;
            // 当pageSize<=0时，设置一个合理的默认值
            int actualPageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            
            Page<Preparation> page = new Page<>(actualPageNum, actualPageSize);
            return this.baseMapper.selectPage(page, wrapper);
        }
    }

    @Override
    public PagedResult<PreparationWithDetailsDto> searchWithDetails(Preparation preparation, int pageNum, int pageSize) {
        Page<Preparation> parentPage = queryPreparations(preparation, pageNum, pageSize);
        List<Preparation> parents = parentPage.getRecords();

        PagedResult<PreparationWithDetailsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        List<Long> parentIds = parents.stream().map(Preparation::getPreparationId).collect(Collectors.toList());

        QueryWrapper<PreparationFormula> formulaWrapper = new QueryWrapper<>();
        formulaWrapper.in("preparation_id", parentIds);
        List<PreparationFormula> allFormulas = preparationFormulaMapper.selectList(formulaWrapper);
        Map<Long, List<PreparationFormula>> formulasMap = allFormulas.stream()
                .collect(Collectors.groupingBy(PreparationFormula::getPreparationId));

        QueryWrapper<PreparationDocument> docWrapper = new QueryWrapper<>();
        docWrapper.in("preparation_id", parentIds);
        List<PreparationDocument> allDocs = preparationDocumentMapper.selectList(docWrapper);
        Map<Long, List<PreparationDocument>> docsMap = allDocs.stream()
                .collect(Collectors.groupingBy(PreparationDocument::getPreparationId));

        QueryWrapper<PreparationProcessTemplate> templateWrapper = new QueryWrapper<>();
        templateWrapper.in("preparation_id", parentIds);
        List<PreparationProcessTemplate> allTemplates = preparationProcessTemplateMapper.selectList(templateWrapper);
        Map<Long, List<PreparationProcessTemplate>> templatesMap = allTemplates.stream()
                .collect(Collectors.groupingBy(PreparationProcessTemplate::getPreparationId));

        List<PreparationWithDetailsDto> dtos = parents.stream().map(parent -> {
            PreparationWithDetailsDto dto = new PreparationWithDetailsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setFormulas(formulasMap.getOrDefault(parent.getPreparationId(), List.of()));
            dto.setDocuments(docsMap.getOrDefault(parent.getPreparationId(), List.of()));
            dto.setProcessTemplates(templatesMap.getOrDefault(parent.getPreparationId(), List.of()));
            return dto;
        }).collect(Collectors.toList());

        result.setItems(dtos);
        result.setTotalCount(parentPage.getTotal());
        result.setPageIndex(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    /**
     * 从安全上下文中获取当前用户ID
     * 
     * @return 当前用户ID，如果无法获取则返回null
     */
    private Long getCurrentUserId() {
        try {
            // 直接从Security上下文获取认证信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                // 检查是否是UsernamePasswordAuthenticationToken类型
                if (authentication instanceof UsernamePasswordAuthenticationToken) {
                    UsernamePasswordAuthenticationToken authToken = (UsernamePasswordAuthenticationToken) authentication;
                    Object principal = authToken.getPrincipal();
                    
                    // 如果principal是字符串形式的用户ID
                    if (principal instanceof String) {
                        try {
                            return Long.valueOf((String) principal);
                        } catch (NumberFormatException e) {
                            // 不是有效的数字格式
                        }
                    }
                    // 如果principal是数字
                    else if (principal instanceof Long) {
                        return (Long) principal;
                    }
                    else if (principal instanceof Integer) {
                        return ((Integer) principal).longValue();
                    }
                }
                
                // 尝试从details中获取用户信息
                Object details = authentication.getDetails();
                if (details != null) {
                    // 根据实际实现情况处理details
                }
                
                // 最后尝试从credentials获取
                Object credentials = authentication.getCredentials();
                if (credentials instanceof String) {
                    String token = (String) credentials;
                    // 从JWT token中解析用户ID
                    String userIdStr = JwtHelper.getUserIdFromToken(token, jwtConfig.getSecretKey());
                    if (userIdStr != null && !userIdStr.isEmpty()) {
                        try {
                            return Long.parseLong(userIdStr);
                        } catch (NumberFormatException e) {
                            // 解析失败
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 为了确保功能正常，暂时返回默认用户ID 1
        // 实际部署时应移除此行并确保能正确获取用户ID
        return null;
    }
//#endregion
}
