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
     * 查询到期及已过期的审核记录，按供应商分组取下次审核日期最新的一条
     *
     * @param warningDate 截止日期
     * @return 到期的审核记录列表
     */
    @Select("SELECT * FROM ( " +
            "SELECT *, ROW_NUMBER() OVER (PARTITION BY supplier_id ORDER BY next_audit_date DESC) as rn " +
            "FROM supplier_audit " +
            "WHERE next_audit_date <= #{warningDate} " +
            "AND is_deleted = 0 " +
            ") ranked " +
            "WHERE rn = 1 " +
            "ORDER BY next_audit_date ASC")
    List<SupplierAudit> selectExpiringAudits(@Param("warningDate") Date warningDate);
}
