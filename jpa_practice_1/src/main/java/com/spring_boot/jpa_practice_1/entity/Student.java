package com.spring_boot.jpa_practice_1.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "students", uniqueConstraints = { })
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "surname", nullable = false, length = 50)
    private String surname;

    @Column(name = "age", nullable = false)
    private Integer age;

    @ManyToMany(
            fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE, CascadeType.PERSIST}
    )
    @JoinTable(
            name = "student_courses",
            joinColumns = @JoinColumn(
                    name = "student_id",
                    referencedColumnName = "id",
                    foreignKey = @ForeignKey(name = "fk_student_courses_student_id")
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "course_id",
                    referencedColumnName = "id",
                    foreignKey = @ForeignKey(name = "fk_student_courses_course_id")
            ),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_student_courses",
                    columnNames = {"student_id", "course_id"}
            )
    )
    private Set<Course> courses = new HashSet<>();

    public void addCourse(Course course) {
        if (course == null) {
            return;
        }

        if (courses.add(course)) {
            course.getStudents().add(this);
        }
    }

    public void removeCourse(Course course) {
        if (course == null) {
            return;
        }

        if (courses.remove(course)) {
            course.getStudents().remove(this);
        }
    }

}
