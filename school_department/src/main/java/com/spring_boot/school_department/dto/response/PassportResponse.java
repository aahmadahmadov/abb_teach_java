package com.spring_boot.school_department.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PassportResponse(
        Long id,
        @JsonProperty("serial_number") String serialNumber,
        @JsonProperty("issue_date") LocalDate issueDate,
        String country,

        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
