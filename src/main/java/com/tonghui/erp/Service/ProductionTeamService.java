package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.ProductionTeam;

import java.util.List;

/**
 * 生产班组服务接口
 */
public interface ProductionTeamService extends IService<ProductionTeam> {

    /**
     * 查询班组列表
     *
     * @param keyword 班组编码/名称关键字
     * @return 班组列表（含成员数量）
     */
    List<ProductionTeam> listTeams(String keyword);

    /**
     * 查询班组详情（含成员）
     *
     * @param id 班组ID
     * @return 班组详情
     */
    ProductionTeam getTeamDetail(Long id);

    /**
     * 查询班组成员（含人员姓名、工号）
     *
     * @param id 班组ID
     * @return 成员列表
     */
    java.util.List<com.tonghui.erp.Data.Entity.ProductionTeamMember> listMembers(Long id);

    /**
     * 新增班组
     *
     * @param team 班组
     * @return 新增后的班组
     */
    ProductionTeam createTeam(ProductionTeam team);

    /**
     * 修改班组
     *
     * @param id   班组ID
     * @param team 班组
     * @return 修改后的班组
     */
    ProductionTeam updateTeam(Long id, ProductionTeam team);

    /**
     * 查询全部班组（含成员明细），用于排产/实际生产按班组快捷选人
     *
     * @return 班组列表（含成员）
     */
    List<ProductionTeam> listActive();
}
