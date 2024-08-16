package com.madeeasy.dto.response;

import com.madeeasy.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserAuthResponseDTO {

    private String id;
    private String fullName;
    private String email;
    private String password;
    private String phone;
    private List<Role> roles;
    private String accessToken;
    private String refreshToken;
}
