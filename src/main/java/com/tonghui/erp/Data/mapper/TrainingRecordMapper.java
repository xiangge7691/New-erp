package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.TrainingRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
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
     *
     * @param year 年份（如2026）
     * @return 最大序号
     */
    @Select("SELECT IFNULL(MAX(CAST(SUBSTRING(training_no, LENGTH(#{year}) + 7) AS UNSIGNED)), 0) " +
            "FROM training_record " +
            "WHERE training_no LIKE CONCAT('TRAIN-', #{year}, '-%') AND is_deleted = 0")
    Integer getMaxSeqByYear(@Param("year") String year);

    /**
     * 查询指定时间段内到期的培训记录
     *
     * @param today       今天日期
     * @param warningDate 截止日期
     * @return 到期的培训记录列表
     */
    @Select("SELECT * FROM training_record " +
            "WHERE next_training_date BETWEEN #{today} AND #{warningDate} " +
            "AND is_deleted = 0 " +
            "ORDER BY next_training_date ASC")
    List<TrainingRecord> selectExpiringTrainings(@Param("today") Date today, @Param("warningDate") Date warningDate);
}
