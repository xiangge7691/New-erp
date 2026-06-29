package com.tonghui.erp.Common.Dto.Auth;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;

/**
 * 当前用户信息响应
 * 与 openapi 中 UserInfo 对齐
 */
@Data
public class UserInfoDto {
    private List<String> buttons = new ArrayList<>();
    private List<String> roles = new ArrayList<>();
    private String userId;
    private String userName;
    private String email;
    private String avatar;
}