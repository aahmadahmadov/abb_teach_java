package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.request.CreateStudentRequest;
import com.spring_boot.school_department.dto.request.UpdateStudentRequest;
import com.spring_boot.school_department.dto.response.StudentResponse;
import com.spring_boot.school_department.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(@RequestBody CreateStudentRequest request) {
        return studentService.create(request);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public StudentResponse getById(@PathVariable Long id) {
        return studentService.getById(id);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<StudentResponse> getAll() {
        return studentService.getAll();
    }

    @GetMapping("/without-courses")
    @ResponseStatus(HttpStatus.OK)
    public List<StudentResponse> getWithoutCourses() {
        return studentService.getWithoutCourses();
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public StudentResponse update(@PathVariable Long id, @RequestBody UpdateStudentRequest request) {
        return studentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        studentService.delete(id);
    }

    @PostMapping("/{studentId}/courses/{courseId}")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse enroll(@PathVariable Long studentId, @PathVariable Long courseId) {
        return studentService.enroll(studentId, courseId);
    }

    @DeleteMapping("/{studentId}/courses/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unenroll(@PathVariable Long studentId, @PathVariable Long courseId) {
        studentService.unenroll(studentId, courseId);
    }
}
