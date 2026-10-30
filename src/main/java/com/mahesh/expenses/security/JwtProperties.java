package com.mahesh.expenses.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;


/**
 * Binds app.jwt.* from application.yml. A record gives an immutable, type-safe config object:
 * "1h" in YAML arrives here as a Duration.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration ttl, String issuer) {
}
