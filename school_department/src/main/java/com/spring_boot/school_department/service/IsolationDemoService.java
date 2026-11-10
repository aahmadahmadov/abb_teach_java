package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.response.EmployeeResponse;
import com.spring_boot.school_department.dto.response.SalaryReportResponse;
import com.spring_boot.school_department.dto.response.StudentResponse;
import com.spring_boot.school_department.entity.Course;
import com.spring_boot.school_department.entity.Employee;
import com.spring_boot.school_department.entity.Student;
import com.spring_boot.school_department.mapper.EmployeeMapper;
import com.spring_boot.school_department.mapper.StudentMapper;
import com.spring_boot.school_department.repository.CourseRepository;
import com.spring_boot.school_department.repository.EmployeeRepository;
import com.spring_boot.school_department.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class IsolationDemoService {

    private final EmployeeRepository employeeRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    private final EnrollmentService enrollmentService;

    private final EmployeeMapper employeeMapper;
    private final StudentMapper studentMapper;

    // non-repeatable read

    @Transactional(isolation = Isolation.READ_COMMITTED, readOnly = true)
    public SalaryReportResponse salaryReportReadCommitted(Long departmentId, long delayMs) {
        return salaryReport(departmentId, delayMs, Isolation.READ_COMMITTED);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public SalaryReportResponse salaryReportRepeatableRead(Long departmentId, long delayMs) {
        return salaryReport(departmentId, delayMs, Isolation.REPEATABLE_READ);
    }

    // lost update

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public EmployeeResponse raiseReadCommitted(Long employeeId, BigDecimal amount, long delayMs) {
        return raise(employeeId, amount, delayMs);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.REPEATABLE_READ)
    public EmployeeResponse raiseRepeatableRead(Long employeeId, BigDecimal amount, long delayMs) {
        return raise(employeeId, amount, delayMs);
    }

    // write skew

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.REPEATABLE_READ)
    public StudentResponse enrollRepeatableRead(Long courseId, Long studentId, long delayMs) {
        return enroll(courseId, studentId, delayMs);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE)
    public StudentResponse enrollSerializable(Long courseId, Long studentId, long delayMs) {
        return enroll(courseId, studentId, delayMs);
    }

    // eger entity ile hesablansaydi her ikis sorguda da first level cacheden goturduyu ucun db-deki real deyisikliyi
    // gormeyecekdi ona gore birbasa method ile hesabliyib reqem aliriq entity yox
    private SalaryReportResponse salaryReport(Long departmentId, long delayMs, Isolation isolation) {

        BigDecimal firstAvg = employeeRepository.findAverageSalaryByDepartmentId(departmentId);

        sleep(delayMs);

        BigDecimal secondAvg = employeeRepository.findAverageSalaryByDepartmentId(departmentId);

        return new SalaryReportResponse(firstAvg, secondAvg, isolation.name());
    }

    private EmployeeResponse raise(Long employeeId, BigDecimal amount, long delayMs) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + employeeId));

        sleep(delayMs);

        // save cagirmiriq, commit zamani dirty checking ozu update edir. BaseEntityde @Version oldugu ucun
        // read committed-de de update itmir ikinci sorgu 409 alir, @Version lost update olamsin deye evvelden elave etmisdim
        // (@Version evvelceden elave etmisdim tekrar buna gore deyisdirmedim)
        employee.setSalary(employee.getSalary().add(amount));

        return employeeMapper.toResponse(employee);
    }

    private StudentResponse enroll(Long courseId, Long studentId, long delayMs) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found: " + studentId));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found: " + courseId));

        // bosh yeri check edib sonra sleep ile her ikisi eyni veziyyeti gorub write skew anomaliyasini yaradiriq
        enrollmentService.reserveSeat(courseId);

        sleep(delayMs);

        student.addCourse(course);

        return studentMapper.toDetailResponse(student);
    }

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
