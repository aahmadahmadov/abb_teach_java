package com.spring_boot.school_department.dto.request;

import com.spring_boot.school_department.enums.GenderEnum;

public record UpdateUserRequest(
        String name,
        String surname,
        String email,
        GenderEnum gender
) {
}
