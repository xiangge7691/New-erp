package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.InspectionRequest;

/**
 * 请检记录业务服务接口
 * <p>
 * 提供请检记录的增删改查、编号生成、编号唯一性校验等功能
 * </p>
 */
public interface InspectionRequestService extends IService<InspectionRequest> {

    /**
     * 生成请检编号
     * <p>
     * 编号格式：QJ-YYYYMMDD-NNN，自动生成当天最大序号的下一个编号
     * </p>
     *
     * @return 生成的唯一请检编号
     */
    String generateCode();

    /**
     * 校验请检编号是否唯一
     *
     * @param code      请检编号
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 唯一返回true，否则返回false
     */
    boolean isCodeUnique(String code, Long excludeId);

    /**
     * 高级查询请检记录（支持多条件 + 分页）
     *
     * @param request       查询条件实体
     * @param keyword       关键字（对请检编号/任务编号/被检物名称/批号/请检人进行模糊匹配，可选）
     * @param processName   工序筛选（可选）
     * @param requestDepartment 请检部门筛选（可选）
     * @param requestTimeStart  请检时间起始（可选）
     * @param requestTimeEnd    请检时间结束（可选）
     * @param pageIndex     页码（从0开始）
     * @param pageSize      每页大小
     * @return 请检记录分页结果
     */
    Page<InspectionRequest> queryRequests(InspectionRequest request,
                                           String keyword,
                                           String processName,
                                           String requestDepartment,
                                           String requestTimeStart,
                                           String requestTimeEnd,
                                           int pageIndex,
                                           int pageSize);
}
