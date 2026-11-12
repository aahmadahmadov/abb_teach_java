package com.spring_boot.school_department.dto.request;

import com.spring_boot.school_department.enums.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record TransferEmployeeRequest(
        @NotNull(message = ValidationMessage.Keys.REQUIRED)
        Long departmentId,

        @NotNull(message = ValidationMessage.Keys.REQUIRED)
        @PositiveOrZero(message = ValidationMessage.Keys.NOT_NEGATIVE)
        Integer raisePercent
) {
}
