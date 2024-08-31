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
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String USER = "user";
    private final RestTemplate restTemplate;
    private final CacheManager cacheManager;
    private final UserRepository userRepository;
    private final HttpServletRequest httpServletRequest;
    private final PasswordEncoder passwordEncoder;

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
        String rawOrEncodedPassword = user.getPassword();

        // Check if the password is already a valid bcrypt hash
        if (!passwordEncoder.matches(rawOrEncodedPassword, rawOrEncodedPassword)) {
            // If not, encrypt it
            rawOrEncodedPassword = passwordEncoder.encode(rawOrEncodedPassword);
        }


        // Check if a user with the given email or phone already exists
        boolean emailExists = userRepository.existsByEmail(user.getEmail());
        boolean phoneExists = userRepository.existsByPhone(user.getPhone());

        if (emailExists && phoneExists) {
            return UserAuthResponseDTO.builder()
                    .message("User with Email: " + user.getEmail() + " and Phone: " + user.getPhone() + " already exists.")
                    .status(HttpStatus.CONFLICT)
                    .build();
        } else if (emailExists) {
            return UserAuthResponseDTO.builder()
                    .message("User with Email: " + user.getEmail() + " already exists.")
                    .status(HttpStatus.CONFLICT)
                    .build();
        } else if (phoneExists) {
            return UserAuthResponseDTO.builder()
                    .message("User with Phone: " + user.getPhone() + " already exists.")
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        // Convert all roles to uppercase
        List<String> normalizedRoles = user.getRoles().stream()
                .map(String::toUpperCase) // Convert each role to uppercase
                .toList();

        // Check if roles contain valid enum names
        if (!normalizedRoles.contains(Role.ADMIN.name()) && !normalizedRoles.contains(Role.USER.name())) {
            return UserAuthResponseDTO.builder()
                    .message("Invalid roles provided. Allowed roles are ADMIN and USER.")
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }

        List<Role> roles = new ArrayList<>();
        if (normalizedRoles.size() == 1) {
            if (normalizedRoles.contains(Role.ADMIN.name())) {
                roles.add(Role.ADMIN);
            }
            if (normalizedRoles.contains(Role.USER.name())) {
                roles.add(Role.USER);
            }
        } else {
            roles.addAll(Arrays.asList(Role.ADMIN, Role.USER));
        }


        User userEntity = User.builder()
                .id(UUID.randomUUID().toString())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .password(rawOrEncodedPassword)
                .phone(user.getPhone())
                .roles(roles)
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
    @CircuitBreaker(name = "myCircuitBreaker", fallbackMethod = "fallbackPartiallyUpdateUser")
    public UserAuthResponseDTO partiallyUpdateUser(String emailId, UserPatchRequestDTO userDetails) {
        User foundUser = getByEmailId(emailId);
        UserRequestDTO userRequestDTO = new UserRequestDTO();

        if (foundUser != null) {
            if (userDetails.getFullName() != null && !userDetails.getFullName().isBlank()) {
                userRequestDTO.setFullName(userDetails.getFullName());
            }

            boolean emailExists = false;
            boolean phoneExists = false;

            // Check if the new email already exists and belongs to another user
            if (userDetails.getEmail() != null && !userDetails.getEmail().isBlank()) {
                emailExists = userRepository.existsByEmail(userDetails.getEmail());
            }

            // Check if the new phone number already exists and belongs to another user
            if (userDetails.getPhone() != null && !userDetails.getPhone().isBlank()) {
                phoneExists = userRepository.existsByPhone(userDetails.getPhone());
            }

            // Handle the case where both email and phone already exist
            if (emailExists && phoneExists) {
                return UserAuthResponseDTO.builder()
                        .status(HttpStatus.CONFLICT)
                        .message("User with Email: " + userDetails.getEmail() + " and Phone: " + userDetails.getPhone() + " already exist.")
                        .build();
            }

            // Handle the case where only email exists
            if (emailExists) {
                return UserAuthResponseDTO.builder()
                        .status(HttpStatus.CONFLICT)
                        .message("User with Email: " + userDetails.getEmail() + " already exists.")
                        .build();
            }

            // Handle the case where only phone exists
            if (phoneExists) {
                return UserAuthResponseDTO.builder()
                        .status(HttpStatus.CONFLICT)
                        .message("User with Phone: " + userDetails.getPhone() + " already exists.")
                        .build();
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
                // Convert all roles to uppercase
                List<String> normalizedRoles = userDetails.getRoles().stream()
                        .map(String::toUpperCase) // Convert each role to uppercase
                        .toList();

                // Check if roles contain valid enum names
                if (!normalizedRoles.contains(Role.ADMIN.name()) && !normalizedRoles.contains(Role.USER.name())) {
                    return UserAuthResponseDTO.builder()
                            .message("Invalid roles provided. Allowed roles are ADMIN and USER.")
                            .status(HttpStatus.BAD_REQUEST)
                            .build();
                }

                List<Role> roles = new ArrayList<>();
                if (normalizedRoles.size() == 1) {
                    if (normalizedRoles.contains(Role.ADMIN.name())) {
                        roles.add(Role.ADMIN);
                    }
                    if (normalizedRoles.contains(Role.USER.name())) {
                        roles.add(Role.USER);
                    }
                } else {
                    roles.addAll(Arrays.asList(Role.ADMIN, Role.USER));
                }
                userRequestDTO.setRoles(roles.stream().map(Role::name).toList());
            }

            // Send update request to auth-service
            String authorizationHeader = this.httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
            String url = "http://auth-service/auth-service/partial-update/" + emailId;
            String accessToken = authorizationHeader.substring(7);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<UserRequestDTO> requestEntity = new HttpEntity<>(userRequestDTO, headers);

            // Send the request to auth-service
            ResponseEntity<AuthResponse> responseEntity =
                    restTemplate.exchange(
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
                    // Ensure the roles collection is mutable
                    // Create a mutable ArrayList
                    List<Role> roles = userRequestDTO.getRoles().stream()
                            .map(role -> Role.valueOf(role.toUpperCase()))
                            .collect(Collectors.toCollection(ArrayList::new)); // Use ArrayList to make it mutable

// Set the roles for the found user
                    foundUser.setRoles(roles);

                }
                log.info("new update object : {}", foundUser);
                // Save the updated user to the local repository
                User updatedUser = this.userRepository.save(foundUser);

                Objects.requireNonNull(this.cacheManager.getCache(USER)).evict(emailId);

                // Return successful response with tokens
                return UserAuthResponseDTO.builder()
                        .id(updatedUser.getId())
                        .fullName(updatedUser.getFullName())
                        .email(updatedUser.getEmail())
                        .password(updatedUser.getPassword())
                        .phone(updatedUser.getPhone())
                        .roles(updatedUser.getRoles())
                        .accessToken((userDetails.getEmail() != null || userDetails.getRoles() != null) ? authResponse.getAccessToken() : null)
                        .refreshToken((userDetails.getEmail() != null || userDetails.getRoles() != null) ? authResponse.getRefreshToken() : null)
                        .build();

            } else {
                // Log error and return appropriate response if the auth-service call fails
                log.error("Failed to update user in auth-service for email: {}. Response status: {}", emailId, responseEntity.getStatusCode());
                return UserAuthResponseDTO.builder()
                        .status(HttpStatus.valueOf(responseEntity.getStatusCodeValue()))
                        .message("Failed to update user in auth-service for email: " + emailId)
                        .build();
            }
        }
        // Return null if the user was not found
        return null;
    }

    public UserAuthResponseDTO fallbackPartiallyUpdateUser(String emailId, UserPatchRequestDTO userDetails, Throwable t) {
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
                .message("Sorry !! Your request failed as Auth Service is unavailable. Please try again later.")
                .build();
    }


    @Override
    @Cacheable(value = USER, key = "#emailId", unless = "#result == null")
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
