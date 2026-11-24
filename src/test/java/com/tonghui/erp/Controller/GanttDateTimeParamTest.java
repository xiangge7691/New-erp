package com.tonghui.erp.Controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 甘特图时间参数兼容测试：同时接受 "yyyy-MM-dd HH:mm:ss" 与 ISO "yyyy-MM-dd'T'HH:mm:ss"
 */
@SpringBootTest
@AutoConfigureMockMvc
class GanttDateTimeParamTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ganttShouldAcceptSpaceAndIsoDateTime() throws Exception {
        String loginBody = "{\"userName\":\"root\",\"password\":\"root\"}";
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(loginResponse, "$.data.token");
        String auth = "Bearer " + token;

        // 空格式：yyyy-MM-dd HH:mm:ss
        mockMvc.perform(get("/api/scheduling/gantt")
                        .param("view", "task")
                        .param("from", "2026-11-01 11:11:02")
                        .param("to", "2026-11-16 11:11:02")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // ISO 格式：yyyy-MM-dd'T'HH:mm:ss
        mockMvc.perform(get("/api/scheduling/gantt")
                        .param("view", "task")
                        .param("from", "2026-11-01T11:11:02")
                        .param("to", "2026-11-16T11:11:02")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 甘特图筛选参数：任务号、设备、房间条件应正常绑定并参与查询
     */
    @Test
    void ganttShouldAcceptTaskEquipmentAndRoomFilters() throws Exception {
        String loginBody = "{\"userName\":\"root\",\"password\":\"root\"}";
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(loginResponse, "$.data.token");
        String auth = "Bearer " + token;

        // 不存在的任务号：查询正常且结果为空
        mockMvc.perform(get("/api/scheduling/gantt")
                        .param("view", "task")
                        .param("from", "2026-11-01 00:00:00")
                        .param("to", "2026-11-30 23:59:59")
                        .param("taskNo", "NO-SUCH-TASK-XYZ")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        // 设备ID/房间名称组合筛选：查询正常
        mockMvc.perform(get("/api/scheduling/gantt")
                        .param("view", "equipment")
                        .param("from", "2026-11-01 00:00:00")
                        .param("to", "2026-11-30 23:59:59")
                        .param("equipmentId", "999999")
                        .param("roomName", "NO-SUCH-ROOM-XYZ")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
