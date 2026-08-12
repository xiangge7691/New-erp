package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.FileInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件信息数据访问Mapper接口
 * <p>
 * 除基础CRUD外，提供回收站所需的原生SQL方法。
 * 注意：MyBatis-Plus 全局逻辑删除配置（logic-delete-field: isDeleted）会导致
 * ① updateById 写入时自动排除 is_deleted 字段（软删标志写不进去）；
 * ② 所有 wrapper 查询自动追加 is_deleted=0 条件（回收站记录查不到）。
 * 因此回收站的删除标记、查询必须使用原生SQL绕过这些行为。
 * </p>
 */
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    /**
     * 查询回收站目录内的全部文件记录（绕过软删除过滤，包含 is_deleted=0/1 的所有行）
     *
     * @param dir 回收站目录名，如 ".recycle-bin"
     * @return 回收站内全部文件记录
     */
    @Select("SELECT * FROM file_info WHERE file_path LIKE CONCAT('%', #{dir}, '%')")
    List<FileInfo> selectRecycleInfos(@Param("dir") String dir);

    /**
     * 查询指定路径（含子路径）下删除时间最新的一条文件记录（绕过软删除过滤）
     * <p>
     * 用于回收站中文件夹条目借取子文件的删除人/删除时间展示
     * </p>
     *
     * @param filePath 文件夹路径
     * @return 删除时间最新的文件记录，无匹配时返回null
     */
    @Select("SELECT * FROM file_info WHERE file_path LIKE CONCAT('%', #{filePath}, '%') ORDER BY deleted_at DESC LIMIT 1")
    FileInfo selectLatestRecycleInfo(@Param("filePath") String filePath);

    /**
     * 按完整路径查询文件记录（绕过软删除过滤）
     * <p>
     * 用于回收站恢复/永久删除时定位记录，此时记录的 is_deleted 可能为1
     * </p>
     *
     * @param filePath 文件完整路径
     * @return 文件记录，无匹配时返回null
     */
    @Select("SELECT * FROM file_info WHERE file_path = #{filePath} LIMIT 1")
    FileInfo selectByPathIgnoreDeleted(@Param("filePath") String filePath);

    /**
     * 按存储文件名（UUID）查询文件记录（绕过软删除过滤）
     * <p>
     * 用于回收站列表还原原始文件名（此时记录 is_deleted 可能为1）
     * </p>
     *
     * @param storedName 存储文件名（UUID）
     * @return 文件记录，无匹配时返回null
     */
    @Select("SELECT * FROM file_info WHERE stored_name = #{storedName} LIMIT 1")
    FileInfo selectByStoredNameIgnoreDeleted(@Param("storedName") String storedName);

    /**
     * 标记文件为软删除（移入回收站）
     * <p>
     * 原生UPDATE强制写入 is_deleted=1（updateById 会因逻辑删除配置排除该字段），
     * 同时写入删除时间、删除人并更新路径为回收站路径
     * </p>
     *
     * @param fileId       文件ID
     * @param filePath     回收站内的新路径
     * @param originalPath 删除前的原始路径
     * @param deletedAt    删除时间
     * @param deletedBy    删除人ID
     * @return 受影响行数
     */
    @Update("UPDATE file_info SET file_path = #{filePath}, original_path = #{originalPath}, is_deleted = 1, deleted_at = #{deletedAt}, deleted_by = #{deletedBy} WHERE file_id = #{fileId}")
    int markSoftDeleted(@Param("fileId") Long fileId, @Param("filePath") String filePath,
                        @Param("originalPath") String originalPath, @Param("deletedAt") LocalDateTime deletedAt,
                        @Param("deletedBy") Long deletedBy);

    /**
     * 清除文件的软删除标记（从回收站恢复）
     * <p>
     * 原生UPDATE强制写入 is_deleted=0 并清空删除时间、删除人，
     * 同时将路径恢复为原始路径
     * </p>
     *
     * @param fileId       文件ID
     * @param filePath     恢复后的路径（原文件路径）
     * @param originalPath 原始路径（恢复后置空或保留）
     * @return 受影响行数
     */
    @Update("UPDATE file_info SET is_deleted = 0, deleted_at = NULL, deleted_by = NULL, file_path = #{filePath}, original_path = #{originalPath} WHERE file_id = #{fileId}")
    int markRestored(@Param("fileId") Long fileId, @Param("filePath") String filePath,
                     @Param("originalPath") String originalPath);

    /**
     * 按路径模糊删除文件记录（永久删除用）
     * <p>
     * 原生DELETE绕过逻辑删除（wrapper.delete 会被转换 UPDATE is_deleted=1，
     * 无法真正删除已标记软删除的记录）
     * </p>
     *
     * @param filePath 文件路径（模糊匹配）
     * @return 删除行数
     */
    @Delete("DELETE FROM file_info WHERE file_path LIKE CONCAT('%', #{filePath}, '%')")
    int deleteByPathLike(@Param("filePath") String filePath);
}