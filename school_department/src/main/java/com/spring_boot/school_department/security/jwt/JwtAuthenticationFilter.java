package com.spring_boot.school_department.security.jwt;

import com.spring_boot.school_department.entity.Account;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.enums.TokenType;
import com.spring_boot.school_department.exception.BaseException;
import com.spring_boot.school_department.exception.ForbiddenException;
import com.spring_boot.school_department.exception.UnauthorizedException;
import com.spring_boot.school_department.repository.AccountRepository;
import com.spring_boot.school_department.security.AuthPrincipal;
import com.spring_boot.school_department.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = JwtService.BEARER + " ";
    private static final String MDC_USERNAME = "username";

    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;
    private final AccountRepository accountRepository;
    private final HandlerExceptionResolver resolver;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   TokenBlacklistService blacklistService,
                                   AccountRepository accountRepository,
                                   @Qualifier("handlerExceptionResolver")
                                   HandlerExceptionResolver resolver) {

        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
        this.accountRepository = accountRepository;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parse(header.substring(BEARER_PREFIX.length()));

            String type = claims.get(JwtService.CLAIM_TYPE, String.class);

            if (!TokenType.ACCESS.name().equals(type)) {
                throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_TYPE_MISMATCH);
            }

            if (blacklistService.isBlacklisted(claims.getId())) {
                throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_REVOKED);
            }

            Account account = accountRepository.findByUsername(claims.getSubject())
                    .orElseThrow(() -> new ForbiddenException(ErrorCode.AUTH_ACCOUNT_DISABLED));

            if (!account.isEnabled()) {
                throw new ForbiddenException(ErrorCode.AUTH_ACCOUNT_DISABLED);
            }

            AuthPrincipal principal = new AuthPrincipal(account.getId(), account.getUsername(), account.getRole(),
                    claims.getId(), claims.get(JwtService.CLAIM_SESSION_ID, String.class));

            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()));

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, authorities));

            MDC.put(MDC_USERNAME, claims.getSubject());
        } catch (ExpiredJwtException e) {
            fail(request, response, new UnauthorizedException(ErrorCode.AUTH_TOKEN_EXPIRED));
            return;
        } catch (JwtException | IllegalArgumentException e) {
            fail(request, response, new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID));
            return;
        } catch (BaseException e) {
            fail(request, response, e);
            return;
        }

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_USERNAME);
        }
    }

    private void fail(HttpServletRequest req, HttpServletResponse res, Exception ex) {
        SecurityContextHolder.clearContext();
        resolver.resolveException(req, res, null, ex);
    }
}
