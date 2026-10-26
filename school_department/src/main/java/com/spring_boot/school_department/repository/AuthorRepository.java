package com.spring_boot.school_department.repository;

import com.spring_boot.school_department.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

    boolean existsByNameAndSurname(String name, String surname);

    boolean existsByNameAndSurnameAndIdNot(String name, String surname, Long id);

    List<Author> findAllByCountry(String country);

    @Query("select a from Author a where a.authorBooks is empty")
    List<Author> findAllWithoutBooks();

    @Query("select a from Author a order by a.surname, a.name")
    List<Author> findAllOrderBySurname();
}
