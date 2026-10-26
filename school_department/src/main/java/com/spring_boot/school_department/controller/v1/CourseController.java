package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.request.CreateCourseRequest;
import com.spring_boot.school_department.dto.request.UpdateCourseRequest;
import com.spring_boot.school_department.dto.response.CourseResponse;
import com.spring_boot.school_department.dto.response.StudentResponse;
import com.spring_boot.school_department.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@RequestBody CreateCourseRequest request) {
        return courseService.create(request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CourseResponse> getAll() {
        return courseService.getAll();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CourseResponse getById(@PathVariable Long id) {
        return courseService.getById(id);
    }

    @GetMapping("/student/{studentId}")
    @ResponseStatus(HttpStatus.OK)
    public List<CourseResponse> getStudentCourses(@PathVariable Long studentId) {
        return courseService.getStudentCourses(studentId);
    }

    @GetMapping("/{id}/students")
    @ResponseStatus(HttpStatus.OK)
    public List<StudentResponse> getCourseStudents(@PathVariable Long id) {
        return courseService.getCourseStudents(id);
    }

    @GetMapping("/{id}/students/count")
    @ResponseStatus(HttpStatus.OK)
    public long getStudentCount(@PathVariable Long id) {
        return courseService.getStudentCount(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CourseResponse update(@PathVariable Long id, @RequestBody UpdateCourseRequest request) {
        return courseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courseService.delete(id);
    }
}
