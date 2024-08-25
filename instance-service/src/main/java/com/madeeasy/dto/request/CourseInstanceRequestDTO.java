package com.madeeasy.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseInstanceRequestDTO {

    @NotNull(message = "year cannot be null")
    @Positive(message = "year must be a positive number")
    private Integer year;

    @NotNull(message = "semester cannot be null")
    @Min(value = 1, message = "semester must be between 1 and 10")
    @Max(value = 10, message = "semester must be between 1 and 10")
    private Integer semester;

    @NotNull(message = "courseId cannot be null")
    @Positive(message = "courseId must be a positive number")
    private Long courseId;
}
