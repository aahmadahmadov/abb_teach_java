package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.request.CreateEmployeeRequest;
import com.spring_boot.school_department.dto.request.UpdateEmployeeRequest;
import com.spring_boot.school_department.dto.response.EmployeeResponse;
import com.spring_boot.school_department.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@RequestBody CreateEmployeeRequest request) {
        return employeeService.create(request);
    }

    @GetMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    public EmployeeResponse getById(@PathVariable Long id) {
        return employeeService.getById(id);
    }

    @GetMapping("/department/{departmentId}")
    @ResponseStatus(HttpStatus.OK)
    public List<EmployeeResponse> getDepartmentEmployees(@PathVariable Long departmentId) {
        return employeeService.getDepartmentEmployees(departmentId);
    }

    @GetMapping("/salary-range")
    @ResponseStatus(HttpStatus.OK)
    public List<EmployeeResponse> getEmployeesBySalaryRange(@RequestParam BigDecimal min, @RequestParam BigDecimal max) {
        return employeeService.getEmployeesBySalaryRange(min, max);
    }

    @GetMapping("/department/{departmentId}/average-salary")
    @ResponseStatus(HttpStatus.OK)
    public BigDecimal getAverageSalaryByDepartment(@PathVariable Long departmentId) {
        return employeeService.getAverageSalaryByDepartment(departmentId);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EmployeeResponse updateEmployee(@PathVariable Long id, @RequestBody UpdateEmployeeRequest employee) {
        return employeeService.updateEmployee(id, employee);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable Long id) {
        employeeService.delete(id);
    }
}
