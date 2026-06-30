package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.ProductionUnitWithDetailsDto;
import com.tonghui.erp.Data.Entity.ProdUnitInvoice;
import com.tonghui.erp.Data.Entity.ProdUnitMaterialFile;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Service.ProductionUnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产单位控制器
 */
@RestController
@RequestMapping("/api/production_unit")
public class ProductionUnitController extends BaseCrudController<ProductionUnit, ProductionUnit, Long> {

    @Autowired
    private ProductionUnitService productionUnitService;

    @Override
    protected PagedResult<ProductionUnit> getAllData(int pageIndex, int pageSize) {
        // 页码从0开始的处理，确保不为负数
        int safePageIndex = Math.max(0, pageIndex);
        // 当pageSize<=0时，设置一个合理的默认值
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        // 使用ProductionUnitService的queryProductionUnits方法进行查询
        ProductionUnit productionUnit = new ProductionUnit();
        Page<ProductionUnit> pageResult = productionUnitService.queryProductionUnits(productionUnit, safePageIndex, safePageSize);

        // 转换为PagedResult
        PagedResult<ProductionUnit> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    @Override
    protected ProductionUnit getDataById(Long id) {
        return productionUnitService.getProductionUnitById(id);
    }

    @Override
    protected ProductionUnit doCreate(ProductionUnit productionUnit) {
        productionUnitService.addProductionUnit(productionUnit);
        return productionUnit;
    }

    @Override
    protected ProductionUnit doUpdate(Long id, ProductionUnit productionUnit) {
        productionUnit.setProdUnitId(id);
        productionUnitService.updateProductionUnit(productionUnit);
        return productionUnit;
    }

    @Override
    protected boolean doDelete(Long id) {
        try {
            productionUnitService.deleteProductionUnit(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // #region 高级查询

    /**
     * 高级查询生产单位（支持多条件 + 分页）
     *
     * 可选查询条件：
     * - prodUnitCode：模糊匹配
     * - prodUnitName：模糊匹配
     * - prodUnitManager：模糊匹配
     * - prodUnitStatus：state过滤
     * - createdTimeStart：创建时间起始（大于等于）
     * - createdTimeEnd：创建时间结束（小于等于）
     * - updatedTimeStart：更新时间起始（大于等于）
     * - updatedTimeEnd：更新时间结束（小于等于）
     *
     * 示例请求：
     *GET /production-unit/search?pageIndex=1&pageSize=20&prodUnitName=原料药&prodUnitManager=张&prodUnitStatus=1&createdTimeStart=2023-01-01T00:00:00&createdTimeEnd=2023-12-31T23:59:59
     *
     * @param productionUnit 查询条件（自动从query参数映射）
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param pageIndex      页码
     * @param pageSize       每页大小
     * @return 分页结果
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<ProductionUnit>> queryProductionUnits(ProductionUnit productionUnit,
                                                                         @RequestParam(required = false) LocalDateTime createdTimeStart,
                                                                         @RequestParam(required = false) LocalDateTime createdTimeEnd,
                                                                         @RequestParam(required = false) LocalDateTime updatedTimeStart,
                                                                         @RequestParam(required = false) LocalDateTime updatedTimeEnd,
                                                                         @RequestParam int pageIndex,
                                                                         @RequestParam int pageSize) {
        try {
            // 页码从0开始的处理，确保不为负数
            int safePageIndex = Math.max(0, pageIndex);
            // 当pageSize<=0时，设置一个合理的默认值
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            // 获取分页结果，将时间参数传递给service方法
            Page<ProductionUnit> pageResult = productionUnitService.queryProductionUnits(productionUnit, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd, safePageIndex, safePageSize);

            // 转换为统一的pagedResult格式
            PagedResult<ProductionUnit> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(safePageIndex);
            pagedResult.setPageSize((int) pageResult.getSize());

            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "搜索生产单位");
        }
    }

    // #endregion

    // #region 带子表查询

    /**
     * 高级查询生产单位（包含发票和材料文件子表）
     *
     * @param productionUnit 查询条件
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果（包含子表）
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<ProductionUnitWithDetailsDto>> searchWithDetails(ProductionUnit productionUnit,
                                                                                     @RequestParam(required = false) LocalDateTime createdTimeStart,
                                                                                     @RequestParam(required = false) LocalDateTime createdTimeEnd,
                                                                                     @RequestParam(required = false) LocalDateTime updatedTimeStart,
                                                                                     @RequestParam(required = false) LocalDateTime updatedTimeEnd,
                                                                                     @RequestParam int pageIndex,
                                                                                     @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<ProductionUnitWithDetailsDto> result = productionUnitService.searchWithDetails(productionUnit, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd, safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    // #endregion

    // #region 发票信息操作接口

    /**
     * 为生产单位添加发票信息
     *
     * @param prodUnitId      生产单位ID
     * @param prodInvoiceInfo 发票信息
     * @return 操作结果
     */
    @PostMapping("/{id}/invoice")
    public ApiResponse<ProdUnitInvoice> addProdUnitInvoice(@PathVariable("id")Long prodUnitId, @RequestBody String prodInvoiceInfo) {
        try {
            ProdUnitInvoice invoice = productionUnitService.addProdUnitInvoice(prodUnitId, prodInvoiceInfo);
            return success(invoice, "添加发票信息成功");
        } catch (Exception e) {
            return exception(e, "添加发票信息失败");
        }
    }

    /**
     * 获取生产单位的发票信息列表
     *
     * @param prodUnitId 生产单位ID
     * @return 发票信息列表
     */
    @GetMapping("/{id}/invoices")
    public ApiResponse<List<ProdUnitInvoice>> getProdUnitInvoices(@PathVariable("id")Long prodUnitId) {
        try {
            List<ProdUnitInvoice> invoices = productionUnitService.getProdUnitInvoices(prodUnitId);
            return success(invoices);
        } catch (Exception e) {
            return exception(e, "获取发票信息失败");
        }
    }

    /**
     * 删除发票信息
     *
     * @param prodInvoiceId 发票信息ID
     * @return 操作结果
     */
    @DeleteMapping("/invoice/{invoiceId}")
    public ApiResponse<Boolean> deleteProdUnitInvoice(@PathVariable("invoiceId")Long prodInvoiceId) {
        try {
            boolean result = productionUnitService.deleteProdUnitInvoice(prodInvoiceId);
            return success(result, "删除发票信息成功");
        } catch (Exception e) {
            return exception(e, "删除发票信息失败");
        }
    }

    // #endregion

    // #region 材料文件操作接口

    /**
     * 为生产单位添加材料文件
     *
     * @param prodUnitId    生产单位ID
     * @param materialType  材料类型
     * @param fileName      文件名
     * @param fileMd5       文件MD5
     * @param fileSize      文件大小
     * @param description   描述
     * @param fileContent   文件内容(base64)
     * @return 操作结果
     */
    @PostMapping("/{id}/material-file")
    public ApiResponse<ProdUnitMaterialFile> addProdUnitMaterialFile(
            @PathVariable("id") Long prodUnitId,
            @RequestParam String materialType,
            @RequestParam String fileName,
            @RequestParam String fileMd5,
            @RequestParam Long fileSize,
            @RequestParam(required = false) String description,
            @RequestBody String fileContent) {
        try {
            ProdUnitMaterialFile materialFile = new ProdUnitMaterialFile();
            materialFile.setProdUnitId(prodUnitId);
            materialFile.setMaterialType(materialType);
            materialFile.setFileName(fileName);
            materialFile.setFileMd5(fileMd5);
            materialFile.setFileSize(fileSize.intValue());
            materialFile.setFileContent(fileContent);
            if (description != null) {
                materialFile.setDescription(description);
            }
            
            ProdUnitMaterialFile result = productionUnitService.addProdUnitMaterialFile(materialFile);
            return success(result, "添加材料文件成功");
        } catch (Exception e) {
            return exception(e, "添加材料文件失败");
        }
    }

    /**
     * 获取生产单位的材料文件列表
     *
     * @param prodUnitId 生产单位ID
     * @return 材料文件列表
     */
    @GetMapping("/{id}/material-files")
    public ApiResponse<List<ProdUnitMaterialFile>> getProdUnitMaterialFiles(@PathVariable("id")Long prodUnitId) {
        try {
            List<ProdUnitMaterialFile> files = productionUnitService.getProdUnitMaterialFiles(prodUnitId);
            return success(files);
        } catch (Exception e) {
            return exception(e, "获取材料文件失败");
        }
    }

    /**
     * 下载材料文件
     *
     * @param prodMaterialId 材料文件ID
     * @return 文件流
     */
    @GetMapping("/material-file/{materialId}")
    public ResponseEntity<Resource> downloadProdUnitMaterialFile(@PathVariable("materialId")Long prodMaterialId) {
        try {
            // 获取文件元数据
            ProdUnitMaterialFile materialFile = productionUnitService.getProdUnitMaterialFileById(prodMaterialId);
            if (materialFile == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 获取文件输入流
            InputStream inputStream = productionUnitService.getFileInputStream(materialFile.getFileContent());
            if (inputStream == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 创建资源
            InputStreamResource resource = new InputStreamResource(inputStream);
            
            // 构建响应头
            String contentType = "application/octet-stream";
            String headerValue = "attachment;filename=\"" + materialFile.getFileName() + "\"";
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除材料文件
     *
     * @param prodMaterialId 材料文件ID
     * @return 操作结果
     */
    @DeleteMapping("/material-file/{materialId}")
    public ApiResponse<Boolean> deleteProdUnitMaterialFile(@PathVariable("materialId")Long prodMaterialId) {
        try {
            boolean result = productionUnitService.deleteProdUnitMaterialFile(prodMaterialId);
            return success(result, "删除材料文件成功");
        } catch (Exception e) {
            return exception(e, "删除材料文件失败");
        }
    }

    // #endregion

    // #region 特殊业务方法

    /**
     * 根据编码查询生产单位
     *
     * @param prodUnitCode 生产单位编码
     * @return 生产单位实体
     */
    @GetMapping("/code/{prodUnitCode}")
    public ApiResponse<ProductionUnit> getProductionUnitByCode(@PathVariable String prodUnitCode) {
        try {
            ProductionUnit unit = productionUnitService.getProductionUnitByCode(prodUnitCode);
            if (unit == null) {
                return error("未找到对应生产单位");
            }
            return success(unit);
        } catch (Exception e) {
            return exception(e, "查询生产单位失败");
        }
    }

    /**
     * 查询所有启用状态的生产单位
     *
     * @return 生产单位集合
     */
    @GetMapping("/enabled")
    public ApiResponse<PagedResult<ProductionUnit>>getEnabledProductionUnits() {
        try {
            // 这里使用分页查询，但返回所有启用status的生产单位
            ProductionUnit productionUnit = new ProductionUnit();
            productionUnit.setProdUnitStatus(1); // 启用status

            Page<ProductionUnit> pageResult = productionUnitService.queryProductionUnits(productionUnit, 0,Integer.MAX_VALUE);

            PagedResult<ProductionUnit> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(0);
            pagedResult.setPageSize(pageResult.getRecords().size());

            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "查询启用生产单位失败");
        }
    }

    /**
     * 启用/停用生产单位
     *
     * @param id     生产单位ID
     * @param status 状态：1启用，0停用
     * @return 操作结果
     */
    @PostMapping("/{id}/status/{status}")
    public ApiResponse<Boolean>toggleProductionUnitStatus(@PathVariable Long id, @PathVariable Integer status) {
        try {
            ProductionUnit productionUnit = productionUnitService.getProductionUnitById(id);
            if (productionUnit != null) {
                productionUnit.setProdUnitStatus(status);
                productionUnitService.updateProductionUnit(productionUnit);
                return success(true, "更新状态成功");
            }
            return error("未找到对应生产单位");
        } catch (Exception e) {
            return exception(e, "更新状态失败");
        }
    }

    // #endregion
}