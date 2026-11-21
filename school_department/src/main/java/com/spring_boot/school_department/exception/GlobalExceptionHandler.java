package com.spring_boot.school_department.exception;

import com.spring_boot.school_department.dto.response.ErrorResponse;
import com.spring_boot.school_department.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    // Base exceptiondan extend eden butun exceptionlar hamisi burdan kecir
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBase(BaseException ex, HttpServletRequest request) {
        return build(request, ex, ex.getErrorCode(), null, ex.getArgs());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        // mesaj validator terefinden artiq secilmis dile cevrilib gelir
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        return build(request, ex, ErrorCode.VALIDATION_ERROR, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(request, ex, ErrorCode.MALFORMED_REQUEST, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(request, ex, ErrorCode.TYPE_MISMATCH, null, ex.getName(), ex.getValue());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(request, ex, ErrorCode.MISSING_PARAMETER, null, ex.getParameterName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(request, ex, ErrorCode.METHOD_NOT_SUPPORTED, null, ex.getMethod());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(request, ex, ErrorCode.URL_NOT_FOUND, null, request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return build(request, ex, ErrorCode.DATA_INTEGRITY_VIOLATION, null);
    }

    // isolation demo-da @Version ve serializable konfliktini tutur
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrency(ConcurrencyFailureException ex, HttpServletRequest request) {
        return build(request, ex, ErrorCode.CONCURRENT_UPDATE, null);
    }

    // MANDATORY metod aciq transaction olmadan cagrilanda tutur
    @ExceptionHandler(IllegalTransactionStateException.class)
    public ResponseEntity<ErrorResponse> handleTransactionState(IllegalTransactionStateException ex, HttpServletRequest request) {
        return build(request, ex, ErrorCode.TRANSACTION_REQUIRED, null);
    }

    // detallari yalniz loga yaziriq, cavaba yox
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex, HttpServletRequest request) {
        return build(request, ex, ErrorCode.INTERNAL_ERROR, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpServletRequest request, Exception ex,
                                                ErrorCode errorCode, Map<String, String> errors,
                                                Object... args) {

        HttpStatus status = errorCode.getStatus();
        logError(request, ex, errorCode, errors, args);

        String key = errorCode.getMessageKey();
        String message = messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());

        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(status.value(), errorCode.getCode(), message, errors != null ? errors : Map.of(), MDC.get("traceId"), LocalDateTime.now()));
    }

    // 4xx WARN ile stack trace-siz, 5xx ERROR ile stack trace ile
    private void logError(HttpServletRequest request,
                          Exception ex,
                          ErrorCode errorCode,
                          Map<String, String> errors,
                          Object[] args) {

        String text = "Request failed: " + request.getMethod() + " " + request.getRequestURI()
                + " -> " + errorCode.getStatus().value() + " " + errorCode.getCode()
                + " (" + errorCode.getMessageKey() + ") [" + ex.getClass().getSimpleName() + "]"
                + " args=" + Arrays.toString(args)
                + (errors != null ? " errors=" + errors : "");

        if (errorCode.getStatus().is5xxServerError()) {
            log.error(text, ex);
        } else {
            log.warn(text);
        }
    }
}
