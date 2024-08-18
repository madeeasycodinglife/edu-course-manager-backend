package com.madeeasy.controller;

import com.madeeasy.dto.request.CourseRequestDTO;
import com.madeeasy.dto.response.CourseResponseDTO;
import com.madeeasy.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;


    @PostMapping
    public ResponseEntity<?> createCourse(@RequestBody CourseRequestDTO course) {
        CourseResponseDTO createdCourse = courseService.createCourse(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
    }

    @GetMapping
    public ResponseEntity<?> getAllCourses() {
        List<CourseResponseDTO> courses = courseService.getAllCourses();

        if (courses.isEmpty()) {
            // Return 204 No Content if there are no courses
            return ResponseEntity.status(HttpStatus.OK).body(List.of());
        }
        // Return 200 OK with the list of courses
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCourseById(@PathVariable Long id) {
        CourseResponseDTO course = courseService.getCourseById(id);
        return ResponseEntity.ok(course);
    }

    @GetMapping("/code/{courseCode}")
    public ResponseEntity<?> getCourseByCourseCode(@PathVariable String courseCode) {
        CourseResponseDTO course = courseService.getCourseByCourseCode(courseCode);
        return ResponseEntity.ok(course);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);

        // Create a response message
        String message = String.format("Course with ID %d has been successfully deleted.", id);

        // Create the response body
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", message);

        // Return the response entity with a custom message and HTTP status 204 No Content
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(responseBody);
    }
}
