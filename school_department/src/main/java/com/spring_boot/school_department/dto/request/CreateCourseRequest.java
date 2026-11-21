package com.spring_boot.school_department.dto.request;

import com.spring_boot.school_department.enums.ValidationMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
        @NotBlank(message = ValidationMessage.Keys.REQUIRED)
        @Size(max = 100, message = ValidationMessage.Keys.SIZE_MAX)
        String title,

        @NotNull(message = ValidationMessage.Keys.REQUIRED)
        @Positive(message = ValidationMessage.Keys.POSITIVE)
        Integer credit,

        @NotBlank(message = ValidationMessage.Keys.REQUIRED)
        @Size(max = 50, message = ValidationMessage.Keys.SIZE_MAX)
        String teacher
) {
}
