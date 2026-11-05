package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.BusinessFile;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 业务附件关系数据访问接口
 * <p>
 * 关系行使用原生 DELETE 物理删除，避免全局逻辑删除改写导致唯一键冲突。
 * </p>
 */
public interface BusinessFileMapper extends BaseMapper<BusinessFile> {

    // region 物理删除
    // ===================================
    // 物理删除
    // ===================================

    /**
     * 按关联ID物理删除
     *
     * @param id 关联ID
     * @return 删除行数
     */
    @Delete("DELETE FROM business_file WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);

    /**
     * 按业务对象物理删除
     * <p>
     * refId 为 null 时删除该业务对象下所有子对象的关联；
     * refId 为 0 时只删除 ref_id = 0 的关联；
     * fileKind 为 null 时删除所有槽位。
     * </p>
     *
     * @param bizType  业务类型编码
     * @param bizId    业务ID
     * @param refId    子对象ID（可为 null，表示不限子对象）
     * @param fileKind 槽位（可为 null，表示不限槽位）
     * @return 删除行数
     */
    @Delete("<script>DELETE FROM business_file WHERE biz_type = #{bizType} AND biz_id = #{bizId}"
            + "<if test='refId != null'> AND ref_id = #{refId}</if>"
            + "<if test='fileKind != null'> AND file_kind = #{fileKind}</if></script>")
    int physicalDelete(@Param("bizType") String bizType,
                       @Param("bizId") Long bizId,
                       @Param("refId") Long refId,
                       @Param("fileKind") String fileKind);

    // endregion

    // region 统计与排序
    // ===================================
    // 统计与排序
    // ===================================

    /**
     * 统计文件被多少条业务关系引用
     *
     * @param fileId 文件ID
     * @return 引用数
     */
    @Select("SELECT COUNT(*) FROM business_file WHERE file_id = #{fileId} AND is_deleted = 0")
    long countByFileId(@Param("fileId") Long fileId);

    /**
     * 获取下一个排序号
     *
     * @param bizType  业务类型编码
     * @param bizId    业务ID
     * @param refId    子对象ID
     * @param fileKind 槽位
     * @return 下一个排序号
     */
    @Select("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM business_file "
            + "WHERE biz_type = #{bizType} AND biz_id = #{bizId} AND ref_id = #{refId} "
            + "AND file_kind = #{fileKind} AND is_deleted = 0")
    int nextSortOrder(@Param("bizType") String bizType,
                      @Param("bizId") Long bizId,
                      @Param("refId") Long refId,
                      @Param("fileKind") String fileKind);

    // endregion
}
