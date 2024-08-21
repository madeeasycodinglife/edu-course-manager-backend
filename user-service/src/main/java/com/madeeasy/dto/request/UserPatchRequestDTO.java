package com.madeeasy.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPatchRequestDTO {

    private String fullName;
    private String email;
    private String password;
    private String phone;
    private List<String> roles;
}
