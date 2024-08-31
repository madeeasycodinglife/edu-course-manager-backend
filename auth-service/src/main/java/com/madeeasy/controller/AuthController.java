package com.madeeasy.controller;


import com.madeeasy.dto.request.AuthRequest;
import com.madeeasy.dto.request.LogOutRequest;
import com.madeeasy.dto.request.SignInRequestDTO;
import com.madeeasy.dto.request.UserRequest;
import com.madeeasy.dto.response.AuthResponse;
import com.madeeasy.service.AuthService;
import com.madeeasy.util.ValidationUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/auth-service")
public class AuthController {

    private final AuthService authService;


    @PostMapping(path = "/sign-up")
    public ResponseEntity<?> singUp(@Valid @RequestBody AuthRequest authRequest) {
        AuthResponse authResponse = authService.singUp(authRequest);
        // Return the appropriate HTTP status based on the response status in AuthResponse
        if (authResponse.getStatus() == HttpStatus.CONFLICT) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(authResponse);
        } else if (authResponse.getStatus() == HttpStatus.BAD_REQUEST) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(authResponse);
        } else if (authResponse.getStatus() == HttpStatus.SERVICE_UNAVAILABLE) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(authResponse);
        }
        return ResponseEntity.ok().body(authResponse);
    }

    @PostMapping(path = "/sign-in")
    public ResponseEntity<?> singIn(@Valid @RequestBody SignInRequestDTO signInRequestDTO) {
        AuthResponse authResponse = this.authService.singIn(signInRequestDTO);
        return ResponseEntity.ok().body(authResponse);
    }

    @PostMapping(path = "/log-out")
    public ResponseEntity<?> logOut(@Valid @RequestBody LogOutRequest logOutRequest) {
        this.authService.logOut(logOutRequest);
        return ResponseEntity.ok().body("Logged out");
    }

    @PatchMapping(path = "/partial-update/{emailId}")
    public ResponseEntity<?> partiallyUpdateUser(@PathVariable("emailId") String emailId,
                                                 @Valid @RequestBody UserRequest userRequest) {
        Map<String, String> validatedEmail = ValidationUtils.validateEmail(emailId);
        if (!validatedEmail.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validatedEmail);
        }
        AuthResponse authResponse = this.authService.partiallyUpdateUser(emailId, userRequest);
        if (authResponse.getStatus() == HttpStatus.CONFLICT) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(authResponse);
        } else if (authResponse.getStatus() == HttpStatus.BAD_REQUEST) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(authResponse);
        }
        return ResponseEntity.ok().body(authResponse);
    }

    @PostMapping(path = "/refresh-token/{refreshToken}")
    public ResponseEntity<?> refreshToken(@PathVariable("refreshToken") String refreshToken) {
        Map<String, String> validatedRefreshToken = ValidationUtils.validateRefreshToken(refreshToken);
        if (!validatedRefreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validatedRefreshToken);
        }
        AuthResponse authResponse = this.authService.refreshToken(refreshToken);
        return ResponseEntity.ok().body(authResponse);
    }

    @PostMapping(path = "/validate-access-token/{accessToken}")
    public ResponseEntity<?> validateAccessToken(@PathVariable("accessToken") String accessToken) {
        Map<String, String> validatedAccessToken = ValidationUtils.validateAccessToken(accessToken);
        if (!validatedAccessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validatedAccessToken);
        }
        boolean flag = this.authService.validateAccessToken(accessToken);
        if (flag) {
            return ResponseEntity.ok().body(true);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
    }
}
