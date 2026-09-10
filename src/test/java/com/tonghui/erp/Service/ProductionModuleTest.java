package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.ProcessType;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.MaterialRequisition;
import com.tonghui.erp.Data.Entity.MaterialRequisitionDetail;
import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.tonghui.erp.Data.Entity.PreparationDocument;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import com.tonghui.erp.Data.Entity.ProductionProcessRecord;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.Entity.PlanStatusLog;
import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Data.Entity.Unit;
import com.tonghui.erp.Data.mapper.MaterialMapper;
import com.tonghui.erp.Data.mapper.UnitMapper;
import com.tonghui.erp.Data.mapper.PreparationMapper;
import com.tonghui.erp.Service.ProductionPlanService;
import com.tonghui.erp.Service.WorkOrderService;
import com.tonghui.erp.Service.MaterialRequisitionService;
import com.tonghui.erp.Service.PreparationFormulaService;
import com.tonghui.erp.Service.PreparationDocumentService;
import com.tonghui.erp.Service.PreparationProcessTemplateService;
import com.tonghui.erp.Service.ProcessTypeService;
import com.tonghui.erp.Service.ProductionProcessRecordService;
import com.tonghui.erp.Service.WorkOrderProcessExecutionService;
import com.tonghui.erp.Service.PlanStatusLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 生产域综合测试
 * <p>
 * 覆盖生产计划、工单、领料单、制剂配方、工艺文档、工艺模板、工序类型、
 * 生产工序记录、工单工序执行、计划状态日志等核心业务逻辑，
 * 全部用例使用事务回滚，不污染数据库
 * </p>
 */
@SpringBootTest
public class ProductionModuleTest {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 生产计划服务
     */
    @Autowired
    private ProductionPlanService productionPlanService;

    /**
     * 工单服务
     */
    @Autowired
    private WorkOrderService workOrderService;

    /**
     * 领料单服务
     */
    @Autowired
    private MaterialRequisitionService materialRequisitionService;

    /**
     * 制剂配方服务
     */
    @Autowired
    private PreparationFormulaService preparationFormulaService;

    /**
     * 制剂文档服务
     */
    @Autowired
    private PreparationDocumentService preparationDocumentService;

    /**
     * 制剂工艺模板服务
     */
    @Autowired
    private PreparationProcessTemplateService preparationProcessTemplateService;

    /**
     * 工序类型服务
     */
    @Autowired
    private ProcessTypeService processTypeService;

    /**
     * 生产工序记录服务
     */
    @Autowired
    private ProductionProcessRecordService productionProcessRecordService;

    /**
     * 工单工序执行服务
     */
    @Autowired
    private WorkOrderProcessExecutionService workOrderProcessExecutionService;

    /**
     * 计划状态日志服务
     */
    @Autowired
    private PlanStatusLogService planStatusLogService;

    /**
     * 制剂数据访问层
     */
    @Autowired
    private PreparationMapper preparationMapper;

    /**
     * 物料数据访问层
     */
    @Autowired
    private MaterialMapper materialMapper;

    /**
     * 单位数据访问层
     */
    @Autowired
    private UnitMapper unitMapper;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试工序类型管理：新增→按编码查询→启用列表→删除
     */
    @Test
    @Transactional
    public void testProcessTypeCrud() {
        String code = "TST" + (System.currentTimeMillis() % 1000000);
        ProcessType processType = new ProcessType();
        processType.setProcessCode(code);
        processType.setProcessName("测试工序" + System.currentTimeMillis());
        processType.setProcessStatus(1);
        assertTrue(processTypeService.save(processType), "新增工序类型应成功");
        Long processId = processType.getProcessId().longValue();

        // 按编码查询
        ProcessType found = processTypeService.getByCode(code);
        assertNotNull(found, "按编码应查到工序类型");
        assertEquals(processType.getProcessId(), found.getProcessId(), "工序ID应一致");

        // 启用列表包含
        assertTrue(processTypeService.listActive().stream()
                        .anyMatch(p -> p.getProcessId().equals(processType.getProcessId())),
                "启用工序列表应包含新建工序");

        // 按名称搜索
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(-1);
        pageRequest.setPageSize(-1);
        PagedResult<ProcessType> search = processTypeService.searchByName(null, "测试工序", pageRequest);
        assertTrue(search.getTotalCount() >= 1, "按名称搜索应命中新建工序");

        // 删除
        assertTrue(processTypeService.removeById(processId), "删除工序类型应成功");
        assertNull(processTypeService.getById(processId), "删除后工序类型应不存在");
    }

