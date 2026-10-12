package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.DestructionRecordDetail;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 销毁记录明细表数据访问Mapper接口
 * <p>
 * 提供销毁记录明细的基础CRUD及更新重建所需的物理删除方法
 * </p>
 */
public interface DestructionRecordDetailMapper extends BaseMapper<DestructionRecordDetail> {

    /**
     * 按主表ID物理删除全部明细行
     * <p>
     * 更新主表时明细整体重建：先物理删除旧行再整单重插。
     * 必须使用原生DELETE（非MP的delete(wrapper)），因全局软删除开启后
     * MP的delete会被改写为UPDATE is_deleted=1，导致重复插入时数据累积
     * </p>
     *
     * @param destructionId 销毁记录主表ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM destruction_record_detail WHERE destruction_id = #{destructionId}")
    int physicalDeleteByDestructionId(@Param("destructionId") Long destructionId);
}
