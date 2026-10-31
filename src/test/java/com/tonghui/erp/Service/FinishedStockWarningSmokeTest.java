package com.tonghui.erp.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonghui.erp.Common.Config.JwtConfig;
import com.tonghui.erp.Common.utils.JwtHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 成品库存预警接口冒烟测试
 * <p>
 * 只读调用预警列表、汇总和首页待办接口，校验接口可访问、
 * 返回结构正确且首页待办已包含“成品库存预警”类型
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FinishedStockWarningSmokeTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /**
     * 随机端口
     */
    @LocalServerPort
    private int port;

    /**
     * REST 测试客户端
     */
    @Autowired
    private TestRestTemplate rest;

    /**
     * JWT 配置
     */
    @Autowired
    private JwtConfig jwtConfig;

    /**
     * JSON 解析器
     */
    private final ObjectMapper mapper = new ObjectMapper();

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 冒烟校验预警列表、汇总与首页待办类型
     *
     * @throws Exception 请求或解析异常
     */
    @Test
    public void smoke() throws Exception {
        JsonNode list = get("/api/finished-stock-warning/list?pageIndex=0&pageSize=5");
        JsonNode listData = list.path("data");
        assertTrue(listData.path("items").isArray(), "预警列表 items 应为数组");
        assertTrue(listData.path("totalCount").asLong() >= listData.path("items").size(),
                "预警列表总记录数不应小于当前页数据量");

        JsonNode summary = get("/api/finished-stock-warning/summary").path("data");
        assertTrue(summary.path("avgMonths").asInt() > 0, "统计月数应大于0");
        assertTrue(summary.path("warningMonths").asInt() > 0, "预警月数应大于0");
        assertNotNull(summary.path("avgStartMonth").asText(), "统计起始月份不能为空");
        System.out.println("@@FINISHED_STOCK_SUMMARY@@" + summary);

        JsonNode todos = get("/api/dashboard/todos").path("data");
        assertTrue(todos.path("typeCounts").has("成品库存预警"),
                "首页待办 typeCounts 应包含“成品库存预警”");
    }

    /**
     * 发送带 JWT 的 GET 请求并解析响应
     *
     * @param path 请求路径
     * @return 响应 JSON
     * @throws Exception 请求异常
     */
    private JsonNode get(String path) throws Exception {
        String token = JwtHelper.generateToken("1", "root", new String[] {"admin"},
                jwtConfig.getSecretKey(), jwtConfig.getIssuer(), jwtConfig.getAudience(), 30);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        String body = rest.exchange("http://localhost:" + port + path, HttpMethod.GET,
                new HttpEntity<>(headers), String.class).getBody();
        JsonNode node = mapper.readTree(body);
        assertTrue(node.path("code").asInt(200) == 200, "接口失败: " + path + " -> " + body);
        return node;
    }

    // endregion
}
