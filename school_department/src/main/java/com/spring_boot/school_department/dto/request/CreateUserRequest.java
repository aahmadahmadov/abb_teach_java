package com.spring_boot.school_department.dto.request;

import com.spring_boot.school_department.enums.GenderEnum;
import com.spring_boot.school_department.enums.ValidationMessage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = ValidationMessage.Keys.REQUIRED)
        @Size(max = 50, message = ValidationMessage.Keys.SIZE_MAX)
        String name,

        @NotBlank(message = ValidationMessage.Keys.REQUIRED)
        @Size(max = 50, message = ValidationMessage.Keys.SIZE_MAX)
        String surname,

        @NotBlank(message = ValidationMessage.Keys.REQUIRED)
        @Email(message = ValidationMessage.Keys.EMAIL)
        @Size(max = 100, message = ValidationMessage.Keys.SIZE_MAX)
        String email,

        @NotNull(message = ValidationMessage.Keys.REQUIRED)
        GenderEnum gender
) {
}
