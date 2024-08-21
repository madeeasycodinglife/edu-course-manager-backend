package com.madeeasy.dto.response;

import com.madeeasy.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO implements Serializable {

    private String id;
    private String fullName;
    private String email;
    private String password;
    private String phone;
    private List<Role> roles;
}
