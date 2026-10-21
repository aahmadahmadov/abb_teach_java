package com.spring_boot.jpa_practice_1.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "author_book", uniqueConstraints = {
        @UniqueConstraint(
                name = "unk_author_book",
                columnNames = {"author_id", "book_id"}
        )
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AuthorBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY, optional = false
    )
    @JoinColumn(
            name = "author_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_author_book_author_id"
            )
    )
    private Author author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "book_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_author_book_book_id")
    )
    private Book book;

}
