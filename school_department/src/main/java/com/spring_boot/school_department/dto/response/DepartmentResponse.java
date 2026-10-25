package com.spring_boot.school_department.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record DepartmentResponse(
        Long id,
        String name,
        String location,
        String phone,

        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt,

        List<EmployeeResponse> employees
) {
}
