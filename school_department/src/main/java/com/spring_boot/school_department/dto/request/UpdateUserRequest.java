package com.spring_boot.school_department.dto.request;

public record UpdateUserRequest(
        String name,
        String surname,
        String email
) {
}
