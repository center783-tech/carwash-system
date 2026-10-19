package com.carwash.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI carwashOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("洗保養洗車場預約管理系統 API")
                        .description("提供會員註冊登入、洗車服務瀏覽、線上預約、時段管理、線上付款、"
                                + "會員消費紀錄、優惠券、後台管理、員工排班、評價等功能的 REST API 文件。")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Carwash Dev Team")
                                .email("dev@carwash.example.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("本機開發環境")
                ))
                // 讓 Swagger UI 可以直接帶 JWT 測試需要登入的 API
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("先呼叫 /api/members/login 或 /api/admin/login 取得 token，"
                                        + "再貼到這裡（不用加 Bearer 前綴，Swagger 會自動加上）")));
    }
}
