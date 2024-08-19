package com.madeeasy.dto.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserPatchRequestDTO {

    private String fullName;
    private String email;
    private String password;
    private String phone;
    private List<String> roles;
}
