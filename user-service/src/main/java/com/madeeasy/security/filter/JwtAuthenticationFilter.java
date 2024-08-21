package com.madeeasy.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madeeasy.exception.TokenValidationException;
import com.madeeasy.security.config.SecurityConfigProperties;
import com.madeeasy.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SecurityConfigProperties securityConfigProperties;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = null;
        String userName = null;
        Boolean tokenValid = false;
        String requestUri = request.getRequestURI();

        // Check if the request URI requires authorization
        if (requiresAuthorization(request)) {
            // Check if Authorization header is present and valid
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                handleInvalidToken(response, "Authorization header missing or malformed.");
                return; // Exit the filter chain
            }

            token = authorizationHeader.substring(7); // Extract token from header

            try {
                userName = jwtUtils.getUserName(token);
            } catch (TokenValidationException e) {
                handleInvalidToken(response, e.getMessage());
                return; // Exit the filter chain
            }

            // Validate token using external service
            String authUrl = "http://auth-service/auth-service/validate-access-token/" + token;

            try {
                ResponseEntity<Boolean> authResponse = restTemplate.exchange(
                        authUrl,
                        HttpMethod.POST,
                        null,  // No request body in this example
                        Boolean.class
                );

                if (authResponse.getStatusCode() == HttpStatus.OK) {
                    tokenValid = authResponse.getBody();
                } else {
                    handleInvalidToken(response, "Invalid token or token not found.");
                    return; // Exit the filter chain
                }
            } catch (HttpClientErrorException e) {
                log.error("Error in JwtAuthenticationFilter: {}", e.getMessage());
                handleInvalidToken(response, "Invalid token or token not found.");
                return;
            } catch (HttpServerErrorException e) {
                log.error("Error in JwtAuthenticationFilter: {}", e.getMessage());
                handleServiceUnavailable(response);
                return; // Exit the filter chain
            } catch (Exception e) {
                log.error("Unexpected error in JwtAuthenticationFilter: {}", e.getMessage());
                handleServiceUnavailable(response);
                return; // Exit the filter chain
            }

            // If the token is valid, set authentication in context
            if (Boolean.TRUE.equals(tokenValid) && userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtUtils.validateToken(token, userName)) {
                    List<SimpleGrantedAuthority> authorities = jwtUtils.getRolesFromToken(token)
                            .stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(userName, null, authorities);

                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }

    private boolean requiresAuthorization(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();


        // Initialize the path matcher to handle wildcard patterns
        PathMatcher pathMatcher = new AntPathMatcher();


        // Check if any configured path matches the URI and HTTP method

        return securityConfigProperties.getPaths().stream()
                .anyMatch(config ->
                        pathMatcher.match(config.getPath(), uri) &&
                                method.equalsIgnoreCase(config.getMethod()) &&
                                !config.getRoles().isEmpty()
                );
    }


    private void handleInvalidToken(HttpServletResponse response, String message) throws IOException {
        Map<String, Object> errorResponse = Map.of(
                "status", HttpStatus.UNAUTHORIZED,
                "message", message
        );

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private void handleServiceUnavailable(HttpServletResponse response) throws IOException {
        Map<String, Object> errorResponse = Map.of(
                "status", HttpStatus.SERVICE_UNAVAILABLE,
                "message", "The Auth-Service is not available."
        );

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
