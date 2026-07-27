package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.DosageForm;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 剂型数据访问Mapper接口
 */
public interface DosageFormMapper extends BaseMapper<DosageForm> {

    /**
     * 根据剂型名称物理删除已软删除的记录（释放唯一键约束）
     *
     * @param dosageName 剂型名称
     * @return 删除的记录数
     */
    @Delete("DELETE FROM dosage_form WHERE dosage_name = #{dosageName} AND is_deleted = 1")
    int physicalDeleteByDosageName(@Param("dosageName") String dosageName);
}




