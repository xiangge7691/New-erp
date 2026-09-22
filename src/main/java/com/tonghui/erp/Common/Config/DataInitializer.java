package com.tonghui.erp.Common.Config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Config.InitDataConfig.*;
import com.tonghui.erp.Common.utils.PasswordHasher;
import com.tonghui.erp.Data.Entity.*;
import com.tonghui.erp.Service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一数据初始化器
 * <p>
 * 在系统启动完成后根据各模块的enabled开关决定是否执行初始化。
 * 替代原有的RootUserInitializer，提供更灵活的初始化控制。
 * </p>
 */
@Component
public class DataInitializer {

    // region 字段定义
    // ===================================
    // 字段定义
    // ===================================

    private final RootInitConfig rootConfig;
    private final UnitInitConfig unitConfig;
    private final DosageFormInitConfig dosageFormConfig;
    private final PreparationInitConfig preparationConfig;
    private final PreparationFormulaInitConfig preparationFormulaConfig;

    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final UserRoleService userRoleService;
    private final RolePermService rolePermService;
    private final UnitService unitService;
    private final DosageFormService dosageFormService;
    private final PreparationService preparationService;
    private final PreparationFormulaService preparationFormulaService;
    private final MaterialService materialService;

    // endregion

    // region 构造方法
    // ===================================
    // 构造方法
    // ===================================

    @Autowired
    public DataInitializer(RootInitConfig rootConfig,
                           UnitInitConfig unitConfig,
                           DosageFormInitConfig dosageFormConfig,
                           PreparationInitConfig preparationConfig,
                           PreparationFormulaInitConfig preparationFormulaConfig,
                           UserService userService,
                           RoleService roleService,
                           PermissionService permissionService,
                           UserRoleService userRoleService,
                           RolePermService rolePermService,
                           UnitService unitService,
                           DosageFormService dosageFormService,
                           PreparationService preparationService,
                           PreparationFormulaService preparationFormulaService,
                           MaterialService materialService) {
        this.rootConfig = rootConfig;
        this.unitConfig = unitConfig;
        this.dosageFormConfig = dosageFormConfig;
        this.preparationConfig = preparationConfig;
        this.preparationFormulaConfig = preparationFormulaConfig;
        this.userService = userService;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.userRoleService = userRoleService;
        this.rolePermService = rolePermService;
        this.unitService = unitService;
        this.dosageFormService = dosageFormService;
        this.preparationService = preparationService;
        this.preparationFormulaService = preparationFormulaService;
        this.materialService = materialService;
    }

    // endregion

    // region 初始化入口
    // ===================================
    // 初始化入口
    // ===================================

    /**
     * 系统启动完成后自动执行初始化逻辑
     * <p>
     * 使用 @EventListener(ApplicationReadyEvent.class) 确保在所有 Bean 初始化完成后再执行
     * 根据各模块的enabled开关决定是否执行对应初始化
     * </p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeData() {
        try {
            System.out.println("===== 开始执行数据初始化 =====");

            // 初始化root用户/角色
            if (rootConfig.isEnabled()) {
                initializeRoot();
            } else {
                System.out.println("[Root] 已禁用，跳过初始化");
            }

            // 初始化计量单位
            if (unitConfig.isEnabled()) {
                initializeUnits();
            } else {
                System.out.println("[Unit] 已禁用，跳过初始化");
            }

            // 初始化剂型信息
            if (dosageFormConfig.isEnabled()) {
                initializeDosageForms();
            } else {
                System.out.println("[DosageForm] 已禁用，跳过初始化");
            }

            // 初始化制剂信息
            if (preparationConfig.isEnabled()) {
                initializePreparations();
            } else {
                System.out.println("[Preparation] 已禁用，跳过初始化");
            }

            // 初始化制剂处方信息
            if (preparationFormulaConfig.isEnabled()) {
                initializePreparationFormulas();
            } else {
                System.out.println("[PreparationFormula] 已禁用，跳过初始化");
            }

            System.out.println("===== 数据初始化完成 =====");
        } catch (Exception e) {
            System.err.println("数据初始化时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // endregion

    // region Root用户/角色初始化
    // ===================================
    // Root用户/角色初始化
    // ===================================

    /**
     * 初始化root用户和角色
     */
    private void initializeRoot() {
        System.out.println("[Root] 开始初始化root用户和角色...");

        // 确保root角色存在
        Role rootRole = ensureRootRoleExists();

        // 确保root用户存在
        User rootUser = ensureRootUserExists();

        // 绑定root用户和root角色
        ensureRootUserRoleBinding(rootUser, rootRole);

        // 确保root角色拥有所有权限
        ensureRootRoleHasAllPermissions(rootRole);

        System.out.println("[Root] root用户和角色初始化完成");
    }

