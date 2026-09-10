package com.tonghui.erp.Common.Dto.Quality;

import com.tonghui.erp.Data.Entity.InspectionRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检验记录数据传输对象
 * <p>
 * 继承InspectionRecord实体，用于扩展检验记录的附加展示信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionRecordDto extends InspectionRecord {

}