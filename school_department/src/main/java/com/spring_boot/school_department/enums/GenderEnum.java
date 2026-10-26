package com.spring_boot.school_department.enums;

import java.util.Arrays;

public enum GenderEnum {

    MALE(1),
    FEMALE(2);

    private final Integer dbCode;

    GenderEnum(Integer dbCode) {
        this.dbCode = dbCode;
    }

    public Integer dbCode() {
        return dbCode;
    }

    public static GenderEnum findByDbCode(Integer dbCode) {
        return Arrays.stream(GenderEnum.values())
                .filter(gender -> gender.dbCode().equals(dbCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid dbCode: " + dbCode));
    }

}