    /**
     * 确保root角色存在
     *
     * @return root角色对象
     */
    private Role ensureRootRoleExists() {
        Role role = roleService.getOne(
                new QueryWrapper<Role>().eq("role_name", rootConfig.getRoleName())
        );

        if (role == null) {
            role = new Role();
            role.setRoleName(rootConfig.getRoleName());
            role.setRoleDesc(rootConfig.getRoleDesc());
            role.setRoleStatus(1);
            role.setCreatedTime(LocalDateTime.now());
            role.setUpdatedTime(LocalDateTime.now());
            roleService.save(role);
        }

        return role;
    }

    /**
     * 确保root用户存在
     *
     * @return root用户对象
     */
    private User ensureRootUserExists() {
        User user = userService.getOne(
                new QueryWrapper<User>().eq("user_account", rootConfig.getUserAccount())
        );

        if (user == null) {
            user = new User();
            user.setUserAccount(rootConfig.getUserAccount());
            user.setUserName(rootConfig.getUserName());
            user.setPassword(PasswordHasher.hashPassword(rootConfig.getPassword()));
            user.setPhone("");
            user.setGender(1);
            user.setUserStatus(1);
            user.setCreatedTime(LocalDateTime.now());
            user.setUpdatedTime(LocalDateTime.now());
            userService.save(user);
        }

        return user;
    }

    /**
     * 确保root用户与root角色绑定
     *
     * @param user root用户
     * @param role root角色
     */
    private void ensureRootUserRoleBinding(User user, Role role) {
        UserRole userRole = userRoleService.getOne(
                new QueryWrapper<UserRole>()
                        .eq("user_id", user.getUserId())
                        .eq("role_id", role.getRoleId())
        );

        if (userRole == null) {
            userRole = new UserRole();
            userRole.setUserId(user.getUserId());
            userRole.setRoleId(role.getRoleId());
            userRole.setCreatedTime(LocalDateTime.now());
            userRoleService.save(userRole);
        }
    }

    /**
     * 确保root角色拥有所有权限
     *
     * @param role root角色
     */
    private void ensureRootRoleHasAllPermissions(Role role) {
        List<Permission> allPermissions = permissionService.list();

        if (allPermissions.isEmpty()) {
            System.out.println("[Root] 当前系统没有权限数据，跳过权限同步");
            return;
        }

        // 清理可能存在的软删除脏数据
        rolePermService.cleanSoftDeletedByRoleId(role.getRoleId());

        // 查询当前已有的有效权限ID
        List<RolePerm> existingRolePerms = rolePermService.list(
                new QueryWrapper<RolePerm>().eq("role_id", role.getRoleId())
        );
        java.util.Set<Long> existingPermIds = new java.util.HashSet<>();
        for (RolePerm rp : existingRolePerms) {
            existingPermIds.add(rp.getPermId());
        }

        // 只插入缺失的权限
        int addedCount = 0;
        for (Permission permission : allPermissions) {
            if (!existingPermIds.contains(permission.getPermId())) {
                RolePerm rolePerm = new RolePerm();
                rolePerm.setRoleId(role.getRoleId());
                rolePerm.setPermId(permission.getPermId());
                rolePerm.setIsDeleted(0);
                rolePerm.setCreatedTime(LocalDateTime.now());
                rolePermService.save(rolePerm);
                addedCount++;
            }
        }

        System.out.println("[Root] root角色权限同步完成，共有 " + allPermissions.size() + " 个权限，新增 " + addedCount + " 个");
    }

    // endregion

    // region 计量单位初始化
    // ===================================
    // 计量单位初始化
    // ===================================

    /**
     * 初始化计量单位数据
     */
    private void initializeUnits() {
        System.out.println("[Unit] 开始初始化计量单位...");

        int addedCount = 0;
        for (UnitInitConfig.UnitData unitData : unitConfig.getData()) {
            // 检查是否已存在同名单位
            Unit existing = unitService.getOne(
                    new QueryWrapper<Unit>().eq("unit_name", unitData.getUnitName())
            );

            if (existing == null) {
                Unit unit = new Unit();
                unit.setUnitName(unitData.getUnitName());
                unit.setSymbol(unitData.getSymbol());
                unit.setStatus(unitData.getStatus() != null ? unitData.getStatus() : 1);
                unit.setIsDeleted(0);
                unit.setCreatedTime(LocalDateTime.now());
                unit.setUpdatedTime(LocalDateTime.now());
                unitService.save(unit);
                addedCount++;
            }
        }

        System.out.println("[Unit] 计量单位初始化完成，共 " + unitConfig.getData().size() + " 条配置，新增 " + addedCount + " 条");
    }

    // endregion

    // region 剂型信息初始化
    // ===================================
    // 剂型信息初始化
    // ===================================

