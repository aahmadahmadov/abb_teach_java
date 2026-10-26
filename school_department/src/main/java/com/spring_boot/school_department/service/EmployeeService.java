package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.request.CreateEmployeeRequest;
import com.spring_boot.school_department.dto.request.UpdateEmployeeRequest;
import com.spring_boot.school_department.dto.response.EmployeeResponse;
import com.spring_boot.school_department.entity.Department;
import com.spring_boot.school_department.entity.Employee;
import com.spring_boot.school_department.mapper.EmployeeMapper;
import com.spring_boot.school_department.repository.DepartmentRepository;
import com.spring_boot.school_department.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    private final EmployeeMapper employeeMapper;

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        Department department = findDepartment(request.departmentId());

        if (employeeRepository.existsByNameAndSurnameAndDepartmentId(request.name(), request.surname(), request.departmentId())) {
            throw new IllegalStateException("Employee with the same name and surname already exists in this department.");
        }

        Employee employee = employeeMapper.toEntity(request);
        department.addEmployee(employee);

        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    public List<EmployeeResponse> getEmployeesBySalaryRange(BigDecimal min, BigDecimal max) {

        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("min must not be greater than max");
        }

        return employeeMapper.toResponseList(employeeRepository.findAllBySalaryBetween(min, max));
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request) {

        Employee employee = findEmployee(id);
        Department department = findDepartment(request.departmentId());

        if (employeeRepository.existsByNameAndSurnameAndDepartmentIdAndIdNot(
                request.name(), request.surname(), department.getId(), id)) {
            throw new IllegalStateException("Employee already exists in this department");
        }

        employeeMapper.updateEntity(request, employee);
        employee.setDepartment(department);

        return employeeMapper.toResponse(employee);
    }

    public BigDecimal getAverageSalaryByDepartment(Long departmentId) {
        BigDecimal avgSalary = employeeRepository
                .findAverageSalaryByDepartmentId(findDepartment(departmentId).getId());

        return avgSalary != null ? avgSalary : BigDecimal.ZERO;
    }

    @Transactional
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
                .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + id));
    }

    private Department findDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found: " + id));
    }
}
