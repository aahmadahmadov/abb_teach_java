package com.spring_boot.school_department.service;

import com.spring_boot.school_department.entity.Employee;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SalaryServiceTest {

    private final SalaryService salaryService = new SalaryService();

    @Test
    void raise_newSalaryUnderLimit_updatesSalary() {
        Employee employee = createEmployee(new BigDecimal("1000"));

        salaryService.raise(employee, 10);

        assertThat(employee.getSalary()).isEqualByComparingTo("1100.00");
    }

    @Test
    void raise_newSalaryEqualsLimit_updatesSalary() {
        Employee employee = createEmployee(new BigDecimal("5000"));

        salaryService.raise(employee, 100);

        assertThat(employee.getSalary()).isEqualByComparingTo("10000.00");
    }

    @Test
    void raise_newSalaryOverLimit_throwsBusiness() {
        Employee employee = createEmployee(new BigDecimal("9500"));

        assertThatThrownBy(() -> salaryService.raise(employee, 10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPLOYEE_SALARY_LIMIT);

        assertThat(employee.getSalary()).isEqualByComparingTo("9500");
    }

    private Employee createEmployee(BigDecimal salary) {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setSalary(salary);
        return employee;
    }
}
