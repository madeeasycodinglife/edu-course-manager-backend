package com.madeeasy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class CourseInstance implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "course_instance_sequence_generator")
    @SequenceGenerator(
            name = "course_instance_sequence_generator",
            sequenceName = "course_instance_sequence",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "instance_year", nullable = false)
    private int year;

    @Column(nullable = false)
    private int semester;

    @Column(name = "course_id", nullable = false)
    private Long courseId;
}
