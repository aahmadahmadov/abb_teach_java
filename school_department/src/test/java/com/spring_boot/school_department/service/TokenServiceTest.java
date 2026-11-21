package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.response.AuthResponse;
import com.spring_boot.school_department.entity.Account;
import com.spring_boot.school_department.entity.AuthToken;
import com.spring_boot.school_department.enums.TokenType;
import com.spring_boot.school_department.repository.AuthTokenRepository;
import com.spring_boot.school_department.security.jwt.IssuedToken;
import com.spring_boot.school_department.security.jwt.JwtProperties;
import com.spring_boot.school_department.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthTokenRepository authTokenRepository;

    private TokenService tokenService;

    // JwtProperties record-dur, mock etmirik, real obyekt verib service-i ozumuz qururuq
    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties("secret", "school-api", Duration.ofMinutes(15), Duration.ofDays(7));
        tokenService = new TokenService(jwtService, properties, authTokenRepository);
    }

    @Test
    void issuePair_valid_savesBothTokensAndReturnsResponse() {
        Account account = new Account();
        account.setId(1L);
        Instant now = Instant.now();
        IssuedToken access = new IssuedToken("access-value", "access-jti", TokenType.ACCESS, now, now.plusSeconds(900));
        IssuedToken refresh = new IssuedToken("refresh-value", "refresh-jti", TokenType.REFRESH, now, now.plusSeconds(604800));
        when(jwtService.generate(account, TokenType.ACCESS, "session-1")).thenReturn(access);
        when(jwtService.generate(account, TokenType.REFRESH, "session-1")).thenReturn(refresh);

        AuthResponse result = tokenService.issuePair(account, "session-1");

        assertThat(result.accessToken()).isEqualTo("access-value");
        assertThat(result.refreshToken()).isEqualTo("refresh-value");
        assertThat(result.accessExpiresIn()).isEqualTo(900);
        assertThat(result.refreshExpiresIn()).isEqualTo(604800);
        assertThat(result.tokenType()).isEqualTo(JwtService.BEARER);

        ArgumentCaptor<AuthToken> captor = ArgumentCaptor.forClass(AuthToken.class);
        verify(authTokenRepository, times(2)).save(captor.capture());
        List<AuthToken> saved = captor.getAllValues();

        assertThat(saved.get(0).getJti()).isEqualTo("access-jti");
        assertThat(saved.get(0).getTokenType()).isEqualTo(TokenType.ACCESS);
        assertThat(saved.get(0).getSessionId()).isEqualTo("session-1");
        assertThat(saved.get(0).getAccount()).isEqualTo(account);
        assertThat(saved.get(0).getIssuedAt()).isEqualTo(now);
        assertThat(saved.get(0).getExpiresAt()).isEqualTo(now.plusSeconds(900));
        assertThat(saved.get(1).getJti()).isEqualTo("refresh-jti");
        assertThat(saved.get(1).getTokenType()).isEqualTo(TokenType.REFRESH);
    }
}