    /**
     * 初始化剂型信息数据
     */
    private void initializeDosageForms() {
        System.out.println("[DosageForm] 开始初始化剂型信息...");

        int addedCount = 0;
        for (DosageFormInitConfig.DosageFormData formData : dosageFormConfig.getData()) {
            // 检查是否已存在同类别同名称的剂型
            QueryWrapper<DosageForm> wrapper = new QueryWrapper<>();
            wrapper.eq("dosage_category", formData.getDosageCategory());
            if (formData.getDosageName() != null && !formData.getDosageName().isEmpty()) {
                wrapper.eq("dosage_name", formData.getDosageName());
            } else {
                wrapper.isNull("dosage_name").or().eq("dosage_name", "");
            }

            DosageForm existing = dosageFormService.getOne(wrapper, false);

            if (existing == null) {
                DosageForm dosageForm = new DosageForm();
                dosageForm.setDosageCategory(formData.getDosageCategory());
                dosageForm.setDosageName(formData.getDosageName());
                dosageForm.setStatus(formData.getStatus() != null ? formData.getStatus() : 1);
                dosageForm.setIsDeleted(0);
                dosageForm.setCreatedTime(LocalDateTime.now());
                dosageForm.setUpdatedTime(LocalDateTime.now());
                dosageFormService.save(dosageForm);
                addedCount++;
            }
        }

        System.out.println("[DosageForm] 剂型信息初始化完成，共 " + dosageFormConfig.getData().size() + " 条配置，新增 " + addedCount + " 条");
    }

    // endregion

    // region 制剂信息初始化
    // ===================================
    // 制剂信息初始化
    // ===================================

    /**
     * 初始化制剂信息数据
     */
    private void initializePreparations() {
        System.out.println("[Preparation] 开始初始化制剂信息...");

        int addedCount = 0;
        for (PreparationInitConfig.PreparationData prepData : preparationConfig.getData()) {
            // 检查是否已存在同编码的制剂
            Preparation existing = preparationService.getOne(
                    new QueryWrapper<Preparation>().eq("preparation_code", prepData.getPreparationCode())
            );

            if (existing == null) {
                Preparation preparation = new Preparation();
                preparation.setPreparationCode(prepData.getPreparationCode());
                preparation.setPreparationName(prepData.getPreparationName());
                preparation.setStatus(prepData.getStatus() != null ? prepData.getStatus() : 1);
                preparation.setIsDeleted(0);
                preparation.setCreatedTime(LocalDateTime.now());
                preparation.setUpdatedTime(LocalDateTime.now());
                preparationService.save(preparation);
                addedCount++;
            }
        }

        System.out.println("[Preparation] 制剂信息初始化完成，共 " + preparationConfig.getData().size() + " 条配置，新增 " + addedCount + " 条");
    }

    // endregion

    // region 制剂处方信息初始化
    // ===================================
    // 制剂处方信息初始化
    // ===================================

    /**
     * 初始化制剂处方信息数据
     * <p>
     * 通过制剂编码和物料编号关联到对应的ID，实现跨表引用
     * </p>
     */
    private void initializePreparationFormulas() {
        System.out.println("[PreparationFormula] 开始初始化制剂处方信息...");

        int addedCount = 0;
        int skippedCount = 0;

        for (PreparationFormulaInitConfig.FormulaData formulaData : preparationFormulaConfig.getData()) {
            // 查找制剂
            Preparation preparation = preparationService.getOne(
                    new QueryWrapper<Preparation>().eq("preparation_code", formulaData.getPreparationCode())
            );
            if (preparation == null) {
                skippedCount++;
                continue;
            }

            // 查找物料
            Material material = materialService.getMaterialByCode(formulaData.getMaterialCode());
            if (material == null) {
                skippedCount++;
                continue;
            }

            // 检查是否已存在同制剂同物料的处方
            long count = preparationFormulaService.count(
                    new QueryWrapper<PreparationFormula>()
                            .eq("preparation_id", preparation.getPreparationId())
                            .eq("material_id", material.getMaterialId())
            );

            if (count == 0) {
                PreparationFormula formula = new PreparationFormula();
                formula.setPreparationId(preparation.getPreparationId());
                formula.setPreparationCode(formulaData.getPreparationCode());
                formula.setPreparationName(preparation.getPreparationName());
                formula.setMaterialId(material.getMaterialId());
                formula.setMaterialCode(formulaData.getMaterialCode());
                formula.setMaterialName(formulaData.getMaterialName());
                formula.setMaterialCategory(formulaData.getMaterialCategory());
                formula.setDosage(formulaData.getDosage());
                formula.setUnitName(formulaData.getUnitName());
                formula.setIsDeleted(0);
                formula.setCreatedTime(LocalDateTime.now());
                formula.setUpdatedTime(LocalDateTime.now());
                preparationFormulaService.save(formula);
                addedCount++;
            }
        }

        System.out.println("[PreparationFormula] 制剂处方信息初始化完成，共 " + preparationFormulaConfig.getData().size() + " 条配置，新增 " + addedCount + " 条，跳过 " + skippedCount + " 条（关联数据不存在）");
    }

    // endregion
}
