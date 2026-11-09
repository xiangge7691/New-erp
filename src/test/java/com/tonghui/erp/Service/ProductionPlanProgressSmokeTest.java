package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.ProductionPlanPurchaseProgressDto;
import com.tonghui.erp.Common.Dto.ProductionPlanWithRecordsDto;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 生产计划采购进度冒烟测试
 * <p>
 * 从测试库取一条生产计划，验证采购进度详情查询可正常执行，
 * 覆盖采购计划、订单、验收、入库等批量查询及新时间字段映射。
 * </p>
 */
@SpringBootTest
public class ProductionPlanProgressSmokeTest {

    /** 生产计划数据访问层 */
    @Autowired
    private ProductionPlanMapper productionPlanMapper;

    /** 生产计划采购进度服务 */
    @Autowired
    private ProductionPlanProgressService productionPlanProgressService;

    /** 生产计划服务 */
    @Autowired
    private ProductionPlanService productionPlanService;

    /**
     * 查询任意一条生产计划的采购进度详情
     */
    @Test
    void testGetPurchaseProgress() {
        List<ProductionPlan> plans = productionPlanMapper.selectList(
                new QueryWrapper<ProductionPlan>().last("LIMIT 1"));
        Assumptions.assumeFalse(plans.isEmpty(), "测试库暂无生产计划数据，跳过冒烟测试");

        ProductionPlanPurchaseProgressDto detail =
                productionPlanProgressService.getPurchaseProgress(plans.get(0).getId());

        assertNotNull(detail);
        assertNotNull(detail.getPurchaseSummary());
        assertNotNull(detail.getChains());
        // 每条链路都应有展示状态，供详情选项卡使用
        detail.getChains().forEach(chain -> assertNotNull(chain.getDisplayStatus()));
    }

    /**
     * 列表接口应填充采购摘要和逾期信息
     */
    @Test
    void testSearchWithDetailsFillPurchaseSummary() {
        PagedResult<ProductionPlanWithRecordsDto> result = productionPlanService.searchWithDetails(
                new ProductionPlan(), null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null,
                null, null,
                0, 5);

        assertNotNull(result);
        assertNotNull(result.getItems());
        result.getItems().forEach(item -> {
            assertNotNull(item.getPurchaseProgress());
            assertNotNull(item.getOverdue());
            // 列表“采购进度”列直接展示最慢链路状态
            assertNotNull(item.getPurchaseProgress().getDisplayStatus());
        });
    }
}
