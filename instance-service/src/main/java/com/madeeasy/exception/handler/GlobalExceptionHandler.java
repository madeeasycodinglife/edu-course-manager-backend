package com.madeeasy.exception.handler;

import com.madeeasy.exception.CourseInstanceNotFoundException;
import com.madeeasy.exception.CourseNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {


    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex,
                                                                   HttpHeaders headers,
                                                                   HttpStatusCode status,
                                                                   WebRequest request) {

        log.error("handleNoHandlerFoundException: {}", ex.getMessage());
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.NOT_FOUND);
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", "The requested URL was not found on this server.");
        errorResponse.put("path", request.getDescription(false).substring(4)); // Removing the 'uri=' prefix
        errorResponse.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {

        log.warn("Invalid value '{}' for parameter '{}'. Expected type is '{}'."
                , ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

        Map<String, Object> errorResponse = Map.of(
                "status", HttpStatus.BAD_REQUEST,
                "message", String.format("Invalid value '%s' for parameter '%s'. Expected type is '%s'.",
                        ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName())
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                         HttpHeaders headers,
                                                                         HttpStatusCode status,
                                                                         WebRequest request) {
        log.error("handleHttpRequestMethodNotSupported: {}", ex.getMessage());
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.METHOD_NOT_ALLOWED);
        errorResponse.put("error", "Method Not Allowed");
        errorResponse.put("message", "The HTTP method is not supported for this endpoint.");

        // Convert HttpMethod set to a list of strings
        List<String> supportedMethods = Objects.requireNonNull(ex.getSupportedHttpMethods()).stream()
                .map(HttpMethod::name)
                .collect(Collectors.toList());
        errorResponse.put("supported_methods", supportedMethods);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST);

        String fieldDetails = extractFieldDetails(ex.getMessage());
        response.put("message", fieldDetails != null ? fieldDetails : "Data integrity violation error occurred.");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    private static String extractFieldDetails(String message) {
        if (message != null) {
            // Regex to capture field names within parentheses after the constraint name
            String regex = "ON [^\\s]+\\(([^)]+?)\\s*(NULLS FIRST)?\\)";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(message);

            if (matcher.find()) {
                // Extract field names and format them
                String fields = matcher.group(1);
                return Arrays.stream(fields.split(","))
                        .map(String::trim)
                        .map(field -> field + " must be unique.")
                        .collect(Collectors.joining(" "));
            }
        }
        return "Data integrity violation error occurred.";
    }


    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCourseNotFound(CourseNotFoundException ex) {
        Map<String, Object> errorResponse = Map.of(
                "status", HttpStatus.NOT_FOUND,
                "message", ex.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CourseInstanceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleInstanceNotFound(CourseInstanceNotFoundException ex) {
        Map<String, Object> errorResponse = Map.of(
                "status", HttpStatus.NOT_FOUND,
                "message", ex.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        Map<String, Object> errorResponse = Map.of(
                "status", HttpStatus.INTERNAL_SERVER_ERROR,
                "message", ex.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
