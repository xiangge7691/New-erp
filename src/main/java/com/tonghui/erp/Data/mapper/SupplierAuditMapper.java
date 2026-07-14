package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.SupplierAudit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * 供应商审核记录数据访问Mapper接口
 */
@Mapper
public interface SupplierAuditMapper extends BaseMapper<SupplierAudit> {

    /**
     * 查询指定时间段内到期的审核记录
     *
     * @param today       今天日期
     * @param warningDate 截止日期
     * @return 到期的审核记录列表
     */
    @Select("SELECT * FROM supplier_audit " +
            "WHERE next_audit_date BETWEEN #{today} AND #{warningDate} " +
            "AND is_deleted = 0 " +
            "ORDER BY next_audit_date ASC")
    List<SupplierAudit> selectExpiringAudits(@Param("today") Date today, @Param("warningDate") Date warningDate);
}
