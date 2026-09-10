package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Energy.EnergyRecordPageResult;
import com.tonghui.erp.Data.Entity.EnergyRecord;
import com.tonghui.erp.Data.Entity.FileInfo;

import java.util.List;

/**
 * 能耗记录服务接口
 * <p>
 * 提供能耗记录的分页查询（含汇总）、新增、编辑、删除及凭证附件关联功能
 * </p>
 */
public interface EnergyRecordService {

    /**
     * 分页查询能耗记录列表（支持月份、类型筛选，返回费用汇总）
     *
     * @param month      月份（格式 YYYY-MM，可为空）
     * @param energyType 能耗类型（自来水/电/燃气，可为空）
     * @param pageIndex  页码（从0开始）
     * @param pageSize   每页数量
     * @return 分页结果（含汇总）
     */
    EnergyRecordPageResult pageQuery(String month, String energyType, int pageIndex, int pageSize);

    /**
     * 新增能耗记录
     * <p>
     * 自动映射计量单位、自动计算实用量与总价、记录操作人
     * </p>
     *
     * @param record          能耗记录（month/energyType 必填）
     * @param attachmentFileId 凭证附件文件ID（可为空，绑定到文件管理模块）
     * @return 保存后的能耗记录
     */
    EnergyRecord create(EnergyRecord record, Long attachmentFileId);

    /**
     * 编辑能耗记录
     *
     * @param id              能耗记录ID
     * @param record          更新内容（同新增规则）
     * @param attachmentFileId 凭证附件文件ID（可为空，绑定到文件管理模块）
     * @return 更新后的能耗记录
     */
    EnergyRecord update(Long id, EnergyRecord record, Long attachmentFileId);

    /**
     * 删除能耗记录（软删除，is_deleted=1）
     * <p>
     * 关联的凭证附件文件保留（业务文件禁止删除），仅不再展示
     * </p>
     *
     * @param id 能耗记录ID
     */
    void delete(Long id);

    /**
     * 查询能耗记录的凭证附件列表
     *
     * @param id 能耗记录ID
     * @return 附件文件列表（按上传时间倒序）
     */
    List<FileInfo> getAttachments(Long id);
}
