package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.request.CreateCourseRequest;
import com.spring_boot.school_department.dto.request.UpdateCourseRequest;
import com.spring_boot.school_department.dto.response.CourseResponse;
import com.spring_boot.school_department.dto.response.StudentResponse;
import com.spring_boot.school_department.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Course", description = "Kurs emeliyyatlari")
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "Kurs yarat", description = "Eyni adda kurs varsa 409 (ERR-2101), validasiya xetasinda 400 (ERR-1000)")
    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CreateCourseRequest request) {
        return courseService.create(request);
    }

    @Operation(summary = "Butun kurslar", description = "Ada gore siralanmis kurslar")
    @GetMapping("/")
    @ResponseStatus(HttpStatus.OK)
    public List<CourseResponse> getAll() {
        return courseService.getAll();
    }

    @Operation(summary = "Kursu id ile getir", description = "Tapilmasa 404 (ERR-2100)")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CourseResponse getById(@PathVariable Long id) {
        return courseService.getById(id);
    }

    @Operation(summary = "Telebenin kurslari", description = "Telebe tapilmasa 404 (ERR-2000)")
    @GetMapping("/student/{studentId}")
    @ResponseStatus(HttpStatus.OK)
    public List<CourseResponse> getStudentCourses(@PathVariable Long studentId) {
        return courseService.getStudentCourses(studentId);
    }

    @Operation(summary = "Kursun telebeleri", description = "Kurs tapilmasa 404 (ERR-2100)")
    @GetMapping("/{id}/students")
    @ResponseStatus(HttpStatus.OK)
    public List<StudentResponse> getCourseStudents(@PathVariable Long id) {
        return courseService.getCourseStudents(id);
    }

    @Operation(summary = "Kursdaki telebe sayi", description = "Kurs tapilmasa 404 (ERR-2100)")
    @GetMapping("/{id}/students/count")
    @ResponseStatus(HttpStatus.OK)
    public long getStudentCount(@PathVariable Long id) {
        return courseService.getStudentCount(id);
    }

    @Operation(summary = "Kursu yenile", description = "Tapilmasa 404 (ERR-2100), ad basqa kursda varsa 409 (ERR-2102)")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCourseRequest request) {
        return courseService.update(id, request);
    }

    @Operation(summary = "Butun kurslarin kreditini artir", description = "Bulk update, courses cache region-unu temizleyir. amount gonderilmese 400 (ERR-1004)")
    @PutMapping("/credits")
    @ResponseStatus(HttpStatus.OK)
    public int increaseAllCredits(@RequestParam Integer amount) {
        return courseService.increaseAllCredits(amount);
    }

    @Operation(summary = "Kursu sil", description = "Tapilmasa 404 (ERR-2100)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courseService.delete(id);
    }
}
