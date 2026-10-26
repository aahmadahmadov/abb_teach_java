package com.spring_boot.school_department.repository;

import com.spring_boot.school_department.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByIdAndCoursesId(Long id, Long courseId);

    List<Student> findAllByCoursesId(Long courseId);

    long countByCoursesId(Long courseId);

    @Query("select s from Student s left join fetch s.courses where s.id = :id")
    Optional<Student> findByIdWithCourses(@Param("id") Long id);

    @Query("select s from Student s where s.courses is empty")
    List<Student> findAllWithoutCourses();
}
