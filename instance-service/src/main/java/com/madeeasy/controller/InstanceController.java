package com.madeeasy.controller;

import com.madeeasy.dto.request.CourseInstanceRequestDTO;
import com.madeeasy.dto.response.CourseInstanceResponseDTO;
import com.madeeasy.service.CourseInstanceService;
import com.madeeasy.util.ValidationUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/instances")
@RequiredArgsConstructor
@Validated
public class InstanceController {

    private final CourseInstanceService instanceService;

    @PostMapping
    public ResponseEntity<?> createInstance(@Valid @RequestBody CourseInstanceRequestDTO instance) {
        CourseInstanceResponseDTO createdInstance = instanceService.createInstance(instance);
        if (createdInstance.getId() == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(createdInstance);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(createdInstance);
    }

    @GetMapping
    public ResponseEntity<?> getAllInstances() {
        List<CourseInstanceResponseDTO> instances = instanceService.getAllInstances();
        if (instances.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(List.of());
        }
        return ResponseEntity.ok(instances);
    }

    @DeleteMapping(path = "/courseId/{courseId}")
    public ResponseEntity<?> deleteInstancesByYear(@PathVariable Long courseId) {
        // Validate courseId
        Map<String, String> validationErrors = ValidationUtils.validatePositiveInteger(courseId.intValue(), "courseId");
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(validationErrors);
        }
        instanceService.deleteInstancesByCourseId(courseId);
        return ResponseEntity.status(HttpStatus.OK).body("Instances deleted successfully");
    }

    @GetMapping("/{year}/{semester}")
    public ResponseEntity<?> getInstancesByYearAndSemester(@PathVariable int year,
                                                           @PathVariable int semester) {
        // Validate year and semester
        Map<String, String> validationErrors = new HashMap<>();
        validationErrors.putAll(ValidationUtils.validatePositiveInteger(year, "year")); // Adjust range as needed
        validationErrors.putAll(ValidationUtils.validateRange(semester, "semester", 1, 20)); // Assuming 1 or 20 semesters
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(validationErrors);
        }
        List<CourseInstanceResponseDTO> instances = instanceService.getInstancesByYearAndSemester(year, semester);
        return ResponseEntity.ok(instances);
    }

    @GetMapping("/{year}/{semester}/{courseId}")
    public ResponseEntity<?> getInstanceByYearSemesterAndCourseId(@PathVariable int year,
                                                                  @PathVariable int semester,
                                                                  @PathVariable Long courseId) {
        // Validate year, semester, and courseId
        Map<String, String> validationErrors = new HashMap<>();
        validationErrors.putAll(ValidationUtils.validatePositiveInteger(year, "year")); // Adjust range as needed
        validationErrors.putAll(ValidationUtils.validateRange(semester, "semester", 1, 20)); // Assuming 1 or 20 semesters
        validationErrors.putAll(ValidationUtils.validatePositiveInteger(courseId.intValue(), "courseId"));

        if (!validationErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(validationErrors);
        }
        CourseInstanceResponseDTO instance = instanceService.getInstanceByYearSemesterAndCourseId(year, semester, courseId);
        return ResponseEntity.ok(instance);
    }

    @DeleteMapping("/{year}/{semester}/{courseId}")
    public ResponseEntity<?> deleteInstance(@PathVariable int year,
                                            @PathVariable int semester,
                                            @PathVariable Long courseId) {

        // Validate year, semester, and courseId
        Map<String, String> validationErrors = new HashMap<>();
        validationErrors.putAll(ValidationUtils.validateRange(year, "year", 1900, 2100)); // Adjust range as needed
        validationErrors.putAll(ValidationUtils.validateRange(semester, "semester", 1, 20)); // Assuming 1 or 20 semesters
        validationErrors.putAll(ValidationUtils.validatePositiveInteger(courseId.intValue(), "courseId"));

        if (!validationErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(validationErrors);
        }

        instanceService.deleteInstance(year, semester, courseId);
        // Create a response message
        String message = String.format(
                "Instance of course ID %d delivered in year %d and semester %d has been successfully deleted.",
                courseId, year, semester);

        // Create the response body
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", message);

        // Return the response entity with a custom message 204 No Content
        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }
}
