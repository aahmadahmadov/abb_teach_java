package com.spring_boot.jpa_practice_1.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum GenderTypeEnum {
    MALE(1, "Kişi"),
    FEMALE(2, "Qadın");

    private final Integer id;
    private final String label;

    public static GenderTypeEnum fromId(Integer id) {

        for (GenderTypeEnum gender: values()) {

            if (gender.getId().equals(id)) {
                return gender;
            }
        }

        throw new IllegalArgumentException("Unknown gender id: " + id);
    }

}
