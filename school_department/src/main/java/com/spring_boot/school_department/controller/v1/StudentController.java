package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.request.CreateStudentRequest;
import com.spring_boot.school_department.dto.request.UpdateStudentRequest;
import com.spring_boot.school_department.dto.response.StudentResponse;
import com.spring_boot.school_department.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Student", description = "Telebe emeliyyatlari ve kursa yazilma")
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @Operation(summary = "Telebe yarat", description = "Validasiya xetasinda 400 (ERR-1000)")
    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(@Valid @RequestBody CreateStudentRequest request) {
        return studentService.create(request);
    }

    @Operation(summary = "Telebeni id ile getir", description = "Kurslari ile birlikde qaytarir. Tapilmasa 404 (ERR-2000)")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public StudentResponse getById(@PathVariable Long id) {
        return studentService.getById(id);
    }

    @Operation(summary = "Butun telebeler", description = "Butun telebelerin siyahisi")
    @GetMapping("/")
    @ResponseStatus(HttpStatus.OK)
    public List<StudentResponse> getAll() {
        return studentService.getAll();
    }

    @Operation(summary = "Kursu olmayan telebeler", description = "Hec bir kursa yazilmayan telebeler")
    @GetMapping("/without-courses")
    @ResponseStatus(HttpStatus.OK)
    public List<StudentResponse> getWithoutCourses() {
        return studentService.getWithoutCourses();
    }

    @Operation(summary = "Telebeni yenile", description = "Tapilmasa 404 (ERR-2000), validasiya xetasinda 400 (ERR-1000)")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody UpdateStudentRequest request) {
        return studentService.update(id, request);
    }

    @Operation(summary = "Telebeni sil", description = "Tapilmasa 404 (ERR-2000)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        studentService.delete(id);
    }

    @Operation(summary = "Telebeni kursa yaz", description = "Telebe tapilmasa 404 (ERR-2000), kurs tapilmasa 404 (ERR-2100), artiq yazilibsa 409 (ERR-2001), kursda yer yoxdursa 409 (ERR-2103)")
    @PostMapping("/{studentId}/courses/{courseId}")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse enroll(@PathVariable Long studentId, @PathVariable Long courseId) {
        return studentService.enroll(studentId, courseId);
    }

    @Operation(summary = "Telebeni kursdan cixar", description = "Telebe tapilmasa 404 (ERR-2000), kurs tapilmasa 404 (ERR-2100), kursa yazilmayibsa 409 (ERR-2002)")
    @DeleteMapping("/{studentId}/courses/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unenroll(@PathVariable Long studentId, @PathVariable Long courseId) {
        studentService.unenroll(studentId, courseId);
    }
}
