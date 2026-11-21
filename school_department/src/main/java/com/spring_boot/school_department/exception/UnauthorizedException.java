package com.spring_boot.school_department.exception;

import com.spring_boot.school_department.enums.ErrorCode;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
