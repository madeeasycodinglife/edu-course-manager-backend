package com.madeeasy.service.impl;

import com.madeeasy.dto.request.CourseRequestDTO;
import com.madeeasy.dto.response.CourseResponseDTO;
import com.madeeasy.dto.response.ResponseDTO;
import com.madeeasy.entity.Course;
import com.madeeasy.exception.CourseInstanceDeletionException;
import com.madeeasy.exception.CourseInstanceNotFoundException;
import com.madeeasy.exception.CourseNotFoundException;
import com.madeeasy.repository.CourseRepository;
import com.madeeasy.service.CourseService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final RestTemplate restTemplate;
    private final HttpServletRequest httpServletRequest;
    private final Logger logger = LoggerFactory.getLogger(CourseServiceImpl.class);
    private final static String COURSE = "course";

    @Override
    @CacheEvict(value = COURSE, key = "'getAllCourses'")
    public CourseResponseDTO createCourse(CourseRequestDTO courseRequestDTO) {

        logger.info("Creating course: {}", courseRequestDTO);

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
    @Cacheable(value = COURSE, key = "#root.methodName", unless = "#result == null")
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
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = COURSE, key = "#id", unless = "#result == null")
    public CourseResponseDTO getCourseById(Long id) {
        logger.info("Fetching course with ID: {}", id);
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
    @Cacheable(value = "courseByCode", key = "#courseCode", unless = "#result == null")
    public CourseResponseDTO getCourseByCourseCode(String courseCode) {
        logger.info("Fetching course with courseCode : {}", courseCode);
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
    @CacheEvict(value = COURSE, key = "#id")
    @Retry(name = "myRetry", fallbackMethod = "fallbackDeleteCourse")
    @CircuitBreaker(name = "myCircuitBreaker", fallbackMethod = "fallbackDeleteCourse")
    public ResponseDTO deleteCourse(Long id) {
        // Check if the course exists before deleting
        if (!courseRepository.existsById(id)) {
            return new ResponseDTO("Course with ID " + id + " does not exist.", NOT_FOUND);
        }
        // Proceed with the deletion of the course
        courseRepository.deleteById(id);

        // Create the URL for deleting the related course instances
        String courseServiceUrl = "http://course-instance-service/api/instances/courseId/" + id;

        // Get the authorization header from the request
        String authorizationHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);

        // Create HttpEntity with the token
        HttpEntity<String> requestEntity = createHttpEntityWithToken(authorizationHeader);

        logger.info("Calling course instance service to delete course instance with ID: {}", id);

        try {
            // Make the DELETE request to the course instance service
            ResponseEntity<String> response = restTemplate.exchange(courseServiceUrl, HttpMethod.DELETE, requestEntity, String.class);

            // Check if the response status is 200 OK
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Course instance deleted successfully for course ID: {}", id);
                return new ResponseDTO("Course with ID " + id + " has been successfully deleted.", HttpStatus.OK);
            } else {
                logger.error("Failed to delete course instance for course ID: {}. Response status: {}", id, response.getStatusCode());
                return new ResponseDTO("Failed to delete course instance for course ID: " + id, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.error("Course instance not found for course ID: {}", id);
                return new ResponseDTO("Course instance not found for course ID: " + id, HttpStatus.NOT_FOUND);
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                logger.error("Bad request while deleting course instance: {}", e.getMessage());
                return new ResponseDTO("Bad request while deleting course instance: " + e.getMessage(), HttpStatus.BAD_REQUEST);
            } else {
                logger.error("An error occurred while trying to delete course instance: {}", e.getMessage());
                return new ResponseDTO("An error occurred while trying to delete course instance: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            logger.error("An unexpected error occurred: {}", e.getMessage());
            return new ResponseDTO("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private HttpEntity<String> createHttpEntityWithToken(String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);  // Strip "Bearer " prefix
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return new HttpEntity<>(headers);
    }

    public ResponseDTO fallbackDeleteCourse(Long id, Throwable t) {
        log.error("message : {}", t.getMessage());
        return ResponseDTO.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .message("Sorry !! Course deletion failed as Course Instance Service is unavailable. Please try again later.")
                .build();
    }
}


