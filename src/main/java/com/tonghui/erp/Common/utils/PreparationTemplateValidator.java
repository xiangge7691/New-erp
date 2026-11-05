package com.tonghui.erp.Common.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Data.mapper.EquipmentMapper;
import com.tonghui.erp.Data.mapper.RoomInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 制剂工序模板关联校验器
 * <p>
 * 负责工序模板中「配置室」与「设备」的关联校验与规范化：
 * <ul>
 *     <li>设备必填，且必须存在且未删除</li>
 *     <li>配置室未传时，由设备所在房间自动带出</li>
 *     <li>配置室与设备所在房间不一致时拦截报错</li>
 *     <li>兼容旧前端：未传ID时按名称（可结合房间）唯一解析，重名直接报错并列出候选编码</li>
 *     <li>统一回填 room_desc / equipment_desc 展示快照</li>
 * </ul>
 * </p>
 */
@Component
public class PreparationTemplateValidator {

    // region 日志与依赖
    // ===================================
    // 日志与依赖
    // ===================================

    private static final Logger log = LoggerFactory.getLogger(PreparationTemplateValidator.class);

    /**
     * 设备Mapper
     */
    @Autowired
    private EquipmentMapper equipmentMapper;

    /**
     * 房间Mapper
     */
    @Autowired
    private RoomInfoMapper roomInfoMapper;

    // endregion

    // region 模板校验与规范化
    // ===================================
    // 模板校验与规范化
    // ===================================

    /**
     * 校验并规范化工序模板的配置室、设备关联
     * <p>
     * 校验通过后会把 roomId、equipmentId 以及展示快照文本写回模板对象；
     * 校验失败抛出 IllegalArgumentException，由全局异常处理器返回 400。
     * </p>
     *
     * @param template 工序模板（入参）
     * @throws IllegalArgumentException 设备为空、设备/房间不存在、设备与房间不匹配或名称无法唯一确定
     */
    public void validateAndNormalize(PreparationProcessTemplate template) {
        if (template == null) {
            throw new IllegalArgumentException("工序模板不能为空");
        }

        Equipment equipment;
        RoomInfo room;

        if (template.getEquipmentId() != null) {
            // 优先按设备ID精确关联（新前端提交方式）
            equipment = equipmentMapper.selectById(template.getEquipmentId());
            if (equipment == null) {
                throw new IllegalArgumentException("设备不存在或已删除（equipmentId=" + template.getEquipmentId() + "）");
            }

            if (template.getRoomId() != null) {
                // 显式传了房间，必须与设备所在房间一致
                room = getRoomOrThrow(template.getRoomId());
                checkRoomMatchesEquipment(room, equipment);
            } else {
                // 未传房间：由设备所在房间自动带出
                room = getEquipmentRoomOrThrow(equipment);
            }
        } else {
            // 兼容旧前端：未传设备ID时按名称唯一解析
            if (!StringUtils.hasText(template.getEquipmentDesc())) {
                throw new IllegalArgumentException("设备不能为空，请选择设备");
            }

            // 房间：优先按ID，其次按名称（名称必须唯一），都未传则不限定房间
            if (template.getRoomId() != null) {
                room = getRoomOrThrow(template.getRoomId());
            } else if (StringUtils.hasText(template.getRoomDesc())) {
                room = resolveRoomByName(template.getRoomDesc());
            } else {
                room = null;
            }

            equipment = resolveEquipmentByName(template.getEquipmentDesc().trim(), room);

            if (room == null) {
                room = getEquipmentRoomOrThrow(equipment);
            } else {
                checkRoomMatchesEquipment(room, equipment);
            }
        }

        // 以ID为准回填关联与展示快照
        template.setEquipmentId(equipment.getEquipmentId());
        template.setRoomId(room.getRoomId());
        template.setEquipmentDesc(equipment.getEquipmentName());
        template.setRoomDesc(room.getRoomName());
        template.setRoomName(room.getRoomName());
        template.setRoomCode(room.getRoomCode());
        template.setEquipmentName(equipment.getEquipmentName());
        template.setFixedAssetCode(equipment.getFixedAssetCode());
    }

    // endregion

    // region 工单绑定兜底解析
    // ===================================
    // 工单绑定兜底解析
    // ===================================

