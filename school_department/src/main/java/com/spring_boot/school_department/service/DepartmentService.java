package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.request.CreateDepartmentRequest;
import com.spring_boot.school_department.dto.request.UpdateDepartmentRequest;
import com.spring_boot.school_department.dto.response.DepartmentResponse;
import com.spring_boot.school_department.entity.Department;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.exception.AlreadyExistsException;
import com.spring_boot.school_department.exception.BusinessException;
import com.spring_boot.school_department.exception.NotFoundException;
import com.spring_boot.school_department.mapper.DepartmentMapper;
import com.spring_boot.school_department.repository.DepartmentRepository;
import com.spring_boot.school_department.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    private final DepartmentMapper departmentMapper;

    @Transactional(propagation = Propagation.REQUIRED)
    public DepartmentResponse create(CreateDepartmentRequest request) {

        if (departmentRepository.existsByName(request.name())) {
            throw new AlreadyExistsException(ErrorCode.DEPARTMENT_ALREADY_EXISTS, request.name());
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
                .orElseThrow(() -> new NotFoundException(ErrorCode.DEPARTMENT_NOT_FOUND, id));

        return departmentMapper.toDetailResponse(department);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public DepartmentResponse update(Long id, UpdateDepartmentRequest request) {
        Department department = findDepartment(id);

        if (departmentRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new AlreadyExistsException(ErrorCode.DEPARTMENT_NAME_USED, request.name());
        }

        departmentMapper.updateEntity(request, department);
        return departmentMapper.toResponse(department);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(Long id) {
        Department department = findDepartment(id);

        if (employeeRepository.existsByDepartmentId(department.getId())) {
            throw new BusinessException(ErrorCode.DEPARTMENT_HAS_EMPLOYEES, id);
        }

        departmentRepository.delete(department);
    }

    private Department findDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.DEPARTMENT_NOT_FOUND, id));
    }

}
