package com.spring_boot.school_department.repository;

import com.spring_boot.school_department.entity.Employee;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByDepartmentId(Long departmentId);

    List<Employee> findAllByDepartmentId(Long departmentId);

    @Query("select e from Employee e join fetch e.department where e.id = :id")
    Optional<Employee> findByIdWithDepartment(@Param("id") Long id);

}
