package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.ProductionUnitWithDetailsDto;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.ProdUnitInvoice;
import com.tonghui.erp.Data.Entity.ProdUnitMaterialFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产单位信息业务接口
 */
public interface ProductionUnitService extends IService<ProductionUnit> {

    // #region 基础操作

    /**
     * 新增生产单位
     *
     * @param productionUnit 生产单位实体
     * @return 是否成功
     */
    boolean addProductionUnit(ProductionUnit productionUnit);

    /**
     * 更新生产单位
     *
     * @param productionUnit 生产单位实体
     * @return 是否成功
     */
    boolean updateProductionUnit(ProductionUnit productionUnit);

    /**
     * 删除生产单位
     *
     * @param prodUnitId 生产单位ID
     * @return 是否成功
     */
    boolean deleteProductionUnit(Long prodUnitId);

    // #endregion

    // #region 查询操作

    /**
     * 根据ID查询生产单位
     *
     * @param prodUnitId 生产单位ID
     * @return 生产单位实体
     */
    ProductionUnit getProductionUnitById(Long prodUnitId);

    /**
     * 根据编码查询生产单位
     *
     * @param prodUnitCode 生产单位编码
     * @return 生产单位实体
     */
    ProductionUnit getProductionUnitByCode(String prodUnitCode);

    /**
     * 查询所有启用状态的生产单位
     *
     * @return 生产单位集合
     */
    List<ProductionUnit> getEnabledProductionUnits();

    /**
     * 查询所有生产单位
     *
     * @return 生产单位集合
     */
    List<ProductionUnit> getAllProductionUnits();

    /**
     * 获取生产单位列表（分页）
     *
     * @param pageRequestDto 分页请求参数
     * @return 分页结果
     */
    PagedResult<ProductionUnit> getProductionUnitList(PageRequestDto pageRequestDto);

    // #endregion

    // #region 高级查询

    /**
     * 高级查询生产单位（支持分页）
     *
     * @param productionUnit 查询条件
     * @param pageNum        页码
     * @param pageSize       每页大小
     * @return 分页结果
     */
    Page<ProductionUnit> queryProductionUnits(ProductionUnit productionUnit, int pageNum, int pageSize);

    /**
     * 高级查询生产单位（支持分页和时间范围查询）
     *
     * @param productionUnit 查询条件
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd   创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd   更新时间结束
     * @param pageNum        页码
     * @param pageSize       每页大小
     * @return 分页结果
     */
    Page<ProductionUnit> queryProductionUnits(ProductionUnit productionUnit, 
                                               LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                               LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                               int pageNum, int pageSize);

    // #endregion

    // #region 带子表查询

    /**
     * 高级查询生产单位（包含发票和材料文件子表）
     *
     * @param productionUnit 查询条件
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd   创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd   更新时间结束
     * @param pageNum        页码
     * @param pageSize       每页大小
     * @return 分页结果（包含子表）
     */
    PagedResult<ProductionUnitWithDetailsDto> searchWithDetails(ProductionUnit productionUnit,
                                                                 LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                                                 LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                                                 int pageNum, int pageSize);

    // #endregion

    // #region 生产单位发票信息操作

    /**
     * 添加生产单位发票信息
     *
     * @param prodUnitId       生产单位ID
     * @param prodInvoiceInfo  发票信息内容
     * @return 是否成功
     */
    boolean addProdUnitInvoice(Long prodUnitId, String prodInvoiceInfo);

    /**
     * 获取生产单位的发票信息列表
     *
     * @param prodUnitId 生产单位ID
     * @return 发票信息列表
     */
    List<ProdUnitInvoice> getProdUnitInvoices(Long prodUnitId);

    /**
     * 删除生产单位发票信息
     *
     * @param prodInvoiceId 发票信息ID
     * @return 是否成功
     */
    boolean deleteProdUnitInvoice(Long prodInvoiceId);

    // #endregion

    // #region 生产单位材料文件操作

    /**
     * 添加生产单位材料文件
     *
     * @param prodUnitId    生产单位ID
     * @param materialType 材料类型
     * @param fileName      文件名称
     * @param fileMd5       文件MD5
     * @param fileSize      文件大小
     * @param description   文件描述
     * @return 是否成功
     */
    boolean addProdUnitMaterialFile(Long prodUnitId, String materialType, String fileName, String fileMd5, Long fileSize, String description);

    /**
     * 添加生产单位材料文件（直接传入实体）
     *
     * @param materialFile 材料文件实体
     * @return 是否成功
     */
    boolean addProdUnitMaterialFile(ProdUnitMaterialFile materialFile);

    /**
     * 添加生产单位材料文件（使用MultipartFile）
     *
     * @param prodUnitId    生产单位ID
     * @param materialType 材料类型
     * @param file         文件
     * @param description   文件描述
     * @return 是否成功
     */
    boolean addProdUnitMaterialFile(Long prodUnitId, String materialType, MultipartFile file, String description);

    /**
     * 获取生产单位的材料文件列表
     *
     * @param prodUnitId 生产单位ID
     * @return 材料文件列表
     */
    List<ProdUnitMaterialFile> getProdUnitMaterialFiles(Long prodUnitId);

    /**
     * 根据ID获取材料文件
     *
     * @param prodMaterialId 材料文件ID
     * @return 材料文件实体
     */
    ProdUnitMaterialFile getProdUnitMaterialFileById(Long prodMaterialId);

    /**
     * 根据文件内容(Base64)获取文件输入流
     *
     * @param fileContent Base64编码的文件内容
     * @return 文件输入流
     */
    InputStream getFileInputStream(String fileContent);

    /**
     * 删除生产单位材料文件
     *
     * @param prodMaterialId 材料文件ID
     * @return 是否成功
     */
    boolean deleteProdUnitMaterialFile(Long prodMaterialId);

    // #endregion
}