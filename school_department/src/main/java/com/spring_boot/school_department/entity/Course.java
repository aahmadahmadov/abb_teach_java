package com.spring_boot.school_department.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "courses")
@Table(name = "courses")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class Course extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "title", nullable = false, unique = true, length = 100)
    private String title;

    @Column(name = "credit", nullable = false)
    private Integer credit;

    @Column(name = "teacher", nullable = false, length = 50)
    private String teacher;

    @Column(name = "capacity", nullable = false)
    private Integer capacity = 30;

    @ManyToMany(mappedBy = "courses", fetch = FetchType.LAZY)
    private Set<Student> students = new HashSet<>();

    public void removeAllStudents() {
        students.forEach(student -> student.getCourses().remove(this));
        students.clear();
    }

}
