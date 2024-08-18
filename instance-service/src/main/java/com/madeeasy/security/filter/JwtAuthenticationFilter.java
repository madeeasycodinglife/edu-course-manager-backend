package com.madeeasy.security.filter;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.madeeasy.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
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

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        String userName = null;
        String token = null;
        Boolean flag = false;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
            userName = jwtUtils.getUserName(token);
            String authUrl = "http://auth-service/auth-service/validate-access-token/" + token;

            try {
                ResponseEntity<Boolean> authResponse = restTemplate.exchange(
                        authUrl,
                        HttpMethod.POST,
                        null,  // No request body in this example
                        Boolean.class
                );

                if (authResponse.getStatusCode() == HttpStatus.OK) {
                    flag = authResponse.getBody();
                } else {
                    handleInvalidToken(response, "Invalid token or token not found.");
                    return; // Exit the filter chain
                }
            } catch (HttpClientErrorException e) {
                log.error("Client error in JwtAuthenticationFilter: {}", e.getMessage());
                handleInvalidToken(response, "Token validation failed.");
                return; // Exit the filter chain
            } catch (HttpServerErrorException e) {
                log.error("Server error in JwtAuthenticationFilter: {}", e.getMessage());
                handleServiceUnavailable(response);
                return; // Exit the filter chain
            } catch (Exception e) {
                log.error("Unexpected error in JwtAuthenticationFilter: {}", e.getMessage());
                handleServiceUnavailable(response);
                return; // Exit the filter chain
            }
        } else {
            handleInvalidToken(response, "Authorization header missing or malformed.");
            return; // Exit the filter chain
        }

        if (userName != null && Boolean.TRUE.equals(flag) && SecurityContextHolder.getContext().getAuthentication() == null) {
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
        filterChain.doFilter(request, response);
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
        // Create the error response using Map.of()
        Map<String, Object> errorResponse = Map.of(
                "status", HttpStatus.SERVICE_UNAVAILABLE,
                "message", "The Auth-Service is not available !!."
        );

        // Set the response type and status
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

        // Write the response as JSON
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

    }
}

