package com.madeeasy.controller;

import com.madeeasy.dto.request.CourseInstanceRequestDTO;
import com.madeeasy.dto.response.CourseInstanceResponseDTO;
import com.madeeasy.service.CourseInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/instances")
@RequiredArgsConstructor
public class InstanceController {

    private final CourseInstanceService instanceService;

    @PostMapping
    public ResponseEntity<?> createInstance(@RequestBody CourseInstanceRequestDTO instance) {
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
        instanceService.deleteInstancesByCourseId(courseId);
        return ResponseEntity.status(HttpStatus.OK).body("Instances deleted successfully");
    }

    @GetMapping("/{year}/{semester}")
    public ResponseEntity<?> getInstancesByYearAndSemester(@PathVariable int year,
                                                           @PathVariable int semester) {
        List<CourseInstanceResponseDTO> instances = instanceService.getInstancesByYearAndSemester(year, semester);
        return ResponseEntity.ok(instances);
    }

    @GetMapping("/{year}/{semester}/{courseId}")
    public ResponseEntity<?> getInstanceByYearSemesterAndCourseId(@PathVariable int year,
                                                                  @PathVariable int semester,
                                                                  @PathVariable Long courseId) {
        CourseInstanceResponseDTO instance = instanceService.getInstanceByYearSemesterAndCourseId(year, semester, courseId);
        return ResponseEntity.ok(instance);
    }

    @DeleteMapping("/{year}/{semester}/{courseId}")
    public ResponseEntity<?> deleteInstance(@PathVariable int year,
                                            @PathVariable int semester,
                                            @PathVariable Long courseId) {
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
