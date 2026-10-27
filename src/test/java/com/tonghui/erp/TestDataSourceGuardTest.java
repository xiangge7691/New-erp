package com.tonghui.erp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 测试数据源安全守卫
 * <p>
 * 确保单元测试始终连接测试库 8.134.48.54/erp_db，
 * 禁止连接线上库 8.148.27.237/erp_db1，避免测试写入正式数据。
 * </p>
 */
@SpringBootTest
public class TestDataSourceGuardTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /**
     * 应用数据源（测试环境应被 src/test/resources/application.properties 覆盖为测试库）
     */
    @Autowired
    private DataSource dataSource;

    // endregion

    // region 测试方法
    // ===================================
    // 测试方法
    // ===================================

    /**
     * 校验测试数据源地址：必须为测试库，禁止线上库
     *
     * @throws Exception 获取连接失败
     */
    @Test
    public void testDataSourceIsNotProduction() throws Exception {
        assertNotNull(dataSource, "数据源不能为空");
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            System.out.println("测试实际连接: " + url);
            assertFalse(url.contains("8.148.27.237"), "测试禁止连接线上数据库 8.148.27.237，当前: " + url);
            assertFalse(url.contains("erp_db1"), "测试禁止连接线上数据库 erp_db1，当前: " + url);
            assertTrue(url.contains("8.134.48.54"), "测试应连接测试数据库 8.134.48.54，当前: " + url);
        }
    }

    // endregion
}
