package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Config.FileStorageConfig;
import com.tonghui.erp.Common.Dto.FileManager.DirectoryListingDto;
import com.tonghui.erp.Common.Dto.FileManager.FileItemDto;
import com.tonghui.erp.Common.Dto.FileManager.FileSearchRequestDto;
import com.tonghui.erp.Common.utils.DateTimeUtils;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.FileOperationLog;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.mapper.FileInfoMapper;
import com.tonghui.erp.Data.mapper.FileOperationLogMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Service.FileManagerService;
import com.tonghui.erp.Service.FileOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件管理器服务实现
 * <p>
 * 基于磁盘目录树的文件管理核心实现，提供：
 * 目录浏览、新建/重命名/移动/复制、回收站删除与恢复、
 * 上传下载预览、组合条件搜索、文件夹ZIP导出等能力
 * 业务文件根目录（root=business）只读保护，仅自定义文件（root=custom）允许增删改
 * 所有文件操作均记录文件操作日志
 * </p>
 */
@Service
public class FileManagerServiceImpl implements FileManagerService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 文件存储配置（基础路径、自定义目录等）
     */
    @Autowired
    private FileStorageConfig fileStorageConfig;

    /**
     * 文件信息Mapper（file_info表）
     */
    @Autowired
    private FileInfoMapper fileInfoMapper;

    /**
     * 文件操作日志服务（记录上传/下载/删除等操作）
     */
    @Autowired
    private FileOperationLogService fileOperationLogService;

    /**
     * 文件操作日志Mapper
     */
    @Autowired
    private FileOperationLogMapper fileOperationLogMapper;

    /**
     * 用户Mapper（解析删除人等用户姓名）
     */
    @Autowired
    private UserMapper userMapper;

    // endregion

    // region 常量定义
    // ===================================
    // 常量定义
    // ===================================

    /** 业务文件根目录类型 */
    private static final String ROOT_BUSINESS = "business";

    /** 自定义文件根目录类型 */
    private static final String ROOT_CUSTOM = "custom";

    /** 回收站目录名（隐藏目录） */
    private static final String RECYCLE_BIN_DIR = ".recycle-bin";

    /** 操作类型：上传 */
    private static final String OP_UPLOAD = "UPLOAD";

    /** 操作类型：下载 */
    private static final String OP_DOWNLOAD = "DOWNLOAD";

    /** 操作类型：预览 */
    private static final String OP_PREVIEW = "PREVIEW";

    /** 操作类型：新建文件夹 */
    private static final String OP_CREATE_FOLDER = "CREATE_FOLDER";

    /** 操作类型：删除 */
    private static final String OP_DELETE = "DELETE";

    /** 操作类型：恢复 */
    private static final String OP_RESTORE = "RESTORE";

    /** 操作类型：重命名 */
    private static final String OP_RENAME = "RENAME";

    /** 操作类型：移动 */
    private static final String OP_MOVE = "MOVE";

    /** 操作类型：复制 */
    private static final String OP_COPY = "COPY";

    // endregion

    // region 目录操作
    // ===================================
    // 目录操作
    // ===================================

    /**
     * 列出目录内容（文件夹与文件分组返回，按名称排序，排除回收站目录）
     *
     * @param relativePath 相对根目录的路径（为空表示根目录）
     * @param root         根目录类型（business=业务文件/custom=自定义文件）
     * @return 目录清单（当前路径、父级路径、子文件夹列表、子文件列表）
     */
    @Override
    public DirectoryListingDto listDirectory(String relativePath, String root) {
        Path basePath = resolveRootPath(root);
        Path targetDir = resolveSafePath(relativePath, root);

        DirectoryListingDto result = new DirectoryListingDto();
        result.setCurrentPath(normalizePath(relativePath));
        result.setParentPath(getParentPath(relativePath));

        List<FileItemDto> folders = new ArrayList<>();
        List<FileItemDto> files = new ArrayList<>();

        if (Files.exists(targetDir) && Files.isDirectory(targetDir)) {
            try (Stream<Path> stream = Files.list(targetDir)) {
                stream.filter(path -> !path.getFileName().toString().equals(RECYCLE_BIN_DIR))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(path -> {
                        FileItemDto item = new FileItemDto();
                        String diskName = path.getFileName().toString();
                        String displayName = Files.isDirectory(path) ? diskName : getOriginalName(path, diskName);
                        item.setName(displayName);
                        item.setPath(normalizePath(basePath.relativize(path).toString()));

                        if (Files.isDirectory(path)) {
                            item.setDirectory(true);
                            item.setSize(calculateFolderSize(path));
                            item.setIconType("folder");
                            try {
                                item.setModifiedTime(formatTime(Files.getLastModifiedTime(path)));
                            } catch (IOException e) {
                                item.setModifiedTime("");
                            }
                            folders.add(item);
                        } else {
                            item.setDirectory(false);
                            try {
                                item.setSize(Files.size(path));
                            } catch (IOException e) {
                                item.setSize(0L);
                            }
                            item.setExtension(getExtension(displayName));
                            item.setIconType(getIconType(item.getExtension()));
                            try {
                                item.setModifiedTime(formatTime(Files.getLastModifiedTime(path)));
                            } catch (IOException e) {
                                item.setModifiedTime("");
                            }
                            files.add(item);
                        }
                    });
            } catch (IOException e) {
                throw new RuntimeException("读取目录失败: " + e.getMessage());
            }
        }

        result.setFolders(folders);
        result.setFiles(files);
        return result;
    }

    /**
     * 在指定目录下新建文件夹（同时记录新建文件夹日志）
     *
     * @param parentPath 父级相对路径（为空表示根目录）
     * @param folderName 新文件夹名称（不允许包含..和路径分隔符）
     * @param root       根目录类型
     */
    @Override
    public void createFolder(String parentPath, String folderName, String root) {
        validatePath(folderName);
        Path dir = resolveSafePath(parentPath, root).resolve(folderName);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("创建文件夹失败: " + e.getMessage());
        }
        String path = normalizePath(StringUtils.hasText(parentPath) ? parentPath + "/" + folderName : folderName);
        fileOperationLogService.log(null, folderName, path, OP_CREATE_FOLDER, root, null);
    }

    /**
     * 重命名文件或文件夹（仅自定义文件允许，业务文件抛出安全异常）
     * <p>
     * 原子移动落盘后同步更新 file_info 的 filePath/originalPath/originalName
     * </p>
     *
     * @param relativePath 待重命名对象的相对路径
     * @param newName      新名称（不允许包含..和路径分隔符）
     * @param root         根目录类型
     */
    @Override
    public void rename(String relativePath, String newName, String root) {
        checkNotBusiness(root, "重命名");
        validatePath(newName);
        Path basePath = resolveRootPath(root);
        Path source = resolveSafePath(relativePath, root);
        Path target = source.getParent().resolve(newName);
        String oldName = source.getFileName().toString();
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("重命名失败: " + e.getMessage());
        }
        // 更新 file_info 中的 filePath、originalPath 以及原始文件名（列表展示名称取自 originalName）
        updateFileInfoPaths(source, target, basePath);
        FileInfo fi = findFileInfoByPath(target.toString());
        if (fi != null && StringUtils.hasText(newName)) {
            fi.setOriginalName(newName);
            fileInfoMapper.updateById(fi);
        }
        fileOperationLogService.log(null, getOriginalName(target, oldName), relativePath, OP_RENAME, root, oldName + " → " + newName);
    }

    /**
     * 移动文件或文件夹到目标目录（仅自定义文件允许）
     * <p>
     * 落盘移动后同步更新 file_info 的 filePath/originalPath
     * </p>
     *
     * @param sourcePath      源对象相对路径
     * @param targetDirectory 目标目录相对路径
     * @param root            根目录类型
     */
    @Override
    public void move(String sourcePath, String targetDirectory, String root) {
        checkNotBusiness(root, "移动");
        Path basePath = resolveRootPath(root);
        Path source = resolveSafePath(sourcePath, root);
        Path targetDir = resolveSafePath(targetDirectory, root);
        Path target = targetDir.resolve(source.getFileName().toString());
        try {
            Files.createDirectories(targetDir);
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("移动失败: " + e.getMessage());
        }
        // 更新 file_info 中的 filePath 和 originalPath
        updateFileInfoPaths(source, target, basePath);
        fileOperationLogService.log(null, getOriginalName(target, source.getFileName().toString()), sourcePath, OP_MOVE, root, "→ " + targetDirectory);
    }

    /**
     * 复制文件或文件夹到目标目录（仅自定义文件允许，文件夹递归复制）
     *
     * @param sourcePath      源对象相对路径
     * @param targetDirectory 目标目录相对路径
     * @param root            根目录类型
     */
    @Override
    public void copy(String sourcePath, String targetDirectory, String root) {
        checkNotBusiness(root, "复制");
        Path source = resolveSafePath(sourcePath, root);
        Path targetDir = resolveSafePath(targetDirectory, root);
        try {
            Files.createDirectories(targetDir);
            if (Files.isDirectory(source)) {
                copyDirectory(source, targetDir.resolve(source.getFileName().toString()));
            } else {
                Files.copy(source, targetDir.resolve(source.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("复制失败: " + e.getMessage());
        }
        fileOperationLogService.log(null, getOriginalName(source, source.getFileName().toString()), sourcePath, OP_COPY, root, "→ " + targetDirectory);
    }

    /**
     * 删除文件或文件夹到回收站（仅自定义文件允许）
     * <p>
     * 磁盘对象移动到 .recycle-bin 隐藏目录，file_info 记录软删除
     * （记录 originalPath 供恢复使用）
     * </p>
     *
     * @param relativePath 待删除对象的相对路径
     * @param root         根目录类型
     */
    @Override
    public void delete(String relativePath, String root) {
        checkNotBusiness(root, "删除");
        Path basePath = resolveRootPath(root);
        Path target = resolveSafePath(relativePath, root);
        Path recycleBin = basePath.resolve(RECYCLE_BIN_DIR);
        String fileName = target.getFileName().toString();
        String originalPath = normalizePath(basePath.relativize(target).toString());

        // 确保回收站目录存在
        try {
            Files.createDirectories(recycleBin);
        } catch (IOException e) {
            throw new RuntimeException("创建回收站目录失败: " + e.getMessage());
        }

        // 移动到回收站
        Path recycleTarget = recycleBin.resolve(fileName);
        try {
            Files.move(target, recycleTarget, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("删除失败: " + e.getMessage());
        }

        // 软删除 file_info 记录，保存原始路径并更新当前路径
        if (Files.isDirectory(recycleTarget)) {
            try (Stream<Path> walk = Files.walk(recycleTarget)) {
                walk.filter(Files::isRegularFile).forEach(path -> {
                    // 正确计算原始路径：以回收站内文件相对文件夹的路径，拼接到删除前文件夹路径上
                    // （原实现用 basePath.relativize(path) 会带上 .recycle-bin 前缀导致路径错误，找不到记录）
                    Path relativeToFolder = recycleTarget.relativize(path);
                    FileInfo fi = findFileInfoByPath(target.resolve(relativeToFolder).toString());
                    if (fi == null) {
                        fi = findFileInfoByPath(path.toString());
                    }
                    if (fi != null) {
                        fi.setOriginalPath(fi.getFilePath());
                        fi.setFilePath(path.toString());
                        softDeleteFileInfo(fi);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException("删除失败: " + e.getMessage());
            }
        } else {
            FileInfo fi = findFileInfoByPath(target.toString());
            if (fi == null) {
                fi = findFileInfoByStoredName(fileName);
            }
            if (fi != null) {
                fi.setOriginalPath(fi.getFilePath());
                fi.setFilePath(recycleTarget.toString());
                softDeleteFileInfo(fi);
            }
        }

        fileOperationLogService.log(null, getOriginalName(target, fileName), normalizePath(relativePath), OP_DELETE, root, null);
    }

    /**
     * 从回收站恢复文件或文件夹（仅自定义文件允许）
     * <p>
     * 优先按 file_info 记录的 originalPath 还原到删除前位置，
     * 无记录时回退到原相对父目录；恢复后清除软删除标记
     * </p>
     *
     * @param relativePath 回收站内的相对路径（如 .recycle-bin/xxx.pdf）
     * @param root         根目录类型
     */
    @Override
    public void restore(String relativePath, String root) {
        checkNotBusiness(root, "恢复");
        Path basePath = resolveRootPath(root);
        Path recycleBin = basePath.resolve(RECYCLE_BIN_DIR);
        String fileName = Paths.get(relativePath).getFileName().toString();
        Path source = recycleBin.resolve(fileName);

        if (!Files.exists(source)) {
            throw new RuntimeException("恢复失败: 回收站中不存在该文件");
        }

        // 从 file_info 获取原始路径（回收站记录 is_deleted 可能为1，须绕过软删除过滤查找）
        String originalPath = null;
        if (Files.isRegularFile(source)) {
            FileInfo fi = findFileInfoByPathIgnoreDeleted(source.toString());
            if (fi != null && StringUtils.hasText(fi.getOriginalPath())) {
                originalPath = fi.getOriginalPath();
            }
        }

        // 确定恢复目标路径
        Path target;
        if (StringUtils.hasText(originalPath)) {
            target = basePath.resolve(originalPath).normalize();
        } else {
            String fallback = Paths.get(relativePath).getParent() != null
                    ? Paths.get(relativePath).getParent().toString() : "";
            target = resolveSafePath(fallback, root).resolve(fileName);
        }

        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("恢复失败: " + e.getMessage());
        }

        // 恢复 file_info 记录并更新路径（回收站记录 is_deleted 可能为1，须绕过软删除过滤查找）
        if (Files.isDirectory(target)) {
            try (Stream<Path> walk = Files.walk(target)) {
                walk.filter(Files::isRegularFile).forEach(path -> {
                    FileInfo fi = findFileInfoByPathIgnoreDeleted(source.resolve(target.relativize(path).toString()).toString());
                    if (fi == null) {
                        fi = findFileInfoByPathIgnoreDeleted(path.toString());
                    }
                    if (fi != null) {
                        fi.setFilePath(path.toString());
                        fi.setOriginalPath(null);
                        restoreFileInfo(fi);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException("恢复失败: " + e.getMessage());
            }
        } else {
            FileInfo fi = findFileInfoByPathIgnoreDeleted(source.toString());
            if (fi != null) {
                fi.setFilePath(target.toString());
                fi.setOriginalPath(null);
                restoreFileInfo(fi);
            }
        }

        fileOperationLogService.log(null, getOriginalName(target, fileName), normalizePath(relativePath), OP_RESTORE, root, null);
    }

    /**
     * 从回收站永久删除（仅自定义文件允许）
     * <p>
     * 物理删除磁盘对象，并通过原生SQL物理删除 file_info 记录
     * （逻辑删除配置下 wrapper.delete 会被转为UPDATE，故用原生DELETE）
     * </p>
     *
     * @param relativePath 回收站内的相对路径
     * @param root         根目录类型
     */
    @Override
    public void permanentDelete(String relativePath, String root) {
        checkNotBusiness(root, "永久删除");
        Path basePath = resolveRootPath(root);
        Path recycleBin = basePath.resolve(RECYCLE_BIN_DIR);
        String fileName = Paths.get(relativePath).getFileName().toString();
        Path target = recycleBin.resolve(fileName);

        if (!Files.exists(target)) {
            throw new RuntimeException("永久删除失败: 回收站中不存在该文件");
        }

        // 物理删除磁盘文件
        try {
            if (Files.isDirectory(target)) {
                deleteDirectory(target);
            } else {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            throw new RuntimeException("永久删除失败: " + e.getMessage());
        }

        // 物理删除 file_info 记录（原生DELETE：wrapper.delete 会因逻辑删除被转为 UPDATE 而失效）
        fileInfoMapper.deleteByPathLike(fileName);

        fileOperationLogService.log(null, getOriginalName(target, fileName), normalizePath(relativePath), OP_DELETE, root, "永久删除");
    }

    /**
     * 列出回收站内容（扫描 .recycle-bin 目录）
     * <p>
     * 通过 file_info 表补充删除人、删除时间字段，
     * 并支持按删除人、删除时间段筛选
     * </p>
     *
     * @param deletedBy     删除人ID（可选，为空不过滤）
     * @param deletedByName 删除人姓名（可选，模糊匹配，兼容真实姓名与登录账号）
     * @param startTime     删除时间起始（可选，含边界）
     * @param endTime       删除时间截止（可选，含边界）
     * @return 已删除的文件列表（含删除人、删除时间）
     */
    @Override
    public List<FileItemDto> listRecycleBin(Long deletedBy, String deletedByName, LocalDateTime startTime, LocalDateTime endTime) {
        Path basePath = resolveRootPath(ROOT_CUSTOM);
        Path recycleBin = basePath.resolve(RECYCLE_BIN_DIR);
        List<FileItemDto> results = new ArrayList<>();

        if (!Files.exists(recycleBin) || !Files.isDirectory(recycleBin)) {
            return results;
        }

        // 预加载回收站内所有 file_info 记录（磁盘路径 -> 记录），用于补充删除人/删除时间
        Map<String, FileInfo> recycleInfos = loadRecycleFileInfos();

        try (Stream<Path> stream = Files.list(recycleBin)) {
            stream.sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .forEach(path -> {
                    FileItemDto item = new FileItemDto();
                    String diskName = path.getFileName().toString();
                    String displayName = Files.isDirectory(path) ? diskName : resolveRecycleOriginalName(path, diskName);
                    item.setName(displayName);
                    item.setPath(normalizePath(RECYCLE_BIN_DIR + "/" + diskName));

                    // 补充删除人/删除时间（文件夹取子文件的删除记录）
                    FileInfo fi = Files.isDirectory(path)
                            ? findFolderRecycleInfo(path)
                            : recycleInfos.get(path.toString());
                    fillRecycleInfo(item, fi);

                    if (Files.isDirectory(path)) {
                        item.setDirectory(true);
                        item.setSize(calculateFolderSize(path));
                        item.setIconType("folder");
                    } else {
                        item.setDirectory(false);
                        try {
                            item.setSize(Files.size(path));
                        } catch (IOException e) {
                            item.setSize(0L);
                        }
                        String ext = getExtension(displayName);
                        item.setExtension(ext);
                        item.setIconType(getIconType(ext));
                    }
                    try {
                        item.setModifiedTime(formatTime(Files.getLastModifiedTime(path)));
                    } catch (IOException e) {
                        item.setModifiedTime("");
                    }
                    results.add(item);
                });
        } catch (IOException e) {
            throw new RuntimeException("读取回收站失败: " + e.getMessage());
        }

        // 按删除人ID、删除人姓名、删除时间段筛选
        return results.stream().filter(item -> {
            if (deletedBy != null && !deletedBy.equals(item.getDeletedBy())) {
                return false;
            }
            // 姓名模糊匹配（同时兼容真实姓名与登录账号）
            if (StringUtils.hasText(deletedByName)
                    && !StringUtils.hasText(item.getDeletedByName())
                    && !StringUtils.hasText(item.getDeletedByAccount())) {
                return false;
            }
            if (StringUtils.hasText(deletedByName)) {
                boolean nameMatched = (StringUtils.hasText(item.getDeletedByName())
                        && item.getDeletedByName().contains(deletedByName))
                        || (StringUtils.hasText(item.getDeletedByAccount())
                        && item.getDeletedByAccount().contains(deletedByName));
                if (!nameMatched) {
                    return false;
                }
            }
            if (startTime != null || endTime != null) {
                LocalDateTime deletedAt = parseDateTime(item.getDeletedTime());
                if (deletedAt == null) {
                    return false;
                }
                if (startTime != null && deletedAt.isBefore(startTime)) {
                    return false;
                }
                if (endTime != null && deletedAt.isAfter(endTime)) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
    }

    /**
     * 加载回收站内全部已删除的 file_info 记录（磁盘路径 -> 记录）
     * <p>
     * 使用原生SQL按路径匹配（绕过软删除过滤，兼容 is_deleted=0/1 两种历史状态）
     * </p>
     *
     * @return 磁盘绝对路径到文件信息的映射
     */
    private Map<String, FileInfo> loadRecycleFileInfos() {
        return fileInfoMapper.selectRecycleInfos(RECYCLE_BIN_DIR).stream()
                .collect(Collectors.toMap(FileInfo::getFilePath, f -> f, (a, b) -> a));
    }

    /**
     * 查找已删除文件夹的代表性 file_info 记录（取子文件中删除时间最新的一条）
     * <p>
     * 使用原生SQL（绕过软删除过滤，兼容 is_deleted=0/1 两种历史状态）
     * </p>
     *
     * @param folderPath 回收站内文件夹的磁盘绝对路径
     * @return file_info 记录，无匹配时返回 null
     */
    private FileInfo findFolderRecycleInfo(Path folderPath) {
        return fileInfoMapper.selectLatestRecycleInfo(folderPath.toString());
    }

    /**
     * 将 file_info 记录的删除人、删除时间填充到回收站条目
     * <p>
     * 当 file_info 记录不存在时（如空文件夹或文件未登记），
     * 从 file_operation_log 操作日志表中补充删除人、删除时间
     * </p>
     *
     * @param item 回收站条目
     * @param fi   file_info 记录（可为空）
     */
    private void fillRecycleInfo(FileItemDto item, FileInfo fi) {
        if (fi == null) {
            // 从操作日志表补充删除人、删除时间（file_operation_log 中 file_id 为 null 表示文件夹操作）
            FileOperationLog opLog = fileOperationLogMapper.selectLatestByNameAndType(
                    Paths.get(item.getPath()).getFileName().toString(), OP_DELETE);
            if (opLog == null) {
                return;
            }
            item.setDeletedBy(opLog.getUserId());
            User user = opLog.getUserId() != null ? userMapper.selectById(opLog.getUserId()) : null;
            item.setDeletedByName(opLog.getUserName() != null ? opLog.getUserName() : resolveUserName(opLog.getUserId()));
            item.setDeletedByAccount(user != null ? user.getUserAccount() : null);
            item.setDeletedTime(opLog.getCreatedTime() != null
                    ? opLog.getCreatedTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
            return;
        }
        item.setDeletedBy(fi.getDeletedBy());
        User user = fi.getDeletedBy() != null ? userMapper.selectById(fi.getDeletedBy()) : null;
        item.setDeletedByName(resolveUserName(fi.getDeletedBy()));
        item.setDeletedByAccount(user != null ? user.getUserAccount() : null);
        item.setDeletedTime(fi.getDeletedAt() != null
                ? fi.getDeletedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
    }

    /**
     * 根据用户ID解析用户显示名称（真实姓名，无则回退登录账号）
     *
     * @param userId 用户ID（可为空）
     * @return 用户名称，用户不存在或ID为空时返回 null
     */
    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getUserName()) ? user.getUserName() : user.getUserAccount();
    }

    // endregion

    // region 文件操作
    // ===================================
    // 文件操作
    // ===================================

    /**
     * 上传文件到指定目录（UUID存储名落盘，写入file_info并记录上传日志）
     *
     * @param file        上传文件
     * @param relativeDir 目标目录相对路径（为空表示根目录）
     * @param root        根目录类型
     * @return 保存后的文件信息（含fileId、fileUrl）
     * @throws IOException 文件读写失败
     */
    @Override
    public FileInfo uploadFile(MultipartFile file, String relativeDir, String root) throws IOException {
        Path dir = resolveSafePath(relativeDir, root);
        Files.createDirectories(dir);

        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        String storedName = UUID.randomUUID().toString().replace("-", "") + (StringUtils.hasText(extension) ? "." + extension : "");
        Path target = dir.resolve(storedName);

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        FileInfo fileInfo = new FileInfo();
        fileInfo.setOriginalName(originalName);
        fileInfo.setStoredName(storedName);
        fileInfo.setFilePath(target.toString());
        fileInfo.setFileSize(file.getSize());
        fileInfo.setContentType(file.getContentType());
        fileInfo.setFileExtension(extension);
        fileInfo.setStorageType("LOCAL");
        fileInfo.setCategory("FILE_MANAGER");

        fileInfoMapper.insert(fileInfo);
        fileInfo.setFileUrl("/api/files/" + fileInfo.getFileId());
        fileInfoMapper.updateById(fileInfo);

        String path = normalizePath(StringUtils.hasText(relativeDir) ? relativeDir + "/" + originalName : originalName);
        fileOperationLogService.log(fileInfo.getFileId(), originalName, path, OP_UPLOAD, root, null);

        return fileInfo;
    }

    /**
     * 下载文件（返回文件输入流，记录DOWNLOAD操作日志）
     *
     * @param relativePath 文件相对路径
     * @param root         根目录类型
     * @return 文件输入流
     * @throws IOException 文件不存在或读取失败
     */
    @Override
    public InputStream downloadFile(String relativePath, String root) throws IOException {
        Path file = resolveSafePath(relativePath, root);
        if (!Files.exists(file) || Files.isDirectory(file)) {
            throw new IOException("文件不存在: " + relativePath);
        }
        String fileName = file.getFileName().toString();
        FileInfo fi = findFileInfoByPath(file.toString());
        fileOperationLogService.log(fi != null ? fi.getFileId() : null, getOriginalName(file, fileName), normalizePath(relativePath), OP_DOWNLOAD, root, null);
        return Files.newInputStream(file);
    }

    /**
     * 预览文件（返回文件输入流，记录PREVIEW操作日志）
     *
     * @param relativePath 文件相对路径
     * @param root         根目录类型
     * @return 文件输入流
     * @throws IOException 文件不存在或读取失败
     */
    @Override
    public InputStream previewFile(String relativePath, String root) throws IOException {
        Path file = resolveSafePath(relativePath, root);
        if (!Files.exists(file) || Files.isDirectory(file)) {
            throw new IOException("文件不存在: " + relativePath);
        }
        String fileName = file.getFileName().toString();
        FileInfo fi = findFileInfoByPath(file.toString());
        fileOperationLogService.log(fi != null ? fi.getFileId() : null, getOriginalName(file, fileName), normalizePath(relativePath), OP_PREVIEW, root, null);
        return Files.newInputStream(file);
    }

    /**
     * 组合条件搜索文件（递归遍历目录树）
     * <p>
     * 支持：原始文件名关键词、路径模糊匹配、大小区间、修改时间区间，
     * 全部命中后按目录列表格式返回（folders/files 分组，文件夹在前，各自按名称排序）
     * 关键词匹配的是 file_info 中的原始文件名（磁盘存储名为UUID，直接按磁盘名匹配会永远搜不到）
     * </p>
     *
     * @param request 搜索请求（关键词、路径、根目录、大小/时间区间）
     * @return 搜索结果（目录列表格式：当前路径、父级路径、文件夹列表、文件列表）
     */
    @Override
    public DirectoryListingDto searchFiles(FileSearchRequestDto request) {
        String keyword = request.getKeyword();
        String searchPath = request.getPath();
        String root = request.getRoot();
        Long minSize = request.getMinSize();
        Long maxSize = request.getMaxSize();
        LocalDateTime modifiedAfter = parseDateTime(request.getModifiedAfter());
        LocalDateTime modifiedBefore = parseDateTime(request.getModifiedBefore());

        Path searchRoot = resolveSafePath(searchPath, root);
        Path basePath = resolveRootPath(root);

        // 构建与目录列表一致的结果结构（当前路径、父级路径、文件夹/文件分组）
        DirectoryListingDto result = new DirectoryListingDto();
        result.setCurrentPath(normalizePath(searchPath));
        result.setParentPath(getParentPath(searchPath));
        List<FileItemDto> folders = new ArrayList<>();
        List<FileItemDto> files = new ArrayList<>();

        if (!Files.exists(searchRoot)) {
            result.setFolders(folders);
            result.setFiles(files);
            return result;
        }

        // 关键词匹配前预加载搜索目录下的原始文件名映射（路径/存储名 -> 原始文件名）
        Map<String, String> originalNameByPath = new HashMap<>();
        Map<String, String> originalNameByStored = new HashMap<>();
        if (StringUtils.hasText(keyword)) {
            for (FileInfo fi : fileInfoMapper.selectActiveByPathPrefix(searchRoot.toString())) {
                if (StringUtils.hasText(fi.getOriginalName())) {
                    originalNameByPath.putIfAbsent(fi.getFilePath(), fi.getOriginalName());
                    originalNameByStored.putIfAbsent(fi.getStoredName(), fi.getOriginalName());
                }
            }
        }

        try (Stream<Path> walk = Files.walk(searchRoot)) {
            walk.filter(path -> {
                // 文件名关键词匹配（可选）：文件优先按原始文件名匹配，无记录时回退磁盘名；目录按目录名匹配
                if (StringUtils.hasText(keyword)) {
                    String diskName = path.getFileName().toString();
                    String matchName = diskName;
                    if (Files.isRegularFile(path)) {
                        matchName = originalNameByPath.getOrDefault(path.toString(),
                                originalNameByStored.getOrDefault(diskName, diskName));
                    }
                    if (!matchName.toLowerCase().contains(keyword.toLowerCase())) {
                        return false;
                    }
                }
                // 路径模糊匹配（可选）
                if (StringUtils.hasText(searchPath)) {
                    String relativePath = normalizePath(basePath.relativize(path).toString()).toLowerCase();
                    String searchLower = searchPath.toLowerCase();
                    if (!relativePath.contains(searchLower)) {
                        return false;
                    }
                }
                // 文件大小筛选（仅文件）
                if (Files.isRegularFile(path)) {
                    try {
                        long size = Files.size(path);
                        if (minSize != null && size < minSize) return false;
                        if (maxSize != null && size > maxSize) return false;
                    } catch (IOException e) {
                        return false;
                    }
                }
                // 修改日期筛选
                try {
                    FileTime ft = Files.getLastModifiedTime(path);
                    LocalDateTime ldt = LocalDateTime.ofInstant(ft.toInstant(), ZoneId.systemDefault());
                    if (modifiedAfter != null && ldt.isBefore(modifiedAfter)) return false;
                    if (modifiedBefore != null && ldt.isAfter(modifiedBefore)) return false;
                } catch (IOException e) {
                    return false;
                }
                return true;
            }).forEach(path -> {
                FileItemDto item = new FileItemDto();
                String diskName = path.getFileName().toString();
                String displayName = Files.isDirectory(path) ? diskName : getOriginalName(path, diskName);
                item.setName(displayName);
                item.setPath(normalizePath(basePath.relativize(path).toString()));

                if (Files.isDirectory(path)) {
                    item.setDirectory(true);
                    item.setSize(calculateFolderSize(path));
                    item.setIconType("folder");
                    try {
                        item.setModifiedTime(formatTime(Files.getLastModifiedTime(path)));
                    } catch (IOException e) {
                        item.setModifiedTime("");
                    }
                    // 目录与文件分组（与目录列表接口一致）
                    folders.add(item);
                } else {
                    item.setDirectory(false);
                    try {
                        item.setSize(Files.size(path));
                    } catch (IOException e) {
                        item.setSize(0L);
                    }
                    String ext = getExtension(displayName);
                    item.setExtension(ext);
                    item.setIconType(getIconType(ext));
                    try {
                        item.setModifiedTime(formatTime(Files.getLastModifiedTime(path)));
                    } catch (IOException e) {
                        item.setModifiedTime("");
                    }
                    files.add(item);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("搜索失败: " + e.getMessage());
        }

        // 文件夹与文件各自按名称排序（与目录列表接口一致）
        folders.sort(Comparator.comparing(FileItemDto::getName, Comparator.nullsLast(String::compareTo)));
        files.sort(Comparator.comparing(FileItemDto::getName, Comparator.nullsLast(String::compareTo)));
        result.setFolders(folders);
        result.setFiles(files);
        return result;
    }

    /**
     * 将文件或文件夹导出为ZIP（文件夹保留目录层级与原始文件名，排除回收站）
     *
     * @param relativePath 待导出对象的相对路径（为空表示根目录）
     * @param root         根目录类型
     * @return ZIP文件字节数组
     */
    @Override
    public byte[] exportFolder(String relativePath, String root) {
        Path target = resolveSafePath(relativePath, root);
        if (!Files.exists(target)) {
            throw new RuntimeException("导出失败: 路径不存在: " + relativePath);
        }
        String folderName = getRootFolderName(relativePath);
        Path basePath = resolveRootPath(root);
        byte[] zipBytes = zipFolder(target, folderName, basePath);
        String logPath = normalizePath(StringUtils.hasText(relativePath) ? relativePath : folderName);
        fileOperationLogService.log(null, folderName, logPath, OP_DOWNLOAD, root, "导出文件夹zip");
        return zipBytes;
    }

    /**
     * 判断相对路径是否指向目录
     *
     * @param relativePath 相对路径
     * @param root         根目录类型
     * @return 是目录返回true
     */
    @Override
    public boolean isDirectory(String relativePath, String root) {
        Path target = resolveSafePath(relativePath, root);
        return Files.isDirectory(target);
    }

    // endregion

    // region 私有方法
    // ===================================
    // 私有方法
    // ===================================

    /**
     * 递归将文件夹打包为 ZIP 文件
     * <p>
     * 保留目录层级结构，zip 内文件名还原为原始文件名（非UUID存储名），排除回收站目录
     * </p>
     *
     * @param target     待导出的文件或文件夹路径
     * @param folderName zip 根级目录名（zip 内第一层目录名称）
     * @param basePath   当前 root 的基础路径（用于排除回收站）
     * @return ZIP 文件字节数组
     */
    private byte[] zipFolder(Path target, String folderName, Path basePath) {
        Path recycleBin = basePath.resolve(RECYCLE_BIN_DIR).toAbsolutePath().normalize();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            if (Files.isDirectory(target)) {
                // 递归收集所有条目（排除回收站目录）
                List<Path> paths;
                try (Stream<Path> walk = Files.walk(target)) {
                    paths = walk.filter(p -> !p.toAbsolutePath().normalize().startsWith(recycleBin))
                            .collect(Collectors.toList());
                }
                for (Path path : paths) {
                    String rel = normalizePath(target.relativize(path).toString());
                    if (rel.isEmpty()) {
                        continue;
                    }
                    if (Files.isDirectory(path)) {
                        // 添加目录占位条目，保留空文件夹结构
                        zos.putNextEntry(new ZipEntry(folderName + "/" + rel + "/"));
                        zos.closeEntry();
                    } else {
                        // 文件使用还原后的原始文件名
                        String diskName = path.getFileName().toString();
                        String displayName = getOriginalName(path, diskName);
                        String parentRel = rel.substring(0, rel.length() - diskName.length());
                        zos.putNextEntry(new ZipEntry(folderName + "/" + parentRel + displayName));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    }
                }
            } else {
                // 导出单个文件
                String displayName = getOriginalName(target, target.getFileName().toString());
                zos.putNextEntry(new ZipEntry(folderName + "/" + displayName));
                Files.copy(target, zos);
                zos.closeEntry();
            }
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    /**
     * 获取导出时的 zip 根级目录名
     * <p>
     * 有相对路径时取路径最后一段，路径为空或为根目录时返回 "root"
     * </p>
     *
     * @param relativePath 相对路径（可为空）
     * @return 根级目录名
     */
    private String getRootFolderName(String relativePath) {
        if (!StringUtils.hasText(relativePath) || "/".equals(relativePath.trim())) {
            return "root";
        }
        String trimmed = relativePath.trim();
        String name = trimmed.substring(trimmed.lastIndexOf('/') + 1);
        return name.isEmpty() ? "root" : name;
    }

    /**
     * 递归计算文件夹大小
     * <p>
     * 遍历文件夹下所有文件，累加文件大小
     * </p>
     *
     * @param folder 文件夹路径
     * @return 文件夹内所有文件的总大小（字节）
     */
    private long calculateFolderSize(Path folder) {
        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            return 0L;
        }
        long[] size = {0L};
        try (Stream<Path> walk = Files.walk(folder)) {
            walk.filter(Files::isRegularFile).forEach(path -> {
                try {
                    size[0] += Files.size(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
        return size[0];
    }

    /**
     * 解析日期时间字符串
     * <p>
     * 兼容 yyyy-MM-dd HH:mm:ss 与 yyyy-MM-dd'T'HH:mm:ss 两种格式
     * </p>
     *
     * @param dateTimeStr 日期时间字符串
     * @return LocalDateTime 对象，解析失败返回 null
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        return DateTimeUtils.tryParseDateTime(dateTimeStr);
    }

    /**
     * 根据磁盘文件路径查找原始文件名（绕过软删除过滤，用于回收站列表）
     * <p>
     * 优先从 file_info 表获取 originalName，找不到时返回磁盘文件名；
     * 回收站内的记录 is_deleted 可能为1，普通查询查不到，须用原生SQL
     * </p>
     *
     * @param filePath     磁盘文件路径
     * @param fallbackName 兜底文件名
     * @return 原始文件名或兜底文件名
     */
    private String resolveRecycleOriginalName(Path filePath, String fallbackName) {
        FileInfo fi = findFileInfoByPathIgnoreDeleted(filePath.toString());
        if (fi != null && StringUtils.hasText(fi.getOriginalName())) {
            return fi.getOriginalName();
        }
        FileInfo byStoredName = fileInfoMapper.selectByStoredNameIgnoreDeleted(filePath.getFileName().toString());
        if (byStoredName != null && StringUtils.hasText(byStoredName.getOriginalName())) {
            return byStoredName.getOriginalName();
        }
        return fallbackName;
    }

    /**
     * 根据磁盘文件路径查找原始文件名
     * 优先从 file_info 表获取 originalName，找不到时返回磁盘文件名
     *
     * @param filePath     磁盘文件路径
     * @param fallbackName 兜底文件名
     * @return 原始文件名或兜底文件名
     */
    private String getOriginalName(Path filePath, String fallbackName) {
        FileInfo fi = findFileInfoByPath(filePath.toString());
        if (fi != null && StringUtils.hasText(fi.getOriginalName())) {
            return fi.getOriginalName();
        }
        String diskFileName = filePath.getFileName().toString();
        fi = findFileInfoByStoredName(diskFileName);
        if (fi != null && StringUtils.hasText(fi.getOriginalName())) {
            return fi.getOriginalName();
        }
        return fallbackName;
    }

    /**
     * 文件移动/重命名后，更新 file_info 中的 filePath 和 originalPath
     *
     * @param oldPath 移动前的路径
     * @param newPath 移动后的路径
     * @param basePath 基础路径
     */
    private void updateFileInfoPaths(Path oldPath, Path newPath, Path basePath) {
        if (Files.isDirectory(newPath)) {
            try (Stream<Path> walk = Files.walk(newPath)) {
                walk.filter(Files::isRegularFile).forEach(path -> {
                    Path oldFilePath = oldPath.resolve(newPath.relativize(path));
                    FileInfo fi = findFileInfoByPath(oldFilePath.toString());
                    if (fi == null) {
                        fi = findFileInfoByPath(path.toString());
                    }
                    if (fi != null) {
                        fi.setFilePath(path.toString());
                        fi.setOriginalPath(basePath.relativize(path).toString());
                        fileInfoMapper.updateById(fi);
                    }
                });
            } catch (IOException ignored) {
            }
        } else {
            FileInfo fi = findFileInfoByPath(oldPath.toString());
            if (fi != null) {
                fi.setFilePath(newPath.toString());
                fi.setOriginalPath(basePath.relativize(newPath).toString());
                fileInfoMapper.updateById(fi);
            }
        }
    }

    /**
     * 根据根目录类型获取对应的基础路径
     *
     * @param root 根目录类型（custom=自定义文件目录，其他=业务文件基础目录）
     * @return 规范化后的绝对基础路径
     */
    private Path resolveRootPath(String root) {
        if (ROOT_CUSTOM.equals(root)) {
            return Paths.get(fileStorageConfig.getCustomPath()).toAbsolutePath().normalize();
        }
        return Paths.get(resolveBasePath()).toAbsolutePath().normalize();
    }

    /**
     * 检查是否为业务文件，是则抛出异常
     *
     * @param root   根目录类型
     * @param action 操作名称（用于异常提示）
     */
    private void checkNotBusiness(String root, String action) {
        if (ROOT_BUSINESS.equals(root)) {
            throw new SecurityException("业务文件不允许" + action);
        }
    }

    /**
     * 根据文件路径查找对应的 FileInfo 记录
     *
     * @param filePath 磁盘文件绝对路径
     * @return 文件信息记录，无匹配时返回null
     */
    private FileInfo findFileInfoByPath(String filePath) {
        QueryWrapper<FileInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("file_path", filePath);
        return fileInfoMapper.selectOne(wrapper);
    }

    /**
     * 根据完整路径查找 FileInfo 记录（绕过软删除过滤）
     * <p>
     * 回收站内的记录 is_deleted 可能为1，MyBatis-Plus 查询会自动过滤，
     * 恢复/永久删除等流程需使用原生SQL查找
     * </p>
     *
     * @param filePath 磁盘文件绝对路径
     * @return 文件信息记录，无匹配时返回null
     */
    private FileInfo findFileInfoByPathIgnoreDeleted(String filePath) {
        return fileInfoMapper.selectByPathIgnoreDeleted(filePath);
    }

    /**
     * 根据存储文件名（UUID）查找 FileInfo 记录
     *
     * @param storedName 磁盘存储文件名（UUID.扩展名）
     * @return 文件信息记录，无匹配时返回null
     */
    private FileInfo findFileInfoByStoredName(String storedName) {
        QueryWrapper<FileInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("stored_name", storedName);
        return fileInfoMapper.selectOne(wrapper);
    }

    /**
     * 软删除 FileInfo 记录（移入回收站）
     * <p>
     * 使用原生SQL强制写入 is_deleted=1（MyBatis-Plus 逻辑删除配置会使
     * updateById 自动排除该字段，导致软删标记写不进去）
     * </p>
     */
    private void softDeleteFileInfo(FileInfo fi) {
        fi.setDeletedAt(LocalDateTime.now());
        fi.setDeletedBy(EntityUtils.getCurrentUserId());
        fileInfoMapper.markSoftDeleted(fi.getFileId(), fi.getFilePath(), fi.getOriginalPath(),
                fi.getDeletedAt(), fi.getDeletedBy());
    }

    /**
     * 恢复 FileInfo 记录（从回收站恢复）
     * <p>
     * 使用原生SQL强制写入 is_deleted=0 并清空删除时间、删除人
     * </p>
     */
    private void restoreFileInfo(FileInfo fi) {
        fileInfoMapper.markRestored(fi.getFileId(), fi.getFilePath(), fi.getOriginalPath());
    }

    /**
     * 解析基础存储路径（优先级：环境变量 ERP_FILE_STORAGE_PATH → 系统属性 erp.file.storage.path → 配置文件 file.base-path）
     *
     * @return 基础存储路径
     */
    private String resolveBasePath() {
        String envPath = System.getenv("ERP_FILE_STORAGE_PATH");
        if (StringUtils.hasText(envPath)) return envPath;
        String sysPath = System.getProperty("erp.file.storage.path");
        if (StringUtils.hasText(sysPath)) return sysPath;
        return fileStorageConfig.getBasePath();
    }

    /**
     * 将相对路径解析为绝对路径，并校验未越出根目录（防目录穿越）
     *
     * @param relativePath 相对路径（为空或"/"表示根目录）
     * @param root         根目录类型
     * @return 规范化后的绝对路径
     * @throws SecurityException 路径越出根目录时抛出
     */
    private Path resolveSafePath(String relativePath, String root) {
        Path basePath = resolveRootPath(root);
        if (!StringUtils.hasText(relativePath) || "/".equals(relativePath.trim())) {
            return basePath;
        }
        Path resolved = basePath.resolve(relativePath.trim()).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new SecurityException("非法路径访问: " + relativePath);
        }
        return resolved;
    }

    /**
     * 校验名称合法性（非空，且不允许包含..、/、\）
     *
     * @param name 文件或文件夹名称
     * @throws IllegalArgumentException 名称为空时抛出
     * @throws SecurityException        包含非法路径字符时抛出
     */
    private void validatePath(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("名称不能为空");
        }
        if (name.contains("..") || name.contains("/") || name.contains("\\")) {
            throw new SecurityException("非法路径: " + name);
        }
    }

    /**
     * 统一路径分隔符为"/"（Windows反斜杠转正斜杠）
     *
     * @param path 原始路径（可为null）
     * @return 统一分隔符后的路径，null返回空串
     */
    private String normalizePath(String path) {
        if (path == null) return "";
        return path.replace("\\", "/");
    }

    /**
     * 获取父级相对路径
     *
     * @param relativePath 相对路径（为空或"/"表示根目录）
     * @return 父级相对路径，无父级时返回null
     */
    private String getParentPath(String relativePath) {
        if (!StringUtils.hasText(relativePath) || "/".equals(relativePath.trim())) {
            return null;
        }
        Path parent = Paths.get(relativePath.trim()).getParent();
        return parent != null ? normalizePath(parent.toString()) : null;
    }

    /**
     * 获取文件扩展名（不含点号，统一小写；无扩展名时返回空串）
     *
     * @param filename 文件名（可为null）
     * @return 小写扩展名
     */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * 根据扩展名映射前端图标类型
     *
     * @param extension 小写扩展名（可为空）
     * @return 图标类型：image/document/video/audio/archive/other
     */
    private String getIconType(String extension) {
        if (!StringUtils.hasText(extension)) return "other";
        Set<String> images = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg");
        Set<String> docs = Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv");
        Set<String> videos = Set.of("mp4", "avi", "mov", "wmv", "flv", "mkv");
        Set<String> audios = Set.of("mp3", "wav", "flac", "aac", "ogg");
        Set<String> archives = Set.of("zip", "rar", "7z", "tar", "gz");

        if (images.contains(extension)) return "image";
        if (docs.contains(extension)) return "document";
        if (videos.contains(extension)) return "video";
        if (audios.contains(extension)) return "audio";
        if (archives.contains(extension)) return "archive";
        return "other";
    }

    /**
     * 将文件修改时间格式化为 yyyy-MM-dd HH:mm 字符串
     *
     * @param fileTime 文件修改时间
     * @return 格式化后的时间字符串
     */
    private String formatTime(java.nio.file.attribute.FileTime fileTime) {
        return LocalDateTime.ofInstant(fileTime.toInstant(), java.time.ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /**
     * 递归复制目录（保持目录层级，已存在文件覆盖）
     *
     * @param source 源目录
     * @param target 目标目录
     * @throws IOException 复制失败时抛出
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (Stream<Path> stream = Files.list(source)) {
            stream.forEach(child -> {
                try {
                    Path childTarget = target.resolve(child.getFileName().toString());
                    if (Files.isDirectory(child)) {
                        copyDirectory(child, childTarget);
                    } else {
                        Files.copy(child, childTarget, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("复制失败: " + e.getMessage());
                }
            });
        }
    }

    /**
     * 递归删除目录（自底向上逐个删除）
     *
     * @param dir 待删除目录
     * @throws IOException 删除失败时抛出
     */
    private void deleteDirectory(Path dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException("删除失败: " + e.getMessage());
                }
            });
        }
    }

    // endregion
}
