package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.CheckOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 盘点单数据访问Mapper接口
 */
public interface CheckOrderMapper extends BaseMapper<CheckOrder> {

    /**
     * 查询指定前缀（PD-YYYYMMDD）下最大的盘点单号
     * <p>
     * 使用原生SQL绕过全局软删除过滤，避免与已软删除单号冲突
     * </p>
     *
     * @param prefix 盘点单号前缀（如 PD-20260817）
     * @return 最大盘点单号，无记录返回null
     */
    @Select("SELECT check_no FROM check_order WHERE check_no LIKE CONCAT(#{prefix}, '%') ORDER BY check_no DESC LIMIT 1")
    String selectMaxCheckNoByPrefix(@Param("prefix") String prefix);
}