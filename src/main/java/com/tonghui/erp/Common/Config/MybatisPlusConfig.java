package com.tonghui.erp.Common.Config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus配置类
 * <p>
 * 用于配置MyBatis-Plus的相关插件和拦截器，提供分页等增强功能
 * </p>
 */
@Configuration
public class MybatisPlusConfig {
    
    //#region MyBatis-Plus拦截器配置
    // ===================================
    // MyBatis-Plus拦截器配置
    // ===================================
    
    /**
     * 配置MyBatis-Plus拦截器
     * <p>
     * 添加分页拦截器，用于支持数据库分页查询功能
     * </p>
     * 
     * @return MybatisPlusInterceptor拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        MybatisPlusInterceptor interceptor=new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
    //#endregion
}

