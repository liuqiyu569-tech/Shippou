package com.fudan.shorturl.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI shortUrlOpenAPI() {
        return new OpenAPI().info(
                new Info()
                        .title("Short URL Service API")
                        .description("分布式短链接系统接口文档")
                        .version("v0.1.0")
                        .contact(new Contact().name("Fudan SE Student"))
        );
    }
}
