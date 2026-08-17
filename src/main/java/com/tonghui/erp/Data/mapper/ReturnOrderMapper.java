package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.ReturnOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 退库单数据访问Mapper接口
 */
public interface ReturnOrderMapper extends BaseMapper<ReturnOrder> {

    /**
     * 查询指定前缀（TK-YYYYMMDD）下最大的退库单号
     * <p>
     * 使用原生SQL绕过全局软删除过滤，避免与已软删除单号冲突
     * </p>
     *
     * @param prefix 退库单号前缀（如 TK-20260817）
     * @return 最大退库单号，无记录返回null
     */
    @Select("SELECT return_no FROM return_order WHERE return_no LIKE CONCAT(#{prefix}, '%') ORDER BY return_no DESC LIMIT 1")
    String selectMaxReturnNoByPrefix(@Param("prefix") String prefix);
}