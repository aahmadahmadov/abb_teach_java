package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.response.AuthResponse;
import com.spring_boot.school_department.entity.Account;
import com.spring_boot.school_department.entity.AuthToken;
import com.spring_boot.school_department.enums.TokenType;
import com.spring_boot.school_department.repository.AuthTokenRepository;
import com.spring_boot.school_department.security.jwt.IssuedToken;
import com.spring_boot.school_department.security.jwt.JwtProperties;
import com.spring_boot.school_department.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenService {

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthTokenRepository authTokenRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public AuthResponse issuePair(Account account, String sessionId) {
        IssuedToken access = jwtService.generate(account, TokenType.ACCESS, sessionId);
        IssuedToken refresh = jwtService.generate(account, TokenType.REFRESH, sessionId);

        saveToken(account, sessionId, access);
        saveToken(account, sessionId, refresh);

        return new AuthResponse(
                access.value(),
                refresh.value(),
                jwtProperties.accessTokenTtl().toSeconds(),
                jwtProperties.refreshTokenTtl().toSeconds(),
                JwtService.BEARER
        );
    }

    private void saveToken(Account account, String sessionId, IssuedToken issued) {
        AuthToken token = new AuthToken();
        token.setJti(issued.jti());
        token.setSessionId(sessionId);
        token.setTokenType(issued.type());
        token.setAccount(account);
        token.setIssuedAt(issued.issuedAt());
        token.setExpiresAt(issued.expiresAt());
        authTokenRepository.save(token);
    }
}
