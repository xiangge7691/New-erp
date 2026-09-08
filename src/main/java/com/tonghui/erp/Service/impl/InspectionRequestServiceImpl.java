package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.InspectionRequest;
import com.tonghui.erp.Data.mapper.InspectionRequestMapper;
import com.tonghui.erp.Service.InspectionRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 请检记录业务服务实现类
 * <p>
 * 实现InspectionRequestService接口，请检记录的编号生成、唯一性校验、
 * 高级查询等业务逻辑
 * </p>
 */
@Service
public class InspectionRequestServiceImpl extends ServiceImpl<InspectionRequestMapper, InspectionRequest>
        implements InspectionRequestService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 序列号生成服务，用于自动生成请检编号 */
    @Autowired
    private SequenceServiceImpl sequenceService;

    // endregion

    // region 编号生成
    // ===================================
    // 编号生成
    // ===================================

    /**
     * 生成请检编号（格式 QJ-YYYYMMDD-NNN）
     *
     * @return 生成的唯一请检编号
     */
    @Override
    public String generateCode() {
        return sequenceService.generateInspectionRequestCode();
    }

    /**
     * 校验请检编号是否唯一（包含已软删除记录的编号也不可复用）
     *
     * @param code      请检编号
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 唯一返回true，否则返回false
     */
    @Override
    public boolean isCodeUnique(String code, Long excludeId) {
        QueryWrapper<InspectionRequest> wrapper = new QueryWrapper<>();
        wrapper.eq("inspection_code", code);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        return this.count(wrapper) == 0;
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询请检记录（支持多条件 + 分页）
     *
     * @param request           查询条件实体
     * @param keyword           关键字（模糊匹配请检编号/任务编号/被检物名称/批号/请检人）
     * @param processName       工序筛选
     * @param requestDepartment 请检部门筛选
     * @param requestTimeStart  请检时间起始
     * @param requestTimeEnd    请检时间结束
     * @param pageIndex         页码（从0开始）
     * @param pageSize          每页大小
     * @return 请检记录分页结果
     */
    @Override
    public Page<InspectionRequest> queryRequests(InspectionRequest request,
                                                  String keyword,
                                                  String processName,
                                                  String requestDepartment,
                                                  String requestTimeStart,
                                                  String requestTimeEnd,
                                                  int pageIndex,
                                                  int pageSize) {
        int actualPageIndex = pageIndex + 1;
        Page<InspectionRequest> page = new Page<>(actualPageIndex, pageSize);
        QueryWrapper<InspectionRequest> wrapper = new QueryWrapper<>();

        // 关键字模糊匹配
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like("inspection_code", keyword)
                    .or().like("work_order_code", keyword)
                    .or().like("inspection_item_name", keyword)
                    .or().like("batch_number", keyword)
                    .or().like("requester", keyword));
        }

        // 精确条件筛选
        if (request != null) {
            if (StringUtils.hasText(request.getInspectionCode())) {
                wrapper.like("inspection_code", request.getInspectionCode());
            }
            if (StringUtils.hasText(request.getWorkOrderCode())) {
                wrapper.like("work_order_code", request.getWorkOrderCode());
            }
            if (StringUtils.hasText(request.getPreparationCode())) {
                wrapper.like("preparation_code", request.getPreparationCode());
            }
            if (StringUtils.hasText(request.getPreparationName())) {
                wrapper.like("preparation_name", request.getPreparationName());
            }
            if (StringUtils.hasText(request.getInspectionItemName())) {
                wrapper.like("inspection_item_name", request.getInspectionItemName());
            }
            if (StringUtils.hasText(request.getBatchNumber())) {
                wrapper.like("batch_number", request.getBatchNumber());
            }
            if (StringUtils.hasText(request.getRequester())) {
                wrapper.like("requester", request.getRequester());
            }
            if (StringUtils.hasText(request.getInspectionPlanCode())) {
                wrapper.like("inspection_plan_code", request.getInspectionPlanCode());
            }
        }

        // 工序筛选
        if (StringUtils.hasText(processName)) {
            wrapper.eq("process_name", processName);
        }

        // 请检部门筛选
        if (StringUtils.hasText(requestDepartment)) {
            wrapper.eq("request_department", requestDepartment);
        }

        // 请检时间范围
        if (StringUtils.hasText(requestTimeStart)) {
            wrapper.ge("request_time", requestTimeStart);
        }
        if (StringUtils.hasText(requestTimeEnd)) {
            wrapper.le("request_time", requestTimeEnd);
        }

        // 按请检时间倒序
        wrapper.orderByDesc("request_time");

        return this.page(page, wrapper);
    }

    // endregion
}
