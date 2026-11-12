package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.response.EmployeeResponse;
import com.spring_boot.school_department.dto.response.SalaryReportResponse;
import com.spring_boot.school_department.dto.response.StudentResponse;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.exception.InvalidRequestException;
import com.spring_boot.school_department.service.IsolationDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@Tag(name = "Isolation demo", description = "Isolation seviyyeleri ucun demo endpoint-ler")
@RequestMapping("/api/isolation")
@RequiredArgsConstructor
public class IsolationDemoController {

    private final IsolationDemoService isolationDemoService;

    @Operation(summary = "Non-repeatable read demo", description = "READ_COMMITTED ve REPEATABLE_READ. Diger isolation-da 400 (ERR-1010), departament tapilmasa 404 (ERR-2200)")
    @GetMapping("/departments/{id}/salary-report")
    @ResponseStatus(HttpStatus.OK)
    public SalaryReportResponse salaryReport(@PathVariable Long id,
                                             @RequestParam Isolation isolation,
                                             @RequestParam(defaultValue = "0") long delayMs) {

        return switch (isolation) {
            case READ_COMMITTED -> isolationDemoService.salaryReportReadCommitted(id, delayMs);
            case REPEATABLE_READ -> isolationDemoService.salaryReportRepeatableRead(id, delayMs);
            default -> throw new InvalidRequestException(ErrorCode.UNSUPPORTED_ISOLATION, isolation);
        };
    }

    @Operation(summary = "Lost update demo", description = "READ_COMMITTED ve REPEATABLE_READ. Paralel update-de 409 (ERR-1008), isci tapilmasa 404 (ERR-2300)")
    @PostMapping("/employees/{id}/raise")
    @ResponseStatus(HttpStatus.OK)
    public EmployeeResponse raise(@PathVariable Long id,
                                  @RequestParam BigDecimal amount,
                                  @RequestParam Isolation isolation,
                                  @RequestParam(defaultValue = "0") long delayMs) {

        return switch (isolation) {
            case READ_COMMITTED -> isolationDemoService.raiseReadCommitted(id, amount, delayMs);
            case REPEATABLE_READ -> isolationDemoService.raiseRepeatableRead(id, amount, delayMs);
            default -> throw new InvalidRequestException(ErrorCode.UNSUPPORTED_ISOLATION, isolation);
        };
    }

    @Operation(summary = "Write skew demo", description = "REPEATABLE_READ ve SERIALIZABLE. Serializable konfliktde 409 (ERR-1008), kursda yer yoxdursa 409 (ERR-2103)")
    @PostMapping("/courses/{courseId}/enroll/{studentId}")
    @ResponseStatus(HttpStatus.OK)
    public StudentResponse enroll(@PathVariable Long courseId,
                                  @PathVariable Long studentId,
                                  @RequestParam Isolation isolation,
                                  @RequestParam(defaultValue = "0") long delayMs) {

        return switch (isolation) {
            case REPEATABLE_READ -> isolationDemoService.enrollRepeatableRead(courseId, studentId, delayMs);
            case SERIALIZABLE -> isolationDemoService.enrollSerializable(courseId, studentId, delayMs);
            default -> throw new InvalidRequestException(ErrorCode.UNSUPPORTED_ISOLATION, isolation);
        };
    }
}
