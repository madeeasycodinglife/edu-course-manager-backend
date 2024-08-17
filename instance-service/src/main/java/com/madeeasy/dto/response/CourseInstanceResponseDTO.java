package com.madeeasy.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseInstanceResponseDTO {

    private Long id;
    private int year;
    private int semester;
    private Long courseId;
}
