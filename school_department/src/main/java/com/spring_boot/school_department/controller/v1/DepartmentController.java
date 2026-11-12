package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.request.CreateDepartmentRequest;
import com.spring_boot.school_department.dto.request.UpdateDepartmentRequest;
import com.spring_boot.school_department.dto.response.DepartmentResponse;
import com.spring_boot.school_department.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Department", description = "Departament emeliyyatlari")
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "Departament yarat", description = "Eyni adda departament varsa 409 (ERR-2201), validasiya xetasinda 400 (ERR-1000)")
    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse create(@Valid @RequestBody CreateDepartmentRequest request) {
        return departmentService.create(request);
    }

    @Operation(summary = "Departamenti id ile getir", description = "Iscileri ile birlikde qaytarir. Tapilmasa 404 (ERR-2200)")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DepartmentResponse getById(@PathVariable Long id) {
        return departmentService.getById(id);
    }

    @Operation(summary = "Butun departamentler", description = "Ada gore siralanmis departamentler")
    @GetMapping("/")
    @ResponseStatus(HttpStatus.OK)
    public List<DepartmentResponse> getAll() {
        return departmentService.getAll();
    }

    @Operation(summary = "Iscisi olmayan departamentler", description = "Hec bir iscisi olmayan departamentler")
    @GetMapping("/without-employees")
    @ResponseStatus(HttpStatus.OK)
    public List<DepartmentResponse> getWithoutEmployees() {
        return departmentService.getWithoutEmployeesDepartments();
    }

    @Operation(summary = "Departamenti yenile", description = "Tapilmasa 404 (ERR-2200), ad basqa departamentde varsa 409 (ERR-2202)")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DepartmentResponse update(@PathVariable Long id, @Valid @RequestBody UpdateDepartmentRequest request) {
        return departmentService.update(id, request);
    }

    @Operation(summary = "Departamenti sil", description = "Tapilmasa 404 (ERR-2200), iscisi varsa 409 (ERR-2203)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        departmentService.delete(id);
    }
}
