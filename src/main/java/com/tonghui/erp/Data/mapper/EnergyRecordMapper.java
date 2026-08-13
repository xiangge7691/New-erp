package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.EnergyRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 能耗记录数据访问Mapper接口
 * <p>
 * 提供基础CRUD（继承 BaseMapper，自动过滤 is_deleted=0）及汇总统计原生SQL
 * </p>
 */
public interface EnergyRecordMapper extends BaseMapper<EnergyRecord> {

    /**
     * 按筛选条件统计能耗费用汇总（绕过逻辑删除过滤的字段，SQL 中手写 is_deleted=0 条件）
     * <p>
     * 返回总金额及各类型金额，任一列无数据时返回0
     * </p>
     *
     * @param month      月份（可为空）
     * @param energyType 能耗类型（可为空）
     * @return 汇总结果
     */
    @Select("SELECT " +
            "COALESCE(SUM(total_amount), 0) AS totalAmount, " +
            "COALESCE(SUM(CASE WHEN energy_type = '自来水' THEN total_amount ELSE 0 END), 0) AS waterAmount, " +
            "COALESCE(SUM(CASE WHEN energy_type = '电' THEN total_amount ELSE 0 END), 0) AS electricityAmount, " +
            "COALESCE(SUM(CASE WHEN energy_type = '燃气' THEN total_amount ELSE 0 END), 0) AS gasAmount " +
            "FROM energy_record " +
            "WHERE is_deleted = 0 " +
            "AND (#{month} IS NULL OR #{month} = '' OR month = #{month}) " +
            "AND (#{energyType} IS NULL OR #{energyType} = '' OR energy_type = #{energyType})")
    EnergySummary selectSummary(@Param("month") String month, @Param("energyType") String energyType);

    /**
     * 能耗汇总结果对象
     */
    class EnergySummary {

        /**
         * 总金额
         */
        private BigDecimal totalAmount;

        /**
         * 自来水费金额
         */
        private BigDecimal waterAmount;

        /**
         * 电费金额
         */
        private BigDecimal electricityAmount;

        /**
         * 燃气费金额
         */
        private BigDecimal gasAmount;

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }

        public BigDecimal getWaterAmount() {
            return waterAmount;
        }

        public void setWaterAmount(BigDecimal waterAmount) {
            this.waterAmount = waterAmount;
        }

        public BigDecimal getElectricityAmount() {
            return electricityAmount;
        }

        public void setElectricityAmount(BigDecimal electricityAmount) {
            this.electricityAmount = electricityAmount;
        }

        public BigDecimal getGasAmount() {
            return gasAmount;
        }

        public void setGasAmount(BigDecimal gasAmount) {
            this.gasAmount = gasAmount;
        }
    }
}
