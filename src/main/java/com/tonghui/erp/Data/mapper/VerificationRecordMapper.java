package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.VerificationRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * 验证记录数据访问Mapper接口
 */
@Mapper
public interface VerificationRecordMapper extends BaseMapper<VerificationRecord> {

    /**
     * 查询到期及已过期的验证记录，排除不再提醒的记录
     *
     * @param warningDate 截止日期
     * @return 到期的验证记录列表
     */
    @Select("SELECT * FROM verification_record " +
            "WHERE next_verify_date <= #{warningDate} " +
            "AND is_deleted = 0 " +
            "AND (reminder_status IS NULL OR reminder_status != 2) " +
            "ORDER BY next_verify_date ASC")
    List<VerificationRecord> selectExpiringVerifications(@Param("warningDate") Date warningDate);
}
