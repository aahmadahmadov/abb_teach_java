package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.request.CreateCourseRequest;
import com.spring_boot.school_department.dto.request.UpdateCourseRequest;
import com.spring_boot.school_department.dto.response.CourseResponse;
import com.spring_boot.school_department.dto.response.StudentResponse;
import com.spring_boot.school_department.entity.Course;
import com.spring_boot.school_department.mapper.CourseMapper;
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
public class CourseService {

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    private final CourseMapper courseMapper;
    private final StudentMapper studentMapper;

    @Transactional
    public CourseResponse create(CreateCourseRequest request) {

        if (courseRepository.existsByTitle(request.title())) {
            throw new IllegalStateException("Course with title already exists: " + request.title());
        }

        Course course = courseMapper.toEntity(request);
        courseRepository.save(course);

        return courseMapper.toResponse(course);
    }

    public List<CourseResponse> getAll() {
        return courseMapper.toResponseList(courseRepository.findAllOrderByTitle());
    }

    public CourseResponse getById(Long id) {
        return courseMapper.toResponse(findCourse(id));
    }

    public List<CourseResponse> getStudentCourses(Long studentId) {

        if (!studentRepository.existsById(studentId)) {
            throw new EntityNotFoundException("Student not found: " + studentId);
        }

        return courseMapper.toResponseList(courseRepository.findAllByStudentsId(studentId));
    }

    public List<StudentResponse> getCourseStudents(Long id) {
        findCourse(id);

        return studentMapper.toResponseList(studentRepository.findAllByCoursesId(id));
    }

    public long getStudentCount(Long id) {
        findCourse(id);

        return studentRepository.countByCoursesId(id);
    }

    @Transactional
    public CourseResponse update(Long id, UpdateCourseRequest request) {
        Course course = findCourse(id);

        if (courseRepository.existsByTitleAndIdNot(request.title(), id)) {
            throw new IllegalStateException("Course with title already exists: " + request.title());
        }

        courseMapper.updateEntity(request, course);

        return courseMapper.toResponse(course);
    }

    @Transactional
    public void delete(Long id) {
        Course course = courseRepository.findByIdWithStudents(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found: " + id));

        course.removeAllStudents();

        courseRepository.delete(course);
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found: " + id));
    }
}
