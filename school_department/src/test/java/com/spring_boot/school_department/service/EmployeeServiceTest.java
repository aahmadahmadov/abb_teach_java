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
import com.spring_boot.school_department.exception.BusinessException;
import com.spring_boot.school_department.exception.InvalidRequestException;
import com.spring_boot.school_department.exception.NotFoundException;
import com.spring_boot.school_department.mapper.EmployeeMapper;
import com.spring_boot.school_department.repository.DepartmentRepository;
import com.spring_boot.school_department.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SalaryService salaryService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void create_validRequest_savesAndReturnsEmployee() {
        CreateEmployeeRequest request = new CreateEmployeeRequest("Ali", "Aliyev", new BigDecimal("1000"), 1L);
        Department department = createDepartment(1L);
        Employee employee = createEmployee(1L);
        EmployeeResponse response = createEmployeeResponse(1L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.existsByNameAndSurnameAndDepartmentId("Ali", "Aliyev", 1L)).thenReturn(false);
        when(employeeMapper.toEntity(request)).thenReturn(employee);
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        EmployeeResponse result = employeeService.create(request);

        assertThat(result).isEqualTo(response);
        assertThat(employee.getDepartment()).isEqualTo(department);
        assertThat(department.getEmployees()).contains(employee);
    }

    @Test
    void create_departmentNotFound_throwsNotFound() {
        CreateEmployeeRequest request = new CreateEmployeeRequest("Ali", "Aliyev", new BigDecimal("1000"), 1L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @Test
    void create_employeeExists_throwsAlreadyExists() {
        CreateEmployeeRequest request = new CreateEmployeeRequest("Ali", "Aliyev", new BigDecimal("1000"), 1L);
        Department department = createDepartment(1L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.existsByNameAndSurnameAndDepartmentId("Ali", "Aliyev", 1L)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPLOYEE_ALREADY_EXISTS);

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void getEmployeesBySalaryRange_validRange_returnsList() {
        BigDecimal min = new BigDecimal("500");
        BigDecimal max = new BigDecimal("2000");
        List<Employee> employees = List.of(createEmployee(1L));
        List<EmployeeResponse> responses = List.of(createEmployeeResponse(1L));
        when(employeeRepository.findAllBySalaryBetween(min, max)).thenReturn(employees);
        when(employeeMapper.toResponseList(employees)).thenReturn(responses);

        List<EmployeeResponse> result = employeeService.getEmployeesBySalaryRange(min, max);

        assertThat(result).isEqualTo(responses);
    }

    @Test
    void getEmployeesBySalaryRange_minGreaterThanMax_throwsInvalidRequest() {
        BigDecimal min = new BigDecimal("2000");
        BigDecimal max = new BigDecimal("500");

        assertThatThrownBy(() -> employeeService.getEmployeesBySalaryRange(min, max))
                .isInstanceOf(InvalidRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPLOYEE_SALARY_RANGE);

        verify(employeeRepository, never()).findAllBySalaryBetween(any(), any());
    }

    @Test
    void updateEmployee_validRequest_updatesAndReturnsEmployee() {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest("Veli", "Veliyev", new BigDecimal("1500"), 2L);
        Employee employee = createEmployee(1L);
        Department department = createDepartment(2L);
        EmployeeResponse response = createEmployeeResponse(1L);
        when(employeeRepository.findByIdWithDepartment(1L)).thenReturn(Optional.of(employee));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        when(employeeRepository.existsByNameAndSurnameAndDepartmentIdAndIdNot("Veli", "Veliyev", 2L, 1L)).thenReturn(false);
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        EmployeeResponse result = employeeService.updateEmployee(1L, request);

        assertThat(result).isEqualTo(response);
        assertThat(employee.getDepartment()).isEqualTo(department);
        verify(employeeMapper).updateEntity(request, employee);
    }

    @Test
    void updateEmployee_employeeNotFound_throwsNotFound() {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest("Veli", "Veliyev", new BigDecimal("1500"), 2L);
        when(employeeRepository.findByIdWithDepartment(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.updateEmployee(1L, request))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    @Test
    void updateEmployee_employeeExists_throwsAlreadyExists() {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest("Veli", "Veliyev", new BigDecimal("1500"), 2L);
        Employee employee = createEmployee(1L);
        Department department = createDepartment(2L);
        when(employeeRepository.findByIdWithDepartment(1L)).thenReturn(Optional.of(employee));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        when(employeeRepository.existsByNameAndSurnameAndDepartmentIdAndIdNot("Veli", "Veliyev", 2L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.updateEmployee(1L, request))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPLOYEE_ALREADY_EXISTS);

        verify(employeeMapper, never()).updateEntity(any(), any());
    }

    @Test
    void transfer_validRequest_movesEmployeeAndLogsSuccess() {
        TransferEmployeeRequest request = new TransferEmployeeRequest(2L, 10);
        Employee employee = createEmployee(1L);
        Department department = createDepartment(2L);
        EmployeeResponse response = createEmployeeResponse(1L);
        when(employeeRepository.findByIdWithDepartment(1L)).thenReturn(Optional.of(employee));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        EmployeeResponse result = employeeService.transfer(1L, request);

        assertThat(result).isEqualTo(response);
        assertThat(employee.getDepartment()).isEqualTo(department);
        verify(salaryService).raise(employee, 10);
        verify(auditLogService).log("TRANSFER", "Employee", 1L, AuditStatus.SUCCESS, "Transferred to department 2");
    }

    @Test
    void transfer_salaryOverLimit_logsFailedAndThrows() {
        TransferEmployeeRequest request = new TransferEmployeeRequest(2L, 50);
        Employee employee = createEmployee(1L);
        Department department = createDepartment(2L);
        when(employeeRepository.findByIdWithDepartment(1L)).thenReturn(Optional.of(employee));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        doThrow(new BusinessException(ErrorCode.EMPLOYEE_SALARY_LIMIT, "15000", "10000"))
                .when(salaryService).raise(employee, 50);

        assertThatThrownBy(() -> employeeService.transfer(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPLOYEE_SALARY_LIMIT);

        verify(auditLogService).log("TRANSFER", "Employee", 1L, AuditStatus.FAILED, ErrorCode.EMPLOYEE_SALARY_LIMIT.getCode() + " " + ErrorCode.EMPLOYEE_SALARY_LIMIT.getMessageKey());
        verify(auditLogService, never()).log(anyString(), anyString(), any(), eq(AuditStatus.SUCCESS), anyString());
    }

    @Test
    void transfer_employeeNotFound_logsFailedAndThrows() {
        TransferEmployeeRequest request = new TransferEmployeeRequest(2L, 10);
        when(employeeRepository.findByIdWithDepartment(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.transfer(1L, request))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);

        verify(auditLogService).log("TRANSFER", "Employee", 1L, AuditStatus.FAILED, ErrorCode.EMPLOYEE_NOT_FOUND.getCode() + " " + ErrorCode.EMPLOYEE_NOT_FOUND.getMessageKey());
    }

    @Test
    void getAverageSalaryByDepartment_hasEmployees_returnsAverage() {
        Department department = createDepartment(1L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.findAverageSalaryByDepartmentId(1L)).thenReturn(new BigDecimal("1500"));

        BigDecimal result = employeeService.getAverageSalaryByDepartment(1L);

        assertThat(result).isEqualByComparingTo("1500");
    }

    @Test
    void getAverageSalaryByDepartment_noEmployees_returnsZero() {
        Department department = createDepartment(1L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.findAverageSalaryByDepartmentId(1L)).thenReturn(null);

        BigDecimal result = employeeService.getAverageSalaryByDepartment(1L);

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getAverageSalaryByDepartment_departmentNotFound_throwsNotFound() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getAverageSalaryByDepartment(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @Test
    void delete_employeeExists_deletesEmployee() {
        Employee employee = createEmployee(1L);
        when(employeeRepository.findByIdWithDepartment(1L)).thenReturn(Optional.of(employee));

        employeeService.delete(1L);

        verify(employeeRepository).delete(employee);
    }

    @Test
    void delete_employeeNotFound_throwsNotFound() {
        when(employeeRepository.findByIdWithDepartment(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.delete(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);

        verify(employeeRepository, never()).delete(any());
    }

    @Test
    void getById_employeeExists_returnsEmployee() {
        Employee employee = createEmployee(1L);
        EmployeeResponse response = createEmployeeResponse(1L);
        when(employeeRepository.findByIdWithDepartment(1L)).thenReturn(Optional.of(employee));
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        EmployeeResponse result = employeeService.getById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getById_employeeNotFound_throwsNotFound() {
        when(employeeRepository.findByIdWithDepartment(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getById(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    @Test
    void getDepartmentEmployees_departmentExists_returnsList() {
        Department department = createDepartment(1L);
        List<Employee> employees = List.of(createEmployee(1L), createEmployee(2L));
        List<EmployeeResponse> responses = List.of(createEmployeeResponse(1L), createEmployeeResponse(2L));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.findAllByDepartmentId(1L)).thenReturn(employees);
        when(employeeMapper.toResponseList(employees)).thenReturn(responses);

        List<EmployeeResponse> result = employeeService.getDepartmentEmployees(1L);

        assertThat(result).isEqualTo(responses);
    }

    @Test
    void getDepartmentEmployees_departmentNotFound_throwsNotFound() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getDepartmentEmployees(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);

        verify(employeeRepository, never()).findAllByDepartmentId(any());
    }

    private Employee createEmployee(Long id) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName("Ali");
        employee.setSurname("Aliyev");
        employee.setSalary(new BigDecimal("1000"));
        return employee;
    }

    private Department createDepartment(Long id) {
        Department department = new Department();
        department.setId(id);
        department.setName("IT");
        return department;
    }

    private EmployeeResponse createEmployeeResponse(Long id) {
        return new EmployeeResponse(id, "Ali", "Aliyev", new BigDecimal("1000"), null, null, 1L, "IT");
    }
}
