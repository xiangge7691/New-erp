package com.tonghui.erp.Service;

import com.tonghui.erp.Common.utils.PreparationTemplateValidator;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import com.tonghui.erp.Data.Entity.ProcessType;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.mapper.EquipmentMapper;
import com.tonghui.erp.Data.mapper.PreparationMapper;
import com.tonghui.erp.Data.mapper.RoomInfoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 制剂工序模板配置室/设备关联测试
 * <p>
 * 覆盖：设备必填、房间由设备自动带出、设备与房间一致性拦截、
 * 同名设备必须按ID指定、工单创建时按设备ID精确绑定工序记录。
 * 所有用例使用事务回滚，不污染测试库。
 * </p>
 */
@SpringBootTest
@Transactional
public class PreparationTemplateAssociationTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /**
     * 工序模板关联校验器
     */
    @Autowired
    private PreparationTemplateValidator preparationTemplateValidator;

    /**
     * 工序模板服务
     */
    @Autowired
    private PreparationProcessTemplateService preparationProcessTemplateService;

    /**
     * 工单服务
     */
    @Autowired
    private WorkOrderService workOrderService;

    /**
     * 工单工序执行服务
     */
    @Autowired
    private WorkOrderProcessExecutionService workOrderProcessExecutionService;

    /**
     * 工序类型服务
     */
    @Autowired
    private ProcessTypeService processTypeService;

    /**
     * 房间数据访问层（测试数据准备）
     */
    @Autowired
    private RoomInfoMapper roomInfoMapper;

    /**
     * 设备数据访问层（测试数据准备）
     */
    @Autowired
    private EquipmentMapper equipmentMapper;

    /**
     * 制剂数据访问层（测试数据准备）
     */
    @Autowired
    private PreparationMapper preparationMapper;

    // endregion

    // region 模板校验测试
    // ===================================
    // 模板校验测试
    // ===================================

    /**
     * 仅传设备ID时，应自动带出设备所在房间并回填名称快照
     */
    @Test
    public void testAutoFillRoomFromEquipment() {
        RoomInfo room = createRoom("AUTO");
        Equipment equipment = createEquipment("自动带出设备", room.getRoomId(), "AUTO-EQ-1");

        PreparationProcessTemplate template = new PreparationProcessTemplate();
        template.setEquipmentId(equipment.getEquipmentId());

        preparationTemplateValidator.validateAndNormalize(template);

        assertEquals(room.getRoomId(), template.getRoomId(), "房间应由设备所在房间自动带出");
        assertEquals(room.getRoomName(), template.getRoomDesc(), "房间名称快照应同步");
        assertEquals(equipment.getEquipmentName(), template.getEquipmentDesc(), "设备名称快照应同步");
        assertEquals(equipment.getEquipmentName(), template.getEquipmentName(), "展示字段设备名称应填充");
        assertEquals(equipment.getFixedAssetCode(), template.getFixedAssetCode(), "展示字段设备编码应填充");
        assertEquals(room.getRoomCode(), template.getRoomCode(), "展示字段房间编码应填充");
    }

    /**
     * 设备与房间不一致时应拦截并提示具体设备/房间
     */
    @Test
    public void testEquipmentRoomMismatchRejected() {
        RoomInfo room = createRoom("MATCH");
        RoomInfo otherRoom = createRoom("OTHER");
        Equipment equipment = createEquipment("房间校验设备", room.getRoomId(), "MATCH-EQ-1");

        PreparationProcessTemplate template = new PreparationProcessTemplate();
        template.setEquipmentId(equipment.getEquipmentId());
        template.setRoomId(otherRoom.getRoomId());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> preparationTemplateValidator.validateAndNormalize(template),
                "设备与房间不一致应被拦截");
        assertTrue(ex.getMessage().contains("不属于配置室"), "提示应说明设备不属于该房间，实际：" + ex.getMessage());
    }

    /**
     * 同房间同名设备：仅传名称应报错并列出候选编码，传ID应成功
     */
    @Test
    public void testDuplicateEquipmentNameRequiresId() {
        RoomInfo room = createRoom("DUP");
        Equipment first = createEquipment("重名设备", room.getRoomId(), "DUP-EQ-1");
        Equipment second = createEquipment("重名设备", room.getRoomId(), "DUP-EQ-2");

        PreparationProcessTemplate byName = new PreparationProcessTemplate();
        byName.setEquipmentDesc("重名设备");
        byName.setRoomId(room.getRoomId());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> preparationTemplateValidator.validateAndNormalize(byName),
                "同名设备按名称解析应报错");
        assertTrue(ex.getMessage().contains("匹配到多台"), "提示应说明匹配到多台，实际：" + ex.getMessage());
        assertTrue(ex.getMessage().contains(first.getFixedAssetCode()), "候选应包含第一台设备编码");
        assertTrue(ex.getMessage().contains(second.getFixedAssetCode()), "候选应包含第二台设备编码");

        PreparationProcessTemplate byId = new PreparationProcessTemplate();
        byId.setEquipmentId(second.getEquipmentId());
        byId.setRoomId(room.getRoomId());
        preparationTemplateValidator.validateAndNormalize(byId);
        assertEquals(second.getEquipmentId(), byId.getEquipmentId(), "按ID应精确绑定第二台同名设备");
    }

    /**
     * 设备为空时应拦截
     */
    @Test
    public void testEquipmentRequired() {
        PreparationProcessTemplate template = new PreparationProcessTemplate();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> preparationTemplateValidator.validateAndNormalize(template),
                "未选择设备应被拦截");
        assertTrue(ex.getMessage().contains("设备不能为空"), "提示应说明设备必填，实际：" + ex.getMessage());
    }

    // endregion

    // region 工单绑定测试
    // ===================================
    // 工单绑定测试
    // ===================================

    /**
     * 携带制剂信息创建工单时，工序记录应按模板设备ID精确绑定，而不是按名称猜
     */
    @Test
    public void testWorkOrderBindingUsesEquipmentId() {
        List<Preparation> preparations = preparationMapper.selectList(null);
        List<ProcessType> processTypes = processTypeService.listActive();
        if (preparations.isEmpty() || processTypes.isEmpty()) {
            System.out.println("无制剂或工序类型数据，跳过该测试");
            return;
        }
        Preparation preparation = preparations.get(0);
        ProcessType processType = processTypes.get(0);

        // 同一房间内准备两台同名设备，模板绑定第二台
        RoomInfo room = createRoom("BIND");
        createEquipment("绑定重名设备", room.getRoomId(), "BIND-EQ-1");
        Equipment target = createEquipment("绑定重名设备", room.getRoomId(), "BIND-EQ-2");

        PreparationProcessTemplate template = new PreparationProcessTemplate();
        template.setPreparationId(preparation.getPreparationId());
        template.setProcessTypeId(processType.getProcessId().longValue());
        template.setStepOrder(1);
        template.setEquipmentId(target.getEquipmentId());
        preparationProcessTemplateService.batchSave(preparation.getPreparationId(), List.of(template));

        // 校验保存结果：房间由设备带出
        PreparationProcessTemplate saved = preparationProcessTemplateService
                .findByPreparationId(preparation.getPreparationId()).get(0);
        assertEquals(target.getEquipmentId(), saved.getEquipmentId(), "模板应保存设备ID");
        assertEquals(room.getRoomId(), saved.getRoomId(), "模板房间应由设备自动带出");

        // 创建工单，触发工序带出
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWorkOrderCode(workOrderService.generateWorkOrderCode());
        workOrder.setWorkOrderName("模板绑定测试工单");
        workOrder.setPreparationId(preparation.getPreparationId());
        workOrder.setBatchQty(new BigDecimal("10.000"));
        assertTrue(workOrderService.addWorkOrder(workOrder), "新增工单应成功");

        List<WorkOrderProcessExecution> executions =
                workOrderProcessExecutionService.getByWorkOrderId(workOrder.getWorkOrderId());
        assertEquals(1, executions.size(), "应带出1条工序执行记录");
        assertEquals(target.getEquipmentId(), executions.get(0).getEquipmentId(),
                "工序记录应按模板设备ID绑定（而非同名设备中随机一台）");
        assertEquals(room.getRoomId(), executions.get(0).getRoomId(), "工序记录应带出设备所在房间");
        assertNotNull(executions.get(0).getEquipmentName(), "工序记录应能展示设备名称");
    }

    // endregion

    // region 测试数据准备
    // ===================================
    // 测试数据准备
    // ===================================

    /**
     * 创建测试房间
     *
     * @param prefix 编码/名称前缀，避免唯一键冲突
     * @return 房间
     */
    private RoomInfo createRoom(String prefix) {
        String suffix = String.valueOf(System.nanoTime());
        RoomInfo room = new RoomInfo();
        room.setRoomCode(prefix + "-" + suffix);
        room.setRoomName(prefix + "测试间-" + suffix);
        room.setRoomStatus(1);
        room.setIsDeleted(0);
        room.setVersion(0);
        roomInfoMapper.insert(room);
        return room;
    }

    /**
     * 创建测试设备
     *
     * @param name    设备名称
     * @param roomId  所在房间ID
     * @param code    固定资产编码
     * @return 设备
     */
    private Equipment createEquipment(String name, Integer roomId, String code) {
        String suffix = String.valueOf(System.nanoTime());
        Equipment equipment = new Equipment();
        equipment.setEquipmentName(name);
        equipment.setFixedAssetCode(code + "-" + suffix);
        equipment.setEquipmentStatus("正常");
        equipment.setRoomId(roomId);
        equipment.setIsDeleted(0);
        equipment.setVersion(0);
        equipmentMapper.insert(equipment);
        return equipment;
    }

    // endregion
}
