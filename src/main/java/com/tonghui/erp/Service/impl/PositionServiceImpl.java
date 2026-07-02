package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.PositionWithDetailsDto;
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
 */
@Service
public class PositionServiceImpl extends ServiceImpl<PositionMapper, Position> implements PositionService {

    @Autowired
    private PersonnelFileMapper personnelFileMapper;

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

    @Override
    public PagedResult<PositionWithDetailsDto> searchWithDetails(Position position, int pageNum, int pageSize) {
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

        List<Long> parentIds = parents.stream().map(Position::getPositionId).collect(Collectors.toList());
        QueryWrapper<PersonnelFile> wrapper = new QueryWrapper<>();
        wrapper.in("position_id", parentIds);
        List<PersonnelFile> allFiles = personnelFileMapper.selectList(wrapper);
        Map<Long, List<PersonnelFile>> filesMap = allFiles.stream()
                .collect(Collectors.groupingBy(PersonnelFile::getPositionId));

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
}
