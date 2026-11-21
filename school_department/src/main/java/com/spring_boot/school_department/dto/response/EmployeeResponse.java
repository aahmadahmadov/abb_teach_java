package com.spring_boot.school_department.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        String name,
        String surname,
        BigDecimal salary,

        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt,

        Long departmentId,
        String departmentName
) {
}
