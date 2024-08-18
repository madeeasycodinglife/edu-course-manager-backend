package com.madeeasy.service.impl;

import com.madeeasy.dto.request.CourseRequestDTO;
import com.madeeasy.dto.response.CourseResponseDTO;
import com.madeeasy.entity.Course;
import com.madeeasy.exception.CourseInstanceDeletionException;
import com.madeeasy.exception.CourseInstanceNotFoundException;
import com.madeeasy.exception.CourseNotFoundException;
import com.madeeasy.repository.CourseRepository;
import com.madeeasy.service.CourseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final RestTemplate restTemplate;
    private final HttpServletRequest httpServletRequest;

    private final Logger logger = LoggerFactory.getLogger(CourseServiceImpl.class);

    @Override
    public CourseResponseDTO createCourse(CourseRequestDTO courseRequestDTO) {

        Course course = Course.builder()
                .title(courseRequestDTO.getTitle())
                .courseCode(courseRequestDTO.getCourseCode())
                .description(courseRequestDTO.getDescription())
                .build();

        courseRepository.save(course);

        return CourseResponseDTO.builder()
                .id(course.getId())
                .title(courseRequestDTO.getTitle())
                .courseCode(courseRequestDTO.getCourseCode())
                .description(courseRequestDTO.getDescription())
                .build();

    }

    @Override
    public List<CourseResponseDTO> getAllCourses() {
        List<Course> courses = this.courseRepository.findAll();

        if (courses.isEmpty()) {
            // Handle the case where there are no courses
            // You can return an empty list or handle it differently
            return List.of(); // or return a custom response indicating no courses found
        }

        // Map the courses to CourseResponseDTO if the list is not empty
        return courses.stream()
                .map(course -> CourseResponseDTO.builder()
                        .id(course.getId())
                        .title(course.getTitle())
                        .courseCode(course.getCourseCode())
                        .description(course.getDescription())
                        .build())
                .toList();
    }

    @Override
    public CourseResponseDTO getCourseById(Long id) {

        Course course = this.courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id : " + id));

        return CourseResponseDTO.builder()
                .id(course.getId())
                .title(course.getTitle())
                .courseCode(course.getCourseCode())
                .description(course.getDescription())
                .build();
    }

    @Override
    public CourseResponseDTO getCourseByCourseCode(String courseCode) {

        Course course = this.courseRepository.findByCourseCode(courseCode)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with courseCode : " + courseCode));

        return CourseResponseDTO.builder()
                .id(course.getId())
                .title(course.getTitle())
                .courseCode(course.getCourseCode())
                .description(course.getDescription())
                .build();
    }

    /**
     * Delete Course by ID :-
     * -----------------------
     * When you delete a course by its ID:
     * <p>
     * Should you delete its instances?
     * Yes, if you delete a course, you should delete all instances associated with that course.
     * This is because the course instances are dependent on the existence of the course.
     * If the course no longer exists, its instances are meaningless since they reference a course that is no longer available.
     * <p>
     * Logic:
     * <p>
     * -> First, retrieve the course by its ID.
     * -> If the course exists, find all instances associated with this course.
     * -> Delete the instances.
     * -> Finally, delete the course.
     */

    @Override
    public void deleteCourse(Long id) {
        // Check if the course exists before deleting
        if (!courseRepository.existsById(id)) {
            throw new CourseNotFoundException("Course not found with ID: " + id);
        }

        try {
            // Proceed with the deletion of the course
            courseRepository.deleteById(id);

            // Create the URL for deleting the related course instances
            String courseServiceUrl = "http://course-instance-service/api/instances/courseId/" + id;

            // Get the authorization header from the request
            String authorizationHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);

            // Create HttpEntity with the token
            HttpEntity<String> requestEntity = createHttpEntityWithToken(authorizationHeader);

            // Make the DELETE request to the course instance service
            ResponseEntity<String> response = restTemplate.exchange(courseServiceUrl, HttpMethod.DELETE, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new CourseInstanceNotFoundException("Course instance with ID " + id + " not found in the course service.");
            } else if (response.getStatusCode() != HttpStatus.OK) {
                throw new CourseInstanceDeletionException("An error occurred while deleting the course instance with ID " + id);
            }

        } catch (HttpClientErrorException e) {
            logger.error("Client-side error: {}", e.getMessage());
            handleClientErrorException(e, id);
        } catch (HttpServerErrorException e) {
            logger.error("Server-side error: {}", e.getMessage());
            handleServerErrorException(e, id);
        } catch (ResourceAccessException e) {
            logger.error("Resource access error: {}", e.getMessage());
            handleResourceAccessException(e, id);
        } catch (Exception e) {
            logger.error("Unexpected error occurred: {}", e.getMessage());
            throw new CourseInstanceDeletionException("An unexpected error occurred while processing the deletion request for course with ID " + id);
        }
    }


    private void handleClientErrorException(HttpClientErrorException e, Long id) {
        HttpStatusCode statusCode = e.getStatusCode();
        switch (statusCode) {
            case NOT_FOUND:
                throw new CourseInstanceNotFoundException("Course instance with ID " + id + " not found.");
            case BAD_REQUEST:
                throw new CourseInstanceDeletionException("Bad request while deleting course instance with ID " + id);
            default:
                throw new CourseInstanceDeletionException("Client-side error occurred while deleting course instance with ID " + id);
        }
    }

    private void handleServerErrorException(HttpServerErrorException e, Long id) {
        HttpStatusCode statusCode = e.getStatusCode();
        throw new CourseInstanceDeletionException("Server-side error occurred while deleting course instance with ID " + id);
    }

    private void handleResourceAccessException(ResourceAccessException e, Long id) {
        throw new CourseInstanceDeletionException("Resource access error occurred while deleting course instance with ID " + id);
    }


    private HttpEntity<String> createHttpEntityWithToken(String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);  // Strip "Bearer " prefix
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return new HttpEntity<>(headers);
    }

}
