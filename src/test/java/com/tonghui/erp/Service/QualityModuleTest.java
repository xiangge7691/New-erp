package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.InspectionPlan;
import com.tonghui.erp.Data.Entity.SamplingRecord;
import com.tonghui.erp.Data.Entity.InspectionRecord;
import com.tonghui.erp.Data.Entity.ReleaseReview;
import com.tonghui.erp.Data.Entity.RetainedSample;
import com.tonghui.erp.Data.Entity.CleanInspectionRecord;
import com.tonghui.erp.Service.InspectionPlanService;
import com.tonghui.erp.Service.SamplingRecordService;
import com.tonghui.erp.Service.InspectionRecordService;
import com.tonghui.erp.Service.ReleaseReviewService;
import com.tonghui.erp.Service.RetainedSampleService;
import com.tonghui.erp.Service.CleanInspectionRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 质量域综合测试
 * <p>
 * 覆盖检验计划、抽样记录、检验记录、放行审核、留样记录、清洁检验等质量模块
 * 核心业务逻辑与自动编号生成，全部用例使用事务回滚，不污染数据库
 * </p>
 */
@SpringBootTest
public class QualityModuleTest {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 检验计划服务
     */
    @Autowired
    private InspectionPlanService inspectionPlanService;

    /**
     * 抽样记录服务
     */
    @Autowired
    private SamplingRecordService samplingRecordService;

    /**
     * 检验记录服务
     */
    @Autowired
    private InspectionRecordService inspectionRecordService;

    /**
     * 放行审核服务
     */
    @Autowired
    private ReleaseReviewService releaseReviewService;

    /**
     * 留样记录服务
     */
    @Autowired
    private RetainedSampleService retainedSampleService;

    /**
     * 清洁检验记录服务
     */
    @Autowired
    private CleanInspectionRecordService cleanInspectionRecordService;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试检验计划管理：自动编号→新增→唯一性校验→更新→查询
     */
    @Test
    @Transactional
    public void testInspectionPlanCrud() {
        // 自动编号格式校验
        String code = inspectionPlanService.generateCode();
        assertNotNull(code, "检验计划编号生成不应为空");
        assertTrue(code.startsWith("JH-"), "检验计划编号应以JH-开头");
        assertTrue(inspectionPlanService.isCodeUnique(code, null), "新编号唯一性校验应通过");

        // 新增检验计划
        InspectionPlan plan = new InspectionPlan();
        plan.setPlanCode(code);
        plan.setPlanPeriod("每年");
        plan.setInspectionType("出厂检验");
        plan.setObjectName("测试制剂");
        plan.setInspectionSummary("年度出厂检验计划");
        plan.setPlanTime(LocalDate.now());
        plan.setStatus("待检验");
        assertTrue(inspectionPlanService.save(plan), "新增检验计划应成功");
        Long planId = plan.getId();

        // 编号重复校验
        assertFalse(inspectionPlanService.isCodeUnique(code, null), "已存在编号唯一性校验应失败");
        assertTrue(inspectionPlanService.isCodeUnique(code, planId), "排除自身后唯一性校验应通过");

        // 更新并查询
        plan.setInspectionSummary("更新后的检验摘要");
        assertTrue(inspectionPlanService.updateById(plan), "更新检验计划应成功");
        InspectionPlan found = inspectionPlanService.getById(planId);
        assertEquals("更新后的检验摘要", found.getInspectionSummary(), "检验摘要应已更新");
        assertEquals(code, found.getPlanCode(), "编号应一致");
    }

    /**
     * 测试抽样记录管理：自动编号→新增→查询
     */
    @Test
    @Transactional
    public void testSamplingRecordCrud() {
        String code = samplingRecordService.generateCode();
        assertNotNull(code, "抽样记录编号生成不应为空");
        assertTrue(code.startsWith("QY-"), "抽样记录编号应以QY-开头");

        SamplingRecord record = new SamplingRecord();
        record.setSamplingCode(code);
        record.setObjectName("测试制剂");
        record.setBatchNo("BT-SAMPLE-001");
        record.setSamplingLocation("一号车间");
        record.setSamplingQuantity("100g");
        record.setSampler("测试人员");
        record.setSamplingTime(LocalDateTime.now());
        assertTrue(samplingRecordService.save(record), "新增抽样记录应成功");
        Long recordId = record.getId();

        // 唯一性校验
        assertFalse(samplingRecordService.isCodeUnique(code, null), "已存在编号唯一性校验应失败");
        assertTrue(samplingRecordService.isCodeUnique(code, recordId), "排除自身后唯一性校验应通过");

        // 查询验证
        SamplingRecord found = samplingRecordService.getById(recordId);
        assertEquals("BT-SAMPLE-001", found.getBatchNo(), "批号应一致");
    }

