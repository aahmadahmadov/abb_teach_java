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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    void create_nameNotExists_savesAndReturnsDepartment() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("IT", "Baku", "+994501234567");
        Department department = createDepartment(1L);
        DepartmentResponse response = createDepartmentResponse(1L);
        when(departmentRepository.existsByName("IT")).thenReturn(false);
        when(departmentMapper.toEntity(request)).thenReturn(department);
        when(departmentMapper.toResponse(department)).thenReturn(response);

        DepartmentResponse result = departmentService.create(request);

        assertThat(result).isEqualTo(response);
        verify(departmentRepository).save(department);
    }

    @Test
    void create_nameExists_throwsAlreadyExists() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("IT", "Baku", "+994501234567");
        when(departmentRepository.existsByName("IT")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.create(request))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPARTMENT_ALREADY_EXISTS);

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void getAll_departmentsExist_returnsList() {
        List<Department> departments = List.of(createDepartment(1L), createDepartment(2L));
        List<DepartmentResponse> responses = List.of(createDepartmentResponse(1L), createDepartmentResponse(2L));
        when(departmentRepository.findAllByOrderByNameAsc()).thenReturn(departments);
        when(departmentMapper.toResponseList(departments)).thenReturn(responses);

        List<DepartmentResponse> result = departmentService.getAll();

        assertThat(result).isEqualTo(responses);
    }

    @Test
    void getWithoutEmployeesDepartments_noDepartments_returnsEmptyList() {
        List<Department> departments = List.of();
        when(departmentRepository.findDepartmentsWithoutEmployees()).thenReturn(departments);
        when(departmentMapper.toResponseList(departments)).thenReturn(List.of());

        List<DepartmentResponse> result = departmentService.getWithoutEmployeesDepartments();

        assertThat(result).isEmpty();
    }

    @Test
    void getById_departmentExists_returnsDetail() {
        Department department = createDepartment(1L);
        DepartmentResponse response = createDepartmentResponse(1L);
        when(departmentRepository.findByIdWithEmployees(1L)).thenReturn(Optional.of(department));
        when(departmentMapper.toDetailResponse(department)).thenReturn(response);

        DepartmentResponse result = departmentService.getById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getById_departmentNotFound_throwsNotFound() {
        when(departmentRepository.findByIdWithEmployees(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getById(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @Test
    void update_validRequest_updatesAndReturnsDepartment() {
        UpdateDepartmentRequest request = new UpdateDepartmentRequest("HR", "Baku", "+994501234567");
        Department department = createDepartment(1L);
        DepartmentResponse response = createDepartmentResponse(1L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.existsByNameAndIdNot("HR", 1L)).thenReturn(false);
        when(departmentMapper.toResponse(department)).thenReturn(response);

        DepartmentResponse result = departmentService.update(1L, request);

        assertThat(result).isEqualTo(response);
        verify(departmentMapper).updateEntity(request, department);
    }

    @Test
    void update_departmentNotFound_throwsNotFound() {
        UpdateDepartmentRequest request = new UpdateDepartmentRequest("HR", "Baku", "+994501234567");
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.update(1L, request))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @Test
    void update_nameUsedByOther_throwsAlreadyExists() {
        UpdateDepartmentRequest request = new UpdateDepartmentRequest("HR", "Baku", "+994501234567");
        Department department = createDepartment(1L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.existsByNameAndIdNot("HR", 1L)).thenReturn(true);

        assertThatThrownBy(() -> departmentService.update(1L, request))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPARTMENT_NAME_USED);

        verify(departmentMapper, never()).updateEntity(any(), any());
    }

    @Test
    void delete_noEmployees_deletesDepartment() {
        Department department = createDepartment(1L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.existsByDepartmentId(1L)).thenReturn(false);

        departmentService.delete(1L);

        verify(departmentRepository).delete(department);
    }

    @Test
    void delete_hasEmployees_throwsBusiness() {
        Department department = createDepartment(1L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.existsByDepartmentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> departmentService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPARTMENT_HAS_EMPLOYEES);

        verify(departmentRepository, never()).delete(any());
    }

    @Test
    void delete_departmentNotFound_throwsNotFound() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.delete(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }

    private Department createDepartment(Long id) {
        Department department = new Department();
        department.setId(id);
        department.setName("IT");
        department.setLocation("Baku");
        department.setPhone("+994501234567");
        return department;
    }

    private DepartmentResponse createDepartmentResponse(Long id) {
        return new DepartmentResponse(id, "IT", "Baku", "+994501234567", null, null, List.of());
    }
}