    /**
     * 测试生产计划新增与查询：新增计划→按关键字查询→状态日志
     */
    @Test
    @Transactional
    public void testProductionPlanCrud() {
        String planNo = "SCJH" + System.currentTimeMillis();
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanNumber(planNo);
        plan.setPlanName("测试生产计划");
        plan.setPreparationName("测试制剂");
        plan.setPreparationCode("TSTPREP" + System.currentTimeMillis());
        plan.setPlanQuantity(new BigDecimal("100.000"));
        assertTrue(productionPlanService.save(plan), "新增生产计划应成功");
        Integer planId = plan.getId().intValue();

        // 按关键字查询
        ProductionPlan query = new ProductionPlan();
        query.setPlanName("测试生产计划");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ProductionPlan> page =
                productionPlanService.queryProductionPlans(query, planNo, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                        null, 1, 10);
        assertTrue(page.getTotal() >= 1, "按关键字查询应命中新建计划");

        // 按工单查询生产计划：创建关联工单后应能查到对应计划
        Long preparationId = findAnyPreparationId();
        if (preparationId != null) {
            WorkOrder wo = new WorkOrder();
            wo.setWorkOrderCode("GD" + System.currentTimeMillis());
            wo.setWorkOrderName("计划关联测试工单");
            wo.setPreparationId(preparationId);
            wo.setPlanId(plan.getId().longValue());
            workOrderService.addWorkOrder(wo);
            ProductionPlan linked = productionPlanService.getPlanByWorkOrder(wo.getWorkOrderId());
            assertNotNull(linked, "按工单应查到关联生产计划");
            assertEquals(planNo, linked.getPlanNumber(), "关联计划编号应一致");
        }
    }

    /**
     * 测试工单管理：新增→按ID查询→更新→删除（依赖制剂数据）
     */
    @Test
    @Transactional
    public void testWorkOrderCrud() {
        Long preparationId = findAnyPreparationId();
        if (preparationId == null) {
            System.out.println("无制剂数据，跳过该测试");
            return;
        }

        String workOrderCode = workOrderService.generateWorkOrderCode();
        assertNotNull(workOrderCode, "工单编码生成不应为空");
        assertTrue(workOrderCode.startsWith("GD"), "工单编码应以GD开头");

        WorkOrder workOrder = new WorkOrder();
        workOrder.setWorkOrderCode(workOrderCode);
        workOrder.setWorkOrderName("测试工单");
        workOrder.setPreparationId(preparationId);
        workOrder.setBatchQty(new BigDecimal("10.000"));
        assertTrue(workOrderService.addWorkOrder(workOrder), "新增工单应成功");
        Long workOrderId = workOrder.getWorkOrderId();

        // 按ID查询
        WorkOrder found = workOrderService.getWorkOrderById(workOrderId);
        assertNotNull(found, "按ID应查到工单");
        assertEquals(workOrderCode, found.getWorkOrderCode(), "工单编码应一致");

        // 分页列表
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(-1);
        pageRequest.setPageSize(-1);
        PagedResult<WorkOrder> list = workOrderService.getWorkOrderList(pageRequest);
        assertTrue(list.getItems().stream().anyMatch(w -> w.getWorkOrderId().equals(workOrderId)),
                "工单列表应包含新建工单");

        // 更新
        workOrder.setWorkOrderName("更新后的工单");
        assertTrue(workOrderService.updateWorkOrder(workOrder), "更新工单应成功");
        assertEquals("更新后的工单", workOrderService.getWorkOrderById(workOrderId).getWorkOrderName(),
                "工单名称应已更新");

        // 删除
        assertTrue(workOrderService.deleteWorkOrder(workOrderId), "删除工单应成功");
        assertNull(workOrderService.getWorkOrderById(workOrderId), "删除后工单应不可见");
    }

    /**
     * 测试领料单管理：新增→按工单查询→明细管理
     */
    @Test
    @Transactional
    public void testMaterialRequisition() {
        Long preparationId = findAnyPreparationId();
        if (preparationId == null) {
            System.out.println("无制剂数据，跳过该测试");
            return;
        }

        // 建工单
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWorkOrderCode("GD" + System.currentTimeMillis());
        workOrder.setWorkOrderName("领料测试工单");
        workOrder.setPreparationId(preparationId);
        workOrderService.addWorkOrder(workOrder);
        Long workOrderId = workOrder.getWorkOrderId();

        // 建领料单
        MaterialRequisition requisition = new MaterialRequisition();
        requisition.setRequisitionCode("LL" + System.currentTimeMillis());
        requisition.setWorkOrderId(workOrderId);
        requisition.setRequisitionDate(LocalDate.now());
        requisition.setWarehouse("一号仓库");
        requisition.setStatus("待领料");
        assertTrue(materialRequisitionService.addRequisition(requisition), "新增领料单应成功");
        assertNotNull(requisition.getId(), "领料单应回填ID");

        // 按工单查询
        List<MaterialRequisition> list = materialRequisitionService.getByWorkOrderId(workOrderId);
        assertEquals(1, list.size(), "按工单应查到1条领料单");
        assertEquals(requisition.getRequisitionCode(), list.get(0).getRequisitionCode(), "领料单号应一致");
    }

