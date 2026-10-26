package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.request.CreateDepartmentRequest;
import com.spring_boot.school_department.dto.request.UpdateDepartmentRequest;
import com.spring_boot.school_department.dto.response.DepartmentResponse;
import com.spring_boot.school_department.entity.Department;
import com.spring_boot.school_department.mapper.DepartmentMapper;
import com.spring_boot.school_department.repository.DepartmentRepository;
import com.spring_boot.school_department.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    private final DepartmentMapper departmentMapper;

    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request) {

        if (departmentRepository.existsByName(request.name())) {
            throw new IllegalStateException("Department with name already exists: " + request.name());
        }

        Department department = departmentMapper.toEntity(request);
        departmentRepository.save(department);

        return departmentMapper.toResponse(department);
    }

    public List<DepartmentResponse> getAll() {
        return departmentMapper.toResponseList(departmentRepository.findAllByOrderByNameAsc());
    }

    public List<DepartmentResponse> getWithoutEmployeesDepartments() {
        return departmentMapper.toResponseList(departmentRepository.findDepartmentsWithoutEmployees());
    }

    public DepartmentResponse getById(Long id) {
        Department department = departmentRepository.findByIdWithEmployees(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));

        return departmentMapper.toDetailResponse(department);
    }

    @Transactional
    public DepartmentResponse update(Long id, UpdateDepartmentRequest request) {
        Department department = findDepartment(id);

        if (departmentRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new IllegalStateException("Department with name already exists: " + request.name());
        }

        departmentMapper.updateEntity(request, department);
        return departmentMapper.toResponse(department);
    }

    @Transactional
    public void delete(Long id) {
        Department department = findDepartment(id);

        if (employeeRepository.existsByDepartmentId(department.getId())) {
            throw new IllegalStateException("Cannot delete department with employees: " + id);
        }

        departmentRepository.delete(department);
    }

    private Department findDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));
    }

}
