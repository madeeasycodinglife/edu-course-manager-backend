package com.madeeasy.service.impl;

import com.madeeasy.dto.request.CourseInstanceRequestDTO;
import com.madeeasy.dto.response.CourseInstanceResponseDTO;
import com.madeeasy.entity.CourseInstance;
import com.madeeasy.exception.CourseInstanceNotFoundException;
import com.madeeasy.exception.CourseNotFoundException;
import com.madeeasy.repository.CourseInstanceRepository;
import com.madeeasy.service.CourseInstanceService;
import com.madeeasy.vo.CourseResponseDTO;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseInstanceServiceImpl implements CourseInstanceService {

    private final CourseInstanceRepository courseInstanceRepository;
    private final RestTemplate restTemplate;
    private final HttpServletRequest httpServletRequest;
    Logger logger = LoggerFactory.getLogger(CourseInstanceServiceImpl.class);


    @Retry(name = "myRetry", fallbackMethod = "retryFallbackCreateInstance")
    @Override
    public CourseInstanceResponseDTO createInstance(CourseInstanceRequestDTO instance) {

        String courseServiceUrl = "http://course-service/api/courses/" + instance.getCourseId();
        String authorizationHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);

        try {
            // Check if the course exists
            ResponseEntity<CourseResponseDTO> courseResponse = fetchCourse(courseServiceUrl, authorizationHeader);

            if (courseResponse.getStatusCode().is2xxSuccessful()) {
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
            } else {
                throw new CourseNotFoundException("Course not found with id: " + instance.getCourseId());
            }
        } catch (HttpClientErrorException.NotFound e) {
            // Handle the case where the course was not found
            throw new CourseNotFoundException("Course not found with id: " + instance.getCourseId());
        } catch (Exception e) {
            // Handle other potential exceptions
            throw new RuntimeException("Error occurred while creating the instance", e);
        }
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
    public CourseInstanceResponseDTO retryFallbackCreateInstance(CourseInstanceRequestDTO instance, Throwable throwable) {
        // Log the error for debugging purposes
        logger.error("Fallback method triggered for createInstance. Error: {}", throwable.getMessage());

        // Return a default or fallback response
        return CourseInstanceResponseDTO.builder()
                .id(null)  // Indicate no valid ID could be created
                .year(null) // you can write instance.getYear()
                .semester(null) // you can write instance.getSemester()
                .courseId(null) // you can write instance.getCourseId()
                .message("Sorry !! Course instance creation failed as Course Service is unavailable. Please try again later.")
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    @Override
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

    @Transactional
    @Override
    public void deleteInstance(int year, int semester, Long courseId) {
        if (!this.courseInstanceRepository.existsByYearAndSemesterAndCourseId(year, semester, courseId)) {
            throw new CourseInstanceNotFoundException("Course instance not found for year " + year + " and semester " + semester + " and course id " + courseId);
        }
        this.courseInstanceRepository
                .deleteByYearAndSemesterAndCourseId(year, semester, courseId);
    }
}
