package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.AcceptanceResendLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 验收明细重新发货操作留痕 Mapper
 * <p>提供 acceptance_resend_log 表的基础 CRUD 能力</p>
 */
@Mapper
public interface AcceptanceResendLogMapper extends BaseMapper<AcceptanceResendLog> {
}
