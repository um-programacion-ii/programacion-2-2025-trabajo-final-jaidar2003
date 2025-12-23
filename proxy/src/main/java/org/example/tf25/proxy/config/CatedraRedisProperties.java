package org.example.tf25.proxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tf25.catedra.redis")
public record CatedraRedisProperties(
        String host,
        int port
) {}
