package com.tonghui.erp.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonghui.erp.Common.Config.JwtConfig;
import com.tonghui.erp.Common.utils.JwtHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 请检单自动生成检验任务集成测试
 * <p>
 * 验证请检单创建成功后自动生成检验任务（完成请检环节、推进到检验计划），
 * 且同一工单+品类+批号下的多张请检单各自生成独立任务。
 * 测试写入测试库，结束后物理清理产生的数据。
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class InspectionTaskAutoCreateTest {

    // region 测试依赖
    // ===================================
    // 测试依赖
    // ===================================

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 创建请检单后应自动生成检验任务，且多张请检单生成多条任务
     */
    @Test
    public void createRequestShouldAutoGenerateInspectionTask() throws Exception {
        Map<String, Object> workOrder = jdbcTemplate.queryForMap(
                "SELECT work_order_id, work_order_code, batch_number, preparation_code, preparation_name "
                        + "FROM work_order WHERE is_deleted = 0 AND batch_number IS NOT NULL "
                        + "AND batch_number <> '' ORDER BY work_order_id DESC LIMIT 1");
        Assumptions.assumeTrue(!workOrder.isEmpty(), "测试库无可用工单，跳过请检单自动生成任务测试");

        long workOrderId = ((Number) workOrder.get("work_order_id")).longValue();
        String workOrderCode = String.valueOf(workOrder.get("work_order_code"));
        String batchNumber = String.valueOf(workOrder.get("batch_number"));
        String preparationCode = String.valueOf(workOrder.get("preparation_code"));
        String preparationName = String.valueOf(workOrder.get("preparation_name"));
        String marker = String.valueOf(System.currentTimeMillis());

        List<Long> requestIds = new ArrayList<>();
        List<Long> taskIds = new ArrayList<>();
        try {
            // 同一工单+品类+批号下连续创建两张请检单（不同工序）
            long firstRequestId = createRequest(workOrderId, workOrderCode, preparationCode, preparationName,
                    batchNumber, "自动生成任务测试工序A-" + marker);
            requestIds.add(firstRequestId);
            long firstTaskId = taskIdOfRequest(firstRequestId);
            taskIds.add(firstTaskId);
            assertTaskGenerated(firstTaskId, batchNumber);

            long secondRequestId = createRequest(workOrderId, workOrderCode, preparationCode, preparationName,
                    batchNumber, "自动生成任务测试工序B-" + marker);
            requestIds.add(secondRequestId);
            long secondTaskId = taskIdOfRequest(secondRequestId);
            taskIds.add(secondTaskId);
            assertTaskGenerated(secondTaskId, batchNumber);

            Assertions.assertNotEquals(firstTaskId, secondTaskId,
                    "同一工单+品类+批号下的两张请检单应生成两条独立任务");
        } finally {
            for (Long taskId : taskIds) {
                jdbcTemplate.update("DELETE FROM inspection_task_step WHERE task_id = ?", taskId);
                jdbcTemplate.update("DELETE FROM inspection_task WHERE id = ?", taskId);
            }
            for (Long requestId : requestIds) {
                jdbcTemplate.update("DELETE FROM inspection_request WHERE id = ?", requestId);
            }
        }
    }

    /**
     * 生产原料请检单没有工单，也应生成检验任务，且只出现在任务视图
     */
    @Test
    public void createRawMaterialRequestShouldGenerateTaskWithoutWorkOrder() throws Exception {
        String marker = String.valueOf(System.currentTimeMillis());
        String materialName = "自动测试原料" + marker;
        String batchNumber = "MAT" + marker;

        List<Long> requestIds = new ArrayList<>();
        List<Long> taskIds = new ArrayList<>();
        try {
            long requestId = createRawMaterialRequest(materialName, batchNumber);
            requestIds.add(requestId);
            long taskId = taskIdOfRequest(requestId);
            taskIds.add(taskId);

            Map<String, Object> task = jdbcTemplate.queryForMap(
                    "SELECT work_order_id, work_order_code, item_category, batch_no, object_name, "
                            + "current_step, task_status, retention_required, destruction_required "
                            + "FROM inspection_task WHERE id = ?", taskId);
            Assertions.assertNull(task.get("work_order_id"), "生产原料任务不应关联工单");
            Assertions.assertNull(task.get("work_order_code"), "生产原料任务不应带工单编号");
            Assertions.assertEquals("生产原料", task.get("item_category"));
            Assertions.assertEquals(batchNumber, task.get("batch_no"));
            Assertions.assertEquals(materialName, task.get("object_name"));
            Assertions.assertEquals("PLAN", task.get("current_step"), "任务应推进到检验计划环节");
            Assertions.assertEquals("进行中", task.get("task_status"));
            Assertions.assertEquals(0, ((Number) task.get("retention_required")).intValue(),
                    "生产原料任务不要求留样");
            Assertions.assertEquals(0, ((Number) task.get("destruction_required")).intValue(),
                    "生产原料任务不要求销毁");

            // 任务视图可以查到
            JsonNode tasks = get("/api/inspection/tasks?keyword=" + batchNumber + "&pageIndex=0&pageSize=20");
            boolean foundInTaskView = false;
            for (JsonNode item : tasks.path("data").path("items")) {
                if (item.path("id").asLong() == taskId) {
                    foundInTaskView = true;
                }
            }
            Assertions.assertTrue(foundInTaskView, "任务视图应包含生产原料任务");

            // 批次视图按工单聚合，不包含无工单的生产原料任务
            JsonNode batches = get("/api/inspection/batches?keyword=" + batchNumber + "&pageIndex=0&pageSize=20");
            Assertions.assertEquals(0, batches.path("data").path("items").size(),
                    "批次视图不应包含无工单的生产原料任务");
        } finally {
            for (Long taskId : taskIds) {
                jdbcTemplate.update("DELETE FROM inspection_task_step WHERE task_id = ?", taskId);
                jdbcTemplate.update("DELETE FROM inspection_task WHERE id = ?", taskId);
            }
            for (Long requestId : requestIds) {
                jdbcTemplate.update("DELETE FROM inspection_request WHERE id = ?", requestId);
            }
        }
    }

    // endregion

    // region 测试工具
    // ===================================
    // 测试工具
    // ===================================

    /**
     * 调用请检单创建接口，返回请检单ID
     */
    private long createRequest(long workOrderId, String workOrderCode, String preparationCode,
                               String preparationName, String batchNumber, String processName) throws Exception {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("workOrderId", workOrderId);
        payload.put("workOrderCode", workOrderCode);
        payload.put("preparationCode", preparationCode);
        payload.put("preparationName", preparationName);
        payload.put("inspectionItemName", preparationName);
        payload.put("itemCategory", "中间产品");
        payload.put("batchNumber", batchNumber);
        payload.put("processName", processName);
        payload.put("configQuantity", "100");
        payload.put("requestDepartment", "自动测试部门");
        payload.put("requester", "自动测试员");
        payload.put("requestTime", "2026-11-09T10:00:00");
        payload.put("inspectionItems", "性状、水分");
        JsonNode node = exchange(HttpMethod.POST, "/api/inspection-request", mapper.writeValueAsString(payload));
        Assertions.assertEquals(200, node.path("code").asInt(-1), "创建请检单失败: " + node);
        long requestId = node.path("data").path("id").asLong();
        Assertions.assertTrue(requestId > 0, "请检单未返回ID: " + node);
        return requestId;
    }

    /**
     * 查询请检单对应的检验任务ID
     */
    private long taskIdOfRequest(long requestId) {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM inspection_task WHERE request_ref_id = ?", Long.class, requestId);
        Assertions.assertEquals(1, ids.size(), "请检单应生成且仅生成一条检验任务: requestId=" + requestId);
        return ids.get(0);
    }

    /**
     * 校验任务已生成、请检环节完成并推进到检验计划
     */
    private void assertTaskGenerated(long taskId, String batchNumber) {
        Map<String, Object> task = jdbcTemplate.queryForMap(
                "SELECT current_step, task_status, item_category, batch_no, retention_required "
                        + "FROM inspection_task WHERE id = ?", taskId);
        Assertions.assertEquals("PLAN", task.get("current_step"), "任务应推进到检验计划环节");
        Assertions.assertEquals("进行中", task.get("task_status"), "任务状态应为进行中");
        Assertions.assertEquals("中间产品", task.get("item_category"));
        Assertions.assertEquals(batchNumber, task.get("batch_no"));
        Assertions.assertEquals(0, ((Number) task.get("retention_required")).intValue(),
                "中间产品任务不要求留样");

        String requestStep = jdbcTemplate.queryForObject(
                "SELECT step_status FROM inspection_task_step WHERE task_id = ? AND step_code = 'REQUEST'",
                String.class, taskId);
        Assertions.assertEquals("已完成", requestStep, "请检环节应已完成");
        String planStep = jdbcTemplate.queryForObject(
                "SELECT step_status FROM inspection_task_step WHERE task_id = ? AND step_code = 'PLAN'",
                String.class, taskId);
        Assertions.assertEquals("进行中", planStep, "检验计划环节应进行中");
    }

    /**
     * 调用请检单创建接口，返回请检单ID（生产原料，无工单）
     */
    private long createRawMaterialRequest(String materialName, String batchNumber) throws Exception {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("inspectionItemName", materialName);
        payload.put("itemCategory", "生产原料");
        payload.put("batchNumber", batchNumber);
        payload.put("spec", "25kg/袋");
        payload.put("configQuantity", "100");
        payload.put("requestDepartment", "自动测试部门");
        payload.put("requester", "自动测试员");
        payload.put("requestTime", "2026-11-09T11:00:00");
        payload.put("inspectionItems", "性状、水分");
        JsonNode node = exchange(HttpMethod.POST, "/api/inspection-request", mapper.writeValueAsString(payload));
        Assertions.assertEquals(200, node.path("code").asInt(-1), "创建生产原料请检单失败: " + node);
        long requestId = node.path("data").path("id").asLong();
        Assertions.assertTrue(requestId > 0, "请检单未返回ID: " + node);
        return requestId;
    }

    /**
     * 带JWT令牌GET接口
     */
    private JsonNode get(String path) throws Exception {
        JsonNode node = exchange(HttpMethod.GET, path, null);
        Assertions.assertEquals(200, node.path("code").asInt(-1), "接口失败: " + path + " -> " + node);
        return node;
    }

    /**
     * 带JWT令牌调用接口
     */
    private JsonNode exchange(HttpMethod method, String path, String json) throws Exception {
        String token = JwtHelper.generateToken("1", "root", new String[] {"admin"},
                jwtConfig.getSecretKey(), jwtConfig.getIssuer(), jwtConfig.getAudience(), 30);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = rest.exchange("http://localhost:" + port + path, method,
                new HttpEntity<>(json, headers), String.class).getBody();
        return mapper.readTree(body);
    }

    // endregion
}
