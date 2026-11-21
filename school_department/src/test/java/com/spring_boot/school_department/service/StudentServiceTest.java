package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.request.CreateStudentRequest;
import com.spring_boot.school_department.dto.request.UpdateStudentRequest;
import com.spring_boot.school_department.dto.response.StudentResponse;
import com.spring_boot.school_department.entity.Course;
import com.spring_boot.school_department.entity.Student;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.exception.AlreadyExistsException;
import com.spring_boot.school_department.exception.BusinessException;
import com.spring_boot.school_department.exception.NotFoundException;
import com.spring_boot.school_department.mapper.StudentMapper;
import com.spring_boot.school_department.repository.CourseRepository;
import com.spring_boot.school_department.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private StudentMapper studentMapper;

    @InjectMocks
    private StudentService studentService;

    @Test
    void create_validRequest_savesAndReturnsResponse() {
        CreateStudentRequest request = new CreateStudentRequest("Ali", "Aliyev", 20);
        Student student = student();
        StudentResponse response = response();
        when(studentMapper.toEntity(request)).thenReturn(student);
        when(studentMapper.toResponse(student)).thenReturn(response);

        StudentResponse result = studentService.create(request);

        assertThat(result).isEqualTo(response);
        verify(studentRepository).save(student);
    }

    @Test
    void getById_studentExists_returnsDetailResponse() {
        Student student = student();
        StudentResponse response = response();
        when(studentRepository.findByIdWithCourses(1L)).thenReturn(Optional.of(student));
        when(studentMapper.toDetailResponse(student)).thenReturn(response);

        StudentResponse result = studentService.getById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getById_studentNotFound_throwsNotFound() {
        when(studentRepository.findByIdWithCourses(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getById(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDENT_NOT_FOUND);
    }

    @Test
    void getAll_studentsExist_returnsResponseList() {
        List<Student> students = List.of(student());
        List<StudentResponse> responses = List.of(response());
        when(studentRepository.findAll()).thenReturn(students);
        when(studentMapper.toResponseList(students)).thenReturn(responses);

        List<StudentResponse> result = studentService.getAll();

        assertThat(result).isEqualTo(responses);
    }

    @Test
    void getWithoutCourses_studentsExist_returnsResponseList() {
        List<Student> students = List.of(student());
        List<StudentResponse> responses = List.of(response());
        when(studentRepository.findAllWithoutCourses()).thenReturn(students);
        when(studentMapper.toResponseList(students)).thenReturn(responses);

        List<StudentResponse> result = studentService.getWithoutCourses();

        assertThat(result).isEqualTo(responses);
    }

    @Test
    void update_studentExists_updatesAndReturnsResponse() {
        UpdateStudentRequest request = new UpdateStudentRequest("Veli", "Veliyev", 21);
        Student student = student();
        StudentResponse response = response();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentMapper.toResponse(student)).thenReturn(response);

        StudentResponse result = studentService.update(1L, request);

        assertThat(result).isEqualTo(response);
        verify(studentMapper).updateEntity(request, student);
    }

    @Test
    void update_studentNotFound_throwsNotFound() {
        UpdateStudentRequest request = new UpdateStudentRequest("Veli", "Veliyev", 21);
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.update(1L, request))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDENT_NOT_FOUND);

        verify(studentMapper, never()).updateEntity(any(), any());
    }

    @Test
    void delete_studentExists_deletesStudent() {
        Student student = student();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        studentService.delete(1L);

        verify(studentRepository).delete(student);
    }

    @Test
    void delete_studentNotFound_throwsNotFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.delete(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDENT_NOT_FOUND);

        verify(studentRepository, never()).delete(any());
    }

    @Test
    void enroll_notEnrolled_addsCourseAndReturnsDetailResponse() {
        Student student = student();
        Course course = course();
        StudentResponse response = response();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));
        when(studentRepository.existsByIdAndCoursesId(1L, 2L)).thenReturn(false);
        when(studentMapper.toDetailResponse(student)).thenReturn(response);

        StudentResponse result = studentService.enroll(1L, 2L);

        assertThat(result).isEqualTo(response);
        assertThat(student.getCourses()).contains(course);
        assertThat(course.getStudents()).contains(student);
        verify(enrollmentService).reserveSeat(2L);
    }

    @Test
    void enroll_alreadyEnrolled_throwsAlreadyExists() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student()));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course()));
        when(studentRepository.existsByIdAndCoursesId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> studentService.enroll(1L, 2L))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDENT_ALREADY_ENROLLED);

        verifyNoInteractions(enrollmentService);
    }

    @Test
    void enroll_studentNotFound_throwsNotFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.enroll(1L, 2L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDENT_NOT_FOUND);

        verifyNoInteractions(courseRepository, enrollmentService);
    }

    @Test
    void enroll_courseNotFound_throwsNotFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student()));
        when(courseRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.enroll(1L, 2L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);

        verifyNoInteractions(enrollmentService);
    }

    @Test
    void unenroll_enrolled_removesCourse() {
        Student student = student();
        Course course = course();
        student.addCourse(course);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));
        when(studentRepository.existsByIdAndCoursesId(1L, 2L)).thenReturn(true);

        studentService.unenroll(1L, 2L);

        assertThat(student.getCourses()).isEmpty();
        assertThat(course.getStudents()).isEmpty();
    }

    @Test
    void unenroll_notEnrolled_throwsBusiness() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student()));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course()));
        when(studentRepository.existsByIdAndCoursesId(1L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> studentService.unenroll(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDENT_NOT_ENROLLED);
    }

    private Student student() {
        Student student = new Student();
        student.setId(1L);
        student.setName("Ali");
        student.setSurname("Aliyev");
        student.setAge(20);
        return student;
    }

    private Course course() {
        Course course = new Course();
        course.setId(2L);
        course.setTitle("Java");
        course.setCredit(5);
        course.setTeacher("Orxan");
        return course;
    }

    private StudentResponse response() {
        return new StudentResponse(1L, "Ali", "Aliyev", 20, null, null, null);
    }
}
