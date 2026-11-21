package com.spring_boot.school_department.exception;

import com.spring_boot.school_department.enums.ErrorCode;

public class ForbiddenException extends BaseException {

    public ForbiddenException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
