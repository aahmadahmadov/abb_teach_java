package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.response.EmployeeResponse;
import com.spring_boot.school_department.entity.Department;
import com.spring_boot.school_department.entity.Employee;
import com.spring_boot.school_department.mapper.EmployeeMapper;
import com.spring_boot.school_department.repository.DepartmentRepository;
import com.spring_boot.school_department.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Getter
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    private final EmployeeMapper employeeMapper;

    @Transactional
    public EmployeeResponse create(Employee employee) {
        //
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
