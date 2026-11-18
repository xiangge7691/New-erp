package com.tonghui.erp.Controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 系数版本发布接口测试：走鉴权、参数绑定与操作日志切面
 */
@SpringBootTest
@AutoConfigureMockMvc
class CoefficientPublishApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Transactional
    void publishShouldSucceed() throws Exception {
        String loginBody = "{\"userName\":\"root\",\"password\":\"root\"}";
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(loginResponse, "$.data.token");

        String body = "{\"note\":\"接口冒烟\",\"processCoefficients\":{\"1\":1.1,\"2\":null},"
                + "\"processItems\":[{\"processTypeId\":3,\"coefficient\":1.2}],"
                + "\"personItems\":[{\"personnelId\":1,\"coefficient\":1.05}]}";
        mockMvc.perform(post("/api/performance/coefficient-versions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.versionNo").exists());

        // 数组结构兼容
        String arrayBody = "{\"processCoefficients\":[{\"processTypeId\":3,\"coefficient\":1.2}],"
                + "\"personCoefficients\":[{\"personnelId\":1,\"coefficient\":1.05}]}";
        mockMvc.perform(post("/api/performance/coefficient-versions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(arrayBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
