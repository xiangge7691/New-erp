package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Data.Entity.Duty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 职务信息模块综合测试
 * <p>
 * 覆盖职务的增删改查、职务编码唯一性校验（含软删除绕过）等核心业务逻辑，
 * 全部用例使用事务回滚，不污染数据库
 * </p>
 */
@SpringBootTest
public class DutyServiceTest {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 职务服务
     */
    @Autowired
    private DutyService dutyService;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试职务管理：创建职务→按编码分页查询→更新→删除
     */
    @Test
    @Transactional
    public void testDutyCrud() {
        String code = "TESTDUTY" + System.currentTimeMillis();
        Duty duty = new Duty();
        duty.setDutyCode(code);
        duty.setDutyName("测试职务");
        duty.setDutyDesc("测试职务描述");
        duty.setStatus(1);
        duty.setSortOrder(1);
        assertTrue(dutyService.save(duty), "创建职务应成功");
        Long dutyId = duty.getDutyId();
        assertNotNull(dutyId, "创建后应回填职务ID");

        // 分页查询应命中（按编码精确匹配）
        Page<Duty> page = dutyService.queryDuties(code, null, 0, 10);
        assertTrue(page.getTotal() >= 1, "按编码查询职务应命中");
        assertEquals(code, page.getRecords().get(0).getDutyCode(), "查询结果编码应一致");

        // 按状态筛选（启用）应命中
        Page<Duty> enabledPage = dutyService.queryDuties(null, 1, 0, 10);
        assertTrue(enabledPage.getRecords().stream().anyMatch(d -> d.getDutyId().equals(dutyId)),
                "启用状态筛选应包含新建职务");

        // 更新职务名称
        Duty update = new Duty();
        update.setDutyId(dutyId);
        update.setDutyName("更新后职务");
        assertTrue(dutyService.updateById(update), "更新职务应成功");
        Duty after = dutyService.getById(dutyId);
        assertEquals("更新后职务", after.getDutyName(), "职务名称应已更新");

        // 删除职务（软删除）
        assertTrue(dutyService.removeById(dutyId), "删除职务应成功");
        assertNull(dutyService.getById(dutyId), "删除后职务应不存在");
    }

    /**
     * 测试职务编码唯一性校验：
     * 1) 新增时重复编码返回不唯一
     * 2) 软删除后同编码仍不唯一（绕过软删除过滤，防唯一索引冲突）
     */
    @Test
    @Transactional
    public void testDutyCodeUnique() {
        String code = "TESTDUTYU" + System.currentTimeMillis();

        // 首次创建：编码唯一
        Duty duty = new Duty();
        duty.setDutyCode(code);
        duty.setDutyName("唯一性测试职务");
        duty.setStatus(1);
        assertTrue(dutyService.save(duty), "首次创建职务应成功");
        Long dutyId = duty.getDutyId();
        assertTrue(dutyService.isCodeUnique(code, dutyId), "排除自身时编码应唯一");

        // 新增第二条同编码：不唯一
        assertFalse(dutyService.isCodeUnique(code, null), "同编码在新增时应判定为不唯一");

        // 软删除第一条后，同编码仍不唯一（绕过软删除过滤）
        assertTrue(dutyService.removeById(dutyId), "软删除职务应成功");
        assertFalse(dutyService.isCodeUnique(code, null), "软删除后同编码仍应判定为不唯一（防唯一索引冲突）");
    }

    // endregion
}