package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.FileRequirementConfig;
import com.tonghui.erp.Data.mapper.FileRequirementConfigMapper;
import com.tonghui.erp.Service.FileRequirementService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 文件必传要求服务实现
 */
@Service
public class FileRequirementServiceImpl extends ServiceImpl<FileRequirementConfigMapper, FileRequirementConfig>
        implements FileRequirementService {

    @Override
    public List<FileRequirementConfig> listAll() {
        QueryWrapper<FileRequirementConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        wrapper.orderByAsc("stage", "biz_type", "file_kind");
        return list(wrapper);
    }

    @Override
    public String resolveRequirement(String bizType, String fileKind, String preparationCode, String itemCategory) {
        List<FileRequirementConfig> configs = listAll();
        String kind = StringUtils.hasText(fileKind) ? fileKind : "DEFAULT";
        // 品种级优先
        FileRequirementConfig match = find(configs, "PREPARATION", preparationCode, bizType, kind);
        if (match == null) {
            match = find(configs, "ITEM_CATEGORY", itemCategory, bizType, kind);
        }
        if (match == null) {
            match = find(configs, "GLOBAL", "", bizType, kind);
        }
        return match == null ? "OPTIONAL" : match.getRequirement();
    }

    private FileRequirementConfig find(List<FileRequirementConfig> configs, String scope, String scopeKey,
                                       String bizType, String kind) {
        if (!StringUtils.hasText(scopeKey)) {
            return null;
        }
        return configs.stream()
                .filter(c -> Objects.equals(c.getScope(), scope))
                .filter(c -> Objects.equals(c.getScopeKey(), scopeKey))
                .filter(c -> Objects.equals(c.getBizType(), bizType))
                .filter(c -> Objects.equals(c.getFileKind(), kind))
                .findFirst()
                .orElse(null);
    }
}
