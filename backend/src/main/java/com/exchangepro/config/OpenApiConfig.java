package com.exchangepro.config;

import com.exchangepro.auth.CurrentUserId;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata. Swagger UI is served at {@code /swagger-ui.html}, the spec at {@code /v3/api-docs}. */
@Configuration
class OpenApiConfig {

    static {
        // @CurrentUserId parameters are filled from the token, not supplied by API clients: hide them.
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUserId.class);
    }

    @Bean
    OpenAPI exchangeProOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("EXCHANGE PRO API")
                        .version("v1")
                        .description("Multi-user stock exchange SIMULATION. All funds are virtual and no real "
                                + "securities are traded. Prices are synthetic unless explicitly labelled otherwise. "
                                + "Authenticate with POST /api/v1/auth/login and use the returned accessToken "
                                + "as a Bearer token.")
                        .license(new License().name("Educational use")))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
