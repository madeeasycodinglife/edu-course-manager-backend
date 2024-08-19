package com.madeeasy.security.config;


import com.madeeasy.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {


    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityConfigProperties securityConfigProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(this.corsConfigurationSource()))
                .authorizeHttpRequests(authorizeRequests -> {
                    // Process each path configuration
                    securityConfigProperties.getPaths().forEach(config -> {
                        HttpMethod method = HttpMethod.valueOf(config.getMethod().toUpperCase());

                        if (config.getRoles().isEmpty()) {
                            // Permit all for paths with empty roles
                            authorizeRequests.requestMatchers(method, config.getPath()).permitAll();
                        } else {
                            // Configure role-based access
                            String[] roles = config.getRoles().stream()
                                    .map(role -> role.replace("ROLE_", ""))
                                    .toArray(String[]::new);

                            if (roles.length == 1) {
                                // If there's only one role, use hasRole
                                authorizeRequests.requestMatchers(method, config.getPath())
                                        .hasRole(roles[0]);
                            } else {
                                // If there are multiple roles, use hasAnyRole
                                authorizeRequests.requestMatchers(method, config.getPath())
                                        .hasAnyRole(roles);
                            }
                        }
                    });
                    // Fallback: permit all other requests
                    authorizeRequests.anyRequest().permitAll();
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:8080")); // Allows requests from any origin
        configuration.setAllowedMethods(List.of("*")); // Allows all methods (GET, POST, PUT, etc.)
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        // You can also set other configurations like allowed headers, exposed headers, etc. if needed
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/actuator/**")
                .requestMatchers("/h2-console/**");
    }

}
