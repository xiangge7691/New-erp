package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Quality.DestructionRecordSaveDto;
import com.tonghui.erp.Common.Dto.Quality.DestructionRecordWithDetailsDto;
import com.tonghui.erp.Common.Dto.Quality.RetainedSampleOptionDto;
import com.tonghui.erp.Data.Entity.DestructionRecord;
import com.tonghui.erp.Data.Entity.FileInfo;

import java.util.List;

/**
 * 销毁记录服务接口
 * <p>
 * 提供销毁记录（不合格品销毁/留样及稳定性考察产品销毁）的CRUD、条件分页查询、
 * 销毁单号生成、作废及关联留样下拉查询等业务能力
 * </p>
 * <p>
 * 作废语义：不提供删除接口，作废后禁止修改；更新时明细整体重建
 * </p>
 * <p>
 * 留样反写联动：仅「留样及稳定性考察产品销毁」类型生效，登记时置关联留样为已销毁，
 * 移除引用/类型变更/作废时回滚为留样中（仍被其他正常销毁单引用则不回滚）
 * </p>
 */
public interface DestructionRecordService extends IService<DestructionRecord> {

    /**
     * 生成销毁单号
     * <p>
     * 按记录类型选择前缀：不合格品销毁BPXH/留样及稳定性考察产品销毁LYXH，
     * 格式前缀-YYYYMMDD-NNN，基于当天最大序号递增，查询绕过软删除过滤
     * </p>
     *
     * @param recordType 记录类型（不合格品销毁/留样及稳定性考察产品销毁）
     * @return 生成的唯一销毁单号
     */
    String generateCode(String recordType);

    /**
     * 条件分页查询销毁记录列表（列表项回填创建人姓名createdByName及明细列表details）
     *
     * @param recordType 记录类型（可空，精确匹配）
     * @param keyword    关键字（可空，模糊匹配销毁单号）
     * @param batchNo    批号（可空，模糊匹配明细批号，存在该批号明细的主表命中）
     * @param status     状态（可空，精确匹配：正常/作废）
     * @param startDate  登记时间起始（可空，格式yyyy-MM-dd）
     * @param endDate    登记时间结束（可空，格式yyyy-MM-dd）
     * @param pageRequest 分页参数
     * @return 分页结果（列表项含创建人姓名createdByName及明细列表details，明细含关联留样编号retainedCode；附件列表仅详情返回）
     */
    PagedResult<DestructionRecordWithDetailsDto> search(String recordType, String keyword, String batchNo,
                                          String status, String startDate, String endDate,
                                          PageRequestDto pageRequest);

    /**
     * 查询销毁记录详情（含明细列表、附件列表与创建人/更新人姓名）
     *
     * @param id 销毁记录ID
     * @return 销毁记录详情，不存在时返回null
     */
    DestructionRecordWithDetailsDto getDetail(Long id);

    /**
     * 新增销毁记录（自动生成单号、按明细求和计算合计数量、明细落库、绑定附件；
     * 留样销毁类型反写关联留样为已销毁）
     *
     * @param dto 保存请求体
     * @return 新增的销毁记录
     */
    DestructionRecord create(DestructionRecordSaveDto dto);

    /**
     * 修改销毁记录（单号与状态不可改，作废后禁改；明细整体重建、合计重算、同步附件；
     * 留样销毁类型按新旧引用差异反写/回滚留样状态）
     *
     * @param id  销毁记录ID
     * @param dto 保存请求体
     * @return 修改后的销毁记录
     */
    DestructionRecord update(Long id, DestructionRecordSaveDto dto);

    /**
     * 作废销毁记录（status置为作废，记录作废时间与原因；已作废记录禁止重复作废；
     * 留样销毁类型同时回滚关联留样为留样中）
     *
     * @param id         销毁记录ID
     * @param voidRemark 作废原因（可选，最长500字符）
     */
    void voidRecord(Long id, String voidRemark);

    /**
     * 查询销毁记录附件列表
     *
     * @param id 销毁记录ID
     * @return 附件文件列表
     */
    List<FileInfo> getAttachments(Long id);

    /**
     * 留样下拉查询（仅返回状态为留样中的记录，关键字模糊匹配留样编号/物料名称/批号）
     *
     * @param keyword 关键字（可空）
     * @return 下拉选项列表（最多20条）
     */
    List<RetainedSampleOptionDto> searchRetainedSamples(String keyword);
}
