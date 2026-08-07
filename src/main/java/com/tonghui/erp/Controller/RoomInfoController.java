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
 * <p>
 * 提供房间信息的CRUD操作、名称搜索、位置搜索、启用列表、带子表查询及消毒到期提醒等功能，用于GMP车间环境管理
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                                │
 * ├────┼──────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/room                            │ GET   │ 获取所有房间列表（分页）            │
 * │ 2  │ /api/room/{id}                       │ GET   │ 根据ID获取房间详情                  │
 * │ 3  │ /api/room                            │ POST  │ 新增房间                            │
 * │ 4  │ /api/room/{id}                       │ PUT   │ 修改房间                            │
 * │ 5  │ /api/room/{id}                       │ DELETE│ 删除房间                            │
 * │ 6  │ /api/room/search                     │ GET   │ 搜索房间                            │
 * │ 7  │ /api/room/search/location            │ GET   │ 按位置搜索房间                      │
 * │ 8  │ /api/room/active                     │ GET   │ 获取所有启用的房间列表              │
 * │ 9  │ /api/room/search-with-details        │ GET   │ 搜索房间（含温湿度、压差等子表）    │
 * │ 10 │ /api/room/disinfection/reminder      │ GET   │ 全局消毒到期提醒                    │
 * └────┴──────────────────────────────────────┴────────┴─────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/room")
