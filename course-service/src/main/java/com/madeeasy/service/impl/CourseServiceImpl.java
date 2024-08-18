package com.madeeasy.service.impl;

import com.madeeasy.dto.request.CourseRequestDTO;
import com.madeeasy.dto.response.CourseResponseDTO;
import com.madeeasy.entity.Course;
import com.madeeasy.exception.CourseNotFoundException;
import com.madeeasy.repository.CourseRepository;
import com.madeeasy.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

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
            return Collections.emptyList(); // or return a custom response indicating no courses found
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

    @Override
    public void deleteCourse(Long id) {
        // Check if the course exists before deleting
        if (!courseRepository.existsById(id)) {
            throw new CourseNotFoundException("Course not found with ID: " + id);
        }
        // Proceed with the deletion
        courseRepository.deleteById(id);
    }
}
