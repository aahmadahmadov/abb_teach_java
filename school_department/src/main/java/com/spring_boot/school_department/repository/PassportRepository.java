package com.spring_boot.school_department.repository;

import com.spring_boot.school_department.entity.Passport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PassportRepository extends JpaRepository<Passport, Long> {

    boolean existsBySerialNumber(String serialNumber);

    Optional<Passport> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("select p from Passport p join fetch p.user where p.country = :country")
    List<Passport> findAllByCountryWithUser(@Param("country") String country);

    @Query("select p from Passport p where p.issueDate < :date")
    List<Passport> findAllIssuedBefore(@Param("date") LocalDate date);

}
