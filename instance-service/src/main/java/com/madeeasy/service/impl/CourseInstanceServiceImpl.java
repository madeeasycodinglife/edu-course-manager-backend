package com.madeeasy.service.impl;

import com.madeeasy.dto.request.CourseInstanceRequestDTO;
import com.madeeasy.dto.response.CourseInstanceResponseDTO;
import com.madeeasy.entity.CourseInstance;
import com.madeeasy.exception.CourseInstanceNotFoundException;
import com.madeeasy.exception.CourseNotFoundException;
import com.madeeasy.repository.CourseInstanceRepository;
import com.madeeasy.service.CourseInstanceService;
import com.madeeasy.vo.CourseResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseInstanceServiceImpl implements CourseInstanceService {

    private final CourseInstanceRepository courseInstanceRepository;
    private final RestTemplate restTemplate;

    @Override
    public CourseInstanceResponseDTO createInstance(CourseInstanceRequestDTO instance) {

        String COURSE_SERVICE_URL = "http://course-service/api/courses";  // Update with the actual URL

        // Check if the course exists
        String courseUrl = COURSE_SERVICE_URL + "/" + instance.getCourseId();
        try {
            ResponseEntity<CourseResponseDTO> courseResponse = restTemplate.getForEntity(courseUrl, CourseResponseDTO.class);
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
