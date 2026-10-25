package com.spring_boot.school_department.repository;

import com.spring_boot.school_department.entity.Passport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PassportRepository extends JpaRepository<Passport, Long> {

    boolean existsBySerialNumber(String serialNumber);

    Optional<Passport> findByUserId(Long userId);

}
