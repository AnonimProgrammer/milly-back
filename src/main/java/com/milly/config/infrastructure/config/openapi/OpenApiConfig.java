package com.milly.config.infrastructure.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String ACCESS_TOKEN_COOKIE = "access-token";

    @Bean
    OpenAPI millyOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Milly API")
                        .description(
                                "REST API for the Milly multi-venue restaurant platform (`/api/v1`). "
                                        + "Staff endpoints use an HttpOnly `access-token` cookie. "
                                        + "For scripted request suites see the Bruno collection under `/bruno`.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(ACCESS_TOKEN_COOKIE, new SecurityScheme()
                                .name(ACCESS_TOKEN_COOKIE)
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)))
                .addSecurityItem(new SecurityRequirement().addList(ACCESS_TOKEN_COOKIE));
    }
}