    /**
     * 测试检验记录管理：自动编号→新增→更新结论
     */
    @Test
    @Transactional
    public void testInspectionRecordCrud() {
        String code = inspectionRecordService.generateCode();
        assertNotNull(code, "检验记录编号生成不应为空");
        assertTrue(code.startsWith("JY-"), "检验记录编号应以JY-开头");

        InspectionRecord record = new InspectionRecord();
        record.setInspectionCode(code);
        record.setObjectName("测试制剂");
        record.setBatchNo("BT-INSP-001");
        record.setInspectionBasis("中国药典");
        record.setInspectionItem("含量测定");
        record.setInspector("检验员A");
        record.setReviewer("复核员B");
        record.setStartTime(LocalDateTime.now());
        record.setEndTime(LocalDateTime.now());
        record.setConclusion("合格");
        assertTrue(inspectionRecordService.save(record), "新增检验记录应成功");
        Long recordId = record.getId();

        // 更新结论为不合格
        record.setConclusion("不合格");
        assertTrue(inspectionRecordService.updateById(record), "更新检验记录应成功");
        assertEquals("不合格", inspectionRecordService.getById(recordId).getConclusion(), "结论应已更新");
        assertEquals(code, inspectionRecordService.getById(recordId).getInspectionCode(), "编号应一致");
    }

    /**
     * 测试放行审核管理：自动编号→新增→更新审核意见
     */
    @Test
    @Transactional
    public void testReleaseReviewCrud() {
        String code = releaseReviewService.generateCode();
        assertNotNull(code, "放行审核编号生成不应为空");
        assertTrue(code.startsWith("FX-"), "放行审核编号应以FX-开头");

        ReleaseReview review = new ReleaseReview();
        review.setReleaseCode(code);
        review.setObjectName("测试制剂");
        review.setBatchNo("BT-REL-001");
        review.setReleaseConclusion("合格");
        review.setReviewer("审核人A");
        review.setReviewTime(LocalDateTime.now());
        assertTrue(releaseReviewService.save(review), "新增放行审核应成功");
        Long reviewId = review.getId();

        review.setReviewOpinion("同意放行");
        assertTrue(releaseReviewService.updateById(review), "更新放行审核应成功");
        ReleaseReview found = releaseReviewService.getById(reviewId);
        assertEquals("同意放行", found.getReviewOpinion(), "审核意见应已更新");
        assertEquals(code, found.getReleaseCode(), "编号应一致");
    }

    /**
     * 测试留样记录管理：自动编号→新增→状态流转→过期销毁
     */
    @Test
    @Transactional
    public void testRetainedSampleCrud() {
        String code = retainedSampleService.generateCode();
        assertNotNull(code, "留样记录编号生成不应为空");
        assertTrue(code.startsWith("LY-"), "留样记录编号应以LY-开头");

        RetainedSample sample = new RetainedSample();
        sample.setRetainedCode(code);
        sample.setMaterialName("测试制剂");
        sample.setBatchNo("BT-RET-001");
        sample.setRetainedQuantity("50g");
        sample.setRetainedDate(LocalDate.now());
        sample.setExpiryDate(LocalDate.now().plusDays(180));
        sample.setStorageLocation("留样室A区");
        sample.setStatus("留样中");
        assertTrue(retainedSampleService.save(sample), "新增留样记录应成功");
        Long sampleId = sample.getId();

        // 状态流转
        sample.setStatus("已销毁");
        sample.setDestroyDate(LocalDate.now());
        assertTrue(retainedSampleService.updateById(sample), "更新留样状态应成功");
        RetainedSample found = retainedSampleService.getById(sampleId);
        assertEquals("已销毁", found.getStatus(), "留样状态应已销毁");
        assertNotNull(found.getDestroyDate(), "销毁日期应已填写");

        // 编号唯一性
        assertFalse(retainedSampleService.isCodeUnique(code, null), "已存在编号唯一性校验应失败");
    }

    /**
     * 测试清洁检验记录管理：新增→按洁净室查询
     */
    @Test
    @Transactional
    public void testCleanInspectionRecord() {
        CleanInspectionRecord record = new CleanInspectionRecord();
        record.setRoomId(1);
        record.setInspectionDate(LocalDate.now());
        record.setInspectionArea("洁净区A");
        record.setInspectionItem("沉降菌");
        record.setInspectionResult("合格");
        record.setInspector("检测员A");
        record.setNextInspectionDate(LocalDate.now().plusDays(7));
        assertTrue(cleanInspectionRecordService.save(record), "新增清洁检验记录应成功");
        Long recordId = record.getId();

        // 按洁净室查询
        List<CleanInspectionRecord> list = cleanInspectionRecordService.findByRoomId(1);
        assertTrue(list.stream().anyMatch(r -> r.getId().equals(recordId)), "按洁净室应查到新增记录");

        // 更新检验结果
        record.setInspectionResult("不合格");
        assertTrue(cleanInspectionRecordService.updateById(record), "更新检验结果应成功");
        assertEquals("不合格", cleanInspectionRecordService.getById(recordId).getInspectionResult(),
                "检验结果应已更新");
    }

    // endregion
}