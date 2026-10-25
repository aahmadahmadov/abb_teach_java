package com.spring_boot.school_department.dto.request;

public record CreateDepartmentRequest(
        String name,
        String location,
        String phone
) {
}
