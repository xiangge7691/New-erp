package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Config.JwtConfig;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.ProductionUnitWithDetailsDto;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.ProdUnitMaterialFile;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.ProdUnitInvoice;
import com.tonghui.erp.Data.mapper.ProdUnitMaterialFileMapper;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.ProdUnitInvoiceMapper;
import com.tonghui.erp.Service.FileStorageService;
import com.tonghui.erp.Service.ProductionUnitService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 生产单位服务实现类
 * <p>
 * 实现ProductionUnitService接口，提供生产单位相关的业务逻辑处理，包括生产单位的增删改查、
 * 高级查询、带子表关联查询、发票信息管理、材料文件管理等功能的具体实现
 * </p>
 *
 */
@Service
public class ProductionUnitServiceImpl extends ServiceImpl<ProductionUnitMapper, ProductionUnit> implements ProductionUnitService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 生产单位发票数据访问层 */
    @Autowired
    private ProdUnitInvoiceMapper prodUnitInvoiceMapper;

    /** 生产单位材料文件数据访问层 */
    @Autowired
    private ProdUnitMaterialFileMapper prodUnitMaterialFileMapper;
    
    /** JWT配置，用于获取当前用户信息 */
    @Autowired
    private JwtConfig jwtConfig;

    /** 文件存储服务，用于处理文件上传和MD5计算 */
    @Autowired
    private FileStorageService fileStorageService;

    // endregion

    // region 分页查询方法
    // ===================================
    // 分页查询方法
    // ===================================

    /**
     * 分页查询生产单位列表
     *
     * @param pageRequestDto 分页请求参数，包含页码和每页数量
     * @return 生产单位分页结果
     */
    @Override
    public PagedResult<ProductionUnit> getProductionUnitList(PageRequestDto pageRequestDto) {
        Page<ProductionUnit> page = new Page<>(pageRequestDto.getPageIndex(), pageRequestDto.getPageSize());
        Page<ProductionUnit> productionUnitPage = this.baseMapper.selectPage(page, null);

        PagedResult<ProductionUnit> pagedResult = new PagedResult<>();
        pagedResult.setItems(productionUnitPage.getRecords());
        pagedResult.setTotalCount(productionUnitPage.getTotal());
        pagedResult.setPageIndex(pageRequestDto.getPageIndex());
        pagedResult.setPageSize(pageRequestDto.getPageSize());

        return pagedResult;
    }

    // endregion

    // region 基础CRUD操作
    // ===================================
    // 基础CRUD操作
    // ===================================

    /**
     * 新增生产单位
     * <p>自动设置创建时间、更新时间、创建人和更新人</p>
     *
     * @param productionUnit 生产单位实体
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean addProductionUnit(ProductionUnit productionUnit) {
        // 设置创建时间和更新时间
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        productionUnit.setCreatedTime(now);
        productionUnit.setUpdatedTime(now);
        
        // 获取当前用户ID
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            productionUnit.setCreatedBy(currentUserId);
            productionUnit.setUpdatedBy(currentUserId);
        }
        
        return this.save(productionUnit);
    }

    /**
     * 更新生产单位
     * <p>自动更新更新时间和更新人</p>
     *
     * @param productionUnit 生产单位实体，包含要更新的字段信息
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean updateProductionUnit(ProductionUnit productionUnit) {
        // 设置更新时间
        productionUnit.setUpdatedTime(java.time.LocalDateTime.now());
        
        // 获取当前用户ID
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            productionUnit.setUpdatedBy(currentUserId);
        }
        
        return this.updateById(productionUnit);
    }

    /**
     * 删除生产单位（含关联的发票和材料文件）
     * <p>先删除关联的发票和材料文件，再删除生产单位主表</p>
     *
     * @param prodUnitId 生产单位ID
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean deleteProductionUnit(Long prodUnitId) {
        // 先删除关联的发票
        QueryWrapper<ProdUnitInvoice> invoiceWrapper = new QueryWrapper<>();
        invoiceWrapper.eq("prod_unit_id", prodUnitId);
        prodUnitInvoiceMapper.delete(invoiceWrapper);

        // 再删除关联的材料文件
        QueryWrapper<ProdUnitMaterialFile> materialWrapper = new QueryWrapper<>();
        materialWrapper.eq("prod_unit_id", prodUnitId);
        prodUnitMaterialFileMapper.delete(materialWrapper);

        // 最后删除生产单位
        return this.removeById(prodUnitId);
    }

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据ID查询生产单位
     *
     * @param prodUnitId 生产单位ID
     * @return 生产单位实体，不存在则返回null
     */
    @Override
    public ProductionUnit getProductionUnitById(Long prodUnitId) {
        return this.getById(prodUnitId);
    }

    /**
     * 根据编码查询生产单位
     *
     * @param prodUnitCode 生产单位编码
     * @return 生产单位实体，不存在则返回null
     */
    @Override
    public ProductionUnit getProductionUnitByCode(String prodUnitCode) {
        QueryWrapper<ProductionUnit> wrapper = new QueryWrapper<>();
        wrapper.eq("prod_unit_code", prodUnitCode);
        return this.getOne(wrapper);
    }

    /**
     * 查询所有启用状态的生产单位
     *
     * @return 启用状态的生产单位集合
     */
    @Override
    public List<ProductionUnit> getEnabledProductionUnits() {
        QueryWrapper<ProductionUnit> wrapper = new QueryWrapper<>();
        wrapper.eq("prod_unit_status", 1);
        return this.list(wrapper);
    }

    /**
     * 查询所有生产单位
     *
     * @return 全部生产单位集合
     */
    @Override
    public List<ProductionUnit> getAllProductionUnits() {
        return this.list();
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询生产单位（使用默认时间范围）
     *
     * @param productionUnit 查询条件实体
     * @param pageNum        页码，从0开始
     * @param pageSize       每页数量
     * @return 生产单位分页结果
     */
    @Override
    public Page<ProductionUnit> queryProductionUnits(ProductionUnit productionUnit, int pageNum, int pageSize) {
        return queryProductionUnits(productionUnit, null, null, null, null, pageNum, pageSize);
    }

    /**
     * 高级查询生产单位（支持自定义时间范围筛选）
     * <p>支持按编码、名称、地址、负责人、电话、状态等条件组合查询</p>
     *
     * @param productionUnit  查询条件实体
     * @param createdTimeStart 创建时间起始值（含）
     * @param createdTimeEnd   创建时间结束值（含）
     * @param updatedTimeStart 更新时间起始值（含）
     * @param updatedTimeEnd   更新时间结束值（含）
     * @param pageNum          页码，从0开始
     * @param pageSize         每页数量
     * @return 生产单位分页结果
     */
    @Override
    public Page<ProductionUnit> queryProductionUnits(ProductionUnit productionUnit, 
                                                     LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                                     LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                                     int pageNum, int pageSize) {
        int actualPageNum = pageNum + 1;

        Page<ProductionUnit> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<ProductionUnit> wrapper = new QueryWrapper<>();

        if (productionUnit.getProdUnitId() != null) {
            wrapper.eq("prod_unit_id", productionUnit.getProdUnitId());
        }
        if (StringUtils.hasText(productionUnit.getProdUnitCode())) {
            wrapper.like("prod_unit_code", productionUnit.getProdUnitCode());
        }
        if (StringUtils.hasText(productionUnit.getProdUnitName())) {
            wrapper.like("prod_unit_name", productionUnit.getProdUnitName());
        }
        if (StringUtils.hasText(productionUnit.getProdUnitAddress())) {
            wrapper.like("prod_unit_address", productionUnit.getProdUnitAddress());
        }
        if (StringUtils.hasText(productionUnit.getProdUnitManager())) {
            wrapper.like("prod_unit_manager", productionUnit.getProdUnitManager());
        }
        if (StringUtils.hasText(productionUnit.getProdUnitPhone())) {
            wrapper.like("prod_unit_phone", productionUnit.getProdUnitPhone());
        }
        if (productionUnit.getProdUnitStatus() != null) {
            wrapper.eq("prod_unit_status", productionUnit.getProdUnitStatus());
        }
        
        // 创建时间范围查询
        if (createdTimeStart != null) {
            wrapper.ge("created_time", createdTimeStart);
        }
        if (createdTimeEnd != null) {
            wrapper.le("created_time", createdTimeEnd);
        }
        
        // 更新时间范围查询
        if (updatedTimeStart != null) {
            wrapper.ge("updated_time", updatedTimeStart);
        }
        if (updatedTimeEnd != null) {
            wrapper.le("updated_time", updatedTimeEnd);
        }

        return this.page(page, wrapper);
    }

    // endregion

    // region 带子表关联查询
    // ===================================
    // 带子表关联查询
    // ===================================

    /**
     * 查询生产单位列表并关联发票和材料文件信息
     * <p>先分页查询生产单位主表数据，再批量查询关联的发票和材料文件</p>
     *
     * @param productionUnit   查询条件实体
     * @param createdTimeStart 创建时间起始值（含）
     * @param createdTimeEnd   创建时间结束值（含）
     * @param updatedTimeStart 更新时间起始值（含）
     * @param updatedTimeEnd   更新时间结束值（含）
     * @param pageNum          页码，从0开始
     * @param pageSize         每页数量
     * @return 带子表关联数据的生产单位分页结果
     */
    @Override
    public PagedResult<ProductionUnitWithDetailsDto> searchWithDetails(ProductionUnit productionUnit,
                                                                       LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                                                       LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                                                       int pageNum, int pageSize) {
        // 查询生产单位主表分页数据
        Page<ProductionUnit> parentPage = queryProductionUnits(productionUnit, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd, pageNum, pageSize);
        List<ProductionUnit> parents = parentPage.getRecords();

        PagedResult<ProductionUnitWithDetailsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        // 批量查询关联的发票
        List<Long> parentIds = parents.stream().map(ProductionUnit::getProdUnitId).collect(Collectors.toList());

        QueryWrapper<ProdUnitInvoice> invoiceWrapper = new QueryWrapper<>();
        invoiceWrapper.in("prod_unit_id", parentIds);
        List<ProdUnitInvoice> allInvoices = prodUnitInvoiceMapper.selectList(invoiceWrapper);
        Map<Long, List<ProdUnitInvoice>> invoicesMap = allInvoices.stream()
                .collect(Collectors.groupingBy(ProdUnitInvoice::getProdUnitId));

        // 批量查询关联的材料文件
        QueryWrapper<ProdUnitMaterialFile> materialWrapper = new QueryWrapper<>();
        materialWrapper.in("prod_unit_id", parentIds);
        List<ProdUnitMaterialFile> allMaterials = prodUnitMaterialFileMapper.selectList(materialWrapper);
        Map<Long, List<ProdUnitMaterialFile>> materialsMap = allMaterials.stream()
                .collect(Collectors.groupingBy(ProdUnitMaterialFile::getProdUnitId));

        // 组装带子表数据的DTO
        List<ProductionUnitWithDetailsDto> dtos = parents.stream().map(parent -> {
            ProductionUnitWithDetailsDto dto = new ProductionUnitWithDetailsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setInvoices(invoicesMap.getOrDefault(parent.getProdUnitId(), List.of()));
            dto.setMaterialFiles(materialsMap.getOrDefault(parent.getProdUnitId(), List.of()));
            return dto;
        }).collect(Collectors.toList());

        result.setItems(dtos);
        result.setTotalCount(parentPage.getTotal());
        result.setPageIndex(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    // endregion

    // region 生产单位发票信息操作
    // ===================================
    // 生产单位发票信息操作
    // ===================================

    /**
     * 新增生产单位发票信息
     *
     * @param prodUnitId      生产单位ID
     * @param prodInvoiceInfo 发票信息
     * @return 新增的发票实体
     */
    @Override
    @Transactional
    public ProdUnitInvoice addProdUnitInvoice(Long prodUnitId, String prodInvoiceInfo) {
        ProdUnitInvoice invoice = new ProdUnitInvoice();
        invoice.setProdUnitId(prodUnitId);
        invoice.setProdInvoiceInfo(prodInvoiceInfo);
        
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        invoice.setCreatedTime(now);
        
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            invoice.setCreatedBy(currentUserId);
        }
        
        prodUnitInvoiceMapper.insert(invoice);
        return invoice;
    }

    /**
     * 查询生产单位的所有发票信息
     *
     * @param prodUnitId 生产单位ID
     * @return 发票信息列表
     */
    @Override
    public List<ProdUnitInvoice> getProdUnitInvoices(Long prodUnitId) {
        QueryWrapper<ProdUnitInvoice> wrapper = new QueryWrapper<>();
        wrapper.eq("prod_unit_id", prodUnitId);
        return prodUnitInvoiceMapper.selectList(wrapper);
    }

    /**
     * 删除生产单位发票信息
     *
     * @param prodInvoiceId 发票信息ID
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean deleteProdUnitInvoice(Long prodInvoiceId) {
        return prodUnitInvoiceMapper.deleteById(prodInvoiceId) > 0;
    }

    // endregion

    // region 生产单位材料文件操作
    // ===================================
    // 生产单位材料文件操作
    // ===================================

    /**
     * 新增生产单位材料文件
     *
     * @param materialFile 材料文件实体
     * @return 新增的材料文件实体
     */
    @Override
    @Transactional
    public ProdUnitMaterialFile addProdUnitMaterialFile(ProdUnitMaterialFile materialFile) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        materialFile.setCreatedTime(now);
        
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            materialFile.setCreatedBy(currentUserId);
        }
        
        prodUnitMaterialFileMapper.insert(materialFile);
        return materialFile;
    }

    /**
     * 新增生产单位材料文件（通过文件属性信息）
     *
     * @param prodUnitId   生产单位ID
     * @param materialType 材料类型
     * @param fileName     文件名
     * @param fileMd5      文件MD5值
     * @param fileSize     文件大小
     * @param description  文件描述
     * @return 新增的材料文件实体
     */
    @Override
    @Transactional
    public ProdUnitMaterialFile addProdUnitMaterialFile(Long prodUnitId, String materialType, String fileName, String fileMd5, Long fileSize, String description) {
        ProdUnitMaterialFile materialFile = new ProdUnitMaterialFile();
        materialFile.setProdUnitId(prodUnitId);
        materialFile.setMaterialType(materialType);
        materialFile.setFileName(fileName);
        materialFile.setFileMd5(fileMd5);
        materialFile.setFileSize(fileSize != null ? fileSize.intValue() : null);
        
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        materialFile.setCreatedTime(now);
        
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            materialFile.setCreatedBy(currentUserId);
        }
        
        prodUnitMaterialFileMapper.insert(materialFile);
        return materialFile;
    }

    /**
     * 新增生产单位材料文件（通过MultipartFile上传）
     * <p>自动计算MD5值并将文件内容转为Base64编码存储</p>
     *
     * @param prodUnitId   生产单位ID
     * @param materialType 材料类型
     * @param file         上传的文件
     * @param description  文件描述
     * @return 新增的材料文件实体
     */
    @Override
    @Transactional
    public ProdUnitMaterialFile addProdUnitMaterialFile(Long prodUnitId, String materialType, MultipartFile file, String description) {
        try {
            // 计算文件MD5值
            String fileMd5 = fileStorageService.calculateMD5(file);
            // 将文件内容转为Base64编码
            String fileContent = fileStorageService.encodeFileToBase64(file);
            
            ProdUnitMaterialFile materialFile = new ProdUnitMaterialFile();
            materialFile.setProdUnitId(prodUnitId);
            materialFile.setMaterialType(materialType);
            materialFile.setFileName(file.getOriginalFilename());
            materialFile.setFileMd5(fileMd5);
            materialFile.setFileSize((int) file.getSize());
            materialFile.setFileContent(fileContent);
            
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            materialFile.setCreatedTime(now);
            
            Long currentUserId = EntityUtils.getCurrentUserId();
            if (currentUserId != null) {
                materialFile.setCreatedBy(currentUserId);
            }
            
            prodUnitMaterialFileMapper.insert(materialFile);
            return materialFile;
        } catch (Exception e) {
            throw new RuntimeException("添加材料文件失败", e);
        }
    }

    /**
     * 将Base64编码的文件内容解码为输入流
     *
     * @param fileContent Base64编码的文件内容
     * @return 文件输入流
     */
    @Override
    public InputStream getFileInputStream(String fileContent) {
        try {
            byte[] fileBytes = java.util.Base64.getDecoder().decode(fileContent);
            return new java.io.ByteArrayInputStream(fileBytes);
        } catch (Exception e) {
            throw new RuntimeException("解码文件内容失败", e);
        }
    }

    /**
     * 根据ID查询材料文件
     *
     * @param prodMaterialId 材料文件ID
     * @return 材料文件实体，不存在则返回null
     */
    @Override
    public ProdUnitMaterialFile getProdUnitMaterialFileById(Long prodMaterialId) {
        return prodUnitMaterialFileMapper.selectById(prodMaterialId);
    }

    /**
     * 查询生产单位的所有材料文件
     *
     * @param prodUnitId 生产单位ID
     * @return 材料文件列表
     */
    @Override
    public List<ProdUnitMaterialFile> getProdUnitMaterialFiles(Long prodUnitId) {
        QueryWrapper<ProdUnitMaterialFile> wrapper = new QueryWrapper<>();
        wrapper.eq("prod_unit_id", prodUnitId);
        return prodUnitMaterialFileMapper.selectList(wrapper);
    }

    /**
     * 删除生产单位材料文件
     *
     * @param prodMaterialId 材料文件ID
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean deleteProdUnitMaterialFile(Long prodMaterialId) {
        return prodUnitMaterialFileMapper.deleteById(prodMaterialId) > 0;
    }

    // endregion
}
