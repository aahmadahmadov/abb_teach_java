package com.spring_boot.school_department.dto.request;

public record UpdateStudentRequest(
        String name,
        String surname,
        Integer age
) {
}
