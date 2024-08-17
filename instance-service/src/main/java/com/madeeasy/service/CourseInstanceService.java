package com.madeeasy.service;

import com.madeeasy.dto.request.CourseInstanceRequestDTO;
import com.madeeasy.dto.response.CourseInstanceResponseDTO;

import java.util.List;

public interface CourseInstanceService {

    CourseInstanceResponseDTO createInstance(CourseInstanceRequestDTO instance);

    List<CourseInstanceResponseDTO> getInstancesByYearAndSemester(int year, int semester);

    CourseInstanceResponseDTO getInstanceByYearSemesterAndCourseId(int year, int semester, Long courseId);

    void deleteInstance(int year, int semester, Long courseId);
}
