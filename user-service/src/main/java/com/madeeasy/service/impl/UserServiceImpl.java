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
                        .roles(user.getRoles())
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
                foundUser.setFullName(userDetails.getFullName());
                userRequestDTO.setFullName(userDetails.getFullName());
            }
            if (userDetails.getEmail() != null && !userDetails.getEmail().isBlank()) {
                foundUser.setEmail(userDetails.getEmail());
                userRequestDTO.setEmail(userDetails.getEmail());
            }
            if (userDetails.getPassword() != null && !userDetails.getPassword().isBlank()) {
                foundUser.setPassword(userDetails.getPassword());
                userRequestDTO.setPassword(userDetails.getPassword());
            }
            if (userDetails.getPhone() != null && !userDetails.getPhone().isBlank()) {
                foundUser.setPhone(userDetails.getPhone());
                userRequestDTO.setPhone(userDetails.getPhone());
            }
            if (userDetails.getRoles() != null && !userDetails.getRoles().isEmpty()) {
                foundUser.setRoles(userDetails.getRoles().stream().map(Role::valueOf).toList());
                userRequestDTO.setRoles(userDetails.getRoles());
            }

            User updatedUser = this.userRepository.save(foundUser);

            // rest-call to the auth-service to let it know that the user has been updated

            // take accessToken from Authorization header and send it to auth-service

            String authorizationHeader = this.httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);

            String url = "http://auth-service/auth-service/partial-update/" + emailId;
            String accessToken = authorizationHeader.substring(7);
            // Prepare headers with the Authorization token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create the HttpEntity with headers and the UserRequest object
            HttpEntity<UserRequestDTO> requestEntity = new HttpEntity<>(userRequestDTO, headers);
            // Make the PATCH request
            var authResponse = restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, AuthResponse.class)
                    .getBody();
            assert authResponse != null;

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
        }
        return null;
    }

    public UserAuthResponseDTO fallbackGetUser(String emailId,
                                               UserRequestDTO userDetails,
                                               Throwable t) {
        log.error("message : {}", t.getMessage());

        // Check if the throwable is an instance of HttpClientErrorException
        if (t instanceof HttpClientErrorException exception) {
            if (exception.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
                try {
                    // Parse the response body as JSON
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(exception.getResponseBodyAsString());

                    // Extract specific fields from the JSON, such as 'message' and 'status'
                    String errorMessage = jsonNode.path("message").asText();
                    String errorStatus = jsonNode.path("status").asText();

                    // Log the extracted information
                    log.error("message : {} , status : {}", errorMessage, errorStatus);

                    return UserAuthResponseDTO.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .message("Bad request : " + errorMessage)
                            .build();
                } catch (Exception e) {
                    log.error("Failed to parse the error response", e);
                }
            } else {
                try {
                    // Parse the response body as JSON
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(exception.getResponseBodyAsString());

                    // Extract specific fields from the JSON, such as 'message' and 'status'
                    String errorMessage = jsonNode.path("message").asText();
                    String errorStatus = jsonNode.path("status").asText();

                    // Log the extracted information
                    log.error("message : {} , status : {}", errorMessage, errorStatus);

                    return UserAuthResponseDTO.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .message("Bad request : " + errorMessage)
                            .build();
                } catch (Exception e) {
                    log.error("Failed to parse the error response", e);
                }
            }
        }

        // Fallback response if the exception is not HttpClientErrorException or any other case
        return UserAuthResponseDTO.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .message("Sorry !! Token creation failed as User Service is unavailable. Please try again later.")
                .build();
    }

    @Override
    @Cacheable(value = USER, key = "#emailId")
    public UserResponseDTO getUserByEmailId(String emailId) {
        User foundUser = this.userRepository.findByEmail(emailId)
                .orElse(null);
        if (foundUser != null) {
            return UserResponseDTO.builder()
                    .id(foundUser.getId())
                    .fullName(foundUser.getFullName())
                    .email(foundUser.getEmail())
                    .password(foundUser.getPassword())
                    .phone(foundUser.getPhone())
                    .roles(foundUser.getRoles())
                    .build();
        }
        return null;
    }

    private User getByEmailId(String emailId) {
        return this.userRepository.findByEmail(emailId)
                .orElse(null);
    }
}
