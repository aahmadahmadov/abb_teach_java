package com.spring_boot.school_department.repository;

import com.spring_boot.school_department.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    boolean existsByJti(String jti);

    Optional<TokenBlacklist> findByJti(String jti);

    long countByAccountId(Long accountId);

    @Modifying
    @Query("delete from TokenBlacklist b where b.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
