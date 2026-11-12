package com.spring_boot.school_department.validation;

import com.spring_boot.school_department.enums.ValidationMessage;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AzPhoneNumberValidator.class)
public @interface AzPhoneNumber {

    String message() default ValidationMessage.Keys.AZ_PHONE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
