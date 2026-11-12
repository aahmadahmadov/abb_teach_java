package com.spring_boot.school_department.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== General: 1000-1099 =====
    VALIDATION_ERROR("ERR-1000", "error.validation", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("ERR-1001", "error.internal", HttpStatus.INTERNAL_SERVER_ERROR),
    MALFORMED_REQUEST("ERR-1002", "error.request.malformed", HttpStatus.BAD_REQUEST),
    TYPE_MISMATCH("ERR-1003", "error.request.typeMismatch", HttpStatus.BAD_REQUEST),
    MISSING_PARAMETER("ERR-1004", "error.request.missingParam", HttpStatus.BAD_REQUEST),
    METHOD_NOT_SUPPORTED("ERR-1005", "error.request.methodNotSupported", HttpStatus.METHOD_NOT_ALLOWED),
    URL_NOT_FOUND("ERR-1006", "error.request.urlNotFound", HttpStatus.NOT_FOUND),
    DATA_INTEGRITY_VIOLATION("ERR-1007", "error.data.integrity", HttpStatus.CONFLICT),
    CONCURRENT_UPDATE("ERR-1008", "error.data.concurrentUpdate", HttpStatus.CONFLICT),
    TRANSACTION_REQUIRED("ERR-1009", "error.transaction.required", HttpStatus.CONFLICT),
    UNSUPPORTED_ISOLATION("ERR-1010", "error.request.unsupportedIsolation", HttpStatus.BAD_REQUEST),
    CACHE_REGION_NOT_FOUND("ERR-1011", "error.cache.regionNotFound", HttpStatus.NOT_FOUND),

    // ===== Student: 2000-2099 =====
    STUDENT_NOT_FOUND("ERR-2000", "error.student.notFound", HttpStatus.NOT_FOUND),
    STUDENT_ALREADY_ENROLLED("ERR-2001", "error.student.alreadyEnrolled", HttpStatus.CONFLICT),
    STUDENT_NOT_ENROLLED("ERR-2002", "error.student.notEnrolled", HttpStatus.CONFLICT),

    // ===== Course: 2100-2199 =====
    COURSE_NOT_FOUND("ERR-2100", "error.course.notFound", HttpStatus.NOT_FOUND),
    COURSE_ALREADY_EXISTS("ERR-2101", "error.course.alreadyExists", HttpStatus.CONFLICT),
    COURSE_TITLE_USED("ERR-2102", "error.course.titleUsed", HttpStatus.CONFLICT),
    COURSE_FULL("ERR-2103", "error.course.full", HttpStatus.CONFLICT),

    // ===== Department: 2200-2299 =====
    DEPARTMENT_NOT_FOUND("ERR-2200", "error.department.notFound", HttpStatus.NOT_FOUND),
    DEPARTMENT_ALREADY_EXISTS("ERR-2201", "error.department.alreadyExists", HttpStatus.CONFLICT),
    DEPARTMENT_NAME_USED("ERR-2202", "error.department.nameUsed", HttpStatus.CONFLICT),
    DEPARTMENT_HAS_EMPLOYEES("ERR-2203", "error.department.hasEmployees", HttpStatus.CONFLICT),

    // ===== Employee: 2300-2399 =====
    EMPLOYEE_NOT_FOUND("ERR-2300", "error.employee.notFound", HttpStatus.NOT_FOUND),
    EMPLOYEE_ALREADY_EXISTS("ERR-2301", "error.employee.alreadyExists", HttpStatus.CONFLICT),
    EMPLOYEE_SALARY_RANGE("ERR-2302", "error.employee.salaryRange", HttpStatus.BAD_REQUEST),
    EMPLOYEE_SALARY_LIMIT("ERR-2303", "error.employee.salaryLimit", HttpStatus.BAD_REQUEST),

    // ===== Author: 2400-2499 =====
    AUTHOR_NOT_FOUND("ERR-2400", "error.author.notFound", HttpStatus.NOT_FOUND),
    AUTHOR_ALREADY_EXISTS("ERR-2401", "error.author.alreadyExists", HttpStatus.CONFLICT),

    // ===== Book: 2500-2599 =====
    BOOK_NOT_FOUND("ERR-2500", "error.book.notFound", HttpStatus.NOT_FOUND),
    BOOK_ALREADY_EXISTS("ERR-2501", "error.book.alreadyExists", HttpStatus.CONFLICT),
    BOOK_AUTHOR_ALREADY_LINKED("ERR-2502", "error.book.authorAlreadyLinked", HttpStatus.CONFLICT),
    BOOK_AUTHOR_NOT_LINKED("ERR-2503", "error.book.authorNotLinked", HttpStatus.NOT_FOUND),

    // ===== User / Passport: 2600-2699 =====
    USER_NOT_FOUND("ERR-2600", "error.user.notFound", HttpStatus.NOT_FOUND),
    USER_EMAIL_EXISTS("ERR-2601", "error.user.emailExists", HttpStatus.CONFLICT),
    USER_EMAIL_USED("ERR-2602", "error.user.emailUsed", HttpStatus.CONFLICT),
    USER_PASSPORT_EXISTS("ERR-2603", "error.user.passportExists", HttpStatus.CONFLICT),
    PASSPORT_NOT_FOUND("ERR-2604", "error.passport.notFound", HttpStatus.NOT_FOUND),
    PASSPORT_SERIAL_EXISTS("ERR-2605", "error.passport.serialExists", HttpStatus.CONFLICT);

    private final String code;
    private final String messageKey;
    private final HttpStatus status;
}
