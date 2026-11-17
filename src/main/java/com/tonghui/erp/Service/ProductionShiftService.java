package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.ProductionShift;

import java.util.List;

/**
 * 生产班次服务接口
 */
public interface ProductionShiftService extends IService<ProductionShift> {

    /**
     * 查询班次列表
     *
     * @param keyword 班次名称关键字
     * @param status  状态：0停用/1启用，为空查全部
     * @return 班次列表（含适用班组名称）
     */
    List<ProductionShift> listShifts(String keyword, Integer status);

    /**
     * 查询班次详情（含适用班组ID）
     *
     * @param id 班次ID
     * @return 班次详情
     */
    ProductionShift getShiftDetail(Long id);

    /**
     * 新增班次
     *
     * @param shift 班次
     * @return 新增后的班次
     */
    ProductionShift createShift(ProductionShift shift);

    /**
     * 修改班次
     *
     * @param id    班次ID
     * @param shift 班次
     * @return 修改后的班次
     */
    ProductionShift updateShift(Long id, ProductionShift shift);

    /**
     * 启用/停用班次
     *
     * @param id     班次ID
     * @param status 状态：0停用/1启用
     * @return 修改后的班次
     */
    ProductionShift changeStatus(Long id, Integer status);

    /**
     * 查询启用班次
     *
     * @return 启用班次列表
     */
    List<ProductionShift> listActive();

    /**
     * 查询班组适用的启用班次
     *
     * @param teamId 班组ID
     * @return 班次列表
     */
    List<ProductionShift> listShiftsForTeam(Long teamId);
}
