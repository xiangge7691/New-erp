package com.tonghui.erp.Common.Dto.Quality;

import com.tonghui.erp.Data.Entity.RetainedSample;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 留样记录数据传输对象
 * <p>
 * 继承RetainedSample实体，用于扩展留样记录的附加展示信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RetainedSampleDto extends RetainedSample {

}