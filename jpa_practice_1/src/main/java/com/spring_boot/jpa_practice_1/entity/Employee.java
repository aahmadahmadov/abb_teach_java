package com.spring_boot.jpa_practice_1.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

@Entity
@Table(name = "employees", uniqueConstraints = { })
@Check(name = "chk_employee_salary", constraints = "salary >= 0")
@Check(name = "chk_employee_age", constraints = "age >= 18 AND age <= 100")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Employee {

    @Id
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "surname", length = 50, nullable = false)
    private String surname;

    @Column(name = "salary", nullable = false, scale = 2, precision = 10)
    private BigDecimal salary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "department_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_employee_department")
    )
    private Department department;
}
