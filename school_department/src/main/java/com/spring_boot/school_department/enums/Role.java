package com.spring_boot.school_department.enums;

import com.spring_boot.school_department.exception.BusinessException;

import java.util.Arrays;

public enum Role {

    ADMIN(1),
    MANAGER(2),
    USER(3);

    private final Integer dbCode;

    Role(Integer dbCode) {
        this.dbCode = dbCode;
    }

    public Integer dbCode() {
        return dbCode;
    }

    public static Role findByDbCode(Integer dbCode) {
        return Arrays.stream(Role.values())
                .filter(role -> role.dbCode().equals(dbCode))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
    }

}
