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
        JsonNode first = list.path("items").get(0);
        long workOrderId = first.path("workOrderId").asLong();
        Assertions.assertTrue(workOrderId > 0, "批次缺少工单ID");

        JsonNode detail = get("/api/product-summary/batches/" + workOrderId).path("data");
        Assertions.assertEquals(workOrderId, detail.path("batch").path("workOrderId").asLong());
        Assertions.assertTrue(detail.path("stages").size() >= 6, "全链路节点少于 6 个");
        Assertions.assertTrue(detail.path("slots").size() > 0, "槽位统计为空");
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
            JsonNode tasks = get("/api/inspection/batches/" + workOrderId + "/tasks").path("data");
            Assertions.assertTrue(tasks.isArray(), "批次任务接口未返回数组");
            JsonNode batch = get("/api/inspection/batches/" + workOrderId).path("data");
            Assertions.assertEquals(workOrderId, batch.path("workOrderId").asLong());
        }
    }

    @Test
    public void inspectionBoardStatsShouldReturnStepCounts() throws Exception {
        JsonNode stats = get("/api/inspection/batches/stats").path("data");
        Assertions.assertTrue(stats.has("batchTotal"), "检验看板缺少批次总数");
        Assertions.assertTrue(stats.has("todoBatches"), "检验看板缺少待办数");
        Assertions.assertEquals(6, stats.path("stepCounts").size(), "检验看板环节数不是 6");
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
