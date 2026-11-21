package com.spring_boot.school_department.security.jwt;

import com.spring_boot.school_department.entity.Account;
import com.spring_boot.school_department.enums.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    public static final String BEARER = "Bearer";

    public static final String CLAIM_SESSION_ID = "sid";
    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_ROLE = "role";

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret()));
    }

    public IssuedToken generate(Account account, TokenType type, String sessionId) {

        Instant now = Instant.now();

        Duration ttl = type == TokenType.ACCESS ? props.accessTokenTtl() : props.refreshTokenTtl();

        Instant exp = now.plus(ttl);
        String jti = UUID.randomUUID().toString();

        JwtBuilder builder = Jwts.builder()
                .issuer(props.issuer())
                .subject(account.getUsername())
                .id(jti)
                .claim(CLAIM_SESSION_ID, sessionId)
                .claim(CLAIM_TYPE, type.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp));

        if (type == TokenType.ACCESS) {
            builder.claim(CLAIM_ROLE, account.getRole().name());
        }

        String value = builder.signWith(key).compact();

        return new IssuedToken(value, jti, type, now, exp);
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
