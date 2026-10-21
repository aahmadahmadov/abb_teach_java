package com.spring_boot.jpa_practice_1.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
public enum GenderTypeEnum {
    MALE(1, "Qadın"),
    FEMALE(2, "Kişi");

    private final Integer code;
    private final String label;

}
