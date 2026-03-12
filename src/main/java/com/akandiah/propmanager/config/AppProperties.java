package com.akandiah.propmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Top-level application properties bound to {@code app} in application.yml.
 * Contains settings that are not specific to any single sub-system.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(@DefaultValue("http://localhost:3000") String baseUrl) {
}
