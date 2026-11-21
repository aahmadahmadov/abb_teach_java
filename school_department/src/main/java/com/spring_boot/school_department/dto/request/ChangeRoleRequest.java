package com.spring_boot.school_department.dto.request;

import com.spring_boot.school_department.enums.Role;
import com.spring_boot.school_department.enums.ValidationMessage;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull(message = ValidationMessage.Keys.REQUIRED)
        Role role
) {
}
