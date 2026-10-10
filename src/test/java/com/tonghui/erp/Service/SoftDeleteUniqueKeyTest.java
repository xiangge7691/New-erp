package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.Position;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 软删除与唯一约束冲突专项测试
 * <p>
 * 覆盖两类核心场景：
 * 1. SoftDeleteCleanHelper 的原生物理清理（绕过 MyBatis-Plus 逻辑删除 UPDATE 改写）
 * 2. 岗位编码（uk_position_code）软删除后复用 / 活动记录重复编码拦截
 * 全部用例使用事务回滚，不污染数据库
 * </p>
 */
@SpringBootTest
public class SoftDeleteUniqueKeyTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /**
     * 岗位服务
     */
    @Autowired
    private PositionService positionService;

    /**
     * 原生 JDBC（用于绕过软删除过滤，直接统计物理行）
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试软删除记录物理清理：软删除后记录仍占用唯一索引，清理后应被物理删除
     */
    @Test
    @Transactional
    public void testCleanByUniqueFieldPhysicallyRemovesSoftDeletedRows() {
        String code = "TSPOS" + (System.currentTimeMillis() % 100000000L);
        Position position = createPosition(code);

        // 软删除：唯一索引仍被占用
        assertTrue(positionService.removeById(position.getPositionId()), "软删除岗位应成功");
        assertEquals(1, countByCodeIncludingDeleted(code), "软删除后记录仍占用唯一索引");

        // 原生 DELETE 清理（而非 MP 的 UPDATE is_deleted=1）
        int cleaned = positionService.cleanSoftDeletedByPositionCode(code);
        assertEquals(1, cleaned, "应物理清理1条软删除记录");
        assertEquals(0, countByCodeIncludingDeleted(code), "清理后记录应被物理删除");
    }

    /**
     * 测试软删除编码复用：清理后同编码可重新保存，不再触发唯一键冲突
     */
    @Test
    @Transactional
    public void testSoftDeletedCodeCanBeReusedAfterClean() {
        String code = "TSPOS" + (System.currentTimeMillis() % 100000000L);
        Position first = createPosition(code);
        assertTrue(positionService.removeById(first.getPositionId()), "软删除岗位应成功");

        // 活动记录查询看不到软删除记录
        assertNull(positionService.getByPositionCode(code), "活动查询不应命中软删除记录");
        assertEquals(1, countByCodeIncludingDeleted(code), "软删除记录应仍占用唯一索引");

        // 模拟创建流程：先清理软删除记录，再保存同编码
        positionService.cleanSoftDeletedByPositionCode(code);
        Position reuse = new Position();
        reuse.setPositionCode(code);
        reuse.setPositionName("复用编码岗位");
        reuse.setStatus(1);
        assertDoesNotThrow(() -> positionService.save(reuse),
                "清理后保存同编码不应触发唯一键冲突");
        assertEquals(1, countByCodeIncludingDeleted(code), "物理清理后仅剩复用记录1行");
    }

    /**
     * 测试活动记录重复编码拦截：唯一索引对未删除行同样生效
     */
    @Test
    @Transactional
    public void testActiveDuplicateCodeRejectedByUniqueIndex() {
        String code = "TSPOS" + (System.currentTimeMillis() % 100000000L);
        createPosition(code);

        Position duplicate = new Position();
        duplicate.setPositionCode(code);
        duplicate.setPositionName("重复编码岗位");
        duplicate.setStatus(1);
        assertThrows(DuplicateKeyException.class, () -> positionService.save(duplicate),
                "活动记录重复编码应被唯一索引 uk_position_code 拦截");
    }

    // endregion

    // region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================

    /**
     * 创建测试岗位
     *
     * @param positionCode 岗位编码
     * @return 创建后的岗位实体（含回填的主键）
     */
    private Position createPosition(String positionCode) {
        Position position = new Position();
        position.setPositionCode(positionCode);
        position.setPositionName("测试岗位");
        position.setStatus(1);
        assertTrue(positionService.save(position), "创建测试岗位应成功");
        assertNotNull(position.getPositionId(), "创建后应回填岗位ID");
        return position;
    }

    /**
     * 统计指定编码的物理行数（绕过软删除过滤，唯一索引对所有行生效）
     *
     * @param positionCode 岗位编码
     * @return 物理行数
     */
    private int countByCodeIncludingDeleted(String positionCode) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `position` WHERE position_code = ?", Integer.class, positionCode);
        return count == null ? 0 : count;
    }

    // endregion
}
