package com.spring_boot.spring_practice_07_10.dto.response;

public record UserInfoResponse(
        Long id, String name, String email
) {
}
