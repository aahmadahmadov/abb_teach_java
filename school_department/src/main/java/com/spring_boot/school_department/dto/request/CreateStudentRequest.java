package com.spring_boot.school_department.dto.request;

public record CreateStudentRequest(
        String name,
        String surname,
        Integer age
) {
}
