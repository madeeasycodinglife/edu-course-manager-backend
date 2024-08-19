package com.madeeasy.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

@Data
@Configuration
@ConfigurationProperties(prefix = "security.authorization")
public class SecurityConfigProperties {

    private List<PathMethodConfig> paths;

    @Data
    public static class PathMethodConfig {
        private String path;
        private String method;
        private Set<String> roles;
    }
}
