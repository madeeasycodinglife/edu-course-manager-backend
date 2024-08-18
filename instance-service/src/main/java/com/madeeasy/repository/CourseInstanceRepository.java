package com.madeeasy.repository;

import com.madeeasy.entity.CourseInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
@Repository
public interface CourseInstanceRepository extends JpaRepository<CourseInstance, Long> {
    List<CourseInstance> findByYearAndSemester(int year, int semester);

    Optional<CourseInstance> findByYearAndSemesterAndCourseId(int year, int semester, Long courseId);

    @Modifying
    @Query("DELETE FROM CourseInstance ci WHERE ci.year = :year AND ci.semester = :semester AND ci.courseId = :courseId")
    void deleteByYearAndSemesterAndCourseId(
            @Param("year") int year,
            @Param("semester") int semester,
            @Param("courseId") Long courseId);

    List<CourseInstance> findByCourseId(Long courseId);

    @Modifying
    @Query("DELETE FROM CourseInstance ci WHERE ci.courseId = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);

    boolean existsByYearAndSemesterAndCourseId(int year, int semester, Long courseId);
}
