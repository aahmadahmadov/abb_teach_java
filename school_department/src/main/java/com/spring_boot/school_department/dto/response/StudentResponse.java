package com.spring_boot.school_department.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Set;

public record StudentResponse(
        Long id,
        String name,
        String surname,
        Integer age,

        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt,

        Set<CourseResponse> courses
) {
}
