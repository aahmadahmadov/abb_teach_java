package com.spring_boot.school_department.exception;

import com.spring_boot.school_department.enums.ErrorCode;

public class AlreadyExistsException extends BaseException {

    public AlreadyExistsException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
