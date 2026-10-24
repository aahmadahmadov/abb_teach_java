package com.spring_boot.school_department.dto.request;

public record CreateUserRequest(
        String name,
        String surname,
        String email,
        Integer gender,
        CreatePassportRequest passport
) {
}
