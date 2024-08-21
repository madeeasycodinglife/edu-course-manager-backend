package com.madeeasy.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Include only non-null properties
public class CourseInstanceResponseDTO implements Serializable {

    private Long id;
    private Integer year;
    private Integer semester;
    private Long courseId;
    private String message;
    private HttpStatus status;
}
