package com.spring_boot.jpa_practice_1.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "students", uniqueConstraints = { })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
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

    // cascade: persist ve merge edirik cunku sadece course ve student arasindaki elaqeni silmek isteyirik.
    // student silinende kurs silinmemelidi sadec student course record silinmelidi
    @ManyToMany(
            fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE, CascadeType.PERSIST}
    )
    @JoinTable(
            name = "student_course",
            joinColumns = @JoinColumn(
                    name = "student_id",
                    referencedColumnName = "id",
                    foreignKey = @ForeignKey(name = "fk_sc_student")
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "course_id",
                    referencedColumnName = "id",
                    foreignKey = @ForeignKey(name = "fk_sc_course")
            ),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_student_course",
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
