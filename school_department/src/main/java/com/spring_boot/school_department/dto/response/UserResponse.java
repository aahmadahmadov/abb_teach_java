package com.spring_boot.school_department.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.spring_boot.school_department.entity.Passport;
import com.spring_boot.school_department.enums.GenderEnum;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String surname,
        String email,
        GenderEnum gender,

        PassportResponse passport,

        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
