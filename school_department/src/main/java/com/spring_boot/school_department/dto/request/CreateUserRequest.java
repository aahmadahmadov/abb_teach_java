package com.spring_boot.school_department.dto.request;

import com.spring_boot.school_department.enums.GenderEnum;

public record CreateUserRequest(
        String name,
        String surname,
        String email,
        GenderEnum gender
) {
}
