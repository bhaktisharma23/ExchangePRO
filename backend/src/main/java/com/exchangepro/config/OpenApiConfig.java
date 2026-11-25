package com.exchangepro.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata. Swagger UI is served at {@code /swagger-ui.html}, the spec at {@code /v3/api-docs}. */
@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI exchangeProOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("EXCHANGE PRO API")
                        .version("v1")
                        .description("Multi-user stock exchange SIMULATION. All funds are virtual and no real "
                                + "securities are traded. Prices are synthetic unless explicitly labelled otherwise.")
                        .license(new License().name("Educational use")));
    }
}
