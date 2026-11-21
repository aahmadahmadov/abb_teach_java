package com.spring_boot.school_department.dto.request;

import com.spring_boot.school_department.enums.ValidationMessage;
import com.spring_boot.school_department.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = ValidationMessage.Keys.REQUIRED)
        String oldPassword,

        @NotBlank(message = ValidationMessage.Keys.REQUIRED)
        @StrongPassword
        String newPassword
) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[oldPassword=***, newPassword=***]";
    }
}
