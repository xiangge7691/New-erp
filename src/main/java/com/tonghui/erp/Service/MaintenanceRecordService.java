package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenancePendingDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenancePendingSummaryDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceRecordListItemDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceRecordSaveDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceRecordWithDetailsDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceStockItemDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.MaintenanceRecord;

import java.io.OutputStream;
import java.util.List;

/**
 * 养护记录服务接口
 * <p>
 * 提供库存养护记录的CRUD、条件分页查询、编号生成、库存明细选择、
 * 待养护查询与统计、台账导出等业务能力。
 * 养护记录关联库存明细（stock），登记本身不产生库存增减
 * </p>
 */
public interface MaintenanceRecordService extends IService<MaintenanceRecord> {

    /**
     * 生成养护记录编号
     * <p>
     * 格式YH-YYYYMMDD-NNN，按实际检查日期确定日期段，基于当天最大序号递增，
     * 查询绕过软删除过滤
     * </p>
     *
     * @param checkDate 实际检查日期（yyyy-MM-dd）
     * @return 生成的唯一养护记录编号
     */
    String generateCode(String checkDate);

    /**
     * 条件分页查询养护记录列表
     * <p>
     * 明细维度筛选（物料编码/名称、批号、入库编号、是否异常）先查明细得到主表ID集合再过滤
     * </p>
     *
     * @param keyword    关键字（可空，模糊匹配养护记录编号）
     * @param warehouseId 仓库ID（可空，精确匹配）
     * @param material   物料关键字（可空，模糊匹配明细物料编码/名称）
     * @param batchNumber 批号（可空，模糊匹配明细批号）
     * @param inCode     入库编号（可空，模糊匹配明细入库编号）
     * @param checkType  检查类型（可空，精确匹配）
     * @param checkerName 检查人员姓名（可空，模糊匹配）
     * @param hasAbnormal 是否发现异常（可空，true仅返回有异常明细的记录）
     * @param startDate  检查日期起始（可空，格式yyyy-MM-dd，含边界）
     * @param endDate    检查日期结束（可空，格式yyyy-MM-dd，含边界）
     * @param pageRequest 分页参数
     * @return 分页结果（列表项含明细数、异常明细数、创建人姓名）
     */
    PagedResult<MaintenanceRecordListItemDto> search(String keyword, Long warehouseId, String material,
                                                     String batchNumber, String inCode, String checkType,
                                                     String checkerName, Boolean hasAbnormal,
                                                     String startDate, String endDate,
                                                     PageRequestDto pageRequest);

    /**
     * 查询养护记录详情（含全部库存检查明细、明细附件、主单附件与创建人/更新人姓名）
     *
     * @param id 养护记录ID
     * @return 养护记录详情，不存在时返回null
     */
    MaintenanceRecordWithDetailsDto getDetail(Long id);

    /**
     * 新增养护记录
     * <p>
     * 自动生成编号（唯一冲突时重试）、快照库存明细与物料养护信息、
     * 明细落库并计算下次养护日期、绑定主单与明细附件
     * </p>
     *
     * @param dto 保存请求体
     * @return 新增的养护记录
     */
    MaintenanceRecord create(MaintenanceRecordSaveDto dto);

    /**
     * 修改养护记录（编号不可改；明细按ID原地更新保留ID、被移除行删除并重算下次养护日期；同步主单附件）
     *
     * @param id  养护记录ID
     * @param dto 保存请求体（明细已有行须回传原 id）
     * @return 修改后的养护记录
     */
    MaintenanceRecord update(Long id, MaintenanceRecordSaveDto dto);

    /**
     * 查询养护记录主单附件列表
     *
     * @param id 养护记录ID
     * @return 附件文件列表
     */
    List<FileInfo> getAttachments(Long id);

    /**
     * 删除养护记录（主单软删除，明细物理删除并解绑明细附件）
     * <p>
     * 明细必须物理删除：否则残留明细会继续出现在待养护提醒中，
     * 且再次保存同库存明细的历史检查记录不应被引用
     * </p>
     *
     * @param id 养护记录ID
     */
    void delete(Long id);

    /**
     * 查询指定仓库的库存明细（新增养护记录时「选择库存物料」用）
     * <p>
     * 按仓库名解析仓库ID，查询库存明细（默认仅quantity>0），
     * 带出入库编号、物料养护注意事项、养护周期、是否重点养护物料
     * </p>
     *
     * @param warehouse 仓库名称（必填）
     * @param keyword   关键字（可空，模糊匹配物料编码/名称/批号/入库编号）
     * @param showZero  是否显示零库存（默认false）
     * @return 库存选择项列表
     */
    List<MaintenanceStockItemDto> getStockDetails(String warehouse, String keyword, Boolean showZero);

    /**
     * 分页查询待养护库存明细
     * <p>
     * 按明细的下次养护日期计算：已逾期（nextDate < 今天）优先，
     * 再按下次养护日期升序；每条待检查的库存明细独立计算
     * </p>
     *
     * @param keyword    关键字（可空，模糊匹配物料编码/名称/批号/入库编号）
     * @param warehouseId 仓库ID（可空，精确匹配明细所在仓库）
     * @param pendingStatus 提醒状态（可空：OVERDUE已逾期/TODAY今日到期/SOON即将到期）
     * @param pageIndex  页码（从0开始）
     * @param pageSize   每页大小
     * @return 分页结果
     */
    PagedResult<MaintenancePendingDto> searchPending(String keyword, Long warehouseId,
                                                      String pendingStatus, int pageIndex, int pageSize);

    /**
     * 查询待养护统计（首页展示）
     *
     * @return 已逾期数量、今日到期数量、即将到期数量与提前提醒天数
     */
    MaintenancePendingSummaryDto getPendingSummary();

    /**
     * 按列表筛选条件导出养护台账Excel
     *
     * @param keyword    关键字（可空，同列表查询）
     * @param warehouseId 仓库ID（可空）
     * @param material   物料关键字（可空）
     * @param batchNumber 批号（可空）
     * @param inCode     入库编号（可空）
     * @param checkType  检查类型（可空）
     * @param checkerName 检查人员姓名（可空）
     * @param hasAbnormal 是否发现异常（可空）
     * @param startDate  检查日期起始（可空）
     * @param endDate    检查日期结束（可空）
     * @param out        Excel输出流
     */
    void exportToExcel(String keyword, Long warehouseId, String material, String batchNumber,
                       String inCode, String checkType, String checkerName, Boolean hasAbnormal,
                       String startDate, String endDate, OutputStream out);
}
