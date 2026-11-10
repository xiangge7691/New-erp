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

/**
 * 生产质量三页面接口冒烟测试（只读，不写库）
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class QualityPagesSmokeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JwtConfig jwtConfig;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void productSummaryShouldReturnChain() throws Exception {
        JsonNode list = get("/api/product-summary/batches?pageIndex=0&pageSize=10").path("data");
        int count = list.path("items").size();
        Assumptions.assumeTrue(count > 0, "测试库无工单数据，跳过产品汇总冒烟");
        JsonNode first = null;
        for (JsonNode item : list.path("items")) {
            if (item.path("workOrderId").asLong() > 0) {
                first = item;
                break;
            }
        }
        Assumptions.assumeTrue(first != null, "测试库无已下达工单，跳过产品详情冒烟");
        long workOrderId = first.path("workOrderId").asLong();

        JsonNode detail = get("/api/product-summary/batches/" + workOrderId).path("data");
        Assertions.assertEquals(workOrderId, detail.path("batch").path("workOrderId").asLong());
        JsonNode stages = detail.path("stages");
        Assertions.assertEquals(10, stages.size(), "全链路节点数量应为 10");
        String[] expectedKeys = {"plan", "purchase", "acceptance", "workOrder", "pick",
                "production", "inspection", "finishedInbound", "release", "finishedOutbound"};
        for (int i = 0; i < expectedKeys.length; i++) {
            Assertions.assertEquals(expectedKeys[i], stages.get(i).path("key").asText(),
                    "第 " + (i + 1) + " 个节点编码不正确");
        }
        Assertions.assertEquals(10, first.path("stageStates").size(), "列表环节状态数量应为 10");
        Assertions.assertTrue(detail.path("slots").size() > 0, "槽位统计为空");
    }

    @Test
    public void productSummaryOverviewCardsPlanDetailAndCsvShouldReturn() throws Exception {
        JsonNode overview = get("/api/product-summary/overview").path("data");
        Assertions.assertTrue(overview.has("verdict"), "概览缺少结论");
        Assertions.assertTrue(overview.has("kpis"), "概览缺少 KPI");
        Assertions.assertTrue(overview.has("health"), "概览缺少文件健康度");
        Assertions.assertTrue(overview.path("preparations").isArray(), "概览缺少制剂卡片");
        Assertions.assertTrue(overview.path("verdict").path("recordCount").asInt() > 0, "概览记录数为空");
        Assertions.assertTrue(overview.path("health").path("allSlots").isArray(), "概览缺少槽位口径");

        JsonNode list = get("/api/product-summary/batches?pageIndex=0&pageSize=20").path("data");
        JsonNode items = list.path("items");
        Assumptions.assumeTrue(items.size() > 0, "测试库无产品汇总记录，跳过产品扩展接口冒烟");
        Assertions.assertTrue(items.get(0).has("recordKey"), "列表缺少记录键");
        Assertions.assertTrue(items.get(0).path("stageStates").isArray(), "列表缺少环节状态");

        JsonNode cards = get("/api/product-summary/cards").path("data");
        Assertions.assertTrue(cards.isArray(), "卡片接口未返回数组");
        Assertions.assertTrue(cards.size() > 0, "卡片接口为空");

        for (JsonNode item : items) {
            long planId = item.path("planId").asLong();
            if (item.path("workOrderId").asLong() <= 0 && planId > 0) {
                JsonNode plan = get("/api/product-summary/plans/" + planId).path("data");
                Assertions.assertEquals(planId, plan.path("batch").path("planId").asLong());
                Assertions.assertTrue(plan.path("stages").isArray(), "计划详情缺少链路节点");
                break;
            }
        }

        byte[] csv = postCsv("/api/product-summary/export",
                "{\"scope\":\"FILTERED\",\"format\":\"CSV\",\"filters\":{\"onlyMissing\":false}}");
        String csvText = new String(csv, java.nio.charset.StandardCharsets.UTF_8);
        Assertions.assertTrue(csvText.contains("品种编码"), "导出清单缺少表头");
        Assertions.assertTrue(csvText.contains("状态"), "导出清单缺少状态列");

        byte[] zip = postCsv("/api/product-summary/export",
                "{\"scope\":\"FILTERED\",\"format\":\"ZIP\"}");
        Assertions.assertTrue(zip.length > 4 && zip[0] == 0x50 && zip[1] == 0x4b,
                "ZIP 导出未返回合法 zip");
    }

    @Test
    public void releaseReviewShouldReturnCompliance() throws Exception {
        JsonNode list = get("/api/release-review/batches?pageIndex=0&pageSize=10").path("data");
        int count = list.path("items").size();
        Assumptions.assumeTrue(count > 0, "测试库无批次数据，跳过审核放行冒烟");
        JsonNode first = list.path("items").get(0);
        long workOrderId = first.path("workOrderId").asLong();
        Assertions.assertTrue(first.has("fileRequired"), "批次列表缺少文件统计");
        Assertions.assertTrue(first.has("processTotal"), "批次列表缺少工序统计");

        JsonNode detail = get("/api/release-review/batches/" + workOrderId).path("data");
        Assertions.assertEquals(workOrderId, detail.path("batch").path("workOrderId").asLong());
        Assertions.assertTrue(detail.path("compliance").has("canRelease"), "详情缺少合规结论");
        Assertions.assertTrue(detail.path("fileChecklist").isArray(), "详情缺少文件完整性清单");

        // 方案 A：批次详情一次返回各单据表单字段
        if (detail.path("purchases").size() > 0) {
            JsonNode purchase = detail.path("purchases").get(0);
            Assertions.assertTrue(purchase.has("orderTime"), "采购表单缺少下单时间");
            Assertions.assertTrue(purchase.path("lines").isArray(), "采购表单缺少明细数组");
        }
        if (detail.path("acceptances").size() > 0) {
            JsonNode acceptance = detail.path("acceptances").get(0);
            Assertions.assertTrue(acceptance.has("acceptor"), "验收表单缺少验收人");
            Assertions.assertTrue(acceptance.path("details").isArray(), "验收表单缺少明细数组");
        }
        if (detail.path("instructions").size() > 0) {
            JsonNode instruction = detail.path("instructions").get(0);
            Assertions.assertTrue(instruction.has("preparation"), "生产指令缺少品种");
            Assertions.assertTrue(instruction.has("processRoute"), "生产指令缺少工艺路线");
        }
        if (detail.path("requisitions").size() > 0) {
            Assertions.assertTrue(detail.path("requisitions").get(0).path("lines").isArray(),
                    "领料单缺少明细数组");
        }
        if (detail.path("returns").size() > 0) {
            JsonNode returned = detail.path("returns").get(0);
            Assertions.assertTrue(returned.path("lines").isArray(), "退库单缺少明细数组");
            Assertions.assertTrue(returned.has("stockOutCode"), "退库单缺少关联出库单号");
        }
        if (detail.path("processes").size() > 0) {
            Assertions.assertTrue(detail.path("processes").get(0).has("roomName"), "工序表单缺少房间");
        }
        if (detail.path("deviations").size() > 0) {
            JsonNode deviation = detail.path("deviations").get(0);
            Assertions.assertTrue(deviation.has("occurTime"), "偏差表单缺少发生时间");
            Assertions.assertTrue(deviation.has("description"), "偏差表单缺少描述");
        }
        for (String group : java.util.List.of("requests", "samplings", "inspections")) {
            if (detail.path(group).size() > 0) {
                JsonNode item = detail.path(group).get(0);
                Assertions.assertTrue(item.hasNonNull("status"), group + " 单据状态为空");
                Assertions.assertTrue(item.has("relatedPlanCode"), group + " 缺少关联计划号字段");
            }
        }
    }

    @Test
    public void releaseVarietiesAndDetailFieldsShouldReturn() throws Exception {
        JsonNode varieties = get("/api/release-review/batches/varieties").path("data");
        Assertions.assertTrue(varieties.isArray(), "品种下拉不是数组");

        JsonNode list = get("/api/release-review/batches?pageIndex=0&pageSize=10").path("data");
        JsonNode items = list.path("items");
        Assumptions.assumeTrue(items.size() > 0, "测试库无批次数据，跳过审核放行字段冒烟");
        long workOrderId = items.get(0).path("workOrderId").asLong();
        JsonNode detail = get("/api/release-review/batches/" + workOrderId).path("data");
        Assertions.assertTrue(detail.path("batch").has("planNumber"), "详情缺少计划号");
        Assertions.assertTrue(detail.path("batch").has("releaseStatus"), "详情缺少放行状态");
        Assertions.assertTrue(detail.path("release").isObject(), "详情 release 不是对象");
        for (JsonNode item : detail.path("fileChecklist")) {
            Assertions.assertNotEquals("PREPARATION", item.path("bizType").asText(),
                    "放行清单不应包含品种资料");
        }
    }

    @Test
    public void releaseExportShouldReturnCsv() throws Exception {
        byte[] csv = postCsv("/api/release-review/batches/export", "{\"status\":\"待审核\"}");
        Assertions.assertNotNull(csv, "导出内容为空");
        String text = new String(csv, java.nio.charset.StandardCharsets.UTF_8);
        Assertions.assertTrue(text.contains("品种编码"), "导出 CSV 缺少表头");
        Assertions.assertTrue(text.contains("放行状态"), "导出 CSV 缺少放行状态列");
    }

    @Test
    public void inspectionTaskDetailShouldReturnSixSteps() throws Exception {
        JsonNode list = get("/api/inspection/tasks?pageIndex=0&pageSize=10").path("data");
        JsonNode items = list.path("items");
        Assumptions.assumeTrue(items.size() > 0, "测试库无检验任务，跳过检验任务冒烟");
        long id = items.get(0).path("id").asLong();
        JsonNode detail = get("/api/inspection/tasks/" + id).path("data");
        Assertions.assertEquals(6, detail.path("steps").size(), "检验任务步骤不是 6 个");
    }

    @Test
    public void inspectionBatchesShouldGroupTasks() throws Exception {
        JsonNode list = get("/api/inspection/batches?pageIndex=0&pageSize=10").path("data");
        JsonNode items = list.path("items");
        Assumptions.assumeTrue(items.size() > 0, "测试库无检验任务，跳过检验批次冒烟");
        JsonNode first = items.get(0);
        Assertions.assertTrue(first.has("taskCount"), "批次缺少任务数");
        Assertions.assertTrue(first.path("tasks").isArray(), "批次缺少任务明细");
        long workOrderId = first.path("workOrderId").asLong();
        if (workOrderId > 0) {
            JsonNode batch = get("/api/inspection/batches/" + workOrderId).path("data");
            Assertions.assertEquals(workOrderId, batch.path("workOrderId").asLong());
            Assertions.assertTrue(batch.path("tasks").isArray(), "批次详情缺少任务明细");
        }
    }

    @Test
    public void inspectionBoardStatsShouldReturnStepCounts() throws Exception {
        JsonNode stats = get("/api/inspection/batches/stats").path("data");
        Assertions.assertTrue(stats.has("batchTotal"), "检验看板缺少批次总数");
        Assertions.assertTrue(stats.has("todoBatches"), "检验看板缺少待办数");
        Assertions.assertEquals(6, stats.path("stepCounts").size(), "检验看板环节数不是 6");

        // 任务三态之和应等于任务总数
        int taskTotal = stats.path("taskTotal").asInt();
        int todoTasks = stats.path("todoTasks").asInt();
        int inspectingTasks = stats.path("inspectingTasks").asInt();
        int completedTasks = stats.path("completedTasks").asInt();
        Assertions.assertEquals(taskTotal, todoTasks + inspectingTasks + completedTasks,
                "任务三态之和应等于任务总数");

        // 批次三态之和应等于批次总数
        int batchTotal = stats.path("batchTotal").asInt();
        int todoBatches = stats.path("todoBatches").asInt();
        int inspectingBatches = stats.path("inspectingBatches").asInt();
        int completedBatches = stats.path("completedBatches").asInt();
        Assertions.assertEquals(batchTotal, todoBatches + inspectingBatches + completedBatches,
                "批次三态之和应等于批次总数");

        // 六步任务数之和加已完成任务数应等于任务总数
        int stepTaskSum = 0;
        for (JsonNode step : stats.path("stepCounts")) {
            stepTaskSum += step.path("taskCount").asInt();
        }
        Assertions.assertEquals(taskTotal, stepTaskSum + completedTasks,
                "六步任务数之和加已完成任务数应等于任务总数");

        // 三态口径与环节计数应与任务列表的 current_step 分布一致（已作废任务不计入）
        JsonNode tasks = get("/api/inspection/tasks?pageIndex=0&pageSize=2000").path("data");
        Assumptions.assumeTrue(tasks.path("totalCount").asInt() <= 2000,
                "测试库任务数超过 2000，跳过全量对账");
        int expectedTodo = 0;
        int expectedInspecting = 0;
        int expectedCompleted = 0;
        java.util.Map<String, Integer> expectedStepCounts = new java.util.HashMap<>();
        for (JsonNode task : tasks.path("items")) {
            String taskStatus = task.path("taskStatus").asText();
            if ("已作废".equals(taskStatus) || "已归档".equals(taskStatus)) {
                continue;
            }
            String step = task.path("currentStep").asText();
            if ("DONE".equals(step)) {
                expectedCompleted++;
            } else if ("SAMPLING".equals(step) || "INSPECTION".equals(step)
                    || "RETENTION".equals(step) || "DESTRUCTION".equals(step)) {
                expectedInspecting++;
            } else {
                expectedTodo++;
            }
            if (!"DONE".equals(step) && !step.isEmpty()) {
                expectedStepCounts.merge(step, 1, Integer::sum);
            }
        }
        Assertions.assertEquals(expectedTodo, todoTasks, "待办任务数口径不一致");
        Assertions.assertEquals(expectedInspecting, inspectingTasks, "检验中任务数口径不一致");
        Assertions.assertEquals(expectedCompleted, completedTasks, "已完成任务数口径不一致");
        for (JsonNode step : stats.path("stepCounts")) {
            int expected = expectedStepCounts.getOrDefault(step.path("stepCode").asText(), 0);
            Assertions.assertEquals(expected, step.path("taskCount").asInt(),
                    step.path("stepName").asText() + "环节任务数口径不一致");
        }
    }

    @Test
    public void releaseBoardStatsAndBlockersShouldReturn() throws Exception {
        JsonNode stats = get("/api/release-review/batches/stats").path("data");
        Assertions.assertTrue(stats.has("total"), "放行看板缺少总数");
        Assertions.assertTrue(stats.has("pendingReview"), "放行看板缺少待审核数");
        Assertions.assertTrue(stats.has("blocked"), "放行看板缺少阻塞数");

        JsonNode list = get("/api/release-review/batches?pageIndex=0&pageSize=10").path("data");
        if (list.path("items").size() > 0) {
            Assertions.assertTrue(list.path("items").get(0).has("blockers"), "放行列表缺少阻塞项");
        }
    }

    private byte[] postCsv(String path, String json) {
        String token = JwtHelper.generateToken("1", "root", new String[] {"admin"},
            jwtConfig.getSecretKey(), jwtConfig.getIssuer(), jwtConfig.getAudience(), 30);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.ResponseEntity<byte[]> response = rest.exchange(
            "http://localhost:" + port + path,
            HttpMethod.POST,
            new HttpEntity<>(json, headers),
            byte[].class);
        Assertions.assertEquals(200, response.getStatusCode().value(), "导出接口失败: " + path);
        return response.getBody();
    }

    private JsonNode get(String path) throws Exception {
        String token = JwtHelper.generateToken("1", "root", new String[] {"admin"},
            jwtConfig.getSecretKey(), jwtConfig.getIssuer(), jwtConfig.getAudience(), 30);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        String body = rest.exchange("http://localhost:" + port + path, HttpMethod.GET,
            new HttpEntity<>(headers), String.class).getBody();
        JsonNode node = mapper.readTree(body);
        Assertions.assertEquals(200, node.path("code").asInt(-1), "接口失败: " + path + " -> " + body);
        return node;
    }
}
