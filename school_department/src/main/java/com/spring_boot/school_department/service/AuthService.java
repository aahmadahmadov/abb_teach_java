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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AccountRepository accountRepository;
    private final AuthTokenRepository authTokenRepository;
    private final TokenService tokenService;
    private final TokenBlacklistService blacklistService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final AccountMapper accountMapper;

    @Transactional(propagation = Propagation.REQUIRED)
    public AccountResponse register(RegisterRequest request) {

        if (accountRepository.existsByUsername(request.username())) {
            throw new AlreadyExistsException(ErrorCode.ACCOUNT_USERNAME_EXISTS, request.username());
        }

        if (accountRepository.existsByEmail(request.email())) {
            throw new AlreadyExistsException(ErrorCode.ACCOUNT_EMAIL_EXISTS, request.email());
        }

        Account account = new Account();
        account.setUsername(request.username());
        account.setEmail(request.email());

        account.setPassword(passwordEncoder.encode(request.password()));
        account.setRole(Role.USER);

        accountRepository.save(account);

        return accountMapper.toResponse(account);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.login(), request.password()));

        Account account = accountRepository.findByUsernameOrEmail(request.login(), request.login())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.AUTH_BAD_CREDENTIALS));

        String sessionId = UUID.randomUUID().toString();

        return tokenService.issuePair(account, sessionId);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse refresh(RefreshRequest request) {

        Claims claims = parseRefreshToken(request.refreshToken());

        String type = claims.get(JwtService.CLAIM_TYPE, String.class);

        if (!TokenType.REFRESH.name().equals(type)) {
            throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_TYPE_MISMATCH);
        }

        Account account = accountRepository.findByUsername(claims.getSubject())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID));

        Optional<RevokeReason> reason = blacklistService.findReason(claims.getId());

        if (reason.isPresent()) {
            if (reason.get() == RevokeReason.REFRESH_ROTATION) {
                blacklistService.revokeAll(account.getId(), RevokeReason.REFRESH_REUSE_DETECTED);
                throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_REUSED);
            }
            throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_REVOKED);
        }

        if (!account.isEnabled()) {
            throw new ForbiddenException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }

        String sessionId = claims.get(JwtService.CLAIM_SESSION_ID, String.class);
        blacklistService.revokeSession(sessionId, RevokeReason.REFRESH_ROTATION);

        return tokenService.issuePair(account, sessionId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void logout(AuthPrincipal principal) {
        blacklistService.revokeSession(principal.sessionId(), RevokeReason.LOGOUT);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void logoutAll(AuthPrincipal principal) {
        blacklistService.revokeAll(principal.accountId(), RevokeReason.LOGOUT_ALL);
    }

    public AccountResponse me(AuthPrincipal principal) {
        return accountMapper.toResponse(findAccount(principal.accountId()));
    }

    public List<SessionResponse> sessions(AuthPrincipal principal) {
        List<AuthToken> tokens = authTokenRepository.findActive(principal.accountId(), TokenType.REFRESH, Instant.now());

        List<SessionResponse> sessions = new ArrayList<>();

        for (AuthToken token: tokens) {
            boolean current = token.getSessionId().equals(principal.sessionId());

            sessions.add(new SessionResponse(token.getSessionId(), token.getIssuedAt(), token.getExpiresAt(), current));
        }

        return sessions;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void revokeSession(AuthPrincipal principal, String sessionId) {
        List<AuthToken> tokens = authTokenRepository.findAllBySessionIdAndRevokedFalse(sessionId);

        if (tokens.isEmpty() || !tokens.get(0).getAccount().getId().equals(principal.accountId())) {
            throw new NotFoundException(ErrorCode.SESSION_NOT_FOUND, sessionId);
        }

        blacklistService.revokeSession(sessionId, RevokeReason.SESSION_REVOKED);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void changePassword(AuthPrincipal principal, ChangePasswordRequest request) {

        Account account = findAccount(principal.accountId());

        if (!passwordEncoder.matches(request.oldPassword(), account.getPassword())) {
            throw new InvalidRequestException(ErrorCode.AUTH_OLD_PASSWORD_WRONG);
        }

        if (request.newPassword().equals(request.oldPassword())) {
            throw new InvalidRequestException(ErrorCode.AUTH_PASSWORD_SAME);
        }

        account.setPassword(passwordEncoder.encode(request.newPassword()));
        account.setPasswordChangedAt(Instant.now());

        blacklistService.revokeAll(account.getId(), RevokeReason.PASSWORD_CHANGED);
    }

    private Claims parseRefreshToken(String refreshToken) {

        try {
            return jwtService.parse(refreshToken);
        } catch (ExpiredJwtException e) { // ExpiredJwtException extend edir JwtException-den, ona gore evvel tuturuq
            throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    private Account findAccount(Long id) {

        return accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND, id));
    }
}
