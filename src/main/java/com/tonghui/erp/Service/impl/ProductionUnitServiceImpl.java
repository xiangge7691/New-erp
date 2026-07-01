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

@Service
public class ProductionUnitServiceImpl extends ServiceImpl<ProductionUnitMapper, ProductionUnit> implements ProductionUnitService {

    @Autowired
    private ProdUnitInvoiceMapper prodUnitInvoiceMapper;

    @Autowired
    private ProdUnitMaterialFileMapper prodUnitMaterialFileMapper;
    
    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private FileStorageService fileStorageService;

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

    //#region 基础操作

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

    @Override
    @Transactional
    public boolean deleteProductionUnit(Long prodUnitId) {
        // 删除生产单位会同时删除关联的发票和材料文件（由于外键约束，数据库层也会级联删除，但这里我们显式删除以确保业务清晰）
        // 先删除关联的发票和材料文件
        QueryWrapper<ProdUnitInvoice> invoiceWrapper = new QueryWrapper<>();
        invoiceWrapper.eq("prod_unit_id", prodUnitId);
        prodUnitInvoiceMapper.delete(invoiceWrapper);

        QueryWrapper<ProdUnitMaterialFile> materialWrapper = new QueryWrapper<>();
        materialWrapper.eq("prod_unit_id", prodUnitId);
        prodUnitMaterialFileMapper.delete(materialWrapper);

        // 再删除生产单位
        return this.removeById(prodUnitId);
    }

    //#endregion

    //#region 查询操作

    @Override
    public ProductionUnit getProductionUnitById(Long prodUnitId) {
        return this.getById(prodUnitId);
    }

    @Override
    public ProductionUnit getProductionUnitByCode(String prodUnitCode) {
        QueryWrapper<ProductionUnit> wrapper = new QueryWrapper<>();
        wrapper.eq("prod_unit_code", prodUnitCode);
        return this.getOne(wrapper);
    }

    @Override
    public List<ProductionUnit> getEnabledProductionUnits() {
        QueryWrapper<ProductionUnit> wrapper = new QueryWrapper<>();
        wrapper.eq("prod_unit_status", 1);
        return this.list(wrapper);
    }

    @Override
    public List<ProductionUnit> getAllProductionUnits() {
        return this.list();
    }

    //#endregion

    //#region 高级查询

    @Override
    public Page<ProductionUnit> queryProductionUnits(ProductionUnit productionUnit, int pageNum, int pageSize) {
        return queryProductionUnits(productionUnit, null, null, null, null, pageNum, pageSize);
    }

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
        
        // 处理创建时间范围查询
        if (createdTimeStart != null) {
            wrapper.ge("created_time", createdTimeStart);
        }
        if (createdTimeEnd != null) {
            wrapper.le("created_time", createdTimeEnd);
        }
        
        // 处理更新时间范围查询
        if (updatedTimeStart != null) {
            wrapper.ge("updated_time", updatedTimeStart);
        }
        if (updatedTimeEnd != null) {
            wrapper.le("updated_time", updatedTimeEnd);
        }

        return this.page(page, wrapper);
    }

    //#endregion

    //#region 带子表查询

    @Override
    public PagedResult<ProductionUnitWithDetailsDto> searchWithDetails(ProductionUnit productionUnit,
                                                                       LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                                                       LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                                                       int pageNum, int pageSize) {
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

        List<Long> parentIds = parents.stream().map(ProductionUnit::getProdUnitId).collect(Collectors.toList());

        QueryWrapper<ProdUnitInvoice> invoiceWrapper = new QueryWrapper<>();
        invoiceWrapper.in("prod_unit_id", parentIds);
        List<ProdUnitInvoice> allInvoices = prodUnitInvoiceMapper.selectList(invoiceWrapper);
        Map<Long, List<ProdUnitInvoice>> invoicesMap = allInvoices.stream()
                .collect(Collectors.groupingBy(ProdUnitInvoice::getProdUnitId));

        QueryWrapper<ProdUnitMaterialFile> materialWrapper = new QueryWrapper<>();
        materialWrapper.in("prod_unit_id", parentIds);
        List<ProdUnitMaterialFile> allMaterials = prodUnitMaterialFileMapper.selectList(materialWrapper);
        Map<Long, List<ProdUnitMaterialFile>> materialsMap = allMaterials.stream()
                .collect(Collectors.groupingBy(ProdUnitMaterialFile::getProdUnitId));

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

    //#endregion

    //#region 生产单位发票信息操作

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

    @Override
    public List<ProdUnitInvoice> getProdUnitInvoices(Long prodUnitId) {
        QueryWrapper<ProdUnitInvoice> wrapper = new QueryWrapper<>();
        wrapper.eq("prod_unit_id", prodUnitId);
        return prodUnitInvoiceMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public boolean deleteProdUnitInvoice(Long prodInvoiceId) {
        return prodUnitInvoiceMapper.deleteById(prodInvoiceId) > 0;
    }

    //#endregion

    //#region 生产单位材料文件操作

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

    @Override
    @Transactional
    public ProdUnitMaterialFile addProdUnitMaterialFile(Long prodUnitId, String materialType, MultipartFile file, String description) {
        try {
            String fileMd5 = fileStorageService.calculateMD5(file);
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

    @Override
    public InputStream getFileInputStream(String fileContent) {
        try {
            byte[] fileBytes = java.util.Base64.getDecoder().decode(fileContent);
            return new java.io.ByteArrayInputStream(fileBytes);
        } catch (Exception e) {
            throw new RuntimeException("解码文件内容失败", e);
        }
    }

    @Override
    public ProdUnitMaterialFile getProdUnitMaterialFileById(Long prodMaterialId) {
        return prodUnitMaterialFileMapper.selectById(prodMaterialId);
    }

    @Override
    public List<ProdUnitMaterialFile> getProdUnitMaterialFiles(Long prodUnitId) {
        QueryWrapper<ProdUnitMaterialFile> wrapper = new QueryWrapper<>();
        wrapper.eq("prod_unit_id", prodUnitId);
        return prodUnitMaterialFileMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public boolean deleteProdUnitMaterialFile(Long prodMaterialId) {
        return prodUnitMaterialFileMapper.deleteById(prodMaterialId) > 0;
    }

    //#endregion

}