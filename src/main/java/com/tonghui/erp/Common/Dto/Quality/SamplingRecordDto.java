package com.tonghui.erp.Common.Dto.Quality;

import com.tonghui.erp.Data.Entity.SamplingRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 取样记录数据传输对象
 * <p>
 * 继承SamplingRecord实体，用于扩展取样记录的附加展示信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SamplingRecordDto extends SamplingRecord {

}