package com.spring_boot.school_department.repository;

import com.spring_boot.school_department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    @Query("select d from Department d where d.employees is empty")
    List<Department> findDepartmentsWithoutEmployees();

}