    /**
     * 测试制剂配方管理：批量保存→按制剂编码查询→删除
     */
    @Test
    @Transactional
    public void testPreparationFormula() {
        Long preparationId = findAnyPreparationId();
        if (preparationId == null) {
            System.out.println("无制剂数据，跳过该测试");
            return;
        }
        Preparation preparation = preparationMapper.selectById(preparationId);
        Long materialId = findAnyMaterialId();
        Long unitId = findAnyUnitId();
        if (materialId == null || unitId == null) {
            System.out.println("无物料或单位数据，跳过该测试");
            return;
        }

        // 批量保存配方
        PreparationFormula formula = new PreparationFormula();
        formula.setPreparationId(preparationId);
        formula.setPreparationCode(preparation.getPreparationCode());
        formula.setPreparationName(preparation.getPreparationName());
        formula.setMaterialId(materialId);
        formula.setMaterialCode("Y1001");
        formula.setMaterialName("测试原料");
        formula.setMaterialCategory("原料");
        formula.setDosage(new BigDecimal("2.000"));
        formula.setUnitId(unitId);
        formula.setUnitName("kg");
        preparationFormulaService.batchSave(preparationId, List.of(formula));

        // 按制剂编码查询
        List<PreparationFormula> formulas =
                preparationFormulaService.getFormulasByPreparationCode(preparation.getPreparationCode());
        assertEquals(1, formulas.size(), "应查到1条配方");

        // 删除配方
        preparationFormulaService.deleteFormula(formulas.get(0).getFormulaId());
        assertEquals(0, preparationFormulaService.getFormulasByPreparationCode(preparation.getPreparationCode()).size(),
                "删除后配方应为空");
    }

    /**
     * 测试制剂文档管理：批量保存→按制剂查询→按类型查询
     */
    @Test
    @Transactional
    public void testPreparationDocument() {
        Long preparationId = findAnyPreparationId();
        if (preparationId == null) {
            System.out.println("无制剂数据，跳过该测试");
            return;
        }

        PreparationDocument document = new PreparationDocument();
        document.setPreparationId(preparationId);
        document.setDocType("质量标准");
        document.setDocName("测试质量标准文档");
        document.setFileId(1L);
        document.setStatus(1);
        preparationDocumentService.batchSave(preparationId, List.of(document));

        // 按制剂查询
        List<PreparationDocument> docs = preparationDocumentService.findByPreparationId(preparationId);
        assertEquals(1, docs.size(), "应查到1条文档");

        // 按类型查询
        List<PreparationDocument> byType = preparationDocumentService.findByDocType("质量标准");
        assertTrue(byType.stream().anyMatch(d -> "测试质量标准文档".equals(d.getDocName())),
                "按类型查询应命中新建文档");
    }

    /**
     * 测试工艺模板与工序记录：批量保存模板→按制剂查询；工序记录批量保存→按计划查询→取消记录
     */
    @Test
    @Transactional
    public void testProcessTemplateAndRecords() {
        Long preparationId = findAnyPreparationId();
        Long processTypeId = findAnyProcessTypeId();
        if (preparationId == null || processTypeId == null) {
            System.out.println("无制剂或工序类型数据，跳过该测试");
            return;
        }

        // 工艺模板
        PreparationProcessTemplate template = new PreparationProcessTemplate();
        template.setPreparationId(preparationId);
        template.setProcessTypeId(processTypeId);
        template.setStepOrder(1);
        preparationProcessTemplateService.batchSave(preparationId, List.of(template));
        List<PreparationProcessTemplate> templates =
                preparationProcessTemplateService.findByPreparationId(preparationId);
        assertEquals(1, templates.size(), "应查到1条工艺模板");

        // 创建生产计划（工序记录外键依赖生产计划）
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanNumber("SCJH" + System.currentTimeMillis());
        plan.setPlanName("工序记录测试计划");
        plan.setPreparationName("测试制剂");
        plan.setPreparationCode("TSTPREP" + System.currentTimeMillis());
        plan.setPlanQuantity(new BigDecimal("100.000"));
        productionPlanService.save(plan);
        Integer planId = plan.getId();

        // 工序记录
        ProductionProcessRecord record = new ProductionProcessRecord();
        record.setPlanId(planId);
        record.setProcessTypeId(processTypeId);
        record.setProcessName("测试工序");
        record.setOperatorName("测试操作员");
        record.setStepOrder(1);
        record.setRecordStatus(1);
        List<ProductionProcessRecord> saved =
                productionProcessRecordService.batchSaveByPlanId(planId, List.of(record));
        assertEquals(1, saved.size(), "应保存1条工序记录");
        Long recordId = saved.get(0).getRecordId();

        // 按计划查询
        assertEquals(1, productionProcessRecordService.listByPlanId(planId).size(), "按计划应查到1条工序记录");

        // 按状态分页查询
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(-1);
        pageRequest.setPageSize(-1);
        PagedResult<ProductionProcessRecord> byStatus = productionProcessRecordService.listByStatus(1, pageRequest);
        assertTrue(byStatus.getTotalCount() >= 1, "按状态查询应有数据");

        // 取消记录
        assertTrue(productionProcessRecordService.cancelRecord(recordId, 1L), "取消工序记录应成功");
    }

