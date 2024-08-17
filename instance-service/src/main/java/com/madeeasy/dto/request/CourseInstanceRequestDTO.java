package com.madeeasy.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseInstanceRequestDTO {

    private int year;
    private int semester;
    private Long courseId;
}
