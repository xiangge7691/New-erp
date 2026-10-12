package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Quality.DeviationRecordSaveDto;
import com.tonghui.erp.Common.Dto.Quality.DeviationRecordWithAttachmentsDto;
import com.tonghui.erp.Common.Dto.Quality.WorkOrderOptionDto;
import com.tonghui.erp.Data.Entity.DeviationRecord;
import com.tonghui.erp.Data.Entity.FileInfo;

import java.util.List;

/**
 * 偏差记录服务接口
 * <p>
 * 提供偏差记录的CRUD、条件分页查询、偏差编号生成及关联生产任务下拉查询等业务能力，
 * 用于质量检验模块的GMP偏差台账管理
 * </p>
 */
public interface DeviationRecordService extends IService<DeviationRecord> {

    /**
     * 生成偏差编号
     * <p>
     * 编号格式：PC-YYYYMMDD-NNN，基于当天最大序号递增，查询绕过软删除过滤
     * </p>
     *
     * @return 生成的唯一偏差编号
     */
    String generateCode();

    /**
     * 条件分页查询偏差记录列表
     *
     * @param keyword      关键字（可空，模糊匹配偏差编号或偏差标题）
     * @param workOrderCode 生产任务编号（可空，模糊匹配）
     * @param startDate    实际发生时间起始（可空，格式yyyy-MM-dd）
     * @param endDate      实际发生时间结束（可空，格式yyyy-MM-dd）
     * @param pageRequest  分页参数
     * @return 分页结果
     */
    PagedResult<DeviationRecord> search(String keyword, String workOrderCode,
                                        String startDate, String endDate, PageRequestDto pageRequest);

    /**
     * 查询偏差记录详情（含附件列表与创建人/更新人姓名）
     *
     * @param id 偏差记录ID
     * @return 偏差记录详情，不存在时返回null
     */
    DeviationRecordWithAttachmentsDto getDetail(Long id);

    /**
     * 新增偏差记录（自动生成编号并快照生产任务字段、绑定附件）
     *
     * @param dto 保存请求体
     * @return 新增的偏差记录
     */
    DeviationRecord create(DeviationRecordSaveDto dto);

    /**
     * 修改偏差记录（编号不可改，重快照生产任务字段、同步附件）
     *
     * @param id  偏差记录ID
     * @param dto 保存请求体
     * @return 修改后的偏差记录
     */
    DeviationRecord update(Long id, DeviationRecordSaveDto dto);

    /**
     * 查询偏差记录附件列表
     *
     * @param id 偏差记录ID
     * @return 附件文件列表
     */
    List<FileInfo> getAttachments(Long id);

    /**
     * 生产任务下拉查询（关键字模糊匹配任务编号/制剂名称）
     *
     * @param keyword 关键字（可空）
     * @return 下拉选项列表（最多20条）
     */
    List<WorkOrderOptionDto> searchWorkOrders(String keyword);
}
