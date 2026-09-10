package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Approval.CurrentHandlerRoleDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class ApprovalInstanceServiceTest {

    @Autowired
    private ApprovalInstanceService approvalInstanceService;

    @Test
    public void testGetCurrentHandlerRoles() {
        // 测试获取当前处理角色列表功能
        try {
            List<CurrentHandlerRoleDto> roles = approvalInstanceService.getCurrentHandlerRoles(1L);
            System.out.println("获取到的角色数量: " + roles.size());
            for (CurrentHandlerRoleDto role : roles) {
                System.out.println("角色: " + role.getRoleName() + ", 是否当前节点: " + role.getIsCurrentNode());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testIsCurrentUserHandler() {
        // 测试检查用户是否为当前处理人功能
        try {
            boolean isHandler = approvalInstanceService.isCurrentUserHandler(1L, 1L);
            System.out.println("用户是否为当前处理人: " + isHandler);
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}