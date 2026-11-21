package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.request.CreateCourseRequest;
import com.spring_boot.school_department.dto.request.UpdateCourseRequest;
import com.spring_boot.school_department.dto.response.CourseResponse;
import com.spring_boot.school_department.dto.response.StudentResponse;
import com.spring_boot.school_department.entity.Course;
import com.spring_boot.school_department.entity.Student;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.exception.AlreadyExistsException;
import com.spring_boot.school_department.exception.NotFoundException;
import com.spring_boot.school_department.mapper.CourseMapper;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private StudentMapper studentMapper;

    @InjectMocks
    private CourseService courseService;

    @Test
    void create_titleIsFree_savesAndReturnsResponse() {
        CreateCourseRequest request = new CreateCourseRequest("Java", 5, "Orxan");
        Course course = course();
        CourseResponse response = response();
        when(courseRepository.existsByTitle("Java")).thenReturn(false);
        when(courseMapper.toEntity(request)).thenReturn(course);
        when(courseMapper.toResponse(course)).thenReturn(response);

        CourseResponse result = courseService.create(request);

        assertThat(result).isEqualTo(response);
        verify(courseRepository).save(course);
    }

    @Test
    void create_titleExists_throwsAlreadyExists() {
        CreateCourseRequest request = new CreateCourseRequest("Java", 5, "Orxan");
        when(courseRepository.existsByTitle("Java")).thenReturn(true);

        assertThatThrownBy(() -> courseService.create(request))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_ALREADY_EXISTS);

        verify(courseRepository, never()).save(any());
    }

    @Test
    void getAll_coursesExist_returnsResponseList() {
        List<Course> courses = List.of(course());
        List<CourseResponse> responses = List.of(response());
        when(courseRepository.findAllOrderByTitle()).thenReturn(courses);
        when(courseMapper.toResponseList(courses)).thenReturn(responses);

        List<CourseResponse> result = courseService.getAll();

        assertThat(result).isEqualTo(responses);
    }

    @Test
    void getById_courseExists_returnsResponse() {
        Course course = course();
        CourseResponse response = response();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseMapper.toResponse(course)).thenReturn(response);

        CourseResponse result = courseService.getById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getById_courseNotFound_throwsNotFound() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getById(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    void getStudentCourses_studentExists_returnsResponseList() {
        List<Course> courses = List.of(course());
        List<CourseResponse> responses = List.of(response());
        when(studentRepository.existsById(2L)).thenReturn(true);
        when(courseRepository.findAllByStudentsId(2L)).thenReturn(courses);
        when(courseMapper.toResponseList(courses)).thenReturn(responses);

        List<CourseResponse> result = courseService.getStudentCourses(2L);

        assertThat(result).isEqualTo(responses);
    }

    @Test
    void getStudentCourses_studentNotFound_throwsNotFound() {
        when(studentRepository.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> courseService.getStudentCourses(2L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDENT_NOT_FOUND);

        verify(courseRepository, never()).findAllByStudentsId(any());
    }

    @Test
    void getCourseStudents_courseExists_returnsStudentList() {
        List<Student> students = List.of(new Student());
        List<StudentResponse> responses = List.of(new StudentResponse(2L, "Ali", "Aliyev", 20, null, null, null));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course()));
        when(studentRepository.findAllByCoursesId(1L)).thenReturn(students);
        when(studentMapper.toResponseList(students)).thenReturn(responses);

        List<StudentResponse> result = courseService.getCourseStudents(1L);

        assertThat(result).isEqualTo(responses);
    }

    @Test
    void getStudentCount_courseExists_returnsCount() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course()));
        when(studentRepository.countByCoursesId(1L)).thenReturn(3L);

        long result = courseService.getStudentCount(1L);

        assertThat(result).isEqualTo(3L);
    }

    @Test
    void update_titleIsFree_updatesAndReturnsResponse() {
        UpdateCourseRequest request = new UpdateCourseRequest("Spring", 6, "Orxan");
        Course course = course();
        CourseResponse response = response();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.existsByTitleAndIdNot("Spring", 1L)).thenReturn(false);
        when(courseMapper.toResponse(course)).thenReturn(response);

        CourseResponse result = courseService.update(1L, request);

        assertThat(result).isEqualTo(response);
        verify(courseMapper).updateEntity(request, course);
    }

    @Test
    void update_titleUsed_throwsAlreadyExists() {
        UpdateCourseRequest request = new UpdateCourseRequest("Spring", 6, "Orxan");
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course()));
        when(courseRepository.existsByTitleAndIdNot("Spring", 1L)).thenReturn(true);

        assertThatThrownBy(() -> courseService.update(1L, request))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_TITLE_USED);

        verify(courseMapper, never()).updateEntity(any(), any());
    }

    @Test
    void increaseAllCredits_amountGiven_returnsUpdatedCount() {
        when(courseRepository.increaseAllCredits(2)).thenReturn(4);

        int result = courseService.increaseAllCredits(2);

        assertThat(result).isEqualTo(4);
    }

    @Test
    void delete_courseExists_removesStudentsAndDeletes() {
        Course course = course();
        Student student = new Student();
        student.addCourse(course);
        when(courseRepository.findByIdWithStudents(1L)).thenReturn(Optional.of(course));

        courseService.delete(1L);

        assertThat(course.getStudents()).isEmpty();
        assertThat(student.getCourses()).isEmpty();
        verify(courseRepository).delete(course);
    }

    @Test
    void delete_courseNotFound_throwsNotFound() {
        when(courseRepository.findByIdWithStudents(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.delete(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);

        verify(courseRepository, never()).delete(any());
    }

    private Course course() {
        Course course = new Course();
        course.setId(1L);
        course.setTitle("Java");
        course.setCredit(5);
        course.setTeacher("Orxan");
        return course;
    }

    private CourseResponse response() {
        return new CourseResponse(1L, "Java", 5, "Orxan", null, null, null);
    }
}
