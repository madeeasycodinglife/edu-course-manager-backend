package com.madeeasy.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Include only non-null properties
public class CourseInstanceResponseDTO {

    private Long id;
    private Integer year;
    private Integer semester;
    private Long courseId;
    private String message;
    private HttpStatus status;
}
