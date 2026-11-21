package com.spring_boot.school_department.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long accessExpiresIn,
        long refreshExpiresIn,
        String tokenType
) {
}
