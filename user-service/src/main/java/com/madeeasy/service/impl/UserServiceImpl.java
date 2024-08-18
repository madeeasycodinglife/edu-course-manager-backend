package com.madeeasy.service.impl;

import com.madeeasy.dto.request.UserRequestDTO;
import com.madeeasy.dto.response.AuthResponse;
import com.madeeasy.dto.response.UserAuthResponseDTO;
import com.madeeasy.dto.response.UserResponseDTO;
import com.madeeasy.entity.Role;
import com.madeeasy.entity.User;
import com.madeeasy.repository.UserRepository;
import com.madeeasy.service.UserService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    @Autowired
    private HttpServletRequest httpServletRequest;
    @Autowired
    private RestTemplate restTemplate;

    @Override
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
    public UserAuthResponseDTO createUser(UserRequestDTO user) {
        User userEntity = User.builder()
                .id(user.getId() == null ? UUID.randomUUID().toString() : user.getId())
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

    @CircuitBreaker(name = "myCircuitBreaker", fallbackMethod = "fallbackGetUser")
    @Override
    public UserAuthResponseDTO partiallyUpdateUser(String emailId, UserRequestDTO userDetails) {
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

            AuthResponse authResponse = null;
            try {
                // Create the HttpEntity with headers and the UserRequest object
                HttpEntity<UserRequestDTO> requestEntity = new HttpEntity<>(userRequestDTO, headers);
                // Make the PATCH request
                authResponse = restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, AuthResponse.class)
                        .getBody();
                assert authResponse != null;
            } catch (ResourceAccessException e) {
                log.error("Resource access error: {}", e.getMessage());
                return UserAuthResponseDTO.builder()
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .message("AuthService is Unavailable !!")
                        .build();
            }

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

    public UserAuthResponseDTO fallbackGetUser(String emailId, UserRequestDTO userDetails, Throwable t) {
        log.error("fallback error: {}", t.getMessage());
        return UserAuthResponseDTO.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .message("AuthService is Unavailable !!")
                .build();
    }

    @Override
    public void deleteUser(String emailId) {
        this.userRepository.deleteByEmail(emailId);
    }

    @Override
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
