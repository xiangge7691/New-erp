package com.tonghui.erp.Common.Dto.Training;

import com.tonghui.erp.Data.Entity.TrainingRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 培训记录数据传输对象
 * <p>
 * 继承TrainingRecord实体，可用于扩展培训记录的附加信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TrainingRecordDto extends TrainingRecord {

}