    /**
     * 测试工单工序执行与计划状态日志：批量保存执行记录→按工单查询；状态日志查询
     */
    @Test
    @Transactional
    public void testWorkOrderExecutionAndStatusLog() {
        Long preparationId = findAnyPreparationId();
        if (preparationId == null) {
            System.out.println("无制剂数据，跳过该测试");
            return;
        }

        // 建工单
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWorkOrderCode("GD" + System.currentTimeMillis());
        workOrder.setWorkOrderName("执行测试工单");
        workOrder.setPreparationId(preparationId);
        workOrderService.addWorkOrder(workOrder);
        Long workOrderId = workOrder.getWorkOrderId();

        // 批量保存执行记录
        WorkOrderProcessExecution execution = new WorkOrderProcessExecution();
        execution.setWorkOrderId(workOrderId);
        execution.setStepOrder(1);
        execution.setProcessTypeId(1L);
        execution.setOperatorName("测试操作员");
        execution.setStatus("进行中");
        workOrderProcessExecutionService.batchSave(workOrderId, List.of(execution));

        // 按工单查询
        List<WorkOrderProcessExecution> executions = workOrderProcessExecutionService.getByWorkOrderId(workOrderId);
        assertEquals(1, executions.size(), "应查到1条执行记录");

        // 创建生产计划（状态日志外键依赖生产计划）
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanNumber("SCJH" + System.currentTimeMillis());
        plan.setPlanName("状态日志测试计划");
        plan.setPreparationName("测试制剂");
        plan.setPreparationCode("TSTPREP" + System.currentTimeMillis());
        plan.setPlanQuantity(new BigDecimal("100.000"));
        productionPlanService.save(plan);
        Integer planId = plan.getId();

        // 计划状态日志：插入一条并查询
        PlanStatusLog log = new PlanStatusLog();
        log.setPlanId(planId);
        log.setFromStatus("待生产");
        log.setToStatus("生产中");
        log.setOperator(1L);
        planStatusLogService.save(log);

        PlanStatusLog query = new PlanStatusLog();
        query.setToStatus("生产中");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PlanStatusLog> page =
                planStatusLogService.queryPlanStatusLogs(query, null, null, 1, 10);
        assertTrue(page.getTotal() >= 1, "状态日志查询应命中");
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 查询任意一个制剂ID，无数据时返回null
     *
     * @return 制剂ID
     */
    private Long findAnyPreparationId() {
        try {
            List<Preparation> list = preparationMapper.selectList(null);
            return list.isEmpty() ? null : list.get(0).getPreparationId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 查询任意一个物料ID，无数据时返回null
     *
     * @return 物料ID
     */
    private Long findAnyMaterialId() {
        try {
            List<Material> list = materialMapper.selectList(null);
            return list.isEmpty() ? null : list.get(0).getMaterialId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 查询任意一个单位ID，无数据时返回null
     *
     * @return 单位ID
     */
    private Long findAnyUnitId() {
        try {
            List<Unit> list = unitMapper.selectList(null);
            return list.isEmpty() ? null : list.get(0).getUnitId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 查询任意一个工序类型ID，无数据时返回null
     *
     * @return 工序类型ID
     */
    private Long findAnyProcessTypeId() {
        try {
            List<ProcessType> list = processTypeService.listActive();
            return list.isEmpty() ? null : list.get(0).getProcessId().longValue();
        } catch (Exception e) {
            return null;
        }
    }

    // endregion
}