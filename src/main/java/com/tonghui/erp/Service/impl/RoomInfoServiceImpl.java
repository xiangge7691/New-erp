package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Service.RoomInfoService;
import com.tonghui.erp.Data.mapper.RoomInfoMapper;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 房间信息服务实现类
 * <p>
 * 针对表【room_info(房间表，用于记录设备所在房间的信息。)】的数据库操作 Service 实现，提供房间的增删改查等业务逻辑的具体实现
 * </p>
 */
@Service
public class RoomInfoServiceImpl extends ServiceImpl<RoomInfoMapper, RoomInfo>
    implements RoomInfoService{

    //#region 房间信息查询实现方法
    // ===================================
    // 房间信息查询实现方法
    // ===================================

    /**
     * 根据房间名称模糊查询（分页）
     *
     * @param roomName 房间名称（模糊匹配），为空时查询所有
     * @param pageRequest 分页参数，包含页码和每页数量等信息
     * @return 分页结果，包含查询到的房间列表和分页信息
     */
    @Override
    public PagedResult<RoomInfo> searchByName(String roomName, PageRequestDto pageRequest) {
        // 创建 Page 对象，处理全量数据的情况
        Page<RoomInfo> page;
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            // 获取所有数据
            page = new Page<>(1, 10000);
        } else {
            // 页码从 0 开始，但 MyBatis Plus 的 Page 页码从 1 开始，所以需要 +1
            page = new Page<>(pageRequest.getPageIndex() + 1, pageRequest.getPageSize());
        }

        // 构建查询条件
        var query = this.lambdaQuery();

        // 如果 roomName 不为空，则添加模糊查询条件
        if (roomName != null && !roomName.isEmpty()) {
            query.like(RoomInfo::getRoomName, roomName);
        }

        Page<RoomInfo> resultPage = query.page(page);

        PagedResult<RoomInfo> pagedResult = new PagedResult<>();
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

        return pagedResult;
    }

    /**
     * 根据房间位置模糊查询（分页）
     *
     * @param location 房间位置（模糊匹配）
     * @param pageRequest 分页参数
     * @return 分页结果
     */
    @Override
    public PagedResult<RoomInfo> searchByLocation(String location, PageRequestDto pageRequest) {
        // 创建 Page 对象
        Page<RoomInfo> page;
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            page = new Page<>(1, 10000);
        } else {
            page = new Page<>(pageRequest.getPageIndex() + 1, pageRequest.getPageSize());
        }

        // 构建查询条件
        var query = this.lambdaQuery();
        
        if (location != null && !location.isEmpty()) {
            query.like(RoomInfo::getRoomLocation, location);
        }

        Page<RoomInfo> resultPage = query.page(page);

        PagedResult<RoomInfo> pagedResult = new PagedResult<>();
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

        return pagedResult;
    }

    /**
     * 获取所有启用的房间
     *
     * @return 启用的房间列表
     */
    @Override
    public List<RoomInfo> listActive() {
        return this.lambdaQuery()
                .eq(RoomInfo::getRoomStatus, 1)
                .orderByAsc(RoomInfo::getRoomName)
                .list();
    }

    /**
     * 根据房间名称精确查询
     *
     * @param roomName 房间名称
     * @return 查询到的房间，不存在则返回 null
     */
    @Override
    public RoomInfo getByName(String roomName) {
        if (roomName == null || roomName.isEmpty()) {
            return null;
        }
        
        return this.lambdaQuery()
                .eq(RoomInfo::getRoomName, roomName)
                .one();
    }

    //#endregion
}




