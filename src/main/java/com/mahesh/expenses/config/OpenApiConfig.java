package com.mahesh.expenses.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at /swagger-ui.html, the OpenAPI JSON at /v3/api-docs.
 * The security scheme adds an "Authorize" button: paste a JWT from /auth/login once,
 * and Swagger sends "Authorization: Bearer ..." with every request.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Personal Finance Ledger API", version = "v1",
                description = """
        A personal finance backend for tracking money across accounts, with budgets,
        recurring-bill reminders, reports and an audit trail. Deployed by GitHub Actions.

        ### Getting started
        1. Register with `POST /api/v1/auth/register`, or log in with `POST /api/v1/auth/login`.
        2. Copy the `token` from the response and click **Authorize** (top right).
        3. Every request after that sends `Authorization: Bearer <token>` automatically.

        ### Features
        - **Accounts**: bank, cash, credit card and wallet accounts with running balances
          (optimistic locking keeps concurrent updates safe)
        - **Categories**: income and expense categories per user
        - **Transactions**: income, expenses and transfers between accounts;
          idempotency keys make retries safe
        - **Search**: filter by date range, amount, type, account, category and text,
          with paging and whitelisted sorting (JPA Specifications)
        - **CSV export**: RFC 4180 output, protected against spreadsheet formula injection
        - **Budgets**: monthly limits per category, with email alerts at the threshold
        - **Recurring bills**: weekly, monthly and yearly bills with scheduled email reminders
        - **Reports**: monthly summary by category plus a month-by-month trend,
          cached and evicted automatically when transactions change
        - **Statement import**: CSV bank statements, auto-categorised by user-defined rules
        - **Audit log**: who changed what and when

        ### Tech stack
        **Backend**
        - Java 21, Spring Boot 4.1, Spring Web MVC
        - Spring Security with stateless JWT authentication, BCrypt password hashing
        - Spring Data JPA / Hibernate, JPA Specifications, JPQL aggregate queries
        - Bean Validation, RFC 7807 Problem Details error responses
        - Spring Cache with Caffeine, transactional event listeners
        - Spring Scheduling for reminders, Spring Mail for notifications
        - springdoc-openapi (this page)

        **Data**
        - PostgreSQL 17
        - Flyway for versioned schema migrations

        **Testing**
        - JUnit 5, Mockito, AssertJ, Spring Boot Test
        - Postman collections with automated assertions

        **DevOps & cloud**
        - Docker (multi-stage build) and Docker Compose for local development
        - GitHub Actions CI/CD: build, test, push the image, deploy
        - AWS (ap-southeast-2): EC2, RDS for PostgreSQL, ECR, Systems Manager
          (Parameter Store for configuration, Run Command for deployment, Session Manager),
          SES for email, IAM

        ---
        **Developed by Mahesh** · [GitHub](https://github.com/<your-github-username>)
        """),
        security = @SecurityRequirement(name = "bearerAuth"),
        tags = {
                @Tag(name = "Auth",         description = "Register and log in (get a JWT)"),
                @Tag(name = "Accounts",     description = "Bank, cash, card and wallet accounts"),
                @Tag(name = "Categories",   description = "Income and expense categories"),
                @Tag(name = "Transactions", description = "Income, expenses, transfers, search and CSV export"),
                @Tag(name = "Budgets",      description = "Monthly limits and alerts"),
                @Tag(name = "Recurring",    description = "Recurring bills and reminders"),
                @Tag(name = "Reports",      description = "Monthly summary and trend"),
                //@Tag(name = "Import",       description = "Statement import and category rules"),
                @Tag(name = "Audit",        description = "Audit log")
        }
)
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
