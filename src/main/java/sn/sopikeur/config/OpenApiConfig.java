package sn.sopikeur.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${app.openapi.local-server-url:http://localhost:8080}")
    private String localServerUrl;

    @Value("${app.openapi.server-url:https://api.sopikeur.sn}")
    private String productionServerUrl;

    @Bean
    public OpenAPI sopiKeurOpenApi() {
        SecurityScheme bearerScheme = new SecurityScheme()
            .name("Authorization")
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT");

        return new OpenAPI()
            .info(new Info().title("Sopi Keur API").version("v1"))
            .addServersItem(new Server().url(localServerUrl).description("Local API"))
            .addServersItem(new Server().url(productionServerUrl).description("Production API"))
            .components(new Components().addSecuritySchemes("bearerAuth", bearerScheme))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
