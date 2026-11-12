package com.spring_boot.school_department.exception;

import com.spring_boot.school_department.enums.ErrorCode;

public class NotFoundException extends BaseException {

    public NotFoundException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
