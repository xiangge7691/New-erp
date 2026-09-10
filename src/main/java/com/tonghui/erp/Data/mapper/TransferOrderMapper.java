package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.TransferOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 调拨单数据访问Mapper接口
 */
public interface TransferOrderMapper extends BaseMapper<TransferOrder> {

    /**
     * 查询指定前缀（DB-YYYYMMDD）下最大的调拨单号
     * <p>
     * 使用原生SQL绕过全局软删除过滤，避免与已软删除单号冲突
     * </p>
     *
     * @param prefix 调拨单号前缀（如 DB-20260817）
     * @return 最大调拨单号，无记录返回null
     */
    @Select("SELECT transfer_no FROM transfer_order WHERE transfer_no LIKE CONCAT(#{prefix}, '%') ORDER BY transfer_no DESC LIMIT 1")
    String selectMaxTransferNoByPrefix(@Param("prefix") String prefix);
}