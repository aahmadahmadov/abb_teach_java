package com.spring_boot.school_department.service;

import com.spring_boot.school_department.entity.Course;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.exception.BusinessException;
import com.spring_boot.school_department.exception.NotFoundException;
import com.spring_boot.school_department.repository.CourseRepository;
import com.spring_boot.school_department.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    // transactiondan kenarda calismasin deye mandatory, aciq bir transaction ile calissin
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserveSeat(Long courseId) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND, courseId));

        long studentCount = studentRepository.countByCoursesId(courseId);

        if (studentCount >= course.getCapacity()) {
            throw new BusinessException(ErrorCode.COURSE_FULL, courseId);
        }
    }
}
