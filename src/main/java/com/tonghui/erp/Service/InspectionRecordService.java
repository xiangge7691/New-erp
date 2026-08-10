package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.InspectionRecord;

/**
 * 检验记录服务接口
 * <p>
 * 提供检验记录的基础CRUD、检验编号生成及编号唯一性校验等业务能力，
 * 用于质量检验模块的检验数据录入管理（承载报告书附件）
 * </p>
 */
public interface InspectionRecordService extends IService<InspectionRecord> {

    /**
     * 生成检验编号
     * <p>
     * 编号格式：JY-YYYYMMDD-NNN，自动生成当天最大序号的下一个编号
     * </p>
     *
     * @return 生成的唯一检验编号
     */
    String generateCode();

    /**
     * 校验检验编号是否唯一
     * <p>
     * 用于新增或修改时校验编号，排除指定ID自身的记录，避免修改时误判重复
     * </p>
     *
     * @param code     检验编号
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 唯一返回true，否则返回false
     */
    boolean isCodeUnique(String code, Long excludeId);
}