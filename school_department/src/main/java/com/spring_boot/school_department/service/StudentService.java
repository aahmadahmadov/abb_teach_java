package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.request.CreateStudentRequest;
import com.spring_boot.school_department.dto.request.UpdateStudentRequest;
import com.spring_boot.school_department.dto.response.StudentResponse;
import com.spring_boot.school_department.entity.Course;
import com.spring_boot.school_department.entity.Student;
import com.spring_boot.school_department.mapper.StudentMapper;
import com.spring_boot.school_department.repository.CourseRepository;
import com.spring_boot.school_department.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    private final StudentMapper studentMapper;

    @Transactional
    public StudentResponse create(CreateStudentRequest request) {
        Student student = studentMapper.toEntity(request);
        studentRepository.save(student);

        return studentMapper.toResponse(student);
    }

    public StudentResponse getById(Long id) {
        Student student = studentRepository.findByIdWithCourses(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found: " + id));

        return studentMapper.toDetailResponse(student);
    }

    public List<StudentResponse> getAll() {
        return studentMapper.toResponseList(studentRepository.findAll());
    }

    public List<StudentResponse> getWithoutCourses() {
        return studentMapper.toResponseList(studentRepository.findAllWithoutCourses());
    }

    @Transactional
    public StudentResponse update(Long id, UpdateStudentRequest request) {
        Student student = findStudent(id);

        studentMapper.updateEntity(request, student);

        return studentMapper.toResponse(student);
    }

    @Transactional
    public void delete(Long id) {
        studentRepository.delete(findStudent(id));
    }

    @Transactional
    public StudentResponse enroll(Long studentId, Long courseId) {
        Student student = findStudent(studentId);
        Course course = findCourse(courseId);

        if (studentRepository.existsByIdAndCoursesId(studentId, courseId)) {
            throw new IllegalStateException("Student already enrolled in course: " + courseId);
        }

        student.addCourse(course);

        return studentMapper.toDetailResponse(student);
    }

    @Transactional
    public void unenroll(Long studentId, Long courseId) {
        Student student = findStudent(studentId);
        Course course = findCourse(courseId);

        if (!studentRepository.existsByIdAndCoursesId(studentId, courseId)) {
            throw new IllegalStateException("Student is not enrolled in course: " + courseId);
        }

        student.removeCourse(course);
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found: " + id));
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found: " + id));
    }
}
