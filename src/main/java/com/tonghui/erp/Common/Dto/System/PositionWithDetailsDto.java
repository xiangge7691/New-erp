package com.tonghui.erp.Common.Dto.System;

import com.tonghui.erp.Data.Entity.PersonnelFile;
import com.tonghui.erp.Data.Entity.Position;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 职位包含关联人员的扩展数据传输对象
 * <p>
 * 在职位基础上扩展了关联的人事档案列表，用于展示完整的职位详情及任职人员信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PositionWithDetailsDto extends Position {

    /**
     * 该职位关联的人事档案列表
     */
    private List<PersonnelFile> personnelFiles;
}
