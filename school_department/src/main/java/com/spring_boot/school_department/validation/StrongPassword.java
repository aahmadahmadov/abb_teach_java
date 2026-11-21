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
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {

    String message() default ValidationMessage.Keys.PASSWORD_STRONG;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
