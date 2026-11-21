package com.spring_boot.school_department.repository;

import com.spring_boot.school_department.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByDepartmentId(Long departmentId);

    boolean existsByNameAndSurnameAndDepartmentId(String name, String surname, Long departmentId);

    boolean existsByNameAndSurnameAndDepartmentIdAndIdNot(String name, String surname, Long departmentId, Long id);

    List<Employee> findAllByDepartmentId(Long departmentId);

    List<Employee> findAllBySalaryBetween(BigDecimal min, BigDecimal max);

    @Query("select e from Employee e join fetch e.department where e.id = :id")
    Optional<Employee> findByIdWithDepartment(@Param("id") Long id);

    @Query("select avg(e.salary) from Employee e where e.department.id = :departmentId")
    BigDecimal findAverageSalaryByDepartmentId(@Param("departmentId") Long departmentId);

}
