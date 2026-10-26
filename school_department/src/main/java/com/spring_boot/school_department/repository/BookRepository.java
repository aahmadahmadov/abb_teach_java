package com.spring_boot.school_department.repository;

import com.spring_boot.school_department.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, Long id);

    List<Book> findAllByTitleContainingIgnoreCase(String title);

    List<Book> findAllByPriceLessThanEqualOrderByPriceAsc(BigDecimal price);

    List<Book> findAllByAuthorBooksAuthorId(Long authorId);

    @Query("select b from Book b left join fetch b.authorBooks ab left join fetch ab.author where b.id = :id")
    Optional<Book> findByIdWithAuthors(@Param("id") Long id);

    @Query("select b from Book b order by b.title")
    List<Book> findAllOrderByTitle();
}
