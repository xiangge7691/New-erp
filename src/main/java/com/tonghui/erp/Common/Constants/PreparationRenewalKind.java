package com.tonghui.erp.Common.Constants;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 制剂续展文件类型
 * <p>
 * 集中维护续展文件 10 个槽位的编码、展示名称与压缩包内文件夹名，
 * 是「续展文件」选项卡、业务附件槽位注册、ZIP 打包目录名的唯一数据源。
 * 槽位编码会在 business_file.file_kind 中持久化，发布后不可随意修改。
 * </p>
 */
@Getter
public enum PreparationRenewalKind {

    // region 续展文件类型
    // ===================================
    // 续展文件类型
    // ===================================

    /** 1、《医疗机构中药制剂委托配制备案申请表》（原件） */
    APPLICATION_FORM("RENEWAL_APPLICATION_FORM", "委托配制备案申请表", "01_委托配制备案申请表"),

    /** 2、委托方的制剂批准证明文件复印件、《医疗机构制剂许可证》 */
    COMMISSIONER_APPROVAL("RENEWAL_COMMISSIONER_APPROVAL", "委托方批准证明文件及许可证",
            "02_委托方批准证明文件及许可证"),

    /** 3、受托方的《药品生产许可证》《药品生产质量管理规范》认证证书复印件 */
    MANUFACTURER_LICENSE_GMP("RENEWAL_MANUFACTURER_LICENSE_GMP", "受托方药品生产许可证及GMP证书",
            "03_受托方药品生产许可证及GMP证书"),

    /** 4、前次批准的《医疗机构中药制剂委托配制备案件》或公告 */
    PREVIOUS_FILING("RENEWAL_PREVIOUS_FILING", "前次备案件或公告", "04_前次备案件或公告"),

    /** 5、前次委托配制期间，配制及制剂质量情况的总结 */
    PREVIOUS_PERIOD_SUMMARY("RENEWAL_PREVIOUS_PERIOD_SUMMARY", "前次配制及质量情况总结",
            "05_前次配制及质量情况总结"),

    /** 6、与前次《医疗机构中药制剂委托配制备案件》发生变化的证明文件 */
    CHANGE_PROOF("RENEWAL_CHANGE_PROOF", "与前次备案变化的证明文件", "06_与前次备案变化的证明文件"),

    /** 7、委托配制合同（原件） */
    COMMISSION_CONTRACT("RENEWAL_COMMISSION_CONTRACT", "委托配制合同", "07_委托配制合同"),

    /** 8、申请材料真实性的自我保证声明（原件） */
    AUTHENTICITY_DECLARATION("RENEWAL_AUTHENTICITY_DECLARATION", "申请材料真实性自我保证声明",
            "08_申请材料真实性自我保证声明"),

    /** 9、《授权委托书》（原件） */
    AUTHORIZATION_LETTER("RENEWAL_AUTHORIZATION_LETTER", "授权委托书", "09_授权委托书"),

    /** 10、其他文件 */
    OTHER("RENEWAL_OTHER", "其他文件", "10_其他文件"),

    // endregion
    ;

    /** 槽位编码（持久化到 business_file.file_kind） */
    private final String code;

    /** 展示名称 */
    private final String label;

    /** ZIP 压缩包内文件夹名 */
    private final String zipFolder;

    PreparationRenewalKind(String code, String label, String zipFolder) {
        this.code = code;
        this.label = label;
        this.zipFolder = zipFolder;
    }

    // region 静态索引
    // ===================================
    // 静态索引
    // ===================================

    /** 编码索引 */
    private static final Map<String, PreparationRenewalKind> INDEX;

    static {
        Map<String, PreparationRenewalKind> index = new LinkedHashMap<>();
        for (PreparationRenewalKind kind : values()) {
            index.put(kind.code, kind);
        }
        INDEX = Collections.unmodifiableMap(index);
    }

    /**
     * 按编码查找续展文件类型
     *
     * @param code 槽位编码
     * @return 续展文件类型；编码为空或不存在时返回 null
     */
    public static PreparationRenewalKind resolve(String code) {
        return code == null || code.isBlank() ? null : INDEX.get(code.trim());
    }

    /**
     * 获取按申报清单顺序排列的全部类型
     *
     * @return 不可变列表（声明顺序）
     */
    public static List<PreparationRenewalKind> ordered() {
        return List.of(values());
    }

    // endregion
}
