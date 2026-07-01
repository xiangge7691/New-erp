package com.tonghui.erp.Common.Config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.tonghui.erp.Common.utils.EntityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * <p>
 * 自动填充审计字段：createdBy、updatedBy、createdTime、updatedTime
 * </p>
 */
@Component
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入操作自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        Long currentUserId = EntityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        this.strictInsertFill(metaObject, "createdBy", Long.class, currentUserId);
        this.strictInsertFill(metaObject, "updatedBy", Long.class, currentUserId);
        this.strictInsertFill(metaObject, "createdTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedTime", LocalDateTime.class, now);
    }

    /**
     * 更新操作自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        Long currentUserId = EntityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        this.strictUpdateFill(metaObject, "updatedBy", Long.class, currentUserId);
        this.strictUpdateFill(metaObject, "updatedTime", LocalDateTime.class, now);
    }
}
