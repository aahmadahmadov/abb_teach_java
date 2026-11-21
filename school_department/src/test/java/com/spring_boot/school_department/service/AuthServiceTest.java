package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.request.ChangePasswordRequest;
import com.spring_boot.school_department.dto.request.LoginRequest;
import com.spring_boot.school_department.dto.request.RefreshRequest;
import com.spring_boot.school_department.dto.request.RegisterRequest;
import com.spring_boot.school_department.dto.response.AccountResponse;
import com.spring_boot.school_department.dto.response.AuthResponse;
import com.spring_boot.school_department.dto.response.SessionResponse;
import com.spring_boot.school_department.entity.Account;
import com.spring_boot.school_department.entity.AuthToken;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.enums.RevokeReason;
import com.spring_boot.school_department.enums.Role;
import com.spring_boot.school_department.enums.TokenType;
import com.spring_boot.school_department.exception.AlreadyExistsException;
import com.spring_boot.school_department.exception.ForbiddenException;
import com.spring_boot.school_department.exception.InvalidRequestException;
import com.spring_boot.school_department.exception.NotFoundException;
import com.spring_boot.school_department.exception.UnauthorizedException;
import com.spring_boot.school_department.mapper.AccountMapper;
import com.spring_boot.school_department.repository.AccountRepository;
import com.spring_boot.school_department.repository.AuthTokenRepository;
import com.spring_boot.school_department.security.AuthPrincipal;
import com.spring_boot.school_department.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String REFRESH_TOKEN = "refresh-token";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AuthTokenRepository authTokenRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private TokenBlacklistService blacklistService;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_usernameExists_throwsAlreadyExists() {
        when(accountRepository.existsByUsername("ali")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_USERNAME_EXISTS);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void register_emailExists_throwsAlreadyExists() {
        when(accountRepository.existsByUsername("ali")).thenReturn(false);
        when(accountRepository.existsByEmail("ali@mail.az")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_EMAIL_EXISTS);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void register_valid_savesAccountWithEncodedPasswordAndUserRole() {
        AccountResponse response = new AccountResponse(1L, "ali", "ali@mail.az", Role.USER, true, null);
        when(accountRepository.existsByUsername("ali")).thenReturn(false);
        when(accountRepository.existsByEmail("ali@mail.az")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("encoded");
        when(accountMapper.toResponse(any(Account.class))).thenReturn(response);

        AccountResponse result = authService.register(registerRequest());

        assertThat(result).isEqualTo(response);
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("ali");
        assertThat(saved.getEmail()).isEqualTo("ali@mail.az");
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void login_accountNotFound_throwsUnauthorized() {
        when(accountRepository.findByUsernameOrEmail("ali", "ali")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ali", "Secret123!")))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_BAD_CREDENTIALS);
        verify(tokenService, never()).issuePair(any(), any());
    }

    @Test
    void login_valid_returnsTokenPair() {
        Account account = account();
        AuthResponse response = authResponse();
        when(accountRepository.findByUsernameOrEmail("ali", "ali")).thenReturn(Optional.of(account));
        when(tokenService.issuePair(eq(account), anyString())).thenReturn(response);

        AuthResponse result = authService.login(new LoginRequest("ali", "Secret123!"));

        assertThat(result).isEqualTo(response);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void refresh_tokenExpired_throwsTokenExpired() {
        when(jwtService.parse(REFRESH_TOKEN)).thenThrow(new ExpiredJwtException(null, null, "expired"));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(REFRESH_TOKEN)))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TOKEN_EXPIRED);
    }

    @Test
    void refresh_tokenBadSignature_throwsTokenInvalid() {
        when(jwtService.parse(REFRESH_TOKEN)).thenThrow(new JwtException("bad signature"));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(REFRESH_TOKEN)))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
    }

    @Test
    void refresh_tokenEmpty_throwsTokenInvalid() {
        when(jwtService.parse(REFRESH_TOKEN)).thenThrow(new IllegalArgumentException("empty"));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(REFRESH_TOKEN)))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
    }

    @Test
    void refresh_accessTokenSent_throwsTypeMismatch() {
        Claims claims = mock(Claims.class);
        when(jwtService.parse(REFRESH_TOKEN)).thenReturn(claims);
        when(claims.get(JwtService.CLAIM_TYPE, String.class)).thenReturn(TokenType.ACCESS.name());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(REFRESH_TOKEN)))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TOKEN_TYPE_MISMATCH);
    }

    @Test
    void refresh_accountMissing_throwsTokenInvalid() {
        Claims claims = refreshClaims();
        when(jwtService.parse(REFRESH_TOKEN)).thenReturn(claims);
        when(accountRepository.findByUsername("ali")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(REFRESH_TOKEN)))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
    }

    @Test
    void refresh_rotatedTokenReused_revokesAllAndThrowsReused() {
        Claims claims = refreshClaims();
        when(claims.getId()).thenReturn("jti-1");
        when(jwtService.parse(REFRESH_TOKEN)).thenReturn(claims);
        when(accountRepository.findByUsername("ali")).thenReturn(Optional.of(account()));
        when(blacklistService.findReason("jti-1")).thenReturn(Optional.of(RevokeReason.REFRESH_ROTATION));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(REFRESH_TOKEN)))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_REFRESH_REUSED);
        verify(blacklistService).revokeAll(1L, RevokeReason.REFRESH_REUSE_DETECTED);
    }

    @Test
    void refresh_tokenRevokedByLogout_throwsTokenRevoked() {
        Claims claims = refreshClaims();
        when(claims.getId()).thenReturn("jti-1");
        when(jwtService.parse(REFRESH_TOKEN)).thenReturn(claims);
        when(accountRepository.findByUsername("ali")).thenReturn(Optional.of(account()));
        when(blacklistService.findReason("jti-1")).thenReturn(Optional.of(RevokeReason.LOGOUT));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(REFRESH_TOKEN)))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TOKEN_REVOKED);
        verify(blacklistService, never()).revokeAll(any(), any());
    }

    @Test
    void refresh_accountDisabled_throwsForbidden() {
        Account account = account();
        account.setEnabled(false);
        Claims claims = refreshClaims();
        when(claims.getId()).thenReturn("jti-1");
        when(jwtService.parse(REFRESH_TOKEN)).thenReturn(claims);
        when(accountRepository.findByUsername("ali")).thenReturn(Optional.of(account));
        when(blacklistService.findReason("jti-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(REFRESH_TOKEN)))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_ACCOUNT_DISABLED);
    }

    @Test
    void refresh_valid_rotatesSessionAndReturnsNewPair() {
        Account account = account();
        AuthResponse response = authResponse();
        Claims claims = refreshClaims();
        when(claims.getId()).thenReturn("jti-1");
        when(claims.get(JwtService.CLAIM_SESSION_ID, String.class)).thenReturn("session-1");
        when(jwtService.parse(REFRESH_TOKEN)).thenReturn(claims);
        when(accountRepository.findByUsername("ali")).thenReturn(Optional.of(account));
        when(blacklistService.findReason("jti-1")).thenReturn(Optional.empty());
        when(tokenService.issuePair(account, "session-1")).thenReturn(response);

        AuthResponse result = authService.refresh(new RefreshRequest(REFRESH_TOKEN));

        assertThat(result).isEqualTo(response);
        verify(blacklistService).revokeSession("session-1", RevokeReason.REFRESH_ROTATION);
    }

    @Test
    void logout_valid_revokesCurrentSession() {
        authService.logout(principal());

        verify(blacklistService).revokeSession("session-1", RevokeReason.LOGOUT);
    }

    @Test
    void logoutAll_valid_revokesAllTokens() {
        authService.logoutAll(principal());

        verify(blacklistService).revokeAll(1L, RevokeReason.LOGOUT_ALL);
    }

    @Test
    void me_accountExists_returnsAccount() {
        Account account = account();
        AccountResponse response = new AccountResponse(1L, "ali", "ali@mail.az", Role.USER, true, null);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(account)).thenReturn(response);

        AccountResponse result = authService.me(principal());

        assertThat(result).isEqualTo(response);
    }

    @Test
    void me_accountMissing_throwsNotFound() {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me(principal()))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    void sessions_noActiveTokens_returnsEmptyList() {
        when(authTokenRepository.findActive(eq(1L), eq(TokenType.REFRESH), any(Instant.class))).thenReturn(List.of());

        List<SessionResponse> result = authService.sessions(principal());

        assertThat(result).isEmpty();
    }

    @Test
    void sessions_activeTokens_marksCurrentSession() {
        AuthToken current = authToken("session-1", account());
        AuthToken other = authToken("session-2", account());
        when(authTokenRepository.findActive(eq(1L), eq(TokenType.REFRESH), any(Instant.class)))
                .thenReturn(List.of(current, other));

        List<SessionResponse> result = authService.sessions(principal());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).sessionId()).isEqualTo("session-1");
        assertThat(result.get(0).current()).isTrue();
        assertThat(result.get(1).sessionId()).isEqualTo("session-2");
        assertThat(result.get(1).current()).isFalse();
    }

    @Test
    void revokeSession_sessionMissing_throwsNotFound() {
        when(authTokenRepository.findAllBySessionIdAndRevokedFalse("session-2")).thenReturn(List.of());

        assertThatThrownBy(() -> authService.revokeSession(principal(), "session-2"))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    void revokeSession_sessionOfOtherAccount_throwsNotFound() {
        Account otherAccount = account();
        otherAccount.setId(2L);
        when(authTokenRepository.findAllBySessionIdAndRevokedFalse("session-2"))
                .thenReturn(List.of(authToken("session-2", otherAccount)));

        assertThatThrownBy(() -> authService.revokeSession(principal(), "session-2"))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
        verify(blacklistService, never()).revokeSession(any(), any());
    }

    @Test
    void revokeSession_ownSession_revokesSession() {
        when(authTokenRepository.findAllBySessionIdAndRevokedFalse("session-2"))
                .thenReturn(List.of(authToken("session-2", account())));

        authService.revokeSession(principal(), "session-2");

        verify(blacklistService).revokeSession("session-2", RevokeReason.SESSION_REVOKED);
    }

    @Test
    void changePassword_oldPasswordWrong_throwsInvalidRequest() {
        Account account = account();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(principal(), new ChangePasswordRequest("wrong", "NewSecret1!")))
                .isInstanceOf(InvalidRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_OLD_PASSWORD_WRONG);
    }

    @Test
    void changePassword_newSameAsOld_throwsInvalidRequest() {
        Account account = account();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Secret123!", "encoded")).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(principal(), new ChangePasswordRequest("Secret123!", "Secret123!")))
                .isInstanceOf(InvalidRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_PASSWORD_SAME);
    }

    @Test
    void changePassword_valid_updatesPasswordAndRevokesAllTokens() {
        Account account = account();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Secret123!", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("NewSecret1!")).thenReturn("new-encoded");

        authService.changePassword(principal(), new ChangePasswordRequest("Secret123!", "NewSecret1!"));

        assertThat(account.getPassword()).isEqualTo("new-encoded");
        assertThat(account.getPasswordChangedAt()).isNotNull();
        verify(blacklistService).revokeAll(1L, RevokeReason.PASSWORD_CHANGED);
    }

    private RegisterRequest registerRequest() {
        return new RegisterRequest("ali", "ali@mail.az", "Secret123!");
    }

    private Account account() {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("ali");
        account.setEmail("ali@mail.az");
        account.setPassword("encoded");
        account.setRole(Role.USER);
        return account;
    }

    private AuthPrincipal principal() {
        return new AuthPrincipal(1L, "ali", Role.USER, "jti-access", "session-1");
    }

    private AuthResponse authResponse() {
        return new AuthResponse("access", "refresh", 900, 604800, JwtService.BEARER);
    }

    private Claims refreshClaims() {
        Claims claims = mock(Claims.class);
        when(claims.get(JwtService.CLAIM_TYPE, String.class)).thenReturn(TokenType.REFRESH.name());
        when(claims.getSubject()).thenReturn("ali");
        return claims;
    }

    private AuthToken authToken(String sessionId, Account account) {
        AuthToken token = new AuthToken();
        token.setSessionId(sessionId);
        token.setAccount(account);
        token.setIssuedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(600));
        return token;
    }
}
