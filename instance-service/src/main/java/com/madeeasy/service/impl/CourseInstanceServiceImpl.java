package com.madeeasy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.madeeasy.dto.request.CourseInstanceRequestDTO;
import com.madeeasy.dto.response.CourseInstanceResponseDTO;
import com.madeeasy.entity.CourseInstance;
import com.madeeasy.exception.CourseInstanceNotFoundException;
import com.madeeasy.repository.CourseInstanceRepository;
import com.madeeasy.service.CourseInstanceService;
import com.madeeasy.vo.CourseResponseDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CourseInstanceServiceImpl implements CourseInstanceService {

    private static final String COURSE_INSTANCE = "courseInstance";
    private final CourseInstanceRepository courseInstanceRepository;
    private final RestTemplate restTemplate;
    private final HttpServletRequest httpServletRequest;


    @Override
    @Caching(evict = {
            @CacheEvict(value = COURSE_INSTANCE, key = "'getAllInstances'"),
            @CacheEvict(value = COURSE_INSTANCE, key = "#year + '-' + #semester")
    })
    @CircuitBreaker(name = "myCircuitBreaker", fallbackMethod = "fallbackCreateInstance")
    public CourseInstanceResponseDTO createInstance(CourseInstanceRequestDTO instance) {

        String courseServiceUrl = "http://course-service/api/courses/" + instance.getCourseId();
        String authorizationHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);

        ResponseEntity<CourseResponseDTO> courseResponse = fetchCourse(courseServiceUrl, authorizationHeader);

        // Course exists, proceed with creating the instance
        CourseInstance courseInstance = CourseInstance.builder()
                .year(instance.getYear())
                .semester(instance.getSemester())
                .courseId(instance.getCourseId())
                .build();

        CourseInstance savedInstance = courseInstanceRepository.save(courseInstance);

        return CourseInstanceResponseDTO.builder()
                .id(savedInstance.getId())
                .year(savedInstance.getYear())
                .semester(savedInstance.getSemester())
                .courseId(savedInstance.getCourseId())
                .build();
    }

    private ResponseEntity<CourseResponseDTO> fetchCourse(String url, String authorizationHeader) {
        HttpEntity<String> entity = createHttpEntityWithToken(authorizationHeader);
        return restTemplate.exchange(url, HttpMethod.GET, entity, CourseResponseDTO.class);
    }

    private HttpEntity<String> createHttpEntityWithToken(String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);  // Strip "Bearer " prefix
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return new HttpEntity<>(headers);
    }


    // Fallback method for createInstance
    public CourseInstanceResponseDTO fallbackCreateInstance(CourseInstanceRequestDTO instance, Throwable t) {
        log.error("message : {}", t.getMessage());

        // Check if the throwable is an instance of HttpClientErrorException
        if (t instanceof HttpClientErrorException exception) {
            if (exception.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
                try {
                    // Parse the response body as JSON
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(exception.getResponseBodyAsString());

                    // Extract specific fields from the JSON, such as 'message' and 'status'
                    String errorMessage = jsonNode.path("message").asText();
                    String errorStatus = jsonNode.path("status").asText();

                    // Log the extracted information
                    log.error("message : {} , status : {}", errorMessage, errorStatus);

                    return CourseInstanceResponseDTO.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .message("Bad request : " + errorMessage)
                            .build();
                } catch (Exception e) {
                    log.error("Failed to parse the error response", e);
                }
            } else {
                try {
                    // Parse the response body as JSON
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(exception.getResponseBodyAsString());

                    // Extract specific fields from the JSON, such as 'message' and 'status'
                    String errorMessage = jsonNode.path("message").asText();
                    String errorStatus = jsonNode.path("status").asText();

                    // Log the extracted information
                    log.error("message : {} , status : {}", errorMessage, errorStatus);

                    return CourseInstanceResponseDTO.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .message("Bad request : " + errorMessage)
                            .build();
                } catch (Exception e) {
                    log.error("Failed to parse the error response", e);
                }
            }
        }

        // Fallback response if the exception is not HttpClientErrorException or any other case
        return CourseInstanceResponseDTO.builder()
                .message("Sorry !! Token creation failed as User Service is unavailable. Please try again later.")
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    @Override
    @Cacheable(value = COURSE_INSTANCE, key = "#year + '-' + #semester")
    public List<CourseInstanceResponseDTO> getInstancesByYearAndSemester(int year, int semester) {
        List<CourseInstance> courseInstance = this.courseInstanceRepository.findByYearAndSemester(year, semester);

        if (courseInstance.isEmpty()) {
            throw new CourseInstanceNotFoundException("Course instance not found for year " + year + " and semester " + semester);
        }

        return courseInstance.stream()
                .map(instance -> CourseInstanceResponseDTO.builder()
                        .id(instance.getId())
                        .year(instance.getYear())
                        .semester(instance.getSemester())
                        .courseId(instance.getCourseId())
                        .build())
                .toList();
    }


    @Override
    @Cacheable(value = COURSE_INSTANCE, key = "#year + '-' + #semester + '-' + #courseId")
    public CourseInstanceResponseDTO getInstanceByYearSemesterAndCourseId(int year, int semester, Long courseId) {

        CourseInstance courseInstance = this.courseInstanceRepository
                .findByYearAndSemesterAndCourseId(year, semester, courseId)
                .orElseThrow(() -> new CourseInstanceNotFoundException("Course instance not found for year " + year + " and semester " + semester + " and course id " + courseId));

        return CourseInstanceResponseDTO.builder()
                .id(courseInstance.getId())
                .year(courseInstance.getYear())
                .semester(courseInstance.getSemester())
                .courseId(courseInstance.getCourseId())
                .build();
    }

    /**
     * Delete Course Instance by Year, Semester, and Course ID
     * <p>
     * When you delete a course instance :-
     * ------------------------------------
     * <p>
     * Should you delete the course?
     * No, deleting a course instance should not delete the course itself.
     * This is because the course may still have other instances, or it might be planned for future offerings.
     * Deleting a course instance only removes the specific instance, not the abstract course itself.
     * <p>
     * Logic:
     * <p>
     * -> First, retrieve the course instance by the year, semester, and course ID.
     * -> If the instance exists, delete the instance.
     * -> The course remains in the system, and only that specific instance is removed.
     */
    @Override
    @Transactional
    @CacheEvict(value = COURSE_INSTANCE, key = "#year + '-' + #semester + '-' + #courseId")
    public void deleteInstance(int year, int semester, Long courseId) {
        if (!this.courseInstanceRepository.existsByYearAndSemesterAndCourseId(year, semester, courseId)) {
            throw new CourseInstanceNotFoundException("Course instance not found for year " + year + " and semester " + semester + " and course id " + courseId);
        }
        this.courseInstanceRepository
                .deleteByYearAndSemesterAndCourseId(year, semester, courseId);
    }

    @Override
    @Cacheable(value = COURSE_INSTANCE, key = "#root.methodName", unless = "#result == null")
    public List<CourseInstanceResponseDTO> getAllInstances() {

        List<CourseInstance> courseInstance = this.courseInstanceRepository.findAll();

        if (courseInstance.isEmpty()) {
            return List.of();
        }
        return courseInstance.stream()
                .map(instance -> CourseInstanceResponseDTO.builder()
                        .id(instance.getId())
                        .year(instance.getYear())
                        .semester(instance.getSemester())
                        .courseId(instance.getCourseId())
                        .build())
                .toList();
    }

    @Override
    @CacheEvict(value = COURSE_INSTANCE, allEntries = true)
    public void deleteInstancesByCourseId(Long courseId) {

        List<CourseInstance> courseInstance = this.courseInstanceRepository.findByCourseId(courseId);
        if (courseInstance.isEmpty()) {
            throw new CourseInstanceNotFoundException("Course Instance Not found With CourseId : " + courseId);
        }

        this.courseInstanceRepository.deleteByCourseId(courseId);
    }
}
