package com.spring_boot.jpa_practice_1.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_department_name", columnNames = {"name"})
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Department {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "phone", length = 20)
    private String phone;

    // all: department silinende isciler de silinecek
    // orphanRemoval: siyahidan cixan user db-deb silinecek
    @OneToMany(
            mappedBy = "department",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Employee> employees = new ArrayList<>();

    public void addEmployee(Employee employee) {

        if (employee == null) {
            return;
        }

        if (employee.getDepartment() == this && employees.contains(employee)) {
            return;
        }

        if (employee.getDepartment() != null) {
            employee.getDepartment().removeEmployee(employee);
        }

        employees.add(employee);
        employee.setDepartment(this);
    }

    public void removeEmployee(Employee employee) {

        if (employee == null) {
            return;
        }

        if (employees.remove(employee) && employee.getDepartment() == this) {
            employee.setDepartment(null);
        }
    }
}
