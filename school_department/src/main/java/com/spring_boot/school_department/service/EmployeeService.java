package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.request.CreateEmployeeRequest;
import com.spring_boot.school_department.dto.request.TransferEmployeeRequest;
import com.spring_boot.school_department.dto.request.UpdateEmployeeRequest;
import com.spring_boot.school_department.dto.response.EmployeeResponse;
import com.spring_boot.school_department.entity.Department;
import com.spring_boot.school_department.entity.Employee;
import com.spring_boot.school_department.enums.AuditStatus;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.exception.AlreadyExistsException;
import com.spring_boot.school_department.exception.InvalidRequestException;
import com.spring_boot.school_department.exception.NotFoundException;
import com.spring_boot.school_department.mapper.EmployeeMapper;
import com.spring_boot.school_department.repository.DepartmentRepository;
import com.spring_boot.school_department.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    private final SalaryService salaryService;
    private final AuditLogService auditLogService;

    private final EmployeeMapper employeeMapper;

    @Transactional(propagation = Propagation.REQUIRED)
    public EmployeeResponse create(CreateEmployeeRequest request) {
        Department department = findDepartment(request.departmentId());

        if (employeeRepository.existsByNameAndSurnameAndDepartmentId(request.name(), request.surname(), request.departmentId())) {
            throw new AlreadyExistsException(ErrorCode.EMPLOYEE_ALREADY_EXISTS, request.name() + " " + request.surname());
        }

        Employee employee = employeeMapper.toEntity(request);
        department.addEmployee(employee);

        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    public List<EmployeeResponse> getEmployeesBySalaryRange(BigDecimal min, BigDecimal max) {

        if (min.compareTo(max) > 0) {
            throw new InvalidRequestException(ErrorCode.EMPLOYEE_SALARY_RANGE, min, max);
        }

        return employeeMapper.toResponseList(employeeRepository.findAllBySalaryBetween(min, max));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public EmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request) {

        Employee employee = findEmployee(id);
        Department department = findDepartment(request.departmentId());

        if (employeeRepository.existsByNameAndSurnameAndDepartmentIdAndIdNot(
                request.name(), request.surname(), department.getId(), id)) {
            throw new AlreadyExistsException(ErrorCode.EMPLOYEE_ALREADY_EXISTS, request.name() + " " + request.surname());
        }

        employeeMapper.updateEntity(request, employee);
        employee.setDepartment(department);

        return employeeMapper.toResponse(employee);
    }

    // departament ve salary eyni transactiondadir, biri qirilanda ikiside rollback olur.
    // audit ayrı transactionda yazılır, buna gore rollback olsa da qalır
    @Transactional(propagation = Propagation.REQUIRED)
    public EmployeeResponse transfer(Long id, TransferEmployeeRequest request) {
        try {
            Employee employee = findEmployee(id);
            Department department = findDepartment(request.departmentId());

            employee.setDepartment(department);
            salaryService.raise(employee, request.raisePercent());

            auditLogService.log("TRANSFER", "Employee", id, AuditStatus.SUCCESS,
                    "Transferred to department " + department.getId());

            return employeeMapper.toResponse(employee);
        } catch (RuntimeException ex) {
            auditLogService.log("TRANSFER", "Employee", id, AuditStatus.FAILED, ex.getMessage());
            throw ex;
        }
    }

    public BigDecimal getAverageSalaryByDepartment(Long departmentId) {
        BigDecimal avgSalary = employeeRepository
                .findAverageSalaryByDepartmentId(findDepartment(departmentId).getId());

        return avgSalary != null ? avgSalary : BigDecimal.ZERO;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(Long id) {
        employeeRepository.delete(findEmployee(id));
    }

    public EmployeeResponse getById(Long id) {
        return employeeMapper.toResponse(findEmployee(id));
    }

    public List<EmployeeResponse> getDepartmentEmployees(Long departmentId) {
        findDepartment(departmentId);
        return employeeMapper.toResponseList(employeeRepository.findAllByDepartmentId(departmentId));
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findByIdWithDepartment(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.EMPLOYEE_NOT_FOUND, id));
    }

    private Department findDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.DEPARTMENT_NOT_FOUND, id));
    }
}
