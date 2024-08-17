package com.madeeasy.repository;

import com.madeeasy.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findById(@NonNull Long id);

    Optional<Course> findByCourseCode(@NonNull String courseCode);
}
