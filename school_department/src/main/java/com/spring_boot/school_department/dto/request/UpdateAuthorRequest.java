package com.spring_boot.school_department.dto.request;

public record UpdateAuthorRequest(
        String name,
        String surname,
        String country
) {
}
