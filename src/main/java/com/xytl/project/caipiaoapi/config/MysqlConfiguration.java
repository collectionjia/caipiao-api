package com.xytl.project.caipiaoapi.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 仅当 mysql.enabled=true 时加载 MyBatis Mapper。
 */
@Configuration
@ConditionalOnProperty(name = "mysql.enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("com.xytl.project.caipiaoapi.**.service")
public class MysqlConfiguration {
}
