package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.RoomInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;

import java.util.List;

/**
 * @author 87954
 * @description 针对表【room_info(房间表，用于记录设备所在房间的信息。)】的数据库操作 Service
 * @createDate 2026-03-09 10:03:26
 */
public interface RoomInfoService extends IService<RoomInfo> {
    
    /**
     * 根据房间名称模糊查询（分页）
     * 
     * @param roomName 房间名称（模糊匹配），为空时查询所有
     * @param pageRequest 分页参数，包含页码和每页数量等信息
     * @return 分页结果，包含查询到的房间列表和分页信息
     */
    PagedResult<RoomInfo> searchByName(String roomName, PageRequestDto pageRequest);
    
    /**
     * 根据房间位置模糊查询（分页）
     * 
     * @param location 房间位置（模糊匹配）
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    PagedResult<RoomInfo> searchByLocation(String location, PageRequestDto pageRequest);
    
    /**
     * 获取所有启用的房间
     * 
     * @return 启用的房间列表
     */
    List<RoomInfo> listActive();
    
    /**
     * 根据房间名称精确查询
     * 
     * @param roomName 房间名称
     * @return 查询到的房间，不存在则返回 null
     */
    RoomInfo getByName(String roomName);
}
