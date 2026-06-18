package com.fudan.shorturl.config;

import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

/**
 * Week 7: ShardingSphere-JDBC 数据源装配
 * <p>
 * 走 Spring 的 {@code ${...:default}} 占位符注入 MySQL 连接信息，sharding-config.yaml 只保留
 * 分片规则；解决 ShardingSphere 自带 {@code $${...}} yaml 占位符在 mvn spring-boot:run
 * 模式下读不到环境变量的问题。
 */
@Configuration
@Profile("!test")
public class ShardingSphereDataSourceConfig {

    private static final String HOST_PLACEHOLDER = "$${MYSQL_HOST::}";
    private static final String PORT_PLACEHOLDER = "$${MYSQL_PORT::}";
    private static final String PASSWORD_PLACEHOLDER = "$${MYSQL_PASSWORD::}";

    @Value("${MYSQL_HOST:localhost}")
    private String mysqlHost;

    @Value("${MYSQL_PORT:3306}")
    private String mysqlPort;

    @Value("${MYSQL_PASSWORD:}")
    private String mysqlPassword;

    @Bean
    @Primary
    public DataSource dataSource() throws Exception {
        byte[] yaml = new ClassPathResource("sharding-config.yaml")
                .getInputStream()
                .readAllBytes();
        String content = new String(yaml, StandardCharsets.UTF_8)
                .replace(HOST_PLACEHOLDER, mysqlHost)
                .replace(PORT_PLACEHOLDER, mysqlPort)
                .replace(PASSWORD_PLACEHOLDER, mysqlPassword);
        return YamlShardingSphereDataSourceFactory
                .createDataSource(content.getBytes(StandardCharsets.UTF_8));
    }
}
