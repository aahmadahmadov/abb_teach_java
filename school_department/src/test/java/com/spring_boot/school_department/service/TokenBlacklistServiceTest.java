package com.spring_boot.school_department.service;

import com.spring_boot.school_department.entity.Account;
import com.spring_boot.school_department.entity.AuthToken;
import com.spring_boot.school_department.entity.TokenBlacklist;
import com.spring_boot.school_department.enums.RevokeReason;
import com.spring_boot.school_department.enums.TokenType;
import com.spring_boot.school_department.repository.AuthTokenRepository;
import com.spring_boot.school_department.repository.TokenBlacklistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private TokenBlacklistRepository blacklistRepository;

    @Mock
    private AuthTokenRepository authTokenRepository;

    // service ozu new TransactionTemplate(transactionManager) qurur, mock manager-de callback birbasa isleyir
    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private TokenBlacklistService blacklistService;

    @Test
    void blacklist_alreadyBlacklisted_doesNothing() {
        AuthToken token = token("jti-1", Instant.now().plusSeconds(600));
        when(blacklistRepository.existsByJti("jti-1")).thenReturn(true);

        blacklistService.blacklist(token, RevokeReason.LOGOUT);

        assertThat(token.isRevoked()).isFalse();
        verify(blacklistRepository, never()).save(any());
    }

    @Test
    void blacklist_tokenExpired_marksRevokedWithoutBlacklistEntry() {
        AuthToken token = token("jti-1", Instant.now().minusSeconds(60));
        when(blacklistRepository.existsByJti("jti-1")).thenReturn(false);

        blacklistService.blacklist(token, RevokeReason.LOGOUT);

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokedAt()).isNotNull();
        assertThat(token.getRevokeReason()).isEqualTo(RevokeReason.LOGOUT);
        verify(blacklistRepository, never()).save(any());
    }

    @Test
    void blacklist_activeToken_savesEntryInNewTransaction() {
        Instant expiresAt = Instant.now().plusSeconds(600);
        AuthToken token = token("jti-1", expiresAt);
        when(blacklistRepository.existsByJti("jti-1")).thenReturn(false);

        blacklistService.blacklist(token, RevokeReason.LOGOUT);

        assertThat(token.isRevoked()).isTrue();

        ArgumentCaptor<TokenBlacklist> entryCaptor = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(blacklistRepository).save(entryCaptor.capture());
        TokenBlacklist entry = entryCaptor.getValue();
        assertThat(entry.getJti()).isEqualTo("jti-1");
        assertThat(entry.getAccountId()).isEqualTo(1L);
        assertThat(entry.getTokenType()).isEqualTo(TokenType.ACCESS);
        assertThat(entry.getReason()).isEqualTo(RevokeReason.LOGOUT);
        assertThat(entry.getExpiresAt()).isEqualTo(expiresAt);

        ArgumentCaptor<TransactionDefinition> definitionCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager).getTransaction(definitionCaptor.capture());
        assertThat(definitionCaptor.getValue().getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    void blacklist_parallelDuplicateInsert_ignoresException() {
        AuthToken token = token("jti-1", Instant.now().plusSeconds(600));
        when(blacklistRepository.existsByJti("jti-1")).thenReturn(false);
        when(blacklistRepository.save(any(TokenBlacklist.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate jti"));

        assertThatCode(() -> blacklistService.blacklist(token, RevokeReason.LOGOUT))
                .doesNotThrowAnyException();

        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void revokeSession_noTokens_doesNothing() {
        when(authTokenRepository.findAllBySessionIdAndRevokedFalse("session-1")).thenReturn(List.of());

        blacklistService.revokeSession("session-1", RevokeReason.LOGOUT);

        verify(blacklistRepository, never()).existsByJti(any());
    }

    @Test
    void revokeSession_tokensExist_marksEachTokenRevoked() {
        AuthToken first = token("jti-1", Instant.now().minusSeconds(60));
        AuthToken second = token("jti-2", Instant.now().minusSeconds(60));
        when(authTokenRepository.findAllBySessionIdAndRevokedFalse("session-1")).thenReturn(List.of(first, second));

        blacklistService.revokeSession("session-1", RevokeReason.LOGOUT);

        assertThat(first.getRevokeReason()).isEqualTo(RevokeReason.LOGOUT);
        assertThat(second.getRevokeReason()).isEqualTo(RevokeReason.LOGOUT);
    }

    @Test
    void revokeAll_noTokens_doesNothing() {
        when(authTokenRepository.findAllByAccountIdAndRevokedFalse(1L)).thenReturn(List.of());

        blacklistService.revokeAll(1L, RevokeReason.LOGOUT_ALL);

        verify(blacklistRepository, never()).existsByJti(any());
    }

    @Test
    void revokeAll_tokensExist_marksEachTokenRevoked() {
        AuthToken first = token("jti-1", Instant.now().minusSeconds(60));
        AuthToken second = token("jti-2", Instant.now().minusSeconds(60));
        when(authTokenRepository.findAllByAccountIdAndRevokedFalse(1L)).thenReturn(List.of(first, second));

        blacklistService.revokeAll(1L, RevokeReason.LOGOUT_ALL);

        assertThat(first.getRevokeReason()).isEqualTo(RevokeReason.LOGOUT_ALL);
        assertThat(second.getRevokeReason()).isEqualTo(RevokeReason.LOGOUT_ALL);
    }

    @Test
    void isBlacklisted_jtiExists_returnsTrue() {
        when(blacklistRepository.existsByJti("jti-1")).thenReturn(true);

        boolean result = blacklistService.isBlacklisted("jti-1");

        assertThat(result).isTrue();
    }

    @Test
    void findReason_entryExists_returnsReason() {
        TokenBlacklist entry = new TokenBlacklist();
        entry.setReason(RevokeReason.REFRESH_ROTATION);
        when(blacklistRepository.findByJti("jti-1")).thenReturn(Optional.of(entry));

        Optional<RevokeReason> result = blacklistService.findReason("jti-1");

        assertThat(result).contains(RevokeReason.REFRESH_ROTATION);
    }

    @Test
    void findReason_entryMissing_returnsEmpty() {
        when(blacklistRepository.findByJti("jti-1")).thenReturn(Optional.empty());

        Optional<RevokeReason> result = blacklistService.findReason("jti-1");

        assertThat(result).isEmpty();
    }

    private AuthToken token(String jti, Instant expiresAt) {
        Account account = new Account();
        account.setId(1L);

        AuthToken token = new AuthToken();
        token.setJti(jti);
        token.setSessionId("session-1");
        token.setTokenType(TokenType.ACCESS);
        token.setAccount(account);
        token.setExpiresAt(expiresAt);
        return token;
    }
}
