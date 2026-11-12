package com.spring_boot.school_department.exception;

import com.spring_boot.school_department.enums.ErrorCode;

public class BusinessException extends BaseException {

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
