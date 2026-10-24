package com.spring_boot.school_department.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "passports")
public class Passport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;


}
