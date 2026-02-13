package com.bntu.chinesecourses.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
    String secret,
    String issuer,
    Duration accessTokenTtl
) {
}