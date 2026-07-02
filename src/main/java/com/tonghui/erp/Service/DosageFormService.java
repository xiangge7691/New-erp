package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.DosageForm.DosageFormWithDetailsDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.DosageForm;

/**
 * 药品剂型服务接口
 * <p>
 * 针对表【dosage_form(药品剂型分类表)】的数据库操作Service接口，提供药品剂型的增删改查等业务逻辑接口
 * </p>
 */
public interface DosageFormService extends IService<DosageForm> {
    
    //#region 剂型查询方法
    // ===================================
    // 剂型查询方法
    // ===================================
    
    /**
     * 根据剂型名称模糊查询（分页）
     * 
     * @param dosageName 剂型名称（模糊匹配），为空时查询所有
     * @param pageRequest 分页参数，包含页码和每页数量等信息
     * @return 分页结果，包含查询到的剂型列表和分页信息
     */
    PagedResult<DosageForm> searchByName(String dosageName, PageRequestDto pageRequest);
    
    Page<DosageForm> queryDosageForms(DosageForm dosageForm, int pageNum, int pageSize);

    PagedResult<DosageFormWithDetailsDto> searchWithDetails(DosageForm dosageForm, int pageNum, int pageSize);
    
    //#endregion
}
