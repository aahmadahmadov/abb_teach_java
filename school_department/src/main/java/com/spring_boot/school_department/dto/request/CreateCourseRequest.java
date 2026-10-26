package com.spring_boot.school_department.dto.request;

public record CreateCourseRequest(
        String title,
        Integer credit,
        String teacher
) {
}
