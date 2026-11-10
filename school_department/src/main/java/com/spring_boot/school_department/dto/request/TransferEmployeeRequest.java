package com.spring_boot.school_department.dto.request;

public record TransferEmployeeRequest(
        Long departmentId,
        Integer raisePercent
) {
}
