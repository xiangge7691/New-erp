package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Service.RoomInfoService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 车间记录控制器基类
 * <p>
 * 提供GMP车间记录（温湿度、洁净检测、消毒、压差、清洁等）的通用CRUD操作模板，
 * 包括按房间筛选查询、房间信息填充等功能
 * </p>
 *
 * @param <T> 车间记录实体类型
 */
public abstract class BaseRoomRecordController<T> extends BaseController {

    // region 房间服务依赖注入
    // ===================================
    // 房间服务依赖注入
    // ===================================

    /**
     * 房间信息服务，用于根据房间名称/编码查询房间信息
     */
    @Autowired
    protected RoomInfoService roomInfoService;

    // endregion

    // region 通用房间查询方法
    // ===================================
    // 通用房间查询方法
    // ===================================

    /**
     * 根据房间名称和编码查询符合条件的房间ID列表
     *
     * @param roomName 房间名称（模糊匹配，可选）
     * @param roomCode 房间编码（精确匹配，可选）
     * @return 符合条件的房间ID列表，无匹配时返回空列表
     */
    protected List<Integer> findRoomIds(String roomName, String roomCode) {
        QueryWrapper<RoomInfo> roomWrapper = new QueryWrapper<>();
        roomWrapper.eq("is_deleted", 0);
        if (roomName != null) roomWrapper.like("room_name", roomName);
        if (roomCode != null) roomWrapper.eq("room_code", roomCode);
        List<RoomInfo> rooms = roomInfoService.list(roomWrapper);
        return rooms.stream().map(RoomInfo::getRoomId).collect(Collectors.toList());
    }

    /**
     * 根据房间名称和编码查询符合条件的房间ID列表，并与指定的roomId取交集
     *
     * @param roomId   房间ID（可选，用于进一步筛选）
     * @param roomName 房间名称（模糊匹配，可选）
     * @param roomCode 房间编码（精确匹配，可选）
     * @return 符合条件的房间ID列表，无匹配时返回空列表
     */
    protected List<Integer> findRoomIds(Integer roomId, String roomName, String roomCode) {
        List<Integer> roomIds = findRoomIds(roomName, roomCode);
        if (roomIds.isEmpty()) {
            return roomIds;
        }
        if (roomId != null) {
            if (!roomIds.contains(roomId)) {
                return List.of();
            }
            return List.of(roomId);
        }
        return roomIds;
    }

    /**
     * 批量填充记录中的房间名称和编码
     * <p>
     * 通过roomIdGetter获取记录的roomId，批量查询房间信息后设置roomName和roomCode
     * </p>
     *
     * @param list           车间记录列表
     * @param roomIdGetter   获取roomId的函数（支持Integer和Long返回类型）
     * @param roomNameSetter 设置roomName的函数
     * @param roomCodeSetter 设置roomCode的函数
     */
    protected <R> void fillRoomInfo(
            List<R> list,
            java.util.function.Function<R, ?> roomIdGetter,
            java.util.function.BiConsumer<R, String> roomNameSetter,
            java.util.function.BiConsumer<R, String> roomCodeSetter) {

        if (list == null || list.isEmpty()) {
            return;
        }

        Set<Integer> roomIds = list.stream()
                .map(r -> {
                    Object val = roomIdGetter.apply(r);
                    if (val instanceof Long) return ((Long) val).intValue();
                    if (val instanceof Integer) return (Integer) val;
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        if (!roomIds.isEmpty()) {
            Map<Integer, RoomInfo> roomMap = roomInfoService.listByIds(roomIds).stream()
                    .collect(Collectors.toMap(RoomInfo::getRoomId, r -> r));
            list.forEach(r -> {
                Object val = roomIdGetter.apply(r);
                Integer roomId = null;
                if (val instanceof Long) roomId = ((Long) val).intValue();
                else if (val instanceof Integer) roomId = (Integer) val;
                if (roomId != null) {
                    RoomInfo room = roomMap.get(roomId);
                    if (room != null) {
                        roomNameSetter.accept(r, room.getRoomName());
                        roomCodeSetter.accept(r, room.getRoomCode());
                    }
                }
            });
        }
    }

    /**
     * 安全处理分页参数
     *
     * @param pageIndex 页码索引
     * @param pageSize  每页数量
     * @return 安全处理后的分页参数数组 [safePageIndex, safePageSize]
     */
    protected int[] safePageParams(int pageIndex, int pageSize) {
        int safePageIndex = Math.max(0, pageIndex);
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
        return new int[]{safePageIndex, safePageSize};
    }

    /**
     * 创建分页查询的Page对象
     *
     * @param pageIndex 页码索引，从0开始
     * @param pageSize  每页数量
     * @param <R>       记录类型
     * @return MyBatis-Plus Page对象
     */
    protected <R> Page<R> createPage(int pageIndex, int pageSize) {
        return new Page<>(pageIndex + 1, pageSize);
    }

    /**
     * 构建分页结果
     *
     * @param page      MyBatis-Plus分页结果
     * @param pageIndex 页码索引，从0开始
     * @param pageSize  每页数量
     * @param <R>       记录类型
     * @return PagedResult分页结果
     */
    protected <R> PagedResult<R> buildPagedResult(Page<R> page, int pageIndex, int pageSize) {
        PagedResult<R> result = new PagedResult<>();
        result.setItems(page.getRecords());
        result.setTotalCount(page.getTotal());
        result.setPageIndex(pageIndex);
        result.setPageSize(pageSize);
        return result;
    }

    // endregion
}
