package com.spring_boot.school_department.dto.request;

import com.spring_boot.school_department.enums.ValidationMessage;
import com.spring_boot.school_department.validation.AzPhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
        @NotBlank(message = ValidationMessage.Keys.REQUIRED)
        @Size(max = 50, message = ValidationMessage.Keys.SIZE_MAX)
        String name,

        @Size(max = 100, message = ValidationMessage.Keys.SIZE_MAX)
        String location,

        @AzPhoneNumber
        String phone
) {
}
