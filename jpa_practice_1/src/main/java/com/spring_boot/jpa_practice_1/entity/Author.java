package com.spring_boot.jpa_practice_1.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "authors", uniqueConstraints = { })
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "surname", nullable = false, length = 50)
    private String surname;

    @Column(name = "country", length = 50)
    private String country;

    // all + orphanRemoval: author silinende sadece author_book recordlari silinecek, birlesdiyir yerdeki datalar qalir
    // sadece relation silinsin, qarsi terefde datalar qalsin kimi bir yanasma lazimdisa islenir
    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuthorBook> authorBooks = new ArrayList<>();

    public void addAuthorBook(AuthorBook authorBook) {

        if (authorBook == null) {
            return;
        }

        if (authorBook.getAuthor() != this) {
            authorBook.setAuthor(this);
        }

        if (!authorBooks.contains(authorBook)) {
            authorBooks.add(authorBook);
        }
    }

    public void removeAuthorBook(AuthorBook authorBook) {

        if (authorBook == null) {
            return;
        }

        if (authorBooks.remove(authorBook) && authorBook.getAuthor() == this) {
            authorBook.setAuthor(null);
        }
    }

}
