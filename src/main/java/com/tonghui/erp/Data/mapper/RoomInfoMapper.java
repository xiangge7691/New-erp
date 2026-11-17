package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.RoomInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 房间信息数据访问Mapper接口
 */
public interface RoomInfoMapper extends BaseMapper<RoomInfo> {

    /**
     * 根据房间编码物理删除已软删除的记录（释放唯一键约束）
     *
     * @param roomCode 房间编码
     * @return 删除的记录数
     */
    @Delete("DELETE FROM room_info WHERE room_code = #{roomCode} AND is_deleted = 1")
    int physicalDeleteByRoomCode(@Param("roomCode") String roomCode);

    /**
     * 根据房间名称物理删除已软删除的记录（释放唯一键约束）
     *
     * @param roomName 房间名称
     * @return 删除的记录数
     */
    @Delete("DELETE FROM room_info WHERE room_name = #{roomName} AND is_deleted = 1")
    int physicalDeleteByRoomName(@Param("roomName") String roomName);

    /**
     * 按房间ID加行锁（排产冲突校验并发控制）
     *
     * @param roomId 房间ID
     * @return 房间信息
     */
    @Select("SELECT * FROM room_info WHERE room_id = #{roomId} AND is_deleted = 0 FOR UPDATE")
    RoomInfo selectByIdForUpdate(@Param("roomId") Integer roomId);
}
