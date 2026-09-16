package com.fitnesspro.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI fitnessProOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fitness Pro API")
                        .version("1.0.0")
                        .description("REST API for the Fitness Pro CRM. "
                                + "Log in through /api/auth/login, copy the token from the response, "
                                + "then use Authorize to call protected endpoints.")
                        .license(new License().name("Educational project")))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT returned by /api/auth/login.")));
    }

    @Bean
    public OpenApiCustomizer jwtSecurityCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().forEach((path, pathItem) -> {
                if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
                    return;
                }
                pathItem.readOperations().forEach(operation ->
                        operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH)));
            });
        };
    }
}
