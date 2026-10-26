package com.spring_boot.school_department.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "author_book", uniqueConstraints = @UniqueConstraint(
        name = "uk_author_book", columnNames = {"author_id", "book_id"}))
@NoArgsConstructor
@Getter
@Setter
public class AuthorBook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", foreignKey = @ForeignKey(name = "fk_ab_author"), nullable = false)
    private Author author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", foreignKey = @ForeignKey(name = "fk_ab_book"), nullable = false)
    private Book book;

    public AuthorBook(Author author, Book book) {
        this.author = author;
        this.book = book;
    }
}
