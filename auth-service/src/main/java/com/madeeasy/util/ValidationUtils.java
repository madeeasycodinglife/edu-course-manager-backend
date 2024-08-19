package com.madeeasy.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ValidationUtils {

    // Regex patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{10,13}$");
    private static final Pattern JWT_PATTERN = Pattern.compile("^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$");

    // Validate non-blank fields
    public static Map<String, String> validateNotBlank(String value, String fieldName) {
        Map<String, String> errors = new HashMap<>();
        if (value == null || value.isBlank()) {
            errors.put(fieldName, fieldName + " must not be blank");
        }
        return errors;
    }

    // Validate email
    public static Map<String, String> validateEmail(String email) {
        Map<String, String> errors = validateNotBlank(email, "Email");
        if (!errors.isEmpty()) {
            return errors;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.put("Email", "Email ID should be valid");
        }
        return errors;
    }

    // Validate password
    public static Map<String, String> validatePassword(String password) {
        Map<String, String> errors = validateNotBlank(password, "Password");
        if (!errors.isEmpty()) {
            return errors;
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            errors.put("Password", "Password must be at least 8 characters long and include a mix of uppercase, lowercase, numbers, and special characters");
        }
        return errors;
    }

    // Validate phone
    public static Map<String, String> validatePhone(String phone) {
        Map<String, String> errors = validateNotBlank(phone, "Phone");
        if (!errors.isEmpty()) {
            return errors;
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            errors.put("Phone", "Phone number must be valid and contain 10 to 13 digits");
        }
        return errors;
    }

    // Validate JWT token
    public static Map<String, String> validateJwtToken(String token) {
        Map<String, String> errors = validateNotBlank(token, "Token");
        if (!errors.isEmpty()) {
            return errors;
        }
        if (!JWT_PATTERN.matcher(token).matches()) {
            errors.put("Token", "Token must be a valid JWT token format");
        }
        return errors;
    }
    // Validate refresh token
    public static Map<String, String> validateRefreshToken(String refreshToken) {
        return validateJwtToken(refreshToken);
    }

    // Validate access token
    public static Map<String, String> validateAccessToken(String accessToken) {
        return validateJwtToken(accessToken);
    }
}
