package com.spring_boot.school_department.enums;

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
        for (GenderEnum gender: GenderEnum.values()) {
            if (gender.dbCode().equals(dbCode)) {
                return gender;
            }
        }

        throw new IllegalArgumentException("Invalid dbCode: " + dbCode);
    }

}
