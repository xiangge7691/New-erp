package com.tonghui.erp.Common.Dto.Quality;

import com.tonghui.erp.Data.Entity.InspectionPlan;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检验计划数据传输对象
 * <p>
 * 继承InspectionPlan实体，用于扩展检验计划的附加展示信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionPlanDto extends InspectionPlan {

}