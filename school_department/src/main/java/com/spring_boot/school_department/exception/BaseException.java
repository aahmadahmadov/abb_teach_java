package com.spring_boot.school_department.exception;

import com.spring_boot.school_department.enums.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

@Getter
public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    protected BaseException(ErrorCode errorCode, Object... args) {
        super(errorCode.getCode() + " " + errorCode.getMessageKey());
        this.errorCode = errorCode;

        // long deyerler ucun string convert
        this.args = Arrays.stream(args).map(String::valueOf).toArray();
    }

    public HttpStatus getStatus() {
        return errorCode.getStatus();
    }
}
