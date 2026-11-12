package com.spring_boot.school_department.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ValidationMessage {

    REQUIRED(Keys.REQUIRED),
    SIZE_MAX(Keys.SIZE_MAX),
    EMAIL(Keys.EMAIL),
    AZ_PHONE(Keys.AZ_PHONE),
    MIN(Keys.MIN),
    MAX(Keys.MAX),
    POSITIVE(Keys.POSITIVE),
    NOT_NEGATIVE(Keys.NOT_NEGATIVE),
    DIGITS(Keys.DIGITS),
    PAST_OR_PRESENT(Keys.PAST_OR_PRESENT),
    SERIAL_NUMBER_PATTERN(Keys.SERIAL_NUMBER_PATTERN);

    private final String key;

    // annotation-in message-i yalniz compile-time sabit qebul edir, ona gore acarlari burda static final saxlayiriq
    public static final class Keys {
        public static final String REQUIRED = "{validation.required}";
        public static final String SIZE_MAX = "{validation.size.max}";
        public static final String EMAIL = "{validation.email}";
        public static final String AZ_PHONE = "{validation.phone.az}";
        public static final String MIN = "{validation.min}";
        public static final String MAX = "{validation.max}";
        public static final String POSITIVE = "{validation.positive}";
        public static final String NOT_NEGATIVE = "{validation.notNegative}";
        public static final String DIGITS = "{validation.digits}";
        public static final String PAST_OR_PRESENT = "{validation.pastOrPresent}";
        public static final String SERIAL_NUMBER_PATTERN = "{validation.serialNumber.pattern}";

        private Keys() {
        }
    }
}
