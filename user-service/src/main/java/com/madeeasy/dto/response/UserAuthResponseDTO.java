package com.madeeasy.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.madeeasy.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAuthResponseDTO implements Serializable {

    private String id;
    private String fullName;
    private String email;
    private String password;
    private String phone;
    private List<Role> roles;
    private String accessToken;
    private String refreshToken;
    private HttpStatus status;
    private String message;
}
