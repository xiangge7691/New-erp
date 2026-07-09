package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Config.FileStorageConfig;
import com.tonghui.erp.Common.Dto.FileManager.DirectoryListingDto;
import com.tonghui.erp.Common.Dto.FileManager.FileItemDto;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.mapper.FileInfoMapper;
import com.tonghui.erp.Service.FileManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件管理器服务实现
 */
@Service
public class FileManagerServiceImpl implements FileManagerService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    @Autowired
    private FileStorageConfig fileStorageConfig;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    // endregion

    // region 常量定义
    // ===================================
    // 常量定义
    // ===================================

    /** 业务文件根目录类型 */
    private static final String ROOT_BUSINESS = "business";

    /** 自定义文件根目录类型 */
    private static final String ROOT_CUSTOM = "custom";

    // endregion

    // region 目录操作
    // ===================================
    // 目录操作
    // ===================================

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
                stream.sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(path -> {
                        FileItemDto item = new FileItemDto();
                        item.setName(path.getFileName().toString());
                        item.setPath(normalizePath(basePath.relativize(path).toString()));

                        if (Files.isDirectory(path)) {
                            item.setDirectory(true);
                            item.setSize(0L);
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
                            String name = path.getFileName().toString();
                            item.setExtension(getExtension(name));
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

    @Override
    public void createFolder(String parentPath, String folderName, String root) {
        validatePath(folderName);
        Path dir = resolveSafePath(parentPath, root).resolve(folderName);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("创建文件夹失败: " + e.getMessage());
        }
    }

    @Override
    public void rename(String relativePath, String newName, String root) {
        checkNotBusiness(root, "重命名");
        validatePath(newName);
        Path source = resolveSafePath(relativePath, root);
        Path target = source.getParent().resolve(newName);
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("重命名失败: " + e.getMessage());
        }
    }

    @Override
    public void move(String sourcePath, String targetDirectory, String root) {
        checkNotBusiness(root, "移动");
        Path source = resolveSafePath(sourcePath, root);
        Path targetDir = resolveSafePath(targetDirectory, root);
        Path target = targetDir.resolve(source.getFileName().toString());
        try {
            Files.createDirectories(targetDir);
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("移动失败: " + e.getMessage());
        }
    }

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
    }

    @Override
    public void delete(String relativePath, String root) {
        checkNotBusiness(root, "删除");
        Path target = resolveSafePath(relativePath, root);
        try {
            if (Files.isDirectory(target)) {
                deleteDirectory(target);
            } else {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            throw new RuntimeException("删除失败: " + e.getMessage());
        }
    }

    // endregion

    // region 文件操作
    // ===================================
    // 文件操作
    // ===================================

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

        Long currentUserId = EntityUtils.getCurrentUserId();
        fileInfo.setCreatedBy(currentUserId);
        fileInfo.setCreatedTime(LocalDateTime.now());
        fileInfo.setUpdatedTime(LocalDateTime.now());

        fileInfoMapper.insert(fileInfo);
        fileInfo.setFileUrl("/api/files/" + fileInfo.getFileId());
        fileInfoMapper.updateById(fileInfo);

        return fileInfo;
    }

    @Override
    public InputStream downloadFile(String relativePath, String root) throws IOException {
        Path file = resolveSafePath(relativePath, root);
        if (!Files.exists(file) || Files.isDirectory(file)) {
            throw new IOException("文件不存在: " + relativePath);
        }
        return Files.newInputStream(file);
    }

    @Override
    public InputStream previewFile(String relativePath, String root) throws IOException {
        return downloadFile(relativePath, root);
    }

    @Override
    public List<FileItemDto> searchFiles(String keyword, String relativePath, String root) {
        Path searchRoot = resolveSafePath(relativePath, root);
        Path basePath = resolveRootPath(root);
        List<FileItemDto> results = new ArrayList<>();

        if (!Files.exists(searchRoot)) {
            return results;
        }

        try (Stream<Path> walk = Files.walk(searchRoot)) {
            walk.filter(path -> {
                String name = path.getFileName().toString().toLowerCase();
                return name.contains(keyword.toLowerCase());
            }).forEach(path -> {
                FileItemDto item = new FileItemDto();
                item.setName(path.getFileName().toString());
                item.setPath(normalizePath(basePath.relativize(path).toString()));

                if (Files.isDirectory(path)) {
                    item.setDirectory(true);
                    item.setSize(0L);
                    item.setIconType("folder");
                } else {
                    item.setDirectory(false);
                    try {
                        item.setSize(Files.size(path));
                    } catch (IOException e) {
                        item.setSize(0L);
                    }
                    String ext = getExtension(item.getName());
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
            throw new RuntimeException("搜索失败: " + e.getMessage());
        }

        return results;
    }

    // endregion

    // region 私有方法
    // ===================================
    // 私有方法
    // ===================================

    /**
     * 根据根目录类型获取对应的基础路径
     *
     * @param root 根目录类型："business" 或 "custom"
     * @return 对应的基础路径
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
     * @param action 操作名称
     */
    private void checkNotBusiness(String root, String action) {
        if (ROOT_BUSINESS.equals(root)) {
            throw new SecurityException("业务文件不允许" + action);
        }
    }

    private String resolveBasePath() {
        String envPath = System.getenv("ERP_FILE_STORAGE_PATH");
        if (StringUtils.hasText(envPath)) return envPath;
        String sysPath = System.getProperty("erp.file.storage.path");
        if (StringUtils.hasText(sysPath)) return sysPath;
        return fileStorageConfig.getBasePath();
    }

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

    private void validatePath(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("名称不能为空");
        }
        if (name.contains("..") || name.contains("/") || name.contains("\\")) {
            throw new SecurityException("非法路径: " + name);
        }
    }

    private String normalizePath(String path) {
        if (path == null) return "";
        return path.replace("\\", "/");
    }

    private String getParentPath(String relativePath) {
        if (!StringUtils.hasText(relativePath) || "/".equals(relativePath.trim())) {
            return null;
        }
        Path parent = Paths.get(relativePath.trim()).getParent();
        return parent != null ? normalizePath(parent.toString()) : null;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

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

    private String formatTime(java.nio.file.attribute.FileTime fileTime) {
        return LocalDateTime.ofInstant(fileTime.toInstant(), java.time.ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

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
