package com.tonghui.erp.Common.Dto.Quality;

import com.tonghui.erp.Data.Entity.ReleaseReview;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审核放行数据传输对象
 * <p>
 * 继承ReleaseReview实体，用于扩展审核放行的附加展示信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReleaseReviewDto extends ReleaseReview {

}