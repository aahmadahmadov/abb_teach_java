package com.spring_boot.school_department.service;

import com.spring_boot.school_department.entity.Course;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.exception.BusinessException;
import com.spring_boot.school_department.exception.NotFoundException;
import com.spring_boot.school_department.repository.CourseRepository;
import com.spring_boot.school_department.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Test
    void reserveSeat_courseNotFound_throwsNotFound() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.reserveSeat(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);

        verifyNoInteractions(studentRepository);
    }

    @Test
    void reserveSeat_courseFull_throwsBusiness() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course(2)));
        when(studentRepository.countByCoursesId(1L)).thenReturn(2L);

        assertThatThrownBy(() -> enrollmentService.reserveSeat(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COURSE_FULL);
    }

    @Test
    void reserveSeat_freeSeat_doesNotThrow() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course(2)));
        when(studentRepository.countByCoursesId(1L)).thenReturn(1L);

        assertThatCode(() -> enrollmentService.reserveSeat(1L)).doesNotThrowAnyException();
    }

    private Course course(int capacity) {
        Course course = new Course();
        course.setId(1L);
        course.setCapacity(capacity);
        return course;
    }
}
