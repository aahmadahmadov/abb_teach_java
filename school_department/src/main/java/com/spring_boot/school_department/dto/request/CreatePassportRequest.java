package com.spring_boot.school_department.dto.request;

import com.spring_boot.school_department.enums.ValidationMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreatePassportRequest(
        @NotBlank(message = ValidationMessage.Keys.REQUIRED)
        @Pattern(regexp = "^[A-Z]{2,3}[0-9]{7,8}$", message = ValidationMessage.Keys.SERIAL_NUMBER_PATTERN)
        String serialNumber,

        @NotNull(message = ValidationMessage.Keys.REQUIRED)
        @PastOrPresent(message = ValidationMessage.Keys.PAST_OR_PRESENT)
        LocalDate issueDate,

        @NotBlank(message = ValidationMessage.Keys.REQUIRED)
        @Size(max = 50, message = ValidationMessage.Keys.SIZE_MAX)
        String country
) {
}
