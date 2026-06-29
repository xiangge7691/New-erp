package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.Equipment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;

import java.util.List;

/**
 * @author 87954
 * @description 针对表【equipment(设备表，记录设备详细信息。)】的数据库操作 Service
 * @createDate 2026-03-09 10:03:26
 */
public interface EquipmentService extends IService<Equipment> {
    
    /**
     * 根据设备名称模糊查询（分页）
     * 
     * @param equipmentName 设备名称（模糊匹配），为空时查询所有
     * @param pageRequest 分页参数，包含页码和每页数量等信息
     * @return 分页结果，包含查询到的设备列表和分页信息
     */
    PagedResult<Equipment> searchByName(String equipmentName, PageRequestDto pageRequest);
    
    /**
     * 高级查询设备（支持多条件 + 分页）
     * 
     * @param equipment 查询条件实体（自动从 query 参数映射）
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<Equipment> queryEquipments(
            Equipment equipment,
            java.time.LocalDateTime createdTimeStart,
            java.time.LocalDateTime createdTimeEnd,
            java.time.LocalDateTime updatedTimeStart,
            java.time.LocalDateTime updatedTimeEnd,
            int pageNum,
            int pageSize);
    
    /**
     * 根据固定资产编号精确查询
     * 
     * @param fixedAssetCode 固定资产编号
     * @return 查询到的设备，不存在则返回 null
     */
    Equipment getByFixedAssetCode(String fixedAssetCode);
    
    /**
     * 根据房间 ID 查询设备列表
     * 
     * @param roomId 房间 ID
     * @return 设备列表
     */
    List<Equipment> listByRoomId(Integer roomId);
    
    /**
     * 根据生产厂家模糊查询（分页）
     * 
     * @param manufacturer 生产厂家（模糊匹配）
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    PagedResult<Equipment> searchByManufacturer(String manufacturer, PageRequestDto pageRequest);
    
    /**
     * 获取所有启用的设备
     * 
     * @return 启用的设备列表
     */
    List<Equipment> listActive();
    
    /**
     * 更新设备的最后维护日期
     * 
     * @param equipmentId 设备 ID
     * @param maintenanceDate 维护日期
     * @param updaterId 更新人 ID
     * @return 操作是否成功
     */
    boolean updateMaintenanceDate(Integer equipmentId, java.time.LocalDate maintenanceDate, Long updaterId);
}
