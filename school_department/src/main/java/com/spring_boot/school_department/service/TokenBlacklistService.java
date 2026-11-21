package com.spring_boot.school_department.service;

import com.spring_boot.school_department.entity.AuthToken;
import com.spring_boot.school_department.entity.TokenBlacklist;
import com.spring_boot.school_department.enums.RevokeReason;
import com.spring_boot.school_department.repository.AuthTokenRepository;
import com.spring_boot.school_department.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenBlacklistService {

    private final TokenBlacklistRepository blacklistRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PlatformTransactionManager transactionManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public void blacklist(AuthToken token, RevokeReason reason) {

        if (blacklistRepository.existsByJti(token.getJti())) {
            return;
        }

        Instant now = Instant.now();

        token.setRevoked(true);
        token.setRevokedAt(now);
        token.setRevokeReason(reason);

        if (token.getExpiresAt().isBefore(now)) {
            return;
        }

        TokenBlacklist entry = new TokenBlacklist();

        entry.setJti(token.getJti());
        entry.setAccountId(token.getAccount().getId());
        entry.setTokenType(token.getTokenType());
        entry.setReason(reason);
        entry.setExpiresAt(token.getExpiresAt());

        saveEntry(entry);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void revokeSession(String sessionId, RevokeReason reason) {

        List<AuthToken> tokens = authTokenRepository.findAllBySessionIdAndRevokedFalse(sessionId);

        for (AuthToken token: tokens) {
            blacklist(token, reason);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void revokeAll(Long accountId, RevokeReason reason) {

        List<AuthToken> tokens = authTokenRepository.findAllByAccountIdAndRevokedFalse(accountId);

        for (AuthToken token: tokens) {
            blacklist(token, reason);
        }
    }

    public boolean isBlacklisted(String jti) {
        return blacklistRepository.existsByJti(jti);
    }

    public Optional<RevokeReason> findReason(String jti) {
        return blacklistRepository.findByJti(jti).map(TokenBlacklist::getReason);
    }

    private void saveEntry(TokenBlacklist entry) {

        TransactionTemplate newTransaction = new TransactionTemplate(transactionManager);
        newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            newTransaction.executeWithoutResult(status -> blacklistRepository.save(entry));
        } catch (DataIntegrityViolationException e) {
            log.debug("Token already blacklisted: jti={}", entry.getJti());
        }
    }
}
