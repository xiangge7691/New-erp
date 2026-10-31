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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 看板 V2 冒烟测试（只读，不写库）
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DashboardV2SmokeTest {

    private static final String RANGE = "?startMonth=2026-08&endMonth=2026-10";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JwtConfig jwtConfig;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void smoke() throws Exception {
        JsonNode metrics = get("/api/dashboard/metrics" + RANGE).path("data");
        JsonNode charts = get("/api/dashboard/charts" + RANGE).path("data");
        JsonNode actualDetails = get("/api/dashboard/metrics/details/actual-cost" + RANGE).path("data");
        JsonNode estimatedDetails = get("/api/dashboard/metrics/details/estimated-cost" + RANGE).path("data");
        JsonNode tree = get("/api/dashboard/details/tree" + RANGE).path("data");

        JsonNode card = metrics.path("cost").path("actualCost");
        System.out.println("@@CARD_ACTUAL@@" + card);
        System.out.println("@@BALANCE@@" + charts.path("production").path("balance"));

        // 测试库无区间数据时跳过（避免环境污染导致误报）
        Assumptions.assumeTrue(actualDetails.path("details").size() > 0,
            "测试库 2026-08~2026-10 无看板数据，跳过冒烟");

        // 1. 卡片实际成本 = 物料 + 水 + 电 + 气 + 其他
        Assertions.assertEquals(0, card.path("actualCost").decimalValue().compareTo(
            card.path("materialAmount").decimalValue()
                .add(card.path("waterAmount").decimalValue())
                .add(card.path("electricityAmount").decimalValue())
                .add(card.path("gasAmount").decimalValue())
                .add(card.path("otherAmount").decimalValue())), "卡片实际成本拆分不平");

        // 2. 明细合计 = 卡片
        BigDecimal detailTotal = BigDecimal.ZERO;
        BigDecimal materialTotal = BigDecimal.ZERO;
        for (JsonNode row : actualDetails.path("details")) {
            detailTotal = detailTotal.add(row.path("actualCost").decimalValue());
            materialTotal = materialTotal.add(row.path("materialAmount").decimalValue());
            BigDecimal split = row.path("rawMaterialAmount").decimalValue()
                .add(row.path("auxiliaryMaterialAmount").decimalValue())
                .add(row.path("packagingAmount").decimalValue());
            Assertions.assertEquals(0, split.compareTo(row.path("materialAmount").decimalValue()),
                "明细行原辅包拆分不平: " + row.path("workOrderCode").asText() + " split=" + split
                    + " material=" + row.path("materialAmount").decimalValue());
            BigDecimal rowActual = row.path("materialAmount").decimalValue()
                .add(row.path("waterAmount").decimalValue())
                .add(row.path("electricityAmount").decimalValue())
                .add(row.path("gasAmount").decimalValue())
                .add(row.path("otherAmount").decimalValue());
            Assertions.assertEquals(0, rowActual.compareTo(row.path("actualCost").decimalValue()),
                "明细行实际成本不平: " + row.path("workOrderCode").asText());
        }
        Assertions.assertEquals(0, detailTotal.compareTo(card.path("actualCost").decimalValue()),
            "实际成本明细合计 != 卡片 " + detailTotal + " vs " + card.path("actualCost").decimalValue());
        Assertions.assertEquals(0, materialTotal.compareTo(card.path("materialAmount").decimalValue()),
            "物料合计 != 卡片 " + materialTotal + " vs " + card.path("materialAmount").decimalValue());

        // 3. 月度构成：material = 原料 + 辅料 + 包材
        BigDecimal structureTotal = BigDecimal.ZERO;
        for (JsonNode item : charts.path("costStructure")) {
            BigDecimal material = item.path("material").decimalValue();
            BigDecimal split = item.path("rawMaterial").decimalValue()
                .add(item.path("auxiliary").decimalValue())
                .add(item.path("packaging").decimalValue());
            Assertions.assertEquals(0, material.compareTo(split), "月度构成物料拆分不平 " + item.path("month"));
            structureTotal = structureTotal.add(material)
                .add(item.path("water").decimalValue())
                .add(item.path("electricity").decimalValue())
                .add(item.path("gas").decimalValue())
                .add(item.path("other").decimalValue());
        }
        Assertions.assertEquals(0, structureTotal.compareTo(card.path("actualCost").decimalValue()),
            "月度构成合计 != 卡片 " + structureTotal);

        // 4. 趋势偏差自洽
        for (JsonNode item : charts.path("costTrend")) {
            BigDecimal estimated = item.path("estimatedCost").decimalValue();
            BigDecimal actual = item.path("actualCost").decimalValue();
            Assertions.assertEquals(0, item.path("costVariance").decimalValue()
                .compareTo(estimated.subtract(actual)), "趋势偏差不平 " + item.path("month"));
        }

        // 5. 生产平衡：已交付 + 待生产 = 计划批数
        JsonNode balance = charts.path("production").path("balance");
        Assertions.assertEquals(balance.path("planCount").asInt(),
            balance.path("fullyDeliveredPlanCount").asInt() + balance.path("pendingPlanCount").asInt(),
            "生产平衡批数不平");

        // 6. 成本构成按品种：合计 = 卡片实际成本
        BigDecimal preparationTotal = BigDecimal.ZERO;
        for (JsonNode item : charts.path("costStructureByPreparation")) {
            preparationTotal = preparationTotal.add(item.path("totalAmount").decimalValue());
        }
        Assertions.assertEquals(0, preparationTotal.compareTo(card.path("actualCost").decimalValue()),
            "品种成本构成合计 != 卡片 " + preparationTotal);

        // 7. 批次偏差排行：按偏差率降序、预计成本 > 0
        JsonNode ranks = charts.path("batchCostVarianceRank");
        System.out.println("@@RANK_SIZE@@" + ranks.size());
        BigDecimal previous = null;
        for (JsonNode rank : ranks) {
            Assertions.assertTrue(rank.path("estimatedCost").decimalValue().signum() > 0, "排行含预计成本为 0 的批次");
            BigDecimal rate = rank.path("costVarianceRate").decimalValue();
            if (previous != null) {
                Assertions.assertTrue(previous.compareTo(rate) >= 0, "排行未按偏差率降序");
            }
            previous = rate;
        }

        // 8. 树：月份偏差自洽 + 批次单位成本/周期字段
        List<String> treeBatchSample = new ArrayList<>();
        for (JsonNode month : tree.path("months")) {
            Assertions.assertEquals(0, month.path("costVariance").decimalValue().compareTo(
                month.path("estimatedCost").decimalValue().subtract(month.path("actualCost").decimalValue())),
                "树月份偏差不平 " + month.path("month"));
            for (JsonNode preparation : month.path("preparations")) {
                for (JsonNode batch : preparation.path("batches")) {
                    if (batch.path("productionDays").isNumber()) {
                        treeBatchSample.add(batch.path("workOrderCode").asText()
                            + "=" + batch.path("productionDays").asInt());
                    }
                }
            }
        }
        System.out.println("@@TREE_PRODUCTION_DAYS@@" + treeBatchSample);

        // 9. 预估成本明细：单位预计成本取快照、周期字段存在
        int estimatedRows = 0;
        int snapshotRows = 0;
        for (JsonNode row : estimatedDetails.path("details")) {
            estimatedRows++;
            if (row.path("unitEstimatedCost").isNumber()) {
                snapshotRows++;
            }
        }
        Assertions.assertTrue(estimatedRows > 0, "预估成本明细为空");
        Assertions.assertEquals(estimatedRows, snapshotRows, "存在未取到快照价的预估成本明细");

        // 10. 实际成本明细：单位实际成本与单位偏差
        for (JsonNode row : actualDetails.path("details")) {
            if (row.path("unitActualCost").isNumber() && row.path("unitEstimatedCost").isNumber()) {
                Assertions.assertEquals(0, row.path("unitCostVariance").decimalValue().compareTo(
                    row.path("unitEstimatedCost").decimalValue()
                        .subtract(row.path("unitActualCost").decimalValue())),
                    "单位成本偏差不平 " + row.path("workOrderCode").asText());
            }
        }

        System.out.println("@@OK@@ actual=" + card.path("actualCost").decimalValue()
            + " other=" + card.path("otherAmount").decimalValue()
            + " rank=" + ranks.size()
            + " prepStructure=" + charts.path("costStructureByPreparation").size());
    }

    private JsonNode get(String path) throws Exception {
        String token = JwtHelper.generateToken("1", "root", new String[] {"admin"},
            jwtConfig.getSecretKey(), jwtConfig.getIssuer(), jwtConfig.getAudience(), 30);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        String body = rest.exchange("http://localhost:" + port + path, HttpMethod.GET,
            new HttpEntity<>(headers), String.class).getBody();
        JsonNode node = mapper.readTree(body);
        Assertions.assertTrue(node.path("success").asBoolean(true), "接口失败: " + path + " -> " + body);
        return node;
    }
}
