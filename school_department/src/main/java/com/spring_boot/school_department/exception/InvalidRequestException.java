package com.spring_boot.school_department.exception;

import com.spring_boot.school_department.enums.ErrorCode;

public class InvalidRequestException extends BaseException {

    public InvalidRequestException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
