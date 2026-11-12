package com.mahesh.expenses.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at /swagger-ui.html, the OpenAPI JSON at /v3/api-docs.
 * The security scheme adds an "Authorize" button: paste a JWT from /auth/login once,
 * and Swagger sends "Authorization: Bearer ..." with every request.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Personal Finance Ledger API", version = "v1",
                description = "Accounts, categories, transactions and transfers, budgets with alerts, "
                        + "recurring bills with reminders. Log in with POST /api/v1/auth/login, then click Authorize."),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
