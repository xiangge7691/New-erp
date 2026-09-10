package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.TrainingRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * 培训记录数据访问Mapper接口
 */
@Mapper
public interface TrainingRecordMapper extends BaseMapper<TrainingRecord> {

    /**
     * 获取当年最大培训序号
     * <p>
     * 统计包含软删除在内的所有记录序号，确保自动生成的编号不与任何
     * 已存在记录（含软删除记录）冲突，避免触发唯一索引冲突
     * </p>
     *
     * @param year 年份（如2026）
     * @return 最大序号
     */
    @Select("SELECT IFNULL(MAX(CAST(SUBSTRING(training_no, LENGTH(#{year}) + 8) AS UNSIGNED)), 0) " +
            "FROM training_record " +
            "WHERE training_no LIKE CONCAT('TRAIN-', #{year}, '-%')")
    Integer getMaxSeqByYear(@Param("year") String year);

    /**
     * 查询到期及已过期的培训记录，仅返回未处理的记录
     *
     * @param warningDate 截止日期
     * @return 到期的培训记录列表
     */
    @Select("SELECT * FROM training_record " +
            "WHERE next_training_date <= #{warningDate} " +
            "AND is_deleted = 0 " +
            "AND (reminder_status IS NULL OR reminder_status = 0) " +
            "ORDER BY next_training_date ASC")
    List<TrainingRecord> selectExpiringTrainings(@Param("warningDate") Date warningDate);

    /**
     * 根据培训编号物理删除已软删除的记录（释放唯一键约束）
     *
     * @param trainingNo 培训编号
     * @return 删除的记录数
     */
    @Delete("DELETE FROM training_record WHERE training_no = #{trainingNo} AND is_deleted = 1")
    int physicalDeleteByTrainingNo(@Param("trainingNo") String trainingNo);
}
