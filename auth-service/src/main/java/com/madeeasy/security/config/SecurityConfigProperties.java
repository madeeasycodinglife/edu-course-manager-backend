package com.madeeasy.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

@Data
@Configuration
@ConfigurationProperties(prefix = "security.authorization")
public class SecurityConfigProperties {

    private Map<String, PathConfig> paths;

    @Data
    public static class PathConfig {
        private Set<String> roles;
        private String method;
    }
}

