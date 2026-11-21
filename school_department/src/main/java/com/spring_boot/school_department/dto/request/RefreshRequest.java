package com.spring_boot.school_department.dto.request;

import com.spring_boot.school_department.enums.ValidationMessage;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = ValidationMessage.Keys.REQUIRED)
        String refreshToken
) {

    @Override
    public String toString() {
        return "RefreshRequest[refreshToken=***]";
    }
}
