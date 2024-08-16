package com.madeeasy.service;

import com.madeeasy.dto.request.UserRequestDTO;
import com.madeeasy.dto.response.UserAuthResponseDTO;
import com.madeeasy.dto.response.UserResponseDTO;

import java.util.List;

public interface UserService {
    List<UserResponseDTO> getAllUsers();

    UserAuthResponseDTO createUser(UserRequestDTO user);

    UserAuthResponseDTO partiallyUpdateUser(String emailId, UserRequestDTO userDetails);

    void deleteUser(String emailId);

    UserResponseDTO getUserByEmailId(String emailId);

}
