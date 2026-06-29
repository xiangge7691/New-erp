package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Service.EquipmentService;
import com.tonghui.erp.Data.mapper.EquipmentMapper;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Service.RoomInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备信息服务实现类
 * <p>
 * 针对表【equipment(设备表，记录设备详细信息。)】的数据库操作 Service 实现，提供设备的增删改查等业务逻辑的具体实现
 * </p>
 */
@Service
public class EquipmentServiceImpl extends ServiceImpl<EquipmentMapper, Equipment>
    implements EquipmentService{

    @Autowired
    private RoomInfoService roomInfoService;

    //#region 设备信息查询实现方法
    // ===================================
    // 设备信息查询实现方法
    // ===================================

    /**
     * 根据设备名称模糊查询（分页）
     *
     * @param equipmentName 设备名称（模糊匹配），为空时查询所有
     * @param pageRequest 分页参数，包含页码和每页数量等信息
     * @return 分页结果，包含查询到的设备列表和分页信息
     */
    @Override
   public PagedResult<Equipment> searchByName(String equipmentName, PageRequestDto pageRequest) {
        // 创建 Page 对象，处理全量数据的情况
        Page<Equipment> page;
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            // 获取所有数据
            page = new Page<>(1, 10000);
        } else {
            // 页码从 0 开始，但 MyBatis Plus 的 Page 页码从 1 开始，所以需要 +1
            page = new Page<>(pageRequest.getPageIndex() + 1, pageRequest.getPageSize());
        }

        // 构建查询条件
        var query = this.lambdaQuery();

        // 如果 equipmentName 不为空，则添加模糊查询条件
        if (equipmentName != null && !equipmentName.isEmpty()) {
            query.like(Equipment::getEquipmentName, equipmentName);
        }

        Page<Equipment> resultPage = query.page(page);

        PagedResult<Equipment> pagedResult = new PagedResult<>();
       pagedResult.setItems(resultPage.getRecords());
        pagedResult.setTotalCount(resultPage.getTotal());

        // 处理分页信息
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            // 全量数据情况
           pagedResult.setPageIndex(0);
            if (resultPage.getTotal() > 0) {
                pagedResult.setPageSize((int) resultPage.getTotal());
            } else {
                pagedResult.setPageSize(0);
            }
        } else {
            // 分页情况，页码从 0 开始
            pagedResult.setPageIndex((int) resultPage.getCurrent() - 1);
           pagedResult.setPageSize((int) resultPage.getSize());
        }

        fillRoomNames(pagedResult.getItems());

        return pagedResult;
    }

    /**
     * 根据固定资产编号精确查询
     *
     * @param fixedAssetCode 固定资产编号
     * @return 查询到的设备，不存在则返回 null
     */
    @Override
    public Equipment getByFixedAssetCode(String fixedAssetCode) {
        if (fixedAssetCode == null || fixedAssetCode.isEmpty()) {
            return null;
        }
        
        return this.lambdaQuery()
                .eq(Equipment::getFixedAssetCode, fixedAssetCode)
                .one();
    }

    /**
     * 根据房间 ID 查询设备列表
     *
     * @param roomId 房间 ID
     * @return 设备列表
     */
    @Override
    public List<Equipment> listByRoomId(Integer roomId) {
        if (roomId == null) {
            return List.of();
        }
        
        return this.lambdaQuery()
                .eq(Equipment::getRoomId, roomId)
                .eq(Equipment::getEquipmentStatus, 1)
                .orderByAsc(Equipment::getEquipmentName)
                .list();
    }

    /**
     * 根据生产厂家模糊查询（分页）
     *
     * @param manufacturer 生产厂家（模糊匹配）
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    @Override
   public PagedResult<Equipment> searchByManufacturer(String manufacturer, PageRequestDto pageRequest) {
        // 创建 Page 对象
        Page<Equipment> page;
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            page = new Page<>(1, 10000);
        } else {
            page = new Page<>(pageRequest.getPageIndex() + 1, pageRequest.getPageSize());
        }

        // 构建查询条件
        var query = this.lambdaQuery();
        
        if (manufacturer != null && !manufacturer.isEmpty()) {
            query.like(Equipment::getManufacturer, manufacturer);
        }

        Page<Equipment> resultPage = query.page(page);

        PagedResult<Equipment> pagedResult = new PagedResult<>();
        pagedResult.setItems(resultPage.getRecords());
        pagedResult.setTotalCount(resultPage.getTotal());

        // 处理分页信息
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            pagedResult.setPageIndex(0);
            if (resultPage.getTotal() > 0) {
                pagedResult.setPageSize((int) resultPage.getTotal());
            } else {
               pagedResult.setPageSize(0);
            }
        } else {
            pagedResult.setPageIndex((int) resultPage.getCurrent() - 1);
           pagedResult.setPageSize((int) resultPage.getSize());
        }

        fillRoomNames(pagedResult.getItems());

        return pagedResult;
    }

    /**
     * 获取所有启用的设备
     *
     * @return 启用的设备列表
     */
    @Override
    public List<Equipment> listActive() {
        return this.lambdaQuery()
                .eq(Equipment::getEquipmentStatus, 1)
                .orderByAsc(Equipment::getEquipmentName)
                .list();
    }

    /**
     * 更新设备的最后维护日期
     *
     * @param equipmentId 设备 ID
     * @param maintenanceDate 维护日期
     * @param updaterId 更新人 ID
     * @return 操作是否成功
     */
    @Override
   public boolean updateMaintenanceDate(Integer equipmentId, LocalDate maintenanceDate, Long updaterId) {
        if (equipmentId == null) {
            return false;
        }
        
        Equipment equipment = this.getById(equipmentId);
        if (equipment == null) {
            return false;
        }
        
        equipment.setLastMaintenanceDate(maintenanceDate);
        equipment.setUpdaterId(updaterId);
        equipment.setUpdatedTime(LocalDateTime.now());
        
        return this.updateById(equipment);
    }

    /**
     * 为设备列表填充房间名称
     *
     * @param equipments 设备列表
     */
    private void fillRoomNames(List<Equipment> equipments) {
        if (equipments == null || equipments.isEmpty()) {
            return;
        }

        for (Equipment equipment : equipments) {
            if (equipment.getRoomId() != null) {
                RoomInfo roomInfo = roomInfoService.getById(equipment.getRoomId());
                if (roomInfo != null) {
                    equipment.setRoomName(roomInfo.getRoomName());
                }
            }
        }
    }

    /**
     * 高级查询设备（支持多条件 + 分页）
     *
     * @param equipment 查询条件实体
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @Override
  public Page<Equipment> queryEquipments(
            Equipment equipment,
            LocalDateTime createdTimeStart,
            LocalDateTime createdTimeEnd,
            LocalDateTime updatedTimeStart,
            LocalDateTime updatedTimeEnd,
            int pageNum,
            int pageSize) {
        
        int actualPageIndex= pageNum + 1;
        Page<Equipment> page = new Page<>(actualPageIndex, pageSize);
        QueryWrapper<Equipment> wrapper = new QueryWrapper<>();

        if (equipment.getEquipmentId() != null) {
            wrapper.eq("equipment_id", equipment.getEquipmentId());
        }
        if (StringUtils.hasText(equipment.getEquipmentName())) {
            wrapper.like("equipment_name", equipment.getEquipmentName());
        }
        if (StringUtils.hasText(equipment.getEquipmentModel())) {
            wrapper.like("equipment_model", equipment.getEquipmentModel());
        }
        if (equipment.getRoomId() != null) {
            wrapper.eq("room_id", equipment.getRoomId());
        }
        if (StringUtils.hasText(equipment.getProductionCapacity())) {
            wrapper.like("production_capacity", equipment.getProductionCapacity());
        }
        if (equipment.getEquipmentStatus() != null) {
            wrapper.eq("equipment_status", equipment.getEquipmentStatus());
        }
        if (StringUtils.hasText(equipment.getFixedAssetCode())) {
            wrapper.eq("fixed_asset_code", equipment.getFixedAssetCode());
        }
        if (StringUtils.hasText(equipment.getManufacturer())) {
            wrapper.like("manufacturer", equipment.getManufacturer());
        }
        if (equipment.getPurchaseDate() != null) {
            wrapper.eq("purchase_date", equipment.getPurchaseDate());
        }
        if (equipment.getPurchaseAmount() != null) {
            wrapper.eq("purchase_amount", equipment.getPurchaseAmount());
        }
        if (equipment.getLastMaintenanceDate() != null) {
            wrapper.eq("last_maintenance_date", equipment.getLastMaintenanceDate());
        }
        if (StringUtils.hasText(equipment.getRemark())) {
            wrapper.like("remark", equipment.getRemark());
        }
        
        if (createdTimeStart != null) {
            wrapper.ge("created_time", createdTimeStart);
        }
        if (createdTimeEnd != null) {
            wrapper.le("created_time", createdTimeEnd);
        }
        if (updatedTimeStart != null) {
            wrapper.ge("updated_time", updatedTimeStart);
        }
        if (updatedTimeEnd != null) {
            wrapper.le("updated_time", updatedTimeEnd);
        }

        Page<Equipment> resultPage = baseMapper.selectPage(page, wrapper);
        
        fillRoomNames(resultPage.getRecords());

        return resultPage;
    }
}




