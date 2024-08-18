package com.madeeasy.service.impl;

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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final TokenRepository tokenRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    private final HttpServletRequest request;


    @CircuitBreaker(name = "myCircuitBreaker", fallbackMethod = "singUpFallback")
    @Override
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
            log.error("User with Email :{} already exists", authRequest.getEmail());
            return AuthResponse.builder()
                    .message("User with Email : " + authRequest.getEmail() + " already exists")
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        userRepository.save(user);


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

        tokenRepository.save(token);

        // rest-call to user-service

        UserRequest userRequest = UserRequest.builder()
                .id(user.getId())
                .fullName(authRequest.getFullName())
                .email(authRequest.getEmail())
                .password(user.getPassword())
                .phone(authRequest.getPhone())
                .roles(authRequestRoles)
                .build();

        String url = "http://user-service/user-service/create";

        // Make the POST request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<UserRequest> requestEntity = new HttpEntity<>(userRequest, headers);

        restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                UserRequest.class
        );


        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
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
    public void logOut(LogOutRequest logOutRequest) {
        String email = logOutRequest.getEmail();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Email not found"));
        jwtUtils.validateToken(logOutRequest.getAccessToken(), jwtUtils.getUserName(logOutRequest.getAccessToken()));
        revokeAllPreviousValidTokens(user);
    }

    @Override
    public boolean validateAccessToken(String accessToken) {

        Token token = tokenRepository.findByToken(accessToken).orElseThrow(() -> new TokenException("Token Not found"));

        try {
            if (token.isExpired() && token.isRevoked()) {
                throw new TokenException("Token is expired or revoked");
            }
        } catch (TokenException e) {
            System.out.println("Token Exception : " + e.getMessage());
        }
        return true;
    }

    @Override
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

    @Override
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

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}