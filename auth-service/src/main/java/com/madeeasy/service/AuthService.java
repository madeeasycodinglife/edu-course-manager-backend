package com.madeeasy.service;

import com.madeeasy.dto.request.AuthRequest;
import com.madeeasy.dto.request.LogOutRequest;
import com.madeeasy.dto.request.SignInRequestDTO;
import com.madeeasy.dto.request.UserRequest;
import com.madeeasy.dto.response.AuthResponse;
import com.madeeasy.entity.User;

public interface AuthService {
    AuthResponse singUp(AuthRequest authRequest);

    AuthResponse singIn(SignInRequestDTO signInRequest);

    void revokeAllPreviousValidTokens(User user);

    void logOut(LogOutRequest logOutRequest);

    boolean validateAccessToken(String accessToken);

    AuthResponse partiallyUpdateUser(String emailId, UserRequest userRequest);

    AuthResponse refreshToken(String refreshToken);
}
