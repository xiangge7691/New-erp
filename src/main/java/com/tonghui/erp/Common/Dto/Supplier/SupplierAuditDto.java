package com.tonghui.erp.Common.Dto.Supplier;

import com.tonghui.erp.Data.Entity.SupplierAudit;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 供应商审核记录数据传输对象
 * <p>
 * 继承SupplierAudit实体，可用于扩展审核记录的附加信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierAuditDto extends SupplierAudit {

}
