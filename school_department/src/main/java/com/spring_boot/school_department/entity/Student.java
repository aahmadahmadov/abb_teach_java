package com.spring_boot.school_department.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "students")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class Student extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "surname", nullable = false, length = 50)
    private String surname;

    @Column(name = "age", nullable = false, check = {
            @CheckConstraint(name = "age_check", constraint = "age > 0 AND age < 120")
    })
    private Integer age;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(
            name = "student_course",
            joinColumns = @JoinColumn(name = "student_id",
                    foreignKey = @ForeignKey(name = "fk_sc_student")),
            inverseJoinColumns = @JoinColumn(name = "course_id",
                    foreignKey = @ForeignKey(name = "fk_sc_course")),
            uniqueConstraints = @UniqueConstraint(name = "uk_student_course",
                    columnNames = {"student_id", "course_id"})
    )
    private Set<Course> courses = new HashSet<>();

    public void addCourse(Course course) {
        courses.add(course);
        course.getStudents().add(this);
    }

    public void removeCourse(Course course) {
        courses.remove(course);
        course.getStudents().remove(this);
    }
}
