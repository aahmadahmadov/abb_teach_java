package com.spring_boot.school_department.repository;

import com.spring_boot.school_department.entity.AuthorBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorBookRepository extends JpaRepository<AuthorBook, Long> {

    boolean existsByAuthorIdAndBookId(Long authorId, Long bookId);

    @Modifying
    @Query("delete from AuthorBook ab where ab.author.id = :authorId and ab.book.id = :bookId")
    int deleteByAuthorIdAndBookId(@Param("authorId") Long authorId, @Param("bookId") Long bookId);
}
