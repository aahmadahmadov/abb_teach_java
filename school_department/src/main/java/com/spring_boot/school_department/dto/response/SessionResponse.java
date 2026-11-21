package com.spring_boot.school_department.dto.response;

import java.time.Instant;

public record SessionResponse(
        String sessionId,
        Instant loginAt,
        Instant expiresAt,
        boolean current
) {
}
