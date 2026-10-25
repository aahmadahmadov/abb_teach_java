package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.request.CreateDepartmentRequest;
import com.spring_boot.school_department.dto.request.UpdateDepartmentRequest;
import com.spring_boot.school_department.dto.response.DepartmentResponse;
import com.spring_boot.school_department.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse create(@RequestBody CreateDepartmentRequest request) {
        return departmentService.create(request);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DepartmentResponse getById(@PathVariable Long id) {
        return departmentService.getById(id);
    }

    @GetMapping("/")
    @ResponseStatus(HttpStatus.OK)
    public List<DepartmentResponse> getAll() {
        return departmentService.getAll();
    }

    @GetMapping("/without-employees")
    @ResponseStatus(HttpStatus.OK)
    public List<DepartmentResponse> getWithoutEmployees() {
        return departmentService.getWithoutEmployeesDepartments();
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DepartmentResponse update(@PathVariable Long id, @RequestBody UpdateDepartmentRequest request) {
        return departmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        departmentService.delete(id);
    }
}
