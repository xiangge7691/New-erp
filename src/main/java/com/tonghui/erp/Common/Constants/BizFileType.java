package com.tonghui.erp.Common.Constants;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 业务附件类型注册表
 * <p>
 * 一表一码：每个业务表只对应一个编码，从根上避免“同一 business_type 被多张表复用”。
 * 生产领料单使用独立的 production_pick 表，与库存出库单（stock_out）分开编码。
 * 每个类型声明允许的文件槽位及每个槽位的最大数量（-1 表示不限）。
 * 业务表名、主键列名均来自枚举常量，可安全用于服务层拼接校验 SQL。
 * </p>
 */
@Getter
public enum BizFileType {

    // region 货物验收
    // ===================================
    // 货物验收
    // ===================================

    /** 货物验收单 */
    ACCEPTANCE_ORDER("ACCEPTANCE_ORDER", "acceptance_order", "acceptance_id", "货物验收单", "货物验收/验收单",
            kinds(BizFileKinds.WAYBILL, 10, BizFileKinds.INSPECTION_REPORT, 10,
                  BizFileKinds.INVOICE, 10, BizFileKinds.INBOUND_ORDER, 10)),

    // endregion

    // region 质量管理
    // ===================================
    // 质量管理
    // ===================================

    /** 销毁记录 */
    DESTRUCTION_RECORD("DESTRUCTION_RECORD", "destruction_record", "id", "销毁记录", "质量管理/销毁记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 偏差记录 */
    DEVIATION_RECORD("DEVIATION_RECORD", "deviation_record", "id", "偏差记录", "质量管理/偏差记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 检验计划 */
    INSPECTION_PLAN("INSPECTION_PLAN", "inspection_plan", "id", "检验计划", "质量管理/检验计划",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 请检单 */
    INSPECTION_REQUEST("INSPECTION_REQUEST", "inspection_request", "id", "请检单", "质量管理/请检单",
            kinds(BizFileKinds.DEFAULT, 10, BizFileKinds.FINISHED_PRODUCT_REPORT, 10)),

    /** 检验记录 */
    INSPECTION_RECORD("INSPECTION_RECORD", "inspection_record", "id", "检验记录", "质量管理/检验记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 取样记录 */
    SAMPLING_RECORD("SAMPLING_RECORD", "sampling_record", "id", "取样记录", "质量管理/取样记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 审核放行 */
    RELEASE_REVIEW("RELEASE_REVIEW", "release_review", "id", "审核放行", "审核放行/放行记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 留样记录 */
    RETAINED_SAMPLE("RETAINED_SAMPLE", "retained_sample", "id", "留样记录", "留样管理/留样记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 验证记录 */
    VERIFICATION_RECORD("VERIFICATION_RECORD", "verification_record", "id", "验证记录", "验证方案/验证记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    // endregion

    // region 制剂与剂型
    // ===================================
    // 制剂与剂型
    // ===================================

    /** 剂型 */
    DOSAGE_FORM("DOSAGE_FORM", "dosage_form", "dosage_id", "剂型", "剂型信息/附件",
            kinds(BizFileKinds.BATCH_RECORD_TEMPLATE, 1,
                  BizFileKinds.VERIFICATION_TEMPLATE, 1,
                  BizFileKinds.INSPECTION_TEMPLATE, 1)),

    /** 制剂 */
    PREPARATION("PREPARATION", "preparation", "preparation_id", "制剂", "制剂管理/附件",
            kinds(BizFileKinds.PROCESS_SPEC, 1,
                  BizFileKinds.BATCH_RECORD_TEMPLATE, 1,
                  BizFileKinds.VERIFICATION_PLAN, 1,
                  BizFileKinds.QUALITY_STANDARD, 1,
                  BizFileKinds.PROCESS_FLOWCHART, 1,
                  BizFileKinds.DOSSIER_579, 1,
                  BizFileKinds.REGISTRATION_CERTIFICATE, 1,
                  BizFileKinds.PACKAGING_DESIGN, 1,
                  BizFileKinds.VERIFICATION_REPORT, 1)),

    /** 制剂工序模板 */
    PREPARATION_PROCESS_TEMPLATE("PREPARATION_PROCESS_TEMPLATE", "preparation_process_template", "template_id",
            "制剂工序模板", "制剂管理/工序模板", kinds(BizFileKinds.DEFAULT, 1)),

    // endregion

    // region 生产管理
    // ===================================
    // 生产管理
    // ===================================

    /** 生产计划 */
    PRODUCTION_PLAN("PRODUCTION_PLAN", "production_plan", "id", "生产计划", "生产管理/计划",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 工单 */
    WORK_ORDER("WORK_ORDER", "work_order", "work_order_id", "生产任务", "生产管理/工单",
            kinds(BizFileKinds.BATCH_RECORD, 10,
                  BizFileKinds.OUTBOUND_ORDER, 10,
                  BizFileKinds.INTERMEDIATE_INSPECTION_REQUEST, 10,
                  BizFileKinds.OTHER, 10)),

    /** 工单工序执行行 */
    WORK_ORDER_PROCESS_EXECUTION("WORK_ORDER_PROCESS_EXECUTION", "work_order_process_execution", "id",
            "工单工序", "生产管理/工序附件", kinds(BizFileKinds.PROCESS_ATTACHMENT, 1)),

    /** 生产单位 */
    PRODUCTION_UNIT("PRODUCTION_UNIT", "production_unit", "prod_unit_id", "生产单位", "生产管理/生产单位",
            kinds(BizFileKinds.DEFAULT, 10)),

    // endregion

    // region 库存出入库
    // ===================================
    // 库存出入库
    // ===================================

    /** 生产领料单 */
    STOCK_OUT_PRODUCTION("STOCK_OUT_PRODUCTION", "production_pick", "id", "生产领料单",
            "生产管理/生产领料", kinds(BizFileKinds.DEFAULT, 10)),

    /** 成品入库单（stock_in，in_type=成品入库） */
    STOCK_IN_PRODUCT("STOCK_IN_PRODUCT", "stock_in", "in_id", "成品入库单",
            "库存管理/成品入库", kinds(BizFileKinds.DEFAULT, 10)),

    /** 成品出库单（stock_out，out_type=销售出库） */
    STOCK_OUT_SALES("STOCK_OUT_SALES", "stock_out", "out_id", "成品出库单",
            "库存管理/成品出库", kinds(BizFileKinds.DEFAULT, 10)),

    /** 退库单 */
    RETURN_ORDER("RETURN_ORDER", "return_order", "id", "退库单",
            "库存管理/退库单", kinds(BizFileKinds.DEFAULT, 10)),

    // endregion

    // region 设备与能耗
    // ===================================
    // 设备与能耗
    // ===================================

    /** 设备 */
    EQUIPMENT("EQUIPMENT", "equipment", "equipment_id", "设备", "设备管理/设备附件",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 设备维保 */
    EQUIPMENT_MAINTENANCE("EQUIPMENT_MAINTENANCE", "equipment_maintenance", "maintenance_id", "设备维保",
            "设备管理/维保", kinds(BizFileKinds.DEFAULT, 10)),

    /** 能耗记录 */
    ENERGY_RECORD("ENERGY_RECORD", "energy_record", "record_id", "能耗记录", "能耗管理/能耗记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 养护记录 */
    MAINTENANCE_RECORD("MAINTENANCE_RECORD", "maintenance_record", "id", "养护记录", "库存管理/养护记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 养护记录明细 */
    MAINTENANCE_RECORD_DETAIL("MAINTENANCE_RECORD_DETAIL", "maintenance_record_detail", "id", "养护明细",
            "库存管理/养护明细", kinds(BizFileKinds.DEFAULT, 10)),

    // endregion

    // region 机构、人员、岗位
    // ===================================
    // 机构、人员、岗位
    // ===================================

    /** 机构信息 */
    ORGANIZATION("ORGANIZATION", "organization", "id", "机构信息", "机构管理/机构信息",
            kinds(BizFileKinds.LICENSE_SCAN, 5, BizFileKinds.PHOTO, 5)),

    /** 机构证书 */
    ORGANIZATION_CERTIFICATE("ORGANIZATION_CERTIFICATE", "organization_certificate", "id", "机构证书",
            "机构管理/机构证书", kinds(BizFileKinds.DEFAULT, 5)),

    /** 人员档案 */
    PERSONNEL_FILE("PERSONNEL_FILE", "personnel_file", "personnel_file_id", "人员档案", "人员管理/人员档案",
            kinds(BizFileKinds.ATTACHMENT, 10, BizFileKinds.HEALTH_FILE, 10)),

    /** 人员证书 */
    PERSONNEL_CERTIFICATE("PERSONNEL_CERTIFICATE", "personnel_certificate", "certificate_id", "人员证书",
            "人员管理/证书", kinds(BizFileKinds.DEFAULT, 5)),

    /** 岗位 */
    POSITION("POSITION", "position", "position_id", "岗位", "岗位信息/岗位附件",
            kinds(BizFileKinds.DEFAULT, 5)),

    /** 职务 */
    DUTY("DUTY", "duty", "duty_id", "职务", "职务信息/职务附件",
            kinds(BizFileKinds.DEFAULT, 5)),

    // endregion

    // region 采购与供应商
    // ===================================
    // 采购与供应商
    // ===================================

    /** 采购订单 */
    PURCHASE_ORDER("PURCHASE_ORDER", "purchase_orders", "id", "采购订单", "采购管理/订单",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 供应商信息 */
    PURCHASE_SUPPLIER("PURCHASE_SUPPLIER", "purchase_suppliers", "id", "供应商信息", "供应商管理/供应商信息",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 供应商审计 */
    SUPPLIER_AUDIT("SUPPLIER_AUDIT", "supplier_audit", "id", "供应商审计", "供应商管理/审计",
            kinds(BizFileKinds.DEFAULT, 10)),

    // endregion

    // region 车间环境
    // ===================================
    // 车间环境
    // ===================================

    /** 房间信息 */
    ROOM_INFO("ROOM_INFO", "room_info", "room_id", "房间信息", "车间环境/房间信息",
            kinds(BizFileKinds.DISINFECTION, 10,
                  BizFileKinds.CLEAN_INSPECTION, 10,
                  BizFileKinds.CLEANING_RECORD, 10)),

    /** 清洁记录 */
    CLEANING_RECORD("CLEANING_RECORD", "cleaning_record", "id", "清洁记录", "车间环境/清洁记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 洁净检测记录 */
    CLEAN_INSPECTION_RECORD("CLEAN_INSPECTION_RECORD", "clean_inspection_record", "id", "洁净检测记录",
            "车间环境/洁净检测", kinds(BizFileKinds.DEFAULT, 10)),

    // endregion

    // region 培训与审批
    // ===================================
    // 培训与审批
    // ===================================

    /** 培训记录 */
    TRAINING_RECORD("TRAINING_RECORD", "training_record", "id", "培训记录", "培训管理/培训记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /**
     * 审批实例
     * <p>
     * 审批节点属于流程定义，多个实例共用同一节点 ID，因此 biz_id 存实例 ID，
     * ref_id 存节点 ID；查询整个实例的附件时不传 refId，查询单个节点时传 refId。
     * </p>
     */
    APPROVAL_INSTANCE("APPROVAL_INSTANCE", "approval_instance", "id", "审批实例", "审批管理/审批附件",
            kinds(BizFileKinds.DEFAULT, 20)),

    // endregion
    ;

    /** 业务类型编码 */
    private final String code;

    /** 业务表名（仅来自枚举常量，可安全拼接 SQL） */
    private final String tableName;

    /** 业务表主键列名（仅来自枚举常量，可安全拼接 SQL） */
    private final String pkColumn;

    /** 显示名称 */
    private final String displayName;

    /** 磁盘存储目录（中文，支持多级） */
    private final String storageDir;

    /** 槽位 -> 最大数量（-1 表示不限） */
    private final Map<String, Integer> kindMaxCount;

    BizFileType(String code, String tableName, String pkColumn, String displayName,
                String storageDir, Map<String, Integer> kindMaxCount) {
        this.code = code;
        this.tableName = tableName;
        this.pkColumn = pkColumn;
        this.displayName = displayName;
        this.storageDir = storageDir;
        this.kindMaxCount = Collections.unmodifiableMap(new LinkedHashMap<>(kindMaxCount));
    }

    /**
     * 按编码查找业务类型
     *
     * @param code 业务类型编码
     * @return 业务类型；编码不存在时返回 null，由调用方给出统一错误信息
     */
    public static BizFileType resolve(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return INDEX.get(code);
    }

    /**
     * 校验并规范化槽位
     * <p>
     * 槽位为空时：若类型只配置了一个槽位，直接使用该槽位；否则使用 DEFAULT。
     * 例如工单工序只配置了 PROCESS_ATTACHMENT，调用方可以不传 fileKind。
     * </p>
     *
     * @param kind 槽位（可为空）
     * @return 规范化后的槽位
     * @throws IllegalArgumentException 槽位不合法
     */
    public String normalizeKind(String kind) {
        String normalized = (kind == null || kind.isBlank())
                ? BizFileKinds.DEFAULT
                : kind.trim().toUpperCase(Locale.ROOT);
        if (!kindMaxCount.containsKey(normalized)
                && BizFileKinds.DEFAULT.equals(normalized)
                && kindMaxCount.size() == 1) {
            return kindMaxCount.keySet().iterator().next();
        }
        if (!kindMaxCount.containsKey(normalized)) {
            throw new IllegalArgumentException("业务类型 " + code + " 不支持文件槽位: " + normalized);
        }
        return normalized;
    }

    /**
     * 获取槽位最大数量
     *
     * @param kind 槽位
     * @return 最大数量，-1 表示不限
     */
    public int maxCount(String kind) {
        return kindMaxCount.getOrDefault(kind, -1);
    }

    /** 编码索引 */
    private static final Map<String, BizFileType> INDEX = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(BizFileType::getCode, type -> type));

    /**
     * 构造槽位配置
     *
     * @param pairs 槽位、数量交替出现
     * @return 槽位配置
     */
    private static Map<String, Integer> kinds(Object... pairs) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }
}
