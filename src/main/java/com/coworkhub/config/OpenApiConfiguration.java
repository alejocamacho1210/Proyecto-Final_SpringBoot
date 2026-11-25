package com.coworkhub.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI coworkHubOpenApi() {
        return new OpenAPI()
            .info(new Info().title("CoWorkHub API").version("1.0.0")
                .description("API REST para sedes, salas, miembros y reservas de CoWorkHub."))
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    @Bean
    GroupedOpenApi coworkHubResources() {
        return GroupedOpenApi.builder().group("coworkhub-api")
            .pathsToMatch("/api/**").build();
    }

    @Bean
    GroupedOpenApi coworkHubPlatform() {
        return GroupedOpenApi.builder().group("plataforma")
            .pathsToMatch("/auth/**", "/actuator/health").build();
    }
}
