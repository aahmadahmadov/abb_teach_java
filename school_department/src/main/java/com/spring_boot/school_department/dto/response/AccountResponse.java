package com.spring_boot.school_department.dto.response;

import com.spring_boot.school_department.enums.Role;

import java.time.Instant;

public record AccountResponse(
        Long id,
        String username,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt
) {
}
