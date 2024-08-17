package com.madeeasy.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseRequestDTO {

    private String title;
    private String courseCode;
    private String description;
}
