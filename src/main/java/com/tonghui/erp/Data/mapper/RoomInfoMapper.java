package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.RoomInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

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
}




