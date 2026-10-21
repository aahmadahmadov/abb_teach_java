package com.spring_boot.jpa_practice_1.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "courses", uniqueConstraints = {
        @UniqueConstraint(name = "uk_title", columnNames = {"title"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "credit", nullable = false)
    private Integer credit;

    @Column(name = "teacher", nullable = false, length = 50)
    private String teacher;

    @ManyToMany(
            mappedBy = "courses",
            fetch = FetchType.LAZY
    )
    private Set<Student> students = new HashSet<>();

}
