package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Room.RoomInfoWithDetailsDto;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.DisinfectionRecord;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Service.DisinfectionRecordService;
import com.tonghui.erp.Service.RoomInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 房间信息管理控制器
 * 提供房间的增删改查及搜索功能
 */
@RestController
@RequestMapping("/api/room")
public class RoomInfoController extends BaseCrudController<RoomInfo, RoomInfo, Integer> {

    private final RoomInfoService roomInfoService;

    private final DisinfectionRecordService disinfectionRecordService;

    @Autowired
    public RoomInfoController(RoomInfoService roomInfoService, DisinfectionRecordService disinfectionRecordService) {
        this.roomInfoService = roomInfoService;
        this.disinfectionRecordService = disinfectionRecordService;
    }

    // CRUD 实现
    @Override
    protected PagedResult<RoomInfo> getAllData(int pageIndex, int pageSize) {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(pageIndex);
        pageRequest.setPageSize(pageSize);
        return roomInfoService.searchByName(null, pageRequest);
    }

    @Override
    protected RoomInfo getDataById(Integer id) {
        return roomInfoService.getById(id);
    }

    @Override
    protected RoomInfo doCreate(RoomInfo roomInfo) {
        // 检查房间名称是否已存在
        if (roomInfoService.getByName(roomInfo.getRoomName()) != null) {
            throw new RuntimeException("房间名称已存在");
        }

        // 设置创建人 ID 和更新人 ID
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            roomInfo.setCreatedBy(currentUserId);
            roomInfo.setUpdatedBy(currentUserId);
        }
        
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        roomInfo.setCreatedTime(now);
        roomInfo.setUpdatedTime(now);

        roomInfoService.save(roomInfo);
        return roomInfo;
    }

    @Override
    protected RoomInfo doUpdate(Integer id, RoomInfo roomInfo) {
        RoomInfo existing = roomInfoService.getById(id);
        if (existing == null) {
            throw new RuntimeException("房间不存在");
        }

        // 检查房间名称是否被其他记录使用
        RoomInfo byName = roomInfoService.getByName(roomInfo.getRoomName());
        if (byName != null && !byName.getRoomId().equals(id)) {
            throw new RuntimeException("房间名称已存在");
        }

        // 设置更新人 ID 和更新时间
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            roomInfo.setUpdatedBy(currentUserId);
        }
        roomInfo.setUpdatedTime(LocalDateTime.now());

        roomInfo.setRoomId(id);
        roomInfoService.updateById(roomInfo);
        return roomInfo;
    }

    @Override
    protected boolean doDelete(Integer id) {
        return roomInfoService.removeById(id);
    }

    /**
     * 搜索房间
     * @param roomName 房间名称（可选，支持模糊搜索）
     * @param pageRequest 分页请求参数（页码、页面大小）
     * @return 房间列表（分页）
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<RoomInfo>> searchRooms(
            @RequestParam(required = false) String roomName,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<RoomInfo> result = roomInfoService.searchByName(roomName, pageRequest);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "搜索房间");
        }
    }

    /**
     * 按位置搜索房间
     * @param location 房间位置（可选，支持模糊搜索）
     * @param pageRequest 分页请求参数（页码、页面大小）
     * @return 房间列表（分页）
     */
    @GetMapping("/search/location")
    public ApiResponse<PagedResult<RoomInfo>> searchByLocation(
            @RequestParam(required = false) String location,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<RoomInfo> result = roomInfoService.searchByLocation(location, pageRequest);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "按位置搜索房间");
        }
    }

    /**
     * 获取所有启用的房间列表
     * @return 启用的房间列表
     */
    @GetMapping("/active")
    public ApiResponse<List<RoomInfo>> listActiveRooms() {
        try {
            List<RoomInfo> result = roomInfoService.listActive();
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "获取启用房间");
        }
    }

    /**
     * 搜索房间（带子表）
     * @param roomName 房间名称（可选，支持模糊搜索）
     * @param pageRequest 分页请求参数
     * @return 房间列表（含温湿度、压差、洁净检测、消毒记录）
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<RoomInfoWithDetailsDto>> searchWithDetails(
            @RequestParam(required = false) String roomName,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<RoomInfoWithDetailsDto> result = roomInfoService.searchWithDetails(roomName, pageRequest);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "搜索房间");
        }
    }

    /**
     * 全局消毒到期提醒（供首页待办使用）
     */
    @GetMapping("/disinfection/reminder")
    public ApiResponse<List<DisinfectionRecord>> disinfectionReminder(
            @RequestParam(defaultValue = "30") int days) {
        try {
            List<DisinfectionRecord> result = disinfectionRecordService.findUpcomingDisinfection(days);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询消毒提醒");
        }
    }
}

