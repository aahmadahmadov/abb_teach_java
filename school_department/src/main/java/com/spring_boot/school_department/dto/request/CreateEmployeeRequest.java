package com.spring_boot.school_department.dto.request;

import com.spring_boot.school_department.enums.ValidationMessage;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateEmployeeRequest(
        @NotBlank(message = ValidationMessage.Keys.REQUIRED)
        @Size(max = 50, message = ValidationMessage.Keys.SIZE_MAX)
        String name,

        @NotBlank(message = ValidationMessage.Keys.REQUIRED)
        @Size(max = 50, message = ValidationMessage.Keys.SIZE_MAX)
        String surname,

        @NotNull(message = ValidationMessage.Keys.REQUIRED)
        @Positive(message = ValidationMessage.Keys.POSITIVE)
        @Digits(integer = 8, fraction = 2, message = ValidationMessage.Keys.DIGITS)
        BigDecimal salary,

        @NotNull(message = ValidationMessage.Keys.REQUIRED)
        Long departmentId
) {
}
