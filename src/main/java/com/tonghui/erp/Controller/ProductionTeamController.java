package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Constants.OperationType;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.annotation.OperationLog;
import com.tonghui.erp.Data.Entity.ProductionTeam;
import com.tonghui.erp.Data.Entity.ProductionTeamMember;
import com.tonghui.erp.Service.ProductionTeamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 生产班组控制器
 * <p>
 * 维护班组名称、所属车间、成员与启停状态，排产时按班组带入人员。
 * </p>
 */
@RestController
@RequestMapping("/api/production-teams")
public class ProductionTeamController extends BaseController {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 生产班组服务 */
    private final ProductionTeamService productionTeamService;

    public ProductionTeamController(ProductionTeamService productionTeamService) {
        this.productionTeamService = productionTeamService;
    }

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 查询班组列表
     *
     * 示例请求：
     * GET /api/production-teams?keyword=灌装
     *
     * @param keyword 班组编码/名称关键字
     * @return 班组列表
     */
    @GetMapping
    public ApiResponse<List<ProductionTeam>> list(@RequestParam(required = false) String keyword) {
        try {
            return success(productionTeamService.listTeams(keyword));
        } catch (Exception ex) {
            return exception(ex, "查询班组列表");
        }
    }

    /**
     * 查询全部班组（含成员明细，排产/实际生产快捷选人用）
     *
     * 示例请求：
     * GET /api/production-teams/active
     *
     * @return 班组列表
     */
    @GetMapping("/active")
    public ApiResponse<List<ProductionTeam>> listActive() {
        try {
            return success(productionTeamService.listActive());
        } catch (Exception ex) {
            return exception(ex, "查询启用班组");
        }
    }

    /**
     * 查询班组详情
     *
     * 示例请求：
     * GET /api/production-teams/1
     *
     * @param id 班组ID
     * @return 班组详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ProductionTeam> detail(@PathVariable Long id) {
        try {
            ProductionTeam team = productionTeamService.getTeamDetail(id);
            return team == null ? error("班组不存在") : success(team);
        } catch (Exception ex) {
            return exception(ex, "查询班组详情");
        }
    }

    /**
     * 查询班组成员
     *
     * 示例请求：
     * GET /api/production-teams/1/members
     *
     * @param id 班组ID
     * @return 成员列表（含姓名、工号）
     */
    @GetMapping("/{id}/members")
    public ApiResponse<List<ProductionTeamMember>> members(@PathVariable Long id) {
        try {
            return success(productionTeamService.listMembers(id));
        } catch (Exception ex) {
            return exception(ex, "查询班组成员");
        }
    }

    // endregion

    // region 维护
    // ===================================
    // 维护
    // ===================================

    /**
     * 新增班组
     *
     * 示例请求：
     * POST /api/production-teams
     * {
     *   "teamCode": "BZ001",
     *   "teamName": "灌装一班",
     *   "memberIds": [71, 72, 73]
     * }
     *
     * @param team 班组
     * @return 新增后的班组
     */
    @OperationLog(module = "生产班组", type = OperationType.CREATE, desc = "新增班组",
            businessType = "PRODUCTION_TEAM", businessId = "#team.id")
    @PostMapping
    public ApiResponse<ProductionTeam> create(@RequestBody ProductionTeam team) {
        try {
            return success(productionTeamService.createTeam(team), "创建成功");
        } catch (Exception ex) {
            return exception(ex, "新增班组");
        }
    }

    /**
     * 修改班组
     *
     * 示例请求：
     * PUT /api/production-teams/1
     * {
     *   "teamCode": "BZ001",
     *   "teamName": "灌装一班",
     *   "memberIds": [71, 72]
     * }
     *
     * @param id   班组ID
     * @param team 班组
     * @return 修改后的班组
     */
    @OperationLog(module = "生产班组", type = OperationType.UPDATE, desc = "修改班组",
            businessType = "PRODUCTION_TEAM", businessId = "#id")
    @PutMapping("/{id}")
    public ApiResponse<ProductionTeam> update(@PathVariable Long id, @RequestBody ProductionTeam team) {
        try {
            return success(productionTeamService.updateTeam(id, team), "更新成功");
        } catch (Exception ex) {
            return exception(ex, "修改班组");
        }
    }

    // endregion
}
