package com.spring_boot.school_department.dto.request;

import com.spring_boot.school_department.enums.ValidationMessage;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = ValidationMessage.Keys.REQUIRED) String login,
        @NotBlank(message = ValidationMessage.Keys.REQUIRED) String password) {

    @Override
    public String toString() {
        return "LoginRequest[login=" + login + ", password=***]";
    }
}
