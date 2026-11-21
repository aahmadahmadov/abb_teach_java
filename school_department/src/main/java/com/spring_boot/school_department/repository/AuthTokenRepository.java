package com.spring_boot.school_department.repository;

import com.spring_boot.school_department.entity.AuthToken;
import com.spring_boot.school_department.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByJti(String jti);

    List<AuthToken> findAllBySessionIdAndRevokedFalse(String sessionId);

    List<AuthToken> findAllByAccountIdAndRevokedFalse(Long accountId);

    @Query("select t from AuthToken t where t.account.id = :accountId and t.tokenType = :type and t.revoked = false and t.expiresAt > :now order by t.issuedAt desc")
    List<AuthToken> findActive(@Param("accountId") Long accountId, @Param("type") TokenType type, @Param("now") Instant now);

    @Modifying
    @Query("delete from AuthToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
