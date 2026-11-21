package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.response.EmployeeResponse;
import com.spring_boot.school_department.dto.response.SalaryReportResponse;
import com.spring_boot.school_department.dto.response.StudentResponse;
import com.spring_boot.school_department.entity.Course;
import com.spring_boot.school_department.entity.Employee;
import com.spring_boot.school_department.entity.Student;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.exception.NotFoundException;
import com.spring_boot.school_department.mapper.EmployeeMapper;
import com.spring_boot.school_department.mapper.StudentMapper;
import com.spring_boot.school_department.repository.CourseRepository;
import com.spring_boot.school_department.repository.EmployeeRepository;
import com.spring_boot.school_department.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IsolationDemoServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private StudentMapper studentMapper;

    @InjectMocks
    private IsolationDemoService isolationDemoService;

    @Test
    void salaryReportReadCommitted_averageChanges_returnsBothAverages() {
        when(employeeRepository.findAverageSalaryByDepartmentId(1L))
                .thenReturn(new BigDecimal("1000"), new BigDecimal("1500"));

        SalaryReportResponse result = isolationDemoService.salaryReportReadCommitted(1L, 1);

        assertThat(result.firstAvg()).isEqualByComparingTo("1000");
        assertThat(result.secondAvg()).isEqualByComparingTo("1500");
        assertThat(result.isolation()).isEqualTo("READ_COMMITTED");
    }

    @Test
    void salaryReportRepeatableRead_noDelay_returnsSameAverages() {
        when(employeeRepository.findAverageSalaryByDepartmentId(1L)).thenReturn(new BigDecimal("1000"));

        SalaryReportResponse result = isolationDemoService.salaryReportRepeatableRead(1L, 0);

        assertThat(result.firstAvg()).isEqualByComparingTo("1000");
        assertThat(result.secondAvg()).isEqualByComparingTo("1000");
        assertThat(result.isolation()).isEqualTo("REPEATABLE_READ");
    }

    @Test
    void salaryReportReadCommitted_threadInterrupted_keepsInterruptFlag() {
        when(employeeRepository.findAverageSalaryByDepartmentId(1L)).thenReturn(new BigDecimal("1000"));
        Thread.currentThread().interrupt();

        try {
            SalaryReportResponse result = isolationDemoService.salaryReportReadCommitted(1L, 1000);

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(result.firstAvg()).isEqualByComparingTo("1000");
        } finally {
            // test fail olsa bele flag diger testlere kecmesin deye silirik
            Thread.interrupted();
        }
    }

    @Test
    void raiseReadCommitted_employeeExists_addsAmountToSalary() {
        Employee employee = newEmployee();
        EmployeeResponse response = newEmployeeResponse(new BigDecimal("1200"));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        EmployeeResponse result = isolationDemoService.raiseReadCommitted(1L, new BigDecimal("200"), 0);

        assertThat(result).isEqualTo(response);
        assertThat(employee.getSalary()).isEqualByComparingTo("1200");
    }

    @Test
    void raiseRepeatableRead_employeeExists_addsAmountToSalary() {
        Employee employee = newEmployee();
        EmployeeResponse response = newEmployeeResponse(new BigDecimal("1300"));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        EmployeeResponse result = isolationDemoService.raiseRepeatableRead(1L, new BigDecimal("300"), 0);

        assertThat(result).isEqualTo(response);
        assertThat(employee.getSalary()).isEqualByComparingTo("1300");
    }

    @Test
    void raiseReadCommitted_employeeNotFound_throwsNotFound() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> isolationDemoService.raiseReadCommitted(1L, new BigDecimal("200"), 0))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);

        verify(employeeMapper, never()).toResponse(any());
    }

    @Test
    void enrollRepeatableRead_studentAndCourseExist_addsCourseToStudent() {
        Student student = new Student();
        Course course = new Course();
        StudentResponse response = newStudentResponse();
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(studentMapper.toDetailResponse(student)).thenReturn(response);

        StudentResponse result = isolationDemoService.enrollRepeatableRead(1L, 2L, 0);

        assertThat(result).isEqualTo(response);
        assertThat(student.getCourses()).containsExactly(course);
        assertThat(course.getStudents()).containsExactly(student);
        verify(enrollmentService).reserveSeat(1L);
    }

    @Test
    void enrollSerializable_studentAndCourseExist_addsCourseToStudent() {
        Student student = new Student();
        Course course = new Course();
        StudentResponse response = newStudentResponse();
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(studentMapper.toDetailResponse(student)).thenReturn(response);

        StudentResponse result = isolationDemoService.enrollSerializable(1L, 2L, 0);

        assertThat(result).isEqualTo(response);
        assertThat(student.getCourses()).containsExactly(course);
        verify(enrollmentService).reserveSeat(1L);
    }

    @Test
    void enrollRepeatableRead_studentNotFound_throwsNotFound() {
        when(studentRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> isolationDemoService.enrollRepeatableRead(1L, 2L, 0))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDENT_NOT_FOUND);

        verify(courseRepository, never()).findById(any());
    }

    @Test
    void enrollRepeatableRead_courseNotFound_throwsNotFound() {
        when(studentRepository.findById(2L)).thenReturn(Optional.of(new Student()));
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> isolationDemoService.enrollRepeatableRead(1L, 2L, 0))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);

        verify(enrollmentService, never()).reserveSeat(any());
    }

    private Employee newEmployee() {
        Employee employee = new Employee();
        employee.setName("Ali");
        employee.setSurname("Aliyev");
        employee.setSalary(new BigDecimal("1000"));
        return employee;
    }

    private EmployeeResponse newEmployeeResponse(BigDecimal salary) {
        return new EmployeeResponse(1L, "Ali", "Aliyev", salary, null, null, 1L, "IT");
    }

    private StudentResponse newStudentResponse() {
        return new StudentResponse(2L, "Leyla", "Mammadova", 20, null, null, null);
    }
}
