package com.madeeasy.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserRequest {

    @Nullable
    private String id;
    @Nullable
    private String fullName;
    @Email(message = "email should be valid")
    @Nullable
    private String email;
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
            message = "Password must be at least 8 characters long, including at least one digit, one lowercase letter, one uppercase letter, and one special character. No spaces are allowed."
    )
    @Nullable
    private String password;
    @Pattern(regexp = "^[+]?[0-9]{10,13}$", message = "phone must be a valid phone number with 10 to 13 digits")
    @Nullable
    private String phone;

    @Nullable
    private List<String> roles;
}
