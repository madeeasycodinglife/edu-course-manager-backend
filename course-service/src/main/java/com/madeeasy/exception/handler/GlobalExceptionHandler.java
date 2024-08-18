package com.madeeasy.exception.handler;

import com.madeeasy.exception.CourseInstanceDeletionException;
import com.madeeasy.exception.CourseInstanceNotFoundException;
import com.madeeasy.exception.CourseNotFoundException;
import com.madeeasy.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {

        logger.warn("Invalid value '{}' for parameter '{}'. Expected type is '{}'.",
                ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

        Map<String, Object> errorResponse = Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "message", String.format("Invalid value '%s' for parameter '%s'. Expected type is '%s'.",
                        ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName())
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCourseNotFoundException(CourseNotFoundException ex) {

        logger.warn("Course not found with id: {}", ex.getMessage());

        Map<String, Object> errorResponse = Map.of(
                "status", HttpStatus.NOT_FOUND.value(),
                "message", ex.getMessage()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CourseInstanceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCourseInstanceNotFoundException(CourseInstanceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", HttpStatus.NOT_FOUND.value(), "message", ex.getMessage()));
    }

    @ExceptionHandler(CourseInstanceDeletionException.class)
    public ResponseEntity<Map<String, Object>> handleCourseInstanceDeletionException(CourseInstanceDeletionException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR.value(), "message", ex.getMessage()));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailableException(ServiceUnavailableException ex) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", HttpStatus.SERVICE_UNAVAILABLE.value(), "message", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", HttpStatus.INTERNAL_SERVER_ERROR.value(), "message", "An unexpected error occurred."));
    }
}