public class RoomInfoController extends BaseCrudController<RoomInfo, RoomInfo, Integer> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 房间信息服务
     */
    private final RoomInfoService roomInfoService;

    /**
     * 消毒记录服务
     */
    private final DisinfectionRecordService disinfectionRecordService;

    @Autowired
    public RoomInfoController(RoomInfoService roomInfoService, DisinfectionRecordService disinfectionRecordService) {
        this.roomInfoService = roomInfoService;
        this.disinfectionRecordService = disinfectionRecordService;
    }

    // endregion

    // region CRUD操作实现
    // ===================================
    // CRUD操作实现
    // ===================================

    /**
     * 获取所有房间列表（分页）
     *
     * 示例请求：
     * GET /api/room?pageIndex=0&pageSize=10
     *
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页大小
     * @return PagedResult&lt;RoomInfo&gt; 分页结果，包含房间列表
     */
    @Override
    protected PagedResult<RoomInfo> getAllData(int pageIndex, int pageSize) {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(pageIndex);
        pageRequest.setPageSize(pageSize);
        return roomInfoService.searchByName(null, null, pageRequest);
    }

    /**
     * 根据ID获取房间详情
     *
     * 示例请求：
     * GET /api/room/1
     *
     * @param id 房间ID
     * @return RoomInfo 房间详情
     */
    @Override
    protected RoomInfo getDataById(Integer id) {
        return roomInfoService.getById(id);
    }

    /**
     * 新增房间
     * <p>
     * 新增前会检查房间名称是否已存在，自动设置创建人、更新人及时间
     * </p>
     *
     * 示例请求：
     * POST /api/room
     * Content-Type: application/json
     * {
     *   "roomName": "1号洁净车间",
     *   "location": "A栋1楼",
     *   "cleanClass": "C级",
     *   "area": 100.00,
     *   "status": 1
     * }
     *
     * @param roomInfo 房间实体对象
     * @return RoomInfo 新增的房间
     */
    @Override
    protected RoomInfo doCreate(RoomInfo roomInfo) {
        // 检查房间名称是否已存在
        if (roomInfoService.getByName(roomInfo.getRoomName()) != null) {
            throw new RuntimeException("房间名称已存在");
        }

        // 清理已软删除的相同名称记录（避免唯一键冲突）
        roomInfoService.cleanSoftDeletedByRoomName(roomInfo.getRoomName());
        // 如果房间编码不为空，也清理已软删除的相同编码记录
        if (roomInfo.getRoomCode() != null && !roomInfo.getRoomCode().isEmpty()) {
            roomInfoService.cleanSoftDeletedByRoomCode(roomInfo.getRoomCode());
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

    /**
     * 修改房间
     * <p>
     * 修改前会检查房间是否存在以及名称是否被其他记录使用
     * </p>
     *
     * 示例请求：
     * PUT /api/room/1
     * Content-Type: application/json
     * {
     *   "roomName": "1号洁净车间（更新）",
     *   "location": "A栋1楼（更新）"
     * }
     *
     * @param id 房间ID
     * @param roomInfo 房间实体对象
     * @return RoomInfo 修改后的房间
     */
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

    /**
     * 删除房间
     *
     * 示例请求：
     * DELETE /api/room/1
     *
     * @param id 房间ID
     * @return boolean 删除结果
     */
    @Override
    protected boolean doDelete(Integer id) {
        return roomInfoService.removeById(id);
    }

    // endregion

    // region 高级查询接口
    // ===================================
    // 高级查询接口
    // ===================================

    /**
     * 搜索房间
     * <p>
     * 支持按房间名称模糊搜索，返回分页结果
     * </p>
     *
     * 示例请求：
     * GET /api/room/search?keyword=洁净车间&pageIndex=0&pageSize=10
     *
     * @param roomName 房间名称（可选，支持模糊搜索）
     * @param keyword 关键字（对房间编码、房间名称进行模糊匹配，可选）
     * @param pageRequest 分页请求参数（页码、页面大小）
     * @return ApiResponse&lt;PagedResult&lt;RoomInfo&gt;&gt; 房间列表（分页）
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<RoomInfo>> searchRooms(
            @RequestParam(required = false) String roomName,
            @RequestParam(required = false) String keyword,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<RoomInfo> result = roomInfoService.searchByName(roomName, keyword, pageRequest);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "搜索房间");
        }
    }

    /**
     * 按位置搜索房间
     * <p>
     * 支持按房间位置模糊搜索，返回分页结果
     * </p>
     *
     * 示例请求：
     * GET /api/room/search/location?location=A栋&pageIndex=0&pageSize=10
     *
     * @param location 房间位置（可选，支持模糊搜索）
     * @param pageRequest 分页请求参数（页码、页面大小）
     * @return ApiResponse&lt;PagedResult&lt;RoomInfo&gt;&gt; 房间列表（分页）
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
     * <p>
     * 仅返回状态为启用的房间，用于下拉选择等场景
     * </p>
     *
     * 示例请求：
     * GET /api/room/active
     *
     * @return ApiResponse&lt;List&lt;RoomInfo&gt;&gt; 启用的房间列表
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
     * <p>
     * 返回房间信息及其关联的温湿度、压差、洁净检测、消毒记录等子表数据
     * </p>
     *
     * 示例请求：
     * GET /api/room/search-with-details?keyword=洁净车间&pageIndex=0&pageSize=10
     *
     * @param roomName 房间名称（可选，支持模糊搜索）
     * @param keyword 关键字（对房间编码、房间名称进行模糊匹配，可选）
     * @param pageRequest 分页请求参数
     * @return ApiResponse&lt;PagedResult&lt;RoomInfoWithDetailsDto&gt;&gt; 房间列表（含温湿度、压差、洁净检测、消毒记录）
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<RoomInfoWithDetailsDto>> searchWithDetails(
            @RequestParam(required = false) String roomName,
            @RequestParam(required = false) String keyword,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<RoomInfoWithDetailsDto> result = roomInfoService.searchWithDetails(roomName, keyword, pageRequest);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "搜索房间");
        }
    }

    /**
     * 全局消毒到期提醒（供首页待办使用）
     * <p>
     * 查询在未来指定天数内需要进行消毒的记录，用于GMP合规管理中的消毒计划跟踪
     * </p>
     *
     * 示例请求：
     * GET /api/room/disinfection/reminder?days=30
     *
     * @param days 提前天数（默认30天）
     * @return ApiResponse&lt;List&lt;DisinfectionRecord&gt;&gt; 即将到期的消毒记录列表
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

    // endregion
}
