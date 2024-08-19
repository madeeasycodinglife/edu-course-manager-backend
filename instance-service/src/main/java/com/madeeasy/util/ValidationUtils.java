package com.madeeasy.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ValidationUtils {

    /**
     * Validates that the given value is not null or blank.
     *
     * @param value     The value to validate.
     * @param fieldName The name of the field being validated, used for error messages.
     * @return A map of validation errors. If there are no errors, the map will be empty.
     */
    public static Map<String, String> validateNotBlank(String value, String fieldName) {
        Map<String, String> errors = new HashMap<>();
        if (value == null || value.isBlank()) {
            errors.put(fieldName, fieldName + " must not be blank");
        }
        return errors;
    }

    /**
     * Validates that the given value is not null or empty.
     *
     * @param value     The value to validate.
     * @param fieldName The name of the field being validated, used for error messages.
     * @return A map of validation errors. If there are no errors, the map will be empty.
     */
    public static Map<String, String> validateNotEmpty(String value, String fieldName) {
        Map<String, String> errors = new HashMap<>();
        if (value == null || value.isEmpty()) {
            errors.put(fieldName, fieldName + " must not be empty");
        }
        return errors;
    }

    /**
     * Validates that the given value matches a specific pattern.
     *
     * @param value     The value to validate.
     * @param fieldName The name of the field being validated, used for error messages.
     * @param pattern   The regex pattern that the value must match.
     * @param patternDescription A description of the pattern for error messages.
     * @return A map of validation errors. If there are no errors, the map will be empty.
     */
    public static Map<String, String> validatePattern(String value, String fieldName, Pattern pattern, String patternDescription) {
        Map<String, String> errors = new HashMap<>();
        if (value == null || !pattern.matcher(value).matches()) {
            errors.put(fieldName, fieldName + " must match the pattern: " + patternDescription);
        }
        return errors;
    }

    /**
     * Validates that the given value is within a specified range.
     *
     * @param value     The value to validate.
     * @param fieldName The name of the field being validated, used for error messages.
     * @param min       The minimum value (inclusive).
     * @param max       The maximum value (inclusive).
     * @return A map of validation errors. If there are no errors, the map will be empty.
     */
    public static Map<String, String> validateRange(Integer value, String fieldName, Integer min, Integer max) {
        Map<String, String> errors = new HashMap<>();
        if (value == null || value < min || value > max) {
            errors.put(fieldName, fieldName + " must be between " + min + " and " + max);
        }
        return errors;
    }

    /**
     * Validates that the given value is a positive integer.
     *
     * @param value     The value to validate.
     * @param fieldName The name of the field being validated, used for error messages.
     * @return A map of validation errors. If there are no errors, the map will be empty.
     */
    public static Map<String, String> validatePositiveInteger(Integer value, String fieldName) {
        Map<String, String> errors = new HashMap<>();
        if (value == null || value <= 0) {
            errors.put(fieldName, fieldName + " must be a positive integer");
        }
        return errors;
    }
}
