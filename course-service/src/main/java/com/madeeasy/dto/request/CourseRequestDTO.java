package com.madeeasy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseRequestDTO {

    @NotBlank(message = "title cannot be blank")
    private String title;
    @NotBlank(message = "courseCode cannot be blank")
    @Pattern(regexp = "^[A-Z]{2} \\d+$", message = "courseCode must start with two uppercase letters, followed by a space and a number")
    private String courseCode;
    @NotBlank(message = "description cannot be blank")
    private String description;
}
