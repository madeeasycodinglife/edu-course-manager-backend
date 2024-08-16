package com.madeeasy.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestDTO {

    private String id;
    private String fullName;
    private String email;
    private String password;
    private String phone;
    private List<String> roles;
}
