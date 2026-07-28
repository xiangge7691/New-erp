package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Config.JwtConfig;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.ProductionUnitWithDetailsDto;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Common.utils.SoftDeleteCleanHelper;
import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.ProdUnitInvoice;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.mapper.MaterialMapper;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.ProdUnitInvoiceMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
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

    /** 物料数据访问层，用于查询所有物料 */
    @Autowired
    private MaterialMapper materialMapper;

    /** 库存数据访问层，用于创建生产单位时自动生成库存记录 */
    @Autowired
    private StockMapper stockMapper;

    /** 软删除统一清理工具 */
    @Autowired
    private SoftDeleteCleanHelper softDeleteCleanHelper;

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
     * <p>自动设置创建时间、更新时间、创建人和更新人。
     * 创建成功后自动为每个物料创建库存基础记录。</p>
     *
     * @param productionUnit 生产单位实体
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean addProductionUnit(ProductionUnit productionUnit) {
        // 清理已软删除的相同编码记录（避免唯一键冲突）
        if (productionUnit.getProdUnitCode() != null && !productionUnit.getProdUnitCode().isEmpty()) {
            cleanSoftDeletedByProdUnitCode(productionUnit.getProdUnitCode());
        }

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

        boolean result = this.save(productionUnit);

        if (result) {
            // 为每个物料创建库存基础记录
            createStockRecordsForAllMaterials(productionUnit);
        }

        return result;
    }

    /**
     * 清理指定生产单位编码下已被软删除的记录（释放唯一键约束）
     * <p>先物理删除子表关联记录，再物理删除 production_unit 记录</p>
     *
     * @param prodUnitCode 生产单位编码
     * @return 清理的记录数
     */
    public int cleanSoftDeletedByProdUnitCode(String prodUnitCode) {
        // 使用原生SQL查询已软删除的生产单位ID（绕过MyBatis-Plus软删除配置）
        Long deletedId = baseMapper.selectDeletedIdByCode(prodUnitCode);
        if (deletedId == null) {
            return 0;
        }

        // 先物理删除子表关联记录（使用原生SQL绕过软删除配置）
        baseMapper.physicalDeleteStockByProdUnitId(deletedId);

        // 再物理删除 production_unit 记录
        return baseMapper.physicalDeleteByProdUnitId(deletedId);
    }

    /**
     * 为所有未删除的物料创建该生产单位的库存记录
     *
     * @param productionUnit 已插入的生产单位实体
     */
    private void createStockRecordsForAllMaterials(ProductionUnit productionUnit) {
        // 查询所有未删除的物料
        QueryWrapper<Material> materialWrapper = new QueryWrapper<>();
        materialWrapper.eq("is_deleted", 0);
        List<Material> materials = materialMapper.selectList(materialWrapper);

        if (materials.isEmpty()) {
            return;
        }

        // 为每个物料创建库存记录
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDate today = java.time.LocalDate.now();
        for (Material material : materials) {
            Stock stock = new Stock();
            stock.setProdUnitId(productionUnit.getProdUnitId());
            stock.setItemType("material");
            stock.setItemId(material.getMaterialId());
            stock.setItemCode(material.getMaterialCode());
            stock.setItemName(material.getMaterialName());
            stock.setCategoryName(material.getCategoryName());
            stock.setUnitName(material.getUnitName());
            stock.setQuantity(java.math.BigDecimal.ZERO);
            stock.setBatchNumber("");
            stock.setProductionDate(today);
            stock.setExpiryDate(today.plusYears(5));
            stock.setStorageLocation("");
            stock.setRemark("");
            stock.setStockStatus(1);
            stock.setIsDeleted(0);
            stock.setVersion(1);
            stock.setCreatedBy(productionUnit.getCreatedBy());
            stock.setUpdatedBy(productionUnit.getUpdatedBy());
            stock.setCreatedTime(now);
            stock.setUpdatedTime(now);
            stockMapper.insert(stock);
        }
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
     * 删除生产单位（含关联的发票和库存）
     * <p>先软删除关联的库存记录，再删除关联的发票，最后删除生产单位主表</p>
     *
     * @param prodUnitId 生产单位ID
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean deleteProductionUnit(Long prodUnitId) {
        // 先软删除关联的库存记录（使用原生SQL绕过MyBatis-Plus软删除配置）
        stockMapper.softDeleteByProdUnitId(prodUnitId);

        // 再删除关联的发票
        QueryWrapper<ProdUnitInvoice> invoiceWrapper = new QueryWrapper<>();
        invoiceWrapper.eq("prod_unit_id", prodUnitId);
        prodUnitInvoiceMapper.delete(invoiceWrapper);

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
     * 查询生产单位列表并关联发票信息
     * <p>先分页查询生产单位主表数据，再批量查询关联的发票</p>
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

        // 组装带子表数据的DTO
        List<ProductionUnitWithDetailsDto> dtos = parents.stream().map(parent -> {
            ProductionUnitWithDetailsDto dto = new ProductionUnitWithDetailsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setInvoices(invoicesMap.getOrDefault(parent.getProdUnitId(), List.of()));
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
     * 批量新增生产单位发票信息
     * <p>先删除原有发票，再批量插入新发票</p>
     *
     * @param prodUnitId       生产单位ID
     * @param prodInvoiceInfos 发票信息列表
     * @return 新增的发票列表
     */
    @Override
    @Transactional
    public List<ProdUnitInvoice> addProdUnitInvoices(Long prodUnitId, List<String> prodInvoiceInfos) {
        // 先删除原有发票
        QueryWrapper<ProdUnitInvoice> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("prod_unit_id", prodUnitId);
        prodUnitInvoiceMapper.delete(deleteWrapper);

        // 批量插入新发票
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        Long currentUserId = EntityUtils.getCurrentUserId();

        List<ProdUnitInvoice> invoices = new java.util.ArrayList<>();
        for (String prodInvoiceInfo : prodInvoiceInfos) {
            ProdUnitInvoice invoice = new ProdUnitInvoice();
            invoice.setProdUnitId(prodUnitId);
            invoice.setProdInvoiceInfo(prodInvoiceInfo);
            invoice.setCreatedTime(now);
            if (currentUserId != null) {
                invoice.setCreatedBy(currentUserId);
            }
            prodUnitInvoiceMapper.insert(invoice);
            invoices.add(invoice);
        }
        return invoices;
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
}
