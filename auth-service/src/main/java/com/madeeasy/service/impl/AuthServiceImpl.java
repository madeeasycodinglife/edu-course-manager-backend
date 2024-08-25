package com.madeeasy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.madeeasy.dto.request.AuthRequest;
import com.madeeasy.dto.request.LogOutRequest;
import com.madeeasy.dto.request.SignInRequestDTO;
import com.madeeasy.dto.request.UserRequest;
import com.madeeasy.dto.response.AuthResponse;
import com.madeeasy.entity.Role;
import com.madeeasy.entity.Token;
import com.madeeasy.entity.TokenType;
import com.madeeasy.entity.User;
import com.madeeasy.exception.TokenException;
import com.madeeasy.repository.TokenRepository;
import com.madeeasy.repository.UserRepository;
import com.madeeasy.service.AuthService;
import com.madeeasy.util.JwtUtils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String AUTH = "auth";
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final TokenRepository tokenRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    private final CacheManager cacheManager;

    @Override
    @Retry(name = "myRetry", fallbackMethod = "singUpFallback")
    @CircuitBreaker(name = "myCircuitBreaker", fallbackMethod = "singUpFallback")
    public AuthResponse singUp(AuthRequest authRequest) {
        List<String> authRequestRoles = authRequest.getRoles();

        if (!authRequestRoles.contains(Role.ADMIN.name()) && !authRequestRoles.contains(Role.USER.name())) {
            return null;
        }
        List<Role> roles = new ArrayList<>();
        if (authRequestRoles.size() == 1) {
            if (authRequestRoles.contains(Role.ADMIN.name())) {
                roles.add(Role.ADMIN);
            }
            if (authRequestRoles.contains(Role.USER.name())) {
                roles.add(Role.USER);
            }
        } else {
            roles.addAll(Arrays.asList(Role.ADMIN, Role.USER));
        }


        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .fullName(authRequest.getFullName())
                .email(authRequest.getEmail())
                .password(passwordEncoder.encode(authRequest.getPassword()))
                .phone(authRequest.getPhone())
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .isEnabled(true)
                .role(roles)
                .build();

        // Check if user with the given email already exists
        if (userRepository.existsByEmail(authRequest.getEmail())) {
            log.error("User with Email : {} already exists", authRequest.getEmail());
            return AuthResponse.builder()
                    .message("User with Email : " + authRequest.getEmail() + " already exists")
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        String accessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getRole().stream().map(Enum::name).toList());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail(), user.getRole().stream().map(Enum::name).toList());

        Token token = Token.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .token(accessToken)
                .isRevoked(false)
                .isExpired(false)
                .tokenType(TokenType.BEARER)
                .build();


        UserRequest userRequest = UserRequest.builder()
                .id(user.getId())
                .fullName(authRequest.getFullName())
                .email(authRequest.getEmail())
                .password(user.getPassword())
                .phone(authRequest.getPhone())
                .roles(authRequestRoles)
                .build();

        String url = "http://user-service/user-service/create";

        // Make the POST request to the user-service
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<UserRequest> requestEntity = new HttpEntity<>(userRequest, headers);
        ResponseEntity<UserRequest> response = null;
        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    UserRequest.class
            );
            // Check if the user-service responded successfully
            if (response.getStatusCode().is2xxSuccessful()) {

                // Save the user in the local repository only after a successful response from user-service
                userRepository.save(user);

                tokenRepository.save(token);

                // Perform cache eviction manually
                Cache cache = cacheManager.getCache(AUTH);
                assert cache != null;
                cache.evict(user.getEmail() + ":accessToken");
                cache.evict(user.getEmail() + ":refreshToken");

                return AuthResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
            } else {
                log.error("Failed to create user in user-service for email: {}. Response status: {}", authRequest.getEmail(), response.getStatusCode());
                return AuthResponse.builder()
                        .message("Failed to create user in user-service for email: " + authRequest.getEmail())
                        .status((HttpStatus) response.getStatusCode())
                        .build();
            }
        } catch (HttpClientErrorException exception) {
            log.error("Failed to create user in user-service for email: {}. Error response: {}", authRequest.getEmail(), exception.getResponseBodyAsString());
            try {
                // Parse the response body as JSON
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(exception.getResponseBodyAsString());

                // Extract specific fields from the JSON, such as 'message' and 'status'
                String errorMessage = jsonNode.path("message").asText();
                String errorStatus = jsonNode.path("status").asText();

                // Log the extracted information
                log.error("message : {} , status : {}", errorMessage, errorStatus);

                return AuthResponse.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("Bad request : " + errorMessage)
                        .build();
            } catch (Exception e) {
                log.error("Failed to parse the error response", e);
            }
        }

        return AuthResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Sorry !! Something went wrong. Please Check It !")
                .build();
    }

    public AuthResponse singUpFallback(AuthRequest authRequest, Throwable t) {
        log.error("message : {}", t.getMessage());
        return AuthResponse.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .message("Sorry !! Token creation failed as User Service is unavailable. Please try again later.")
                .build();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = AUTH, key = "#signInRequest.email + ':accessToken'"),
            @CacheEvict(value = AUTH, key = "#signInRequest.email + ':refreshToken'")
    })
    public AuthResponse singIn(SignInRequestDTO signInRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(signInRequest.getEmail(), signInRequest.getPassword()));
        if (authentication.isAuthenticated()) {
            User user = userRepository.findByEmail(signInRequest.getEmail()).orElseThrow(() -> new UsernameNotFoundException("Email not found"));
            revokeAllPreviousValidTokens(user);
            String accessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getRole().stream().map(role -> role.name()).collect(Collectors.toList()));
            String refreshToken = jwtUtils.generateRefreshToken(user.getEmail(), user.getRole().stream().map(role -> role.name()).collect(Collectors.toList()));


            Token token = Token.builder()
                    .id(UUID.randomUUID().toString())
                    .user(user)
                    .token(accessToken)
                    .isRevoked(false)
                    .isExpired(false)
                    .tokenType(TokenType.BEARER)
                    .build();

            tokenRepository.save(token);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        } else {
            throw new BadCredentialsException("Bad Credential Exception !!");
        }
    }

    @Override
    public void revokeAllPreviousValidTokens(User user) {
        List<Token> tokens = tokenRepository.findAllValidTokens(user.getId());
        tokens.forEach(token -> {
            token.setRevoked(true);
            token.setExpired(true);
        });
        tokenRepository.saveAll(tokens);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = AUTH, key = "#logOutRequest.email + ':accessToken'"),
            @CacheEvict(value = AUTH, key = "#logOutRequest.email + ':refreshToken'")
    })
    public void logOut(LogOutRequest logOutRequest) {
        String email = logOutRequest.getEmail();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Email not found"));
        jwtUtils.validateToken(logOutRequest.getAccessToken(), jwtUtils.getUserName(logOutRequest.getAccessToken()));
        revokeAllPreviousValidTokens(user);


        // Perform cache eviction manually
        Cache cache = cacheManager.getCache(AUTH);
        assert cache != null;
        log.info("cache : {}", cache.get("accessToken"));
    }

    @Override
    @Cacheable(value = AUTH, keyGenerator = "customKeyGenerator", unless = "#result == null")
    public boolean validateAccessToken(String accessToken) {

        Token token = tokenRepository.findByToken(accessToken).orElseThrow(() -> new TokenException("Token Not found"));

        if (token.isExpired() && token.isRevoked()) {
            throw new TokenException("Token is expired or revoked");
        }
        return !token.isExpired() && !token.isRevoked();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = AUTH, key = "#userRequest.email + ':accessToken'"),
            @CacheEvict(value = AUTH, key = "#userRequest.email + ':refreshToken'")
    })
    public AuthResponse partiallyUpdateUser(String emailId, UserRequest userRequest) {
        User user = userRepository.findByEmail(emailId).orElseThrow(() -> new UsernameNotFoundException("Email not found"));

        if (userRequest.getFullName() != null) {
            user.setFullName(userRequest.getFullName());
        }
        if (userRequest.getEmail() != null) {
            user.setEmail(userRequest.getEmail());
        }
        if (userRequest.getPhone() != null) {
            user.setPhone(userRequest.getPhone());
        }
        if (userRequest.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        }
        if (userRequest.getRoles() != null) {
            user.setRole(userRequest.getRoles().stream().map(Role::valueOf).collect(Collectors.toList()));
        }
        User savedUser = userRepository.save(user);

        if (userRequest.getRoles() != null || userRequest.getEmail() != null) {
            revokeAllPreviousValidTokens(savedUser);
            String accessToken = jwtUtils.generateAccessToken(savedUser.getEmail(), savedUser.getRole().stream().map(role -> role.name()).collect(Collectors.toList()));
            String refreshToken = jwtUtils.generateRefreshToken(savedUser.getEmail(), savedUser.getRole().stream().map(role -> role.name()).collect(Collectors.toList()));


            Token token = Token.builder()
                    .id(UUID.randomUUID().toString())
                    .user(savedUser)
                    .token(accessToken)
                    .isRevoked(false)
                    .isExpired(false)
                    .tokenType(TokenType.BEARER)
                    .build();

            tokenRepository.save(token);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }

        return AuthResponse.builder()
                .status(HttpStatus.OK)
                .message("User updated successfully")
                .build();
    }

    @Override
    @Cacheable(value = AUTH, keyGenerator = "customKeyGenerator", unless = "#result == null")
    public AuthResponse refreshToken(String refreshToken) {

        boolean isValid = jwtUtils.validateToken(refreshToken, jwtUtils.getUserName(refreshToken));

        if (!isValid) {
            throw new TokenException("Token is invalid");
        }
        User user = userRepository.findByEmail(jwtUtils.getUserName(refreshToken)).orElseThrow(() -> new UsernameNotFoundException("Email not found"));
        revokeAllPreviousValidTokens(user);
        String accessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getRole().stream().map(Enum::name).collect(Collectors.toList()));
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getEmail(), user.getRole().stream().map(Enum::name).collect(Collectors.toList()));


        Token token = Token.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .token(accessToken)
                .isRevoked(false)
                .isExpired(false)
                .tokenType(TokenType.BEARER)
                .build();

        tokenRepository.save(token);

        // Perform cache eviction manually
        Cache cache = cacheManager.getCache(AUTH);
        assert cache != null;
        cache.evict(user.getEmail() + ":accessToken");

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}