package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Performance.CoefficientPublishRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 系数版本发布冒烟测试（事务回滚，不落库）
 */
@SpringBootTest
class CoefficientPublishSmokeTest {

    @Autowired
    private PerformanceService performanceService;

    @Test
    @Transactional
    void publishShouldSucceed() {
        CoefficientPublishRequest request = new CoefficientPublishRequest();
        request.setNote("冒烟测试");
        var version = performanceService.publish(request);
        assertNotNull(version.getId());
        assertNotNull(version.getVersionNo());
    }
}
