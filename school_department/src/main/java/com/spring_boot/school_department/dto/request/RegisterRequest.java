package com.spring_boot.school_department.dto.request;

import com.spring_boot.school_department.enums.ValidationMessage;
import com.spring_boot.school_department.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = ValidationMessage.Keys.REQUIRED)
        @Size(min = 3, max = 50, message = ValidationMessage.Keys.SIZE_RANGE)
        @Pattern(regexp = "^[A-Za-z0-9._]+$", message = ValidationMessage.Keys.USERNAME_PATTERN)
        String username,

        @NotBlank(message = ValidationMessage.Keys.REQUIRED)
        @Email(message = ValidationMessage.Keys.EMAIL)
        String email,

        @NotBlank(message = ValidationMessage.Keys.REQUIRED)
        @StrongPassword
        String password
) {

    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", email=" + email + ", password=***]";
    }
}
