package com.spring_boot.jpa_practice_1.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "books", uniqueConstraints = { })
@Check(name = "chk_book_price", constraints = "price >= 0")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "price", nullable = false, precision = 8, scale = 2)
    private BigDecimal price;

    @Column(name = "page_count", nullable = false)
    private Integer pageCount;

    @OneToMany(
            mappedBy = "book",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<AuthorBook> authorBooks = new ArrayList<>();

    public void addAuthorBook(AuthorBook authorBook) {

        if (authorBook == null) {
            return;
        }

        if (authorBook.getBook() != this) {
            authorBook.setBook(this);
        }

        if (!authorBooks.contains(authorBook)) {
            authorBooks.add(authorBook);
        }
    }

    public void removeAuthorBook(AuthorBook authorBook) {

        if (authorBook == null) {
            return;
        }

        if (authorBooks.remove(authorBook) && authorBook.getBook() == this) {
            authorBook.setBook(null);
        }
    }

}
