package com.madeeasy.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseResponseDTO {

    private Long id;
    private String title;
    private String courseCode;
    private String description;
}
