package com.madeeasy.service;

import com.madeeasy.dto.request.CourseRequestDTO;
import com.madeeasy.dto.response.CourseResponseDTO;

import java.util.List;

public interface CourseService {
    CourseResponseDTO createCourse(CourseRequestDTO course);

    List<CourseResponseDTO> getAllCourses();

    CourseResponseDTO getCourseById(Long id);
    CourseResponseDTO getCourseByCourseCode(String courseCode);

    void deleteCourse(Long id);
}
