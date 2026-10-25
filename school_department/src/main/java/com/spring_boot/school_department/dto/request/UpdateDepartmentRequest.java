package com.spring_boot.school_department.dto.request;

public record UpdateDepartmentRequest(
        String name,
        String location,
        String phone
) {
}
