package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Constants.OperationType;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PreparationRenewalDtos;
import com.tonghui.erp.Common.annotation.OperationLog;
import com.tonghui.erp.Service.PreparationRenewalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 制剂续展文件控制器
 * <p>
 * 续展文件的上传/删除复用业务附件接口：
 * POST /api/business-files/upload（bizType=PREPARATION_RENEWAL）
 * DELETE /api/business-files/{relationId}
 * 本控制器只提供分组查询、续展备注保存与打包下载。
 * </p>
 *
 * 接口清单：
 * ┌────┬────────────────────────────────────────────────┬────────┬─────────────────────┐
 * │ #  │ 接口                                           │ 方法   │ 说明                │
 * ├────┼────────────────────────────────────────────────┼────────┼─────────────────────┤
 * │ 1  │ /api/preparation/{id}/renewal-files            │ GET    │ 查询 10 组续展文件  │
 * │ 2  │ /api/preparation/{id}/renewal-remark           │ PUT    │ 保存续展备注        │
 * │ 3  │ /api/preparation/{id}/renewal-files/export     │ GET    │ 打包下载 ZIP        │
 * └────┴────────────────────────────────────────────────┴────────┴─────────────────────┘
 */
@RestController
@RequestMapping("/api/preparation/{preparationId}")
public class PreparationRenewalController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 制剂续展文件服务 */
    @Autowired
    private PreparationRenewalService preparationRenewalService;

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 查询制剂的续展文件
     * <p>
     * 固定返回 10 个文件类型分组（按申报清单顺序），无文件的类型返回空数组，
     * 同时返回续展备注，供「续展文件」选项卡一次渲染。
     * </p>
     *
     * 示例请求：
     * GET /api/preparation/12/renewal-files
     *
     * @param preparationId 制剂ID（路径参数）
     * @return 续展文件分组与续展备注
     */
    @GetMapping("/renewal-files")
    public ApiResponse<PreparationRenewalDtos.RenewalFiles> getRenewalFiles(@PathVariable Long preparationId) {
        try {
            return success(preparationRenewalService.getRenewalFiles(preparationId));
        } catch (Exception ex) {
            return exception(ex, "查询续展文件");
        }
    }

    // endregion

    // region 备注
    // ===================================
    // 备注
    // ===================================

    /**
     * 保存续展备注
     * <p>仅更新 renewal_remark 字段，传空串表示清空。</p>
     *
     * 示例请求：
     * PUT /api/preparation/12/renewal-remark
     * {
     *   "renewalRemark": "2027-03 到期，已启动续展"
     * }
     *
     * @param preparationId 制剂ID（路径参数）
     * @param request       续展备注请求体
     * @return 操作结果
     */
    @OperationLog(module = "制剂管理", type = OperationType.UPDATE, desc = "保存续展备注",
            businessType = "PREPARATION", businessId = "#preparationId")
    @PutMapping("/renewal-remark")
    public ApiResponse<Void> updateRenewalRemark(@PathVariable Long preparationId,
                                                 @RequestBody PreparationRenewalDtos.RenewalRemarkRequest request) {
        try {
            preparationRenewalService.updateRenewalRemark(preparationId,
                    request == null ? null : request.getRenewalRemark());
            return success(null, "续展备注保存成功");
        } catch (Exception ex) {
            return exception(ex, "保存续展备注");
        }
    }

    // endregion

    // region 打包下载
    // ===================================
    // 打包下载
    // ===================================

    /**
     * 打包下载续展文件
     * <p>
     * 压缩包名为「{制剂名}续展文件.zip」，包内按文件类型建立 10 个文件夹，
     * 无文件的类型也会保留空文件夹。
     * </p>
     *
     * 示例请求：
     * GET /api/preparation/12/renewal-files/export
     *
     * @param preparationId 制剂ID（路径参数）
     * @return ZIP 文件（application/zip）；失败时返回 JSON 错误信息
     */
    @OperationLog(module = "制剂管理", type = OperationType.EXPORT, desc = "下载续展文件包",
            businessType = "PREPARATION", businessId = "#preparationId")
    @GetMapping("/renewal-files/export")
    public ResponseEntity<?> exportRenewalFiles(@PathVariable Long preparationId) {
        try {
            PreparationRenewalDtos.RenewalExport export = preparationRenewalService.exportRenewalFiles(preparationId);
            String encodedName = URLEncoder.encode(export.getFileName(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .body(export.getData());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.errorResponse("下载续展文件失败: " + ex.getMessage()));
        }
    }

    // endregion
}
