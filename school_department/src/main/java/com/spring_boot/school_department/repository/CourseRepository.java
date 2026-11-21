package com.spring_boot.school_department.repository;

import com.spring_boot.school_department.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, Long id);

    List<Course> findAllByStudentsId(Long studentId);

    @Query("select c from Course c left join fetch c.students where c.id = :id")
    Optional<Course> findByIdWithStudents(@Param("id") Long id);

    @Query("select c from Course c order by c.title")
    List<Course> findAllOrderByTitle();

    // bulk update cache-den kecmir, ona gore hibernate butun courses regionunu temizleyir
    @Modifying
    @Query("update Course c set c.credit = c.credit + :amount")
    int increaseAllCredits(@Param("amount") Integer amount);
}
