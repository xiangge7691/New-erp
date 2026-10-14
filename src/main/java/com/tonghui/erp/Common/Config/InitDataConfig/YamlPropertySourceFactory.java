package com.tonghui.erp.Common.Config.InitDataConfig;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.util.Objects;
import java.util.Properties;

/**
 * YAML属性源工厂
 * <p>
 * 使Spring的@PropertySource注解支持YAML格式文件
 * 原生@PropertySource仅支持.properties格式，对.yml文件会静默失败
 * </p>
 */
public class YamlPropertySourceFactory implements PropertySourceFactory {

    /**
     * 创建YAML属性源
     * <p>
     * 将YAML资源解析为Properties并包装为PropertySource，供Spring环境读取
     * </p>
     *
     * @param name     属性源名称（可为null，为null时使用资源文件名）
     * @param resource 编码后的YAML资源
     * @return 解析后的属性源
     */
    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource.getResource());
        Properties properties = factory.getObject();
        String sourceName = name != null ? name : resource.getResource().getFilename();
        return new PropertiesPropertySource(sourceName, Objects.requireNonNull(properties));
    }
}
