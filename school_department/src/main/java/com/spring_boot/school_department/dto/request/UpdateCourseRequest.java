package com.spring_boot.school_department.dto.request;

public record UpdateCourseRequest(
        String title,
        Integer credit,
        String teacher
) {
}
