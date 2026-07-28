package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.ProcessType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 工艺类型数据访问Mapper接口
 */
public interface ProcessTypeMapper extends BaseMapper<ProcessType> {

    /**
     * 根据工序编码物理删除已软删除的记录（释放唯一键约束）
     *
     * @param processCode 工序编码
     * @return 删除的记录数
     */
    @Delete("DELETE FROM process_type WHERE process_code = #{processCode} AND is_deleted = 1")
    int physicalDeleteByProcessCode(@Param("processCode") String processCode);

    /**
     * 根据工序名称物理删除已软删除的记录（释放唯一键约束）
     *
     * @param processName 工序名称
     * @return 删除的记录数
     */
    @Delete("DELETE FROM process_type WHERE process_name = #{processName} AND is_deleted = 1")
    int physicalDeleteByProcessName(@Param("processName") String processName);
}




