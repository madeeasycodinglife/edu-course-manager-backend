package com.madeeasy.controller;

import com.madeeasy.dto.request.UserPatchRequestDTO;
import com.madeeasy.dto.request.UserRequestDTO;
import com.madeeasy.dto.response.UserAuthResponseDTO;
import com.madeeasy.dto.response.UserResponseDTO;
import com.madeeasy.service.UserService;
import com.madeeasy.util.ValidationUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(path = "/user-service")
@RequiredArgsConstructor
@Validated
public class UserServiceController {

    private final UserService userService;

    @PostMapping(path = "/create")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserRequestDTO user) {
        UserAuthResponseDTO savedUser = this.userService.createUser(user);
        if (savedUser.getStatus() == HttpStatus.CONFLICT) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(savedUser);
        } else if (savedUser.getStatus() == HttpStatus.BAD_REQUEST) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(savedUser);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PatchMapping(path = "/partial-update/{emailId}")
    public ResponseEntity<?> partiallyUpdateUser(@PathVariable("emailId") String emailId,
                                                 @RequestBody UserPatchRequestDTO user) {
        Map<String, String> validationErrors = ValidationUtils.validateEmail(emailId);
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationErrors);
        }
        UserAuthResponseDTO updatedUser = this.userService.partiallyUpdateUser(emailId, user);
        if (updatedUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("status", HttpStatus.NOT_FOUND,
                            "message", "User not found with emailId: " + emailId)
            );
        }
        if (updatedUser.getStatus() == HttpStatus.CONFLICT) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(updatedUser);
        } else if (updatedUser.getStatus() == HttpStatus.BAD_REQUEST) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(updatedUser);
        } else if (updatedUser.getStatus() == HttpStatus.SERVICE_UNAVAILABLE) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(updatedUser);
        }
        return ResponseEntity.status(HttpStatus.OK).body(updatedUser);
    }

    @GetMapping(path = "/{emailId}")
    public ResponseEntity<?> getUserByEmailId(@PathVariable("emailId") String emailId) {
        Map<String, String> validationErrors = ValidationUtils.validateEmail(emailId);
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationErrors);
        }
        UserResponseDTO user = this.userService.getUserByEmailId(emailId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with emailId: " + emailId);
        }
        return ResponseEntity.status(HttpStatus.OK).body(user);
    }
}
