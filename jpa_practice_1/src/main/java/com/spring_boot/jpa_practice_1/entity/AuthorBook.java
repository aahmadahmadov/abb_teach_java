package com.spring_boot.jpa_practice_1.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "author_book", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_author_book",
                columnNames = {"author_id", "book_id"}
        )
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
// pivot vedvel ucun entity yaratmaqda ustunluk ola biler elave datalar vere bilerik burda sonradan, information tipli column elave edile biler.
// castismazliq ise elave kod yazmali oluruq, kodda +1 class yaradiriq, kitaba getmek ucun pivot ustunden gedirik bunlar ola biler.
public class AuthorBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    // cascade vermirik cunku AuthorBook silinende muellif recordu silinmemelidi
    @ManyToOne(
            fetch = FetchType.LAZY, optional = false
    )
    @JoinColumn(
            name = "author_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_ab_author"
            )
    )
    @Setter(AccessLevel.PACKAGE)
    private Author author;

    // cascade vermiyirk cunku AuthorBook silinende kitab silinmemelidi
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "book_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ab_book")
    )
    @Setter(AccessLevel.PACKAGE)
    private Book book;

}
