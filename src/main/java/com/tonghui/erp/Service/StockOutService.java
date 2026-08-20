package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.BatchOutboundRequest;
import com.tonghui.erp.Common.Dto.Stock.PlanDetailItemDto;
import com.tonghui.erp.Common.Dto.Stock.StockOutWithDetailsDto;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 出库单业务接口
 */
public interface StockOutService extends IService<StockOut> {

    // region 基础操作
    // ===================================
    // 基础操作
    // ===================================

    /**
     * 新增出库单（包含明细）
     *
     * @param stockOut 出库单实体
     * @param details 出库明细列表
     */
    void addStockOut(StockOut stockOut, List<StockOutDetail> details);

    /**
     * 更新出库单（包含明细）
     *
     * @param stockOut 出库单实体
     * @param details 出库明细列表
     */
    void updateStockOut(StockOut stockOut, List<StockOutDetail> details);

    /**
     * 部分更新出库单（只更新非 null 字段）
     *
     * @param stockOut 出库单实体
     */
    void partialUpdateStockOut(StockOut stockOut);

    /**
     * 删除出库单（同时删除明细）
     *
     * @param stockOutId 出库单 ID
     */
    void deleteStockOut(Long stockOutId);

    /**
     * 确认出库：草稿 → 已出库
     * <p>校验出库单为草稿状态且有明细，随后联动库存表（扣减库存批次）并写入库存流水</p>
     *
     * @param stockOutId 出库单 ID
     */
    void confirmStockOut(Long stockOutId);

    /**
     * 取消出库：已出库 → 已取消
     * <p>校验出库单为已出库状态，随后回滚库存（恢复对应库存批次）并写入调整流水</p>
     *
     * @param stockOutId 出库单 ID
     */
    void cancelStockOut(Long stockOutId);

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据ID查询出库单
     *
     * @param stockOutId 出库单ID
     * @return 出库单实体
     */
    StockOut getStockOutById(Long stockOutId);

    /**
     * 根据出库单号查询出库单
     *
     * @param stockOutCode 出库单号
     * @return 出库单实体
     */
    StockOut getStockOutByCode(String stockOutCode);

    /**
     * 查询所有出库单
     *
     * @return 出库单集合
     */
    List<StockOut> getAllStockOuts();

    // endregion

    // region 出库单明细操作
    // ===================================
    // 出库单明细操作
    // ===================================

    /**
     * 根据出库单ID获取明细列表
     *
     * @param stockOutId 出库单ID
     * @return 明细列表
     */
    List<StockOutDetail> getStockOutDetailsByStockOutId(Long stockOutId);

    /**
     * 添加出库明细
     *
     * @param detail 出库明细
     */
    void addStockOutDetail(StockOutDetail detail);

    /**
     * 批量添加出库明细
     *
     * @param details 出库明细列表
     */
    void addStockOutDetails(List<StockOutDetail> details);

    /**
     * 更新出库明细
     *
     * @param detail 出库明细
     */
    void updateStockOutDetail(StockOutDetail detail);

    /**
     * 删除出库明细
     *
     * @param detailId 明细ID
     */
    void deleteStockOutDetail(Long detailId);

    // endregion

    // region 单号生成
    // ===================================
    // 单号生成
    // ===================================

    /**
     * 生成出库单号
     *
     * @return 出库单号
     */
    String generateStockOutCode();

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询出库单（支持分页）
     *
     * @param stockOut  查询条件
     * @param keyword   关键字（模糊匹配出库单号、生产计划编号、生产计划名称，可空）
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    Page<StockOut> queryStockOuts(StockOut stockOut, String keyword, LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd, LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd, LocalDate startDate, LocalDate endDate, int pageNum, int pageSize);

    // endregion

    // region 带子表查询
    // ===================================
    // 带子表查询
    // ===================================

    /**
     * 高级查询出库单（包含明细子表）
     *
     * @param stockOut  查询条件
     * @param keyword   关键字（模糊匹配出库单号、生产计划编号、生产计划名称，可空）
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @return 分页结果（包含明细）
     */
    PagedResult<StockOutWithDetailsDto> searchWithDetails(StockOut stockOut, String keyword, LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd, LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd, LocalDate startDate, LocalDate endDate, int pageNum, int pageSize);

    // endregion

    // region 批量出库（按制剂处方）
    // ===================================
    // 批量出库（按制剂处方）
    // ===================================

    /**
     * 按生产计划获取批量出库处方明细
     * <p>
     * 根据生产计划关联的制剂，取制剂处方明细，计算每个物料应出数量（处方量×生产倍数），
     * 并匹配合格的可用库存批次（FIFO排序）
     * </p>
     *
     * @param planCode   生产计划编号
     * @param multiplier 生产倍数（可为空，默认1倍）
     * @return 处方明细列表（含可用库存批次）
     */
    List<PlanDetailItemDto> getPlanDetail(String planCode, BigDecimal multiplier);

    /**
     * 根据制剂ID获取批量出库处方明细
     * <p>
     * 按制剂ID取制剂处方明细，计算每个物料应出数量（处方量×生产倍数），
     * 并匹配合格的可用库存批次（FIFO排序），批次中携带单价与金额（应出数量×单价）
     * </p>
     *
     * @param preparationId 制剂ID（必填）
     * @param multiplier    生产倍数（可为空，默认1倍）
     * @return 处方明细列表（含序号、可用库存批次、单价、金额、库存状态）
     */
    List<PlanDetailItemDto> getPreparationDetail(Long preparationId, BigDecimal multiplier);

    /**
     * 批量出库确认：一次事务内创建出库单并确认生效
     * <p>
     * 创建出库单（自动生成单号）及明细，随后扣减对应库存批次并写入库存流水，
     * 库存不足时整体回滚
     * </p>
     *
     * @param request 批量出库请求（出库类型、关联单号、仓库、明细列表）
     * @return 已确认的出库单
     */
    StockOut batchConfirm(BatchOutboundRequest request);

    // endregion
}
