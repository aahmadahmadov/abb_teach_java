package com.spring_boot.school_department.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class AzPhoneNumberValidator implements ConstraintValidator<AzPhoneNumber, String> {

    private static final Pattern AZ_PHONE = Pattern.compile("^\\+994(50|51|55|70|77|99)\\d{7}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (value == null || value.isBlank()) {
            return true;
        }

        return AZ_PHONE.matcher(value.replaceAll("\\s", "")).matches();
    }
}
