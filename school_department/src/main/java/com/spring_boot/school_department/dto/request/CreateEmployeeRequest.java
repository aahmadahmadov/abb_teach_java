package com.spring_boot.school_department.dto.request;

import java.math.BigDecimal;

public record CreateEmployeeRequest(
        String name,
        String surname,
        BigDecimal salary,
        Long departmentId
) {
}
