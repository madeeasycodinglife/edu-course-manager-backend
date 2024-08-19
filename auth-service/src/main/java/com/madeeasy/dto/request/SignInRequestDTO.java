package com.madeeasy.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignInRequestDTO {

    @Email(message = "email should be valid")
    @NotBlank(message = "email cannot be blank")
    private String email;
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
            message = "Password must be at least 8 characters long, including at least one digit, one lowercase letter, one uppercase letter, and one special character. No spaces are allowed."
    )
    @NotBlank(message = "password cannot be blank")
    private String password;
}
