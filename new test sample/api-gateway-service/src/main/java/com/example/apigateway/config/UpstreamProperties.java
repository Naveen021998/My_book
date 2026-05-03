package com.example.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "upstreams")
public record UpstreamProperties(
        String auth,
        String core,
        String user,
        String notification,
        String file
) {
}

