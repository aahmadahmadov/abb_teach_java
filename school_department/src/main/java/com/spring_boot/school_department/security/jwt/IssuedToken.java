package com.spring_boot.school_department.security.jwt;

import com.spring_boot.school_department.enums.TokenType;

import java.time.Instant;

public record IssuedToken(
        String value,
        String jti,
        TokenType type,
        Instant issuedAt,
        Instant expiresAt
) {
}
