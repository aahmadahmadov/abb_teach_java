package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.request.CreateEmployeeRequest;
import com.spring_boot.school_department.dto.request.TransferEmployeeRequest;
import com.spring_boot.school_department.dto.request.UpdateEmployeeRequest;
import com.spring_boot.school_department.dto.response.EmployeeResponse;
import com.spring_boot.school_department.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@Tag(name = "Employee", description = "Isci emeliyyatlari")
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @Operation(summary = "Isci yarat", description = "Departament tapilmasa 404 (ERR-2200), eyni isci varsa 409 (ERR-2301)")
    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody CreateEmployeeRequest request) {
        return employeeService.create(request);
    }

    @Operation(summary = "Iscini id ile getir", description = "Tapilmasa 404 (ERR-2300)")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EmployeeResponse getById(@PathVariable Long id) {
        return employeeService.getById(id);
    }

    @Operation(summary = "Departamentin iscileri", description = "Departament tapilmasa 404 (ERR-2200)")
    @GetMapping("/department/{departmentId}")
    @ResponseStatus(HttpStatus.OK)
    public List<EmployeeResponse> getDepartmentEmployees(@PathVariable Long departmentId) {
        return employeeService.getDepartmentEmployees(departmentId);
    }

    @Operation(summary = "Maas araligina gore isciler", description = "min > max olsa 400 (ERR-2302), parametr yoxdursa 400 (ERR-1004)")
    @GetMapping("/salary-range")
    @ResponseStatus(HttpStatus.OK)
    public List<EmployeeResponse> getEmployeesBySalaryRange(@RequestParam BigDecimal min, @RequestParam BigDecimal max) {
        return employeeService.getEmployeesBySalaryRange(min, max);
    }

    @Operation(summary = "Departamentin orta maasi", description = "Departament tapilmasa 404 (ERR-2200)")
    @GetMapping("/department/{departmentId}/average-salary")
    @ResponseStatus(HttpStatus.OK)
    public BigDecimal getAverageSalaryByDepartment(@PathVariable Long departmentId) {
        return employeeService.getAverageSalaryByDepartment(departmentId);
    }

    @Operation(summary = "Iscini yenile", description = "Isci tapilmasa 404 (ERR-2300), departament tapilmasa 404 (ERR-2200), eyni isci varsa 409 (ERR-2301)")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EmployeeResponse updateEmployee(@PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest employee) {
        return employeeService.updateEmployee(id, employee);
    }

    @Operation(summary = "Iscini basqa departamente kecir ve maasini artir", description = "Isci tapilmasa 404 (ERR-2300), departament tapilmasa 404 (ERR-2200), maas limiti kecse 400 (ERR-2303)")
    @PostMapping("/{id}/transfer")
    @ResponseStatus(HttpStatus.OK)
    public EmployeeResponse transfer(@PathVariable Long id, @Valid @RequestBody TransferEmployeeRequest request) {
        return employeeService.transfer(id, request);
    }

    @Operation(summary = "Iscini sil", description = "Tapilmasa 404 (ERR-2300)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable Long id) {
        employeeService.delete(id);
    }
}