    /**
     * 工单创建绑定工序时解析设备（不抛异常）
     * <p>
     * 优先按设备ID精确解析；ID为空的历史模板才按名称兜底，
     * 且必须「名称+房间」或「名称全局」唯一才采用，否则返回 null 并由调用方跳过绑定。
     * </p>
     *
     * @param equipmentId   设备ID（可为空）
     * @param equipmentName 设备名称（历史快照，可为空）
     * @param roomId        配置室ID（可为空）
     * @return 唯一确定的设备；无法唯一确定时返回 null
     */
    public Equipment resolveEquipmentForBinding(Integer equipmentId, String equipmentName, Integer roomId) {
        if (equipmentId != null) {
            Equipment equipment = equipmentMapper.selectById(equipmentId);
            if (equipment == null) {
                log.warn("工序模板绑定的设备不存在或已删除，equipmentId={}，跳过设备绑定", equipmentId);
            }
            return equipment;
        }

        if (!StringUtils.hasText(equipmentName)) {
            return null;
        }

        List<Equipment> candidates = listEquipmentsByName(equipmentName.trim());
        if (candidates.isEmpty()) {
            log.warn("工序模板设备名称【{}】未匹配到设备，跳过设备绑定", equipmentName);
            return null;
        }

        // 优先在模板房间内唯一匹配
        if (roomId != null) {
            List<Equipment> inRoom = candidates.stream()
                    .filter(e -> Objects.equals(e.getRoomId(), roomId))
                    .toList();
            if (inRoom.size() == 1) {
                return inRoom.get(0);
            }
            if (inRoom.size() > 1) {
                log.warn("工序模板房间[{}]内设备名称【{}】匹配到多台设备，无法唯一确定，跳过设备绑定：{}",
                        roomId, equipmentName, describeCandidates(inRoom));
                return null;
            }
            // 房间内没有匹配，继续看名称是否全局唯一（设备可能未维护房间）
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        log.warn("工序模板设备名称【{}】匹配到多台设备，无法唯一确定，跳过设备绑定：{}",
                equipmentName, describeCandidates(candidates));
        return null;
    }

    // endregion

    // region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================

    /**
     * 查询房间，不存在或已删除时抛出异常
     *
     * @param roomId 房间ID
     * @return 房间信息
     */
    private RoomInfo getRoomOrThrow(Integer roomId) {
        RoomInfo room = roomInfoMapper.selectById(roomId);
        if (room == null) {
            throw new IllegalArgumentException("配置室不存在或已删除（roomId=" + roomId + "）");
        }
        return room;
    }

    /**
     * 按房间名称解析房间（要求名称唯一）
     *
     * @param roomName 房间名称
     * @return 房间信息
     */
    private RoomInfo resolveRoomByName(String roomName) {
        List<RoomInfo> rooms = roomInfoMapper.selectList(new LambdaQueryWrapper<RoomInfo>()
                .eq(RoomInfo::getRoomName, roomName.trim()));
        if (rooms.isEmpty()) {
            throw new IllegalArgumentException("配置室不存在：" + roomName);
        }
        if (rooms.size() > 1) {
            String candidates = rooms.stream()
                    .map(r -> r.getRoomName() + "(" + r.getRoomCode() + ")")
                    .collect(Collectors.joining("、"));
            throw new IllegalArgumentException("配置室名称【" + roomName + "】匹配到多个，请改用 roomId 指定，候选：" + candidates);
        }
        return rooms.get(0);
    }

    /**
     * 获取设备所在房间，设备未维护房间时抛出异常
     *
     * @param equipment 设备
     * @return 设备所在房间
     */
    private RoomInfo getEquipmentRoomOrThrow(Equipment equipment) {
        if (equipment.getRoomId() == null) {
            throw new IllegalArgumentException("设备【" + equipmentLabel(equipment) + "】未关联配置室，请先在设备台账维护所在房间");
        }
        RoomInfo room = roomInfoMapper.selectById(equipment.getRoomId());
        if (room == null) {
            throw new IllegalArgumentException("设备【" + equipmentLabel(equipment) + "】关联的配置室不存在或已删除（roomId="
                    + equipment.getRoomId() + "）");
        }
        return room;
    }

    /**
     * 校验房间与设备是否一致
     *
     * @param room      房间
     * @param equipment 设备
     */
    private void checkRoomMatchesEquipment(RoomInfo room, Equipment equipment) {
        if (!Objects.equals(room.getRoomId(), equipment.getRoomId())) {
            throw new IllegalArgumentException("设备【" + equipmentLabel(equipment) + "】不属于配置室【"
                    + room.getRoomName() + "】");
        }
    }

    /**
     * 按名称解析设备（可限定房间），无法唯一确定时抛出异常
     *
     * @param equipmentName 设备名称
     * @param room          限定房间（可为空）
     * @return 设备
     */
    private Equipment resolveEquipmentByName(String equipmentName, RoomInfo room) {
        List<Equipment> candidates = listEquipmentsByName(equipmentName);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("未找到设备【" + equipmentName + "】，请先在设备台账维护");
        }

        if (room != null) {
            List<Equipment> inRoom = candidates.stream()
                    .filter(e -> Objects.equals(e.getRoomId(), room.getRoomId()))
                    .toList();
            if (inRoom.size() == 1) {
                return inRoom.get(0);
            }
            if (inRoom.size() > 1) {
                throw new IllegalArgumentException("配置室【" + room.getRoomName() + "】内设备名称【" + equipmentName
                        + "】匹配到多台，请改用 equipmentId 指定，候选：" + describeCandidates(inRoom));
            }
            throw new IllegalArgumentException("设备【" + equipmentName + "】不属于配置室【" + room.getRoomName()
                    + "】，候选设备：" + describeCandidates(candidates));
        }

        if (candidates.size() > 1) {
            throw new IllegalArgumentException("设备名称【" + equipmentName
                    + "】匹配到多台设备，请改用 equipmentId 指定，候选：" + describeCandidates(candidates));
        }
        return candidates.get(0);
    }

    /**
     * 按名称查询未删除设备
     *
     * @param equipmentName 设备名称
     * @return 设备列表
     */
    private List<Equipment> listEquipmentsByName(String equipmentName) {
        return equipmentMapper.selectList(new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getEquipmentName, equipmentName));
    }

    /**
     * 设备展示文本（名称 + 编码），用于提示信息
     *
     * @param equipment 设备
     * @return 展示文本
     */
    private String equipmentLabel(Equipment equipment) {
        String code = StringUtils.hasText(equipment.getFixedAssetCode()) ? equipment.getFixedAssetCode() : "-";
        return equipment.getEquipmentName() + "(" + code + ")";
    }

    /**
     * 候选设备展示文本（名称 + 编码 + 房间）
     *
     * @param equipments 候选设备列表
     * @return 展示文本
     */
    private String describeCandidates(List<Equipment> equipments) {
        return equipments.stream()
                .map(e -> equipmentLabel(e) + "@room=" + (e.getRoomId() == null ? "未维护" : e.getRoomId()))
                .collect(Collectors.joining("、"));
    }

    // endregion
}
