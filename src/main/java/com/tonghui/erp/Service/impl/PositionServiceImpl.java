package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.PositionWithDetailsDto;
import com.tonghui.erp.Common.utils.SoftDeleteCleanHelper;
import com.tonghui.erp.Data.Entity.PersonnelFile;
import com.tonghui.erp.Data.Entity.Position;
import com.tonghui.erp.Data.mapper.PersonnelFileMapper;
import com.tonghui.erp.Data.mapper.PositionMapper;
import com.tonghui.erp.Service.PositionService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 岗位信息服务实现类
 * <p>
 * 实现PositionService接口，提供岗位信息相关的业务逻辑处理，包括岗位的
 * 高级查询、带子表关联查询等功能的具体实现
 * </p>
 *
 */
@Service
public class PositionServiceImpl extends ServiceImpl<PositionMapper, Position> implements PositionService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 人员档案数据访问层，用于关联查询岗位关联的人员档案 */
    @Autowired
    private PersonnelFileMapper personnelFileMapper;

    /** 软删除统一清理工具，用于物理清理同编码的软删除记录以释放唯一键 */
    @Autowired
    private SoftDeleteCleanHelper softDeleteCleanHelper;

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询岗位（支持按岗位ID、编码、名称、部门ID、状态条件组合查询）
     *
     * @param position  查询条件实体，非null字段将作为等值或模糊查询条件
     * @param pageNum   页码，从0开始
     * @param pageSize  每页数量
     * @return 岗位分页结果
     */
    @Override
    public Page<Position> queryPositions(Position position, int pageNum, int pageSize) {
        int actualPageNum = pageNum + 1;

        Page<Position> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<Position> wrapper = new QueryWrapper<>();

        if (position.getPositionId() != null) {
            wrapper.eq("position_id", position.getPositionId());
        }
        if (StringUtils.hasText(position.getPositionCode())) {
            wrapper.like("position_code", position.getPositionCode());
        }
        if (StringUtils.hasText(position.getPositionName())) {
            wrapper.like("position_name", position.getPositionName());
        }
        if (position.getDepartmentId() != null) {
            wrapper.eq("department_id", position.getDepartmentId());
        }
        if (position.getStatus() != null) {
            wrapper.eq("status", position.getStatus());
        }

        return this.page(page, wrapper);
    }

    // endregion

    // region 带子表关联查询
    // ===================================
    // 带子表关联查询
    // ===================================

    /**
     * 查询岗位列表并关联人员档案信息
     * <p>先分页查询岗位主表数据，再批量查询关联的人员档案</p>
     *
     * @param position  查询条件实体
     * @param pageNum   页码，从0开始
     * @param pageSize  每页数量
     * @return 带子表关联数据的岗位分页结果
     */
    @Override
    public PagedResult<PositionWithDetailsDto> searchWithDetails(Position position, int pageNum, int pageSize) {
        // 查询岗位主表分页数据
        Page<Position> parentPage = queryPositions(position, pageNum, pageSize);
        List<Position> parents = parentPage.getRecords();

        PagedResult<PositionWithDetailsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        // 批量查询关联的人员档案
        List<Long> parentIds = parents.stream().map(Position::getPositionId).collect(Collectors.toList());
        QueryWrapper<PersonnelFile> wrapper = new QueryWrapper<>();
        wrapper.in("position_id", parentIds);
        List<PersonnelFile> allFiles = personnelFileMapper.selectList(wrapper);
        Map<Long, List<PersonnelFile>> filesMap = allFiles.stream()
                .collect(Collectors.groupingBy(PersonnelFile::getPositionId));

        // 组装带子表数据的DTO
        List<PositionWithDetailsDto> dtos = parents.stream().map(parent -> {
            PositionWithDetailsDto dto = new PositionWithDetailsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setPersonnelFiles(filesMap.getOrDefault(parent.getPositionId(), List.of()));
            return dto;
        }).collect(Collectors.toList());

        result.setItems(dtos);
        result.setTotalCount(parentPage.getTotal());
        result.setPageIndex(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    // endregion

    // region 唯一性校验与软删除清理
    // ===================================
    // 唯一性校验与软删除清理
    // ===================================

    /**
     * 按岗位编码查询未删除的岗位
     * <p>MyBatis-Plus 全局软删除会自动过滤 is_deleted=1 的记录，因此只返回活动记录</p>
     *
     * @param positionCode 岗位编码
     * @return 岗位实体，不存在返回null；同编码存在多条时取第一条
     */
    @Override
    public Position getByPositionCode(String positionCode) {
        QueryWrapper<Position> wrapper = new QueryWrapper<>();
        wrapper.eq("position_code", positionCode);
        return getOne(wrapper, false);
    }

    /**
     * 清理指定岗位编码下已被软删除的记录（物理删除，释放唯一键约束）
     * <p>唯一索引 uk_position_code 对所有行生效（含软删除行），需物理清理后同编码方可复用</p>
     *
     * @param positionCode 岗位编码
     * @return 清理的记录数
     */
    @Override
    public int cleanSoftDeletedByPositionCode(String positionCode) {
        return softDeleteCleanHelper.cleanByUniqueField(baseMapper, "position_code", positionCode);
    }

    // endregion
}
