package com.spring_boot.school_department.repository;

import com.spring_boot.school_department.entity.User;
import com.spring_boot.school_department.enums.GenderEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @EntityGraph(attributePaths = {"passport"})
    List<User> findAllByGender(GenderEnum gender);

    @EntityGraph(attributePaths = "passport")
    Optional<User> findByEmail(String email);

    List<User> findAllByPassportIsNull();

    @Query("select u from User u left join fetch u.passport where u.id = :id")
    Optional<User> findByIdWithPassport(@Param("id") Long id);

    @Query("select u from User u left join fetch u.passport")
    List<User> findAllWithPassport();
}
