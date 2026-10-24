package com.spring_boot.school_department.dto.request;

import java.time.LocalDate;

public record CreatePassportRequest(
        String serialNumber,
        LocalDate issueDate,
        String country
) {
}
