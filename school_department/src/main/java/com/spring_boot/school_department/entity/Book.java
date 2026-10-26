package com.spring_boot.school_department.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books")
@NoArgsConstructor
@Getter
@Setter
public class Book extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "price", nullable = false, precision = 8, scale = 2, check = {
            @CheckConstraint(name = "chk_book_price", constraint = "price >= 0")
    })
    private BigDecimal price;

    @Column(name = "page_count", nullable = false)
    private Integer pageCount;

    @OneToMany(mappedBy = "book", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuthorBook> authorBooks = new ArrayList<>();
}
