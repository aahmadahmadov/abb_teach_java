package com.spring_boot.school_department.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

@Entity
@Table(name = "employees")
@RequiredArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Employee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "surname", nullable = false, length = 50)
    private String surname;

    @Column(name = "salary", nullable = false, precision = 10, scale = 2, check = {
            @CheckConstraint(name = "chk_salary_positive", constraint = "salary > 0")
    })
    private BigDecimal salary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", foreignKey = @ForeignKey(name = "fk_employee_department"), nullable = false)
    private Department department;
}
