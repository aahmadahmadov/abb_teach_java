package com.spring_boot.school_department.service;

import com.spring_boot.school_department.entity.Employee;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class SalaryService {

    private static final BigDecimal MAX_SALARY = new BigDecimal("10000");

    // transfer-in transaction qoşulur, burda exception atılsa departament deyisikliyide rollback olur
    @Transactional(propagation = Propagation.REQUIRED)
    public void raise(Employee employee, Integer percent) {

        BigDecimal newSalary = employee.getSalary()
                .multiply(BigDecimal.valueOf(100 + percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (newSalary.compareTo(MAX_SALARY) > 0) {
            throw new BusinessException(ErrorCode.EMPLOYEE_SALARY_LIMIT, newSalary, MAX_SALARY);
        }

        employee.setSalary(newSalary);
    }
}
