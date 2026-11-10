package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.response.EmployeeResponse;
import com.spring_boot.school_department.dto.response.SalaryReportResponse;
import com.spring_boot.school_department.dto.response.StudentResponse;
import com.spring_boot.school_department.service.IsolationDemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/isolation")
@RequiredArgsConstructor
public class IsolationDemoController {

    private final IsolationDemoService isolationDemoService;

    @GetMapping("/departments/{id}/salary-report")
    @ResponseStatus(HttpStatus.OK)
    public SalaryReportResponse salaryReport(@PathVariable Long id,
                                             @RequestParam Isolation isolation,
                                             @RequestParam(defaultValue = "0") long delayMs) {

        return switch (isolation) {
            case READ_COMMITTED -> isolationDemoService.salaryReportReadCommitted(id, delayMs);
            case REPEATABLE_READ -> isolationDemoService.salaryReportRepeatableRead(id, delayMs);
            default -> throw new IllegalArgumentException("Supported isolation: READ_COMMITTED, REPEATABLE_READ");
        };
    }

    @PostMapping("/employees/{id}/raise")
    @ResponseStatus(HttpStatus.OK)
    public EmployeeResponse raise(@PathVariable Long id,
                                  @RequestParam BigDecimal amount,
                                  @RequestParam Isolation isolation,
                                  @RequestParam(defaultValue = "0") long delayMs) {

        return switch (isolation) {
            case READ_COMMITTED -> isolationDemoService.raiseReadCommitted(id, amount, delayMs);
            case REPEATABLE_READ -> isolationDemoService.raiseRepeatableRead(id, amount, delayMs);
            default -> throw new IllegalArgumentException("Supported isolation: READ_COMMITTED, REPEATABLE_READ");
        };
    }

    @PostMapping("/courses/{courseId}/enroll/{studentId}")
    @ResponseStatus(HttpStatus.OK)
    public StudentResponse enroll(@PathVariable Long courseId,
                                  @PathVariable Long studentId,
                                  @RequestParam Isolation isolation,
                                  @RequestParam(defaultValue = "0") long delayMs) {

        return switch (isolation) {
            case REPEATABLE_READ -> isolationDemoService.enrollRepeatableRead(courseId, studentId, delayMs);
            case SERIALIZABLE -> isolationDemoService.enrollSerializable(courseId, studentId, delayMs);
            default -> throw new IllegalArgumentException("Supported isolation: REPEATABLE_READ, SERIALIZABLE");
        };
    }
}
