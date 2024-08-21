package com.madeeasy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.madeeasy.dto.request.UserPatchRequestDTO;
import com.madeeasy.dto.request.UserRequestDTO;
import com.madeeasy.dto.response.AuthResponse;
import com.madeeasy.dto.response.UserAuthResponseDTO;
import com.madeeasy.dto.response.UserResponseDTO;
import com.madeeasy.entity.Role;
import com.madeeasy.entity.User;
import com.madeeasy.exception.UserNotFoundException;
import com.madeeasy.repository.UserRepository;
import com.madeeasy.service.UserService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String USER = "user";
    private final UserRepository userRepository;
    private final HttpServletRequest httpServletRequest;
    private final RestTemplate restTemplate;

    @Override
    @Cacheable(value = USER, key = "#root.methodName", unless = "#result == null")
    public List<UserResponseDTO> getAllUsers() {
        List<User> userList = this.userRepository.findAll();
        return userList.stream()
                .map(user -> UserResponseDTO.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .password(user.getPassword())
                        .phone(user.getPhone())
                        .roles(new ArrayList<>(user.getRoles()))
                        .build())
                .toList();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = USER, key = "'getAllUsers'")
    })
    public UserAuthResponseDTO createUser(UserRequestDTO user) {
        User userEntity = User.builder()
                .id(UUID.randomUUID().toString())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .password(user.getPassword())
                .phone(user.getPhone())
                .roles(user.getRoles().stream().map(Role::valueOf).toList())
                .build();

        User savedUser = this.userRepository.save(userEntity);
        return UserAuthResponseDTO.builder()
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .password(savedUser.getPassword())
                .phone(savedUser.getPhone())
                .roles(savedUser.getRoles())
                .build();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = USER, key = "'getAllUsers'")
    })
    @CircuitBreaker(name = "myCircuitBreaker", fallbackMethod = "fallbackGetUser")
    public UserAuthResponseDTO partiallyUpdateUser(String emailId, UserPatchRequestDTO userDetails) {
        User foundUser = getByEmailId(emailId);
        UserRequestDTO userRequestDTO = new UserRequestDTO();

        if (foundUser != null) {
            if (userDetails.getFullName() != null && !userDetails.getFullName().isBlank()) {
                userRequestDTO.setFullName(userDetails.getFullName());
            }
            if (userDetails.getEmail() != null && !userDetails.getEmail().isBlank()) {
                userRequestDTO.setEmail(userDetails.getEmail());
            }
            if (userDetails.getPassword() != null && !userDetails.getPassword().isBlank()) {
                userRequestDTO.setPassword(userDetails.getPassword());
            }
            if (userDetails.getPhone() != null && !userDetails.getPhone().isBlank()) {
                userRequestDTO.setPhone(userDetails.getPhone());
            }
            if (userDetails.getRoles() != null && !userDetails.getRoles().isEmpty()) {
                userRequestDTO.setRoles(userDetails.getRoles());
            }

            // Send update request to auth-service
            String authorizationHeader = this.httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
            String url = "http://auth-service/auth-service/partial-update/" + emailId;
            String accessToken = authorizationHeader.substring(7);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<UserRequestDTO> requestEntity = new HttpEntity<>(userRequestDTO, headers);
            try {
                // Send the request to auth-service
                ResponseEntity<AuthResponse> responseEntity = restTemplate.exchange(
                        url, HttpMethod.PATCH, requestEntity, AuthResponse.class);

                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    AuthResponse authResponse = responseEntity.getBody();
                    assert authResponse != null;

                    // Update foundUser with the new details
                    if (userDetails.getFullName() != null && !userDetails.getFullName().isBlank()) {
                        foundUser.setFullName(userDetails.getFullName());
                    }
                    if (userDetails.getEmail() != null && !userDetails.getEmail().isBlank()) {
                        foundUser.setEmail(userDetails.getEmail());
                    }
                    if (userDetails.getPassword() != null && !userDetails.getPassword().isBlank()) {
                        foundUser.setPassword(userDetails.getPassword());
                    }
                    if (userDetails.getPhone() != null && !userDetails.getPhone().isBlank()) {
                        foundUser.setPhone(userDetails.getPhone());
                    }
                    if (userDetails.getRoles() != null && !userDetails.getRoles().isEmpty()) {
                        foundUser.setRoles(userDetails.getRoles().stream().map(Role::valueOf).toList());
                    }

                    // Save the updated user to the local repository
                    User updatedUser = this.userRepository.save(foundUser);

                    // Return successful response with tokens
                    return UserAuthResponseDTO.builder()
                            .id(updatedUser.getId())
                            .fullName(updatedUser.getFullName())
                            .email(updatedUser.getEmail())
                            .password(updatedUser.getPassword())
                            .phone(updatedUser.getPhone())
                            .roles(updatedUser.getRoles())
                            .accessToken(authResponse.getAccessToken())
                            .refreshToken(authResponse.getRefreshToken())
                            .build();
                } else {
                    // Log error and return appropriate response if the auth-service call fails
                    log.error("Failed to update user in auth-service for email: {}. Response status: {}", emailId, responseEntity.getStatusCode());
                    return UserAuthResponseDTO.builder()
                            .status((HttpStatus) responseEntity.getStatusCode())
                            .message("Failed to update user in auth-service for email: " + emailId)
                            .build();
                }
            } catch (HttpClientErrorException exception) {
                log.error("Failed to update user in auth-service for email: {}. Error response: {}", emailId, exception.getResponseBodyAsString());
                return handleHttpClientErrorException(exception);
            }
        }
        // Return null if the user was not found
        return null;
    }

    public UserAuthResponseDTO fallbackGetUser(String emailId, UserPatchRequestDTO userDetails, Throwable t) {
        log.error("message : {}", t.getMessage());

        // Handle different types of exceptions and provide appropriate responses
        if (t instanceof HttpClientErrorException exception) {
            return handleHttpClientErrorException(exception);
        }

        // Default fallback response
        return UserAuthResponseDTO.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .message("Sorry !! Your request failed as Auth Service is unavailable. Please try again later.")
                .build();
    }

    private UserAuthResponseDTO handleHttpClientErrorException(HttpClientErrorException exception) {
        try {
            // Parse the error response body as JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(exception.getResponseBodyAsString());

            // Extract specific fields from the JSON
            String errorMessage = jsonNode.path("message").asText();
            String errorStatus = jsonNode.path("status").asText();

            // Log the extracted information
            log.error("message : {} , status : {}", errorMessage, errorStatus);

            // Return a response based on the parsed error
            return UserAuthResponseDTO.builder()
                    .status((HttpStatus) exception.getStatusCode())
                    .message("Bad request : " + errorMessage)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse the error response", e);
        }

        // Fallback if parsing fails
        return UserAuthResponseDTO.builder()
                .status((HttpStatus) exception.getStatusCode())
                .message("An error occurred while processing the request.")
                .build();
    }


    @Override
    @Cacheable(value = USER, key = "#emailId")
    public UserResponseDTO getUserByEmailId(String emailId) {
        User foundUser = this.userRepository.findByEmail(emailId)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + emailId));
        if (foundUser != null) {
            return UserResponseDTO.builder()
                    .id(foundUser.getId())
                    .fullName(foundUser.getFullName())
                    .email(foundUser.getEmail())
                    .password(foundUser.getPassword())
                    .phone(foundUser.getPhone())
                    .roles(new ArrayList<>(foundUser.getRoles()))
                    .build();
        }
        return null;
    }

    private User getByEmailId(String emailId) {
        return this.userRepository.findByEmail(emailId)
                .orElse(null);
    }
}
