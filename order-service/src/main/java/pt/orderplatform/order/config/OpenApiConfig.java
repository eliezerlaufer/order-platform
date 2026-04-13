package pt.orderplatform.order.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// =============================================================================
// OPENAPI CONFIG — configura o Swagger UI
// =============================================================================
// O SpringDoc lê automaticamente as anotações do Controller e gera a spec.
// Aqui apenas personalizamos o aspecto e configuramos a autenticação JWT
// no Swagger UI (para poder testar endpoints protegidos directamente no browser).
// =============================================================================
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        // Define o esquema de segurança "bearerAuth" (JWT)
        // Isto adiciona um botão "Authorize" no Swagger UI onde podes colar o token
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .description("Gestão do ciclo de vida dos pedidos — parte da Order Platform")
                        .version("1.0.0"))
                // Aplica autenticação JWT a todos os endpoints por defeito
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT emitido pelo Keycloak. " +
                                        "Formato: Bearer <token>")));
    }
}
