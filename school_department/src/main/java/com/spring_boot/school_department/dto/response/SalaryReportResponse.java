package com.spring_boot.school_department.dto.response;

import java.math.BigDecimal;

public record SalaryReportResponse(
        BigDecimal firstAvg,
        BigDecimal secondAvg,
        String isolation
) {
}
