package com.tonghui.erp.Common.Dto.System;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
public class AppRouteRecordDto {
    private String path;
    private String name;
    private String component;
    private String redirect;
    private RouteMetaDto meta;
    private List<AppRouteRecordDto> children = new ArrayList<>();
}