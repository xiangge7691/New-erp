package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.BusinessFile.BizFileTypeDto;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileBatchListDto;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileDto;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileSyncDto;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Service.BusinessFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 业务附件控制器
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬─────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                        │
 * ├────┼──────────────────────────────────────┼────────┼─────────────────────────────┤
 * │ 1  │ /api/business-files/upload           │ POST   │ 上传并绑定业务记录          │
 * │ 2  │ /api/business-files/upload-temp      │ POST   │ 临时上传（保存时再绑定）    │
 * │ 3  │ /api/business-files                  │ GET    │ 查询业务附件                │
 * │ 4  │ /api/business-files/batch-list       │ POST   │ 批量查询多个业务记录的附件  │
 * │ 5  │ /api/business-files/sync             │ POST   │ 保存单据时同步附件          │
 * │ 6  │ /api/business-files/{id}             │ DELETE │ 解绑并删除附件              │
 * │ 7  │ /api/business-files/types            │ GET    │ 业务类型注册表              │
 * └────┴──────────────────────────────────────┴────────┴─────────────────────────────┘
 */
@RestController
@RequestMapping("/api/business-files")
public class BusinessFileController extends BaseController {

    /** 业务附件服务 */
    @Autowired
    private BusinessFileService businessFileService;

    // region 上传
    // ===================================
    // 上传
    // ===================================

    /**
     * 上传并绑定业务附件
     *
     * 示例请求：
     * POST /api/business-files/upload
     * Content-Type: multipart/form-data
     * body:
     *   file:         [选择文件]
     *   bizType:      WORK_ORDER_PROCESS_EXECUTION
     *   bizId:        123
     *   refId:        0
     *   fileKind:     PROCESS_ATTACHMENT
     *   entityName:   工单工序
     *   description:  工序附件
     *
     * @param file        文件
     * @param bizType     业务类型编码
     * @param bizId       业务ID
     * @param refId       子对象ID（可选）
     * @param fileKind    文件槽位（可选，默认 DEFAULT）
     * @param entityName  实体名称（可选，用于目录）
     * @param description 文件描述（可选）
     * @return 附件信息
     */
    @PostMapping("/upload")
    public ApiResponse<BusinessFileDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String bizType,
            @RequestParam Long bizId,
            @RequestParam(required = false) Long refId,
            @RequestParam(required = false) String fileKind,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String description) {
        try {
            return success(businessFileService.upload(file, bizType, bizId, refId, fileKind, entityName, description),
                    "文件上传成功");
        } catch (IOException e) {
            return exception(e, "文件上传");
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    /**
     * 临时上传（不绑定业务，保存单据时通过 sync 绑定）
     *
     * 示例请求：
     * POST /api/business-files/upload-temp
     * Content-Type: multipart/form-data
     * body:
     *   file:         [选择文件]
     *   bizType:      WORK_ORDER_PROCESS_EXECUTION
     *   entityName:   工单工序
     *   description:  工序附件
     *
     * @param file        文件
     * @param bizType     业务类型编码
     * @param entityName  实体名称（可选）
     * @param description 文件描述（可选）
     * @return 文件元数据（含 fileId）
     */
    @PostMapping("/upload-temp")
    public ApiResponse<FileInfo> uploadTemp(
            @RequestParam("file") MultipartFile file,
            @RequestParam String bizType,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String description) {
        try {
            return success(businessFileService.uploadTemp(file, bizType, entityName, description),
                    "文件上传成功");
        } catch (IOException e) {
            return exception(e, "文件上传");
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 查询业务附件
     *
     * 示例请求：
     * GET /api/business-files?bizType=WORK_ORDER_PROCESS_EXECUTION&bizId=123
     * GET /api/business-files?bizType=APPROVAL_INSTANCE&bizId=88&refId=45
     *
     * @param bizType  业务类型编码
     * @param bizId    业务ID
     * @param refId    子对象ID（可选）
     * @param fileKind 文件槽位（可选）
     * @return 附件列表
     */
    @GetMapping
    public ApiResponse<List<BusinessFileDto>> list(
            @RequestParam String bizType,
            @RequestParam Long bizId,
            @RequestParam(required = false) Long refId,
            @RequestParam(required = false) String fileKind) {
        try {
            return success(businessFileService.list(bizType, bizId, refId, fileKind));
        } catch (Exception e) {
            return exception(e, "查询业务附件");
        }
    }

    /**
     * 批量查询多个业务记录的附件
     *
     * 示例请求：
     * POST /api/business-files/batch-list
     * {
     *   "bizType": "APPROVAL_INSTANCE",
     *   "bizIds": [88, 89, 90]
     * }
     *
     * @param dto 批量查询请求
     * @return 业务ID -> 附件列表
     */
    @PostMapping("/batch-list")
    public ApiResponse<Map<Long, List<BusinessFileDto>>> batchList(@RequestBody BusinessFileBatchListDto dto) {
        try {
            return success(businessFileService.listBatch(dto.getBizType(), dto.getBizIds(), dto.getFileKind()));
        } catch (Exception e) {
            return exception(e, "批量查询业务附件");
        }
    }

    // endregion

    // region 同步与删除
    // ===================================
    // 同步与删除
    // ===================================

    /**
     * 保存单据时同步附件
     *
     * 示例请求：
     * POST /api/business-files/sync
     * {
     *   "bizType": "WORK_ORDER_PROCESS_EXECUTION",
     *   "bizId": 123,
     *   "refId": 0,
     *   "fileKind": "PROCESS_ATTACHMENT",
     *   "fileIds": [101, 102]
     * }
     *
     * @param dto 同步请求
     * @return 同步后的附件列表
     */
    @PostMapping("/sync")
    public ApiResponse<List<BusinessFileDto>> sync(@RequestBody BusinessFileSyncDto dto) {
        try {
            return success(businessFileService.sync(dto), "附件同步成功");
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return exception(e, "附件同步");
        }
    }

    /**
     * 解绑并删除附件
     *
     * 示例请求：
     * DELETE /api/business-files/1001
     *
     * @param id 关系ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        try {
            businessFileService.delete(id);
            return success(true, "附件删除成功");
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return exception(e, "附件删除");
        }
    }

    // endregion

    // region 注册表
    // ===================================
    // 注册表
    // ===================================

    /**
     * 获取业务类型注册表
     *
     * 示例请求：
     * GET /api/business-files/types
     *
     * @return 业务类型列表
     */
    @GetMapping("/types")
    public ApiResponse<List<BizFileTypeDto>> types() {
        return success(businessFileService.listTypes());
    }

    // endregion
}
