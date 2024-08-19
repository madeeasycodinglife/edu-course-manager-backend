package com.madeeasy.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequest {

    @NotBlank(message = "Full name is required.")
    private String fullName;

    @NotBlank(message = "Email address is required.")
    @Email(message = "Please provide a valid email address.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
            message = "Password must be at least 8 characters long, including at least one digit, one lowercase letter, one uppercase letter, and one special character. No spaces are allowed."
    )
    private String password;

    @NotBlank(message = "Phone number is required.")
    @Pattern(
            regexp = "^[+]?[0-9]{10,13}$",
            message = "Phone number must be between 10 and 13 digits long and may optionally start with a '+' sign."
    )
    private String phone;

    @NotEmpty(message = "roles cannot be empty")
    private List<@NotBlank(message = "role cannot be blank") String> roles;
}
