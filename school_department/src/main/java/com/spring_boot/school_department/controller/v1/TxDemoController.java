package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tx-demo")
@RequiredArgsConstructor
public class TxDemoController {

    private final EnrollmentService enrollmentService;

    // burda @Transactional annotation isletmedik ve daxlideki ise mandatory-dir buna gore exception atacaq
    @PostMapping("/reserve-seat/{courseId}")
    @ResponseStatus(HttpStatus.OK)
    public void reserveSeat(@PathVariable Long courseId) {
        enrollmentService.reserveSeat(courseId);
    }
}
