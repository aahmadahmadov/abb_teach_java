package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Transaction demo", description = "Propagation MANDATORY demo")
@RequestMapping("/api/tx-demo")
@RequiredArgsConstructor
public class TxDemoController {

    private final EnrollmentService enrollmentService;

    // burda @Transactional annotation isletmedik ve daxlideki ise mandatory-dir buna gore exception atacaq
    @Operation(summary = "MANDATORY metodu transaction-siz cagir", description = "Aciq transaction olmadigi ucun 409 (ERR-1009) qaytarir")
    @PostMapping("/reserve-seat/{courseId}")
    @ResponseStatus(HttpStatus.OK)
    public void reserveSeat(@PathVariable Long courseId) {
        enrollmentService.reserveSeat(courseId);
    }
}
