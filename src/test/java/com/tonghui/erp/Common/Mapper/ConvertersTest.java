package com.tonghui.erp.Common.Mapper;

import com.tonghui.erp.Common.Dto.System.UserDto;
import com.tonghui.erp.Data.Entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 用户实体-DTO 转换单元测试
 * <p>
 * 重点校验账号与姓名的字段语义：UserDto.userName 必须取登录账号 user_account，
 * UserDto.name 才是真实姓名 user_name。两者混淆会导致登录日志的账号被写成姓名。
 * </p>
 */
public class ConvertersTest {

    // region 测试对象
    // ===================================
    // 测试对象
    // ===================================

    private final Converters converters = Converters.INSTANCE;

    // endregion

    // region 用户转换用例
    // ===================================
    // 用户转换用例
    // ===================================

    /**
     * 基础转换：userName 取账号、name 取真实姓名
     */
    @Test
    public void testToUserDtoMapsAccountAndNameSeparately() {
        UserDto dto = converters.toUserDto(buildRootUser());

        assertEquals(1L, dto.getId());
        assertEquals("root", dto.getUserName(), "userName 应为登录账号");
        assertEquals("超级管理员", dto.getName(), "name 应为真实姓名");
    }

    /**
     * 含关联信息的转换：账号与姓名字段语义与基础转换保持一致
     */
    @Test
    public void testToUserDtoWithAssociationsMapsAccountAndNameSeparately() {
        UserDto dto = converters.toUserDtoWithAssociations(buildRootUser());

        assertEquals("root", dto.getUserName(), "userName 应为登录账号");
        assertEquals("超级管理员", dto.getName(), "name 应为真实姓名");
    }

    // endregion

    // region 测试辅助
    // ===================================
    // 测试辅助
    // ===================================

    /**
     * 构造 root 用户实体（账号 root、姓名 超级管理员）
     *
     * @return 用户实体
     */
    private User buildRootUser() {
        User user = new User();
        user.setUserId(1L);
        user.setUserAccount("root");
        user.setUserName("超级管理员");
        user.setUserStatus(1);
        return user;
    }

    // endregion
}
