**Praktiki Tapşırıq**

Spring Security, JWT (access + refresh) və DB səviyyəsində Blacklist ilə Stateful Token İdarəetməsi

| **Sahə**               | **Doldurun**                                                   |
|------------------------|----------------------------------------------------------------|
| Tələbənin adı, soyadı: |                                                                |
| Qrup:                  |                                                                |
| Təhvil tarixi:         |                                                                |
| Layihənin adı:         | school-api (paket: `az.iktex.organization.structure.testders`) |

# 1. Tapşırığın məqsədi

Əvvəlki iki tapşırıqda qurduğunuz layihəyə (entity əlaqələri, ErrorCode enum, i18n bundle, GlobalExceptionHandler, traceId, Swagger) indi təhlükəsizlik qatı əlavə edəcəksiniz. API-yə yalnız qeydiyyatdan keçmiş və daxil olmuş istifadəçilər çata biləcək, hər rol isə yalnız ona icazə verilən əməliyyatları edə biləcək.

Tapşırıq bitəndə API-nizdə bunlar olacaq:

- **Spring Security** ilə qorunan endpoint-lər (yalnız register, login, refresh və Swagger açıqdır)

- BCrypt ilə saxlanan şifrələr və **3 rol**: ADMIN, MANAGER, USER

- **JWT access token** (15 dəqiqə) və **refresh token** (7 gün)

- Verilmiş hər tokenin bazada qeydi — **stateful** token idarəetməsi

- Ləğv edilmiş tokenlər üçün bazada **blacklist** cədvəli: logout edən kimi token işləməz olur

- Refresh token **rotation** və köhnə refresh-in təkrar istifadəsinin aşkarlanması (**reuse detection**)

- 401 və 403 xətalarının da əvvəlki **ErrorResponse** formatında, 3 dildə və traceId ilə qaytarılması

> **Əsas qayda**
>
> Tokenin imzası və müddəti düzgün olsa belə, **jti-si blacklist-dədirsə token etibarsızdır**. Bu yoxlama hər qorunan sorğuda bazada aparılır.
>
> Əvvəlki tapşırığın qaydası qüvvədədir: yeni xətaların hamısı da `ErrorCode` enum-undan gəlir, mesaj mətni bundle faylındadır.

# 2. Konsepsiya: niyə stateful JWT?

Klassik (stateless) JWT-də server heç nə saxlamır: token imzalıdır və müddəti bitənə qədər etibarlıdır. Bu sürətlidir, amma bir böyük problemi var — istifadəçi logout etsə, şifrəsini dəyişsə və ya admin onu bloklasa belə, token müddəti bitənə qədər işləməyə davam edir.

| **Xüsusiyyət**     | **Stateless JWT**                              | **Stateful JWT (bu tapşırıq)**                                                  |
|--------------------|------------------------------------------------|---------------------------------------------------------------------------------|
| Server nə saxlayır | Heç nə                                         | Verilmiş tokenlərin qeydi (`auth_tokens`) və ləğv edilənlər (`token_blacklist`) |
| Logout             | Client tokeni silir, token işləməyə davam edir | Token dərhal blacklist-ə düşür, növbəti sorğu 401                               |
| Şifrə dəyişəndə    | Köhnə tokenlər işləyir                         | Hesabın bütün tokenləri ləğv olunur                                             |
| Aktiv sessiyalar   | Görmək mümkün deyil                            | `GET /api/auth/sessions` — hansı cihazlardan daxil olunub                       |
| Qiyməti            | Bazaya sorğu yoxdur                            | Hər sorğuda 1 indeksli sorğu (`existsByJti`)                                    |

> **Stateful ≠ HttpSession**
>
> Bu tapşırıqda `SessionCreationPolicy.STATELESS` istifadə olunur — server HttpSession yaratmır, JSESSIONID cookie-si yoxdur.
>
> "Stateful" o deməkdir ki, tokenlərin vəziyyəti (aktiv / ləğv edilib) **verilənlər bazasında** saxlanır. Ona görə tətbiqin bir neçə nüsxəsi eyni bazaya qoşulanda da logout hamısında dərhal təsir edir.

Blacklist-dəki sətir yalnız token öz-özünə bitənə qədər lazımdır. Müddəti keçmiş token onsuz da yoxlamadan keçmir, ona görə planlaşdırılmış job (`@Scheduled`) belə sətirləri silir və cədvəl sonsuz böyümür.

Bir sorğunun yolu:

``` text
HTTP sorgu
  -> TraceIdFilter            (traceId -> MDC, evvelki tapsiriq)
  -> SecurityFilterChain
       -> JwtAuthenticationFilter
            1. Authorization: Bearer ... var?
            2. imza + issuer + exp      (JwtService.parse)
            3. type == ACCESS?
            4. jti blacklist-de deyil?   (DB: token_blacklist)
            5. hesab movcud ve enabled?  (DB: accounts)
            6. SecurityContext <- AuthPrincipal + ROLE_xxx
       -> AuthorizationFilter    (URL qaydalari)
  -> Controller -> @PreAuthorize -> Service
```

# 3. İstifadə olunacaq texnologiyalar

| **Texnologiya**                 | **Qeyd**                                                                              |
|---------------------------------|---------------------------------------------------------------------------------------|
| spring-boot-starter-security    | Spring Security 7 (Boot 3-də 6.x — API eynidir, lambda DSL)                           |
| io.jsonwebtoken:jjwt-api 0.12.6 | JWT yaratmaq və yoxlamaq                                                              |
| jjwt-impl, jjwt-jackson 0.12.6  | `runtimeOnly` kimi əlavə olunur                                                       |
| Mövcud infrastruktur            | ErrorCode, i18n bundle, GlobalExceptionHandler, TraceIdFilter, LoggingAspect, Swagger |

`build.gradle`-a əlavə edin:

``` groovy
// Security
implementation 'org.springframework.boot:spring-boot-starter-security'

// JWT
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
```

`application.yaml`-a əlavə edin:

``` yaml
app:
  jwt:
    secret: ${JWT_SECRET}               # Base64, en az 256 bit (32 bayt)
    issuer: school-api
    access-token-ttl: 15m
    refresh-token-ttl: 7d
  security:
    token-cleanup-cron: "0 0 * * * *"   # her saat
    admin:
      username: admin
      email: admin@school.az
      password: ${ADMIN_PASSWORD}

logging:
  pattern:
    level: "%5p [%X{traceId:-}] [%X{username:-anonymous}]"
```

> **Diqqət!**
>
> Secret və admin şifrəsi kodda və git-də **olmamalıdır**. Lokalda mühit dəyişəni (environment variable) və ya `.gitignore`-a əlavə olunmuş `.env` faylı istifadə edin.
>
> Secret yaratmaq: `openssl rand -base64 32`. Secret qısa olsa jjwt `WeakKeyException` atacaq.

# 4. Yaradılacaq paket strukturu

``` text
az.iktex.organization.structure.testders
├── security/
│   ├── SecurityConfig.java              (filter chain, PasswordEncoder, AuthManager)
│   ├── AuthPrincipal.java               (accountId, username, role, jti, sid)
│   ├── CustomUserDetailsService.java
│   ├── RestAuthenticationEntryPoint.java  (401)
│   ├── RestAccessDeniedHandler.java       (403)
│   ├── AdminSeeder.java                 (ilk ADMIN hesabi)
│   └── jwt/
│       ├── JwtProperties.java           (@ConfigurationProperties)
│       ├── IssuedToken.java             (record: value, jti, type, iat, exp)
│       ├── JwtService.java
│       └── JwtAuthenticationFilter.java
├── entity/
│   ├── Account.java
│   ├── AuthToken.java
│   ├── TokenBlacklist.java
│   └── converter/RoleConverter.java
├── enums/
│   ├── Role.java                        (ADMIN=1, MANAGER=2, USER=3)
│   ├── TokenType.java                   (ACCESS, REFRESH)
│   └── RevokeReason.java
├── repository/
│   ├── AccountRepository.java
│   ├── AuthTokenRepository.java
│   └── TokenBlacklistRepository.java
├── service/
│   ├── AuthService.java                 (register, login, refresh, logout, sifre)
│   ├── TokenService.java                (token cutu yaratmaq + DB qeydi)
│   ├── TokenBlacklistService.java       (blacklist, revokeSession, revokeAll)
│   └── AccountService.java              (admin emeliyyatlari)
├── scheduler/
│   └── TokenCleanupJob.java
├── controller/
│   ├── AuthController.java
│   └── AdminAccountController.java
├── exception/
│   ├── UnauthorizedException.java       (BaseException-dan)
│   └── ForbiddenException.java
├── validation/
│   ├── StrongPassword.java
│   └── StrongPasswordValidator.java
├── request/   RegisterRequest, LoginRequest, RefreshRequest,
│              ChangePasswordRequest, ChangeRoleRequest
└── response/  AuthResponse, AccountResponse, SessionResponse
```

# 5. Tapşırıq 1 — Hesablar, rollar və SecurityConfig (15 bal)

## 5.1. Role enum-u və Account entity-si

`Role` enum-unu `Gender`-dəki kimi qurun: `Integer dbCode`, `findByDbCode(...)` və `RoleConverter` (`AttributeConverter<Role, Integer>`). Bazada `role_id` sütununda 1/2/3 saxlanır. Mövcud `User` (User ↔ Passport) entity-sinə toxunmayın — `Account` yalnız giriş üçün olan ayrıca cədvəldir.

**Account** (cədvəl: `accounts`)

| **Sahə**          | **Tip** | **Sütun / Məhdudiyyət**                                          |
|-------------------|---------|------------------------------------------------------------------|
| id                | Long    | PK, identity                                                     |
| username          | String  | not null, unique (`uk_account_username`), length 50              |
| email             | String  | not null, unique (`uk_account_email`), length 100                |
| password          | String  | not null, length 100 — **yalnız BCrypt hash**                    |
| role              | Role    | `role_id`, not null, `@Convert(converter = RoleConverter.class)` |
| enabled           | boolean | not null, default true                                           |
| passwordChangedAt | Instant | `password_changed_at`                                            |
| createdAt         | Instant | `created_at`, not null, `@CreationTimestamp`                     |

## 5.2. SecurityConfig

1.  `security/SecurityConfig` yaradın: `@Configuration`, `@EnableWebSecurity`, `@EnableMethodSecurity`.

2.  CSRF, `formLogin` və `httpBasic` söndürülsün, session policy — `STATELESS`.

3.  `PasswordEncoder` bean-i — `new BCryptPasswordEncoder(12)`.

4.  `AuthenticationManager`-i `AuthenticationConfiguration`-dan bean kimi çıxarın (login üçün lazımdır).

5.  `CustomUserDetailsService`: login sahəsində həm username, həm email qəbul etsin (`findByUsernameOrEmail`). Tapılmasa `UsernameNotFoundException`.

6.  `AdminSeeder` (`CommandLineRunner`): bazada ADMIN yoxdursa `app.security.admin.*` dəyərləri ilə yaratsın. Şifrə loga yazılmamalıdır.

Skelet:

``` java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_URLS = {
            "/api/auth/register", "/api/auth/login", "/api/auth/refresh",
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error"
    };

    private final JwtAuthenticationFilter jwtFilter;
    private final RestAuthenticationEntryPoint entryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // TODO: niye CSRF sondurulur? Serh kimi yazin
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                    .authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_URLS).permitAll()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    // TODO: qalan qaydalar 9-cu bolmedeki cedvele gore
                    .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // Filter @Component oldugu ucun Boot onu umumi servlet filter kimi de
    // qeyde alir.
    // Yalniz security chain daxilinde islesin deye avtomatik qeydiyyati sondururuk.
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        var reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

> **Sual (cavabı kodda şərh kimi yazın)**
>
> Niyə CSRF söndürülür? İpucu: CSRF hücumu brauzerin cookie-ni **avtomatik** göndərməsinə əsaslanır. Bizim token isə `Authorization` header-ində gedir — brauzer onu özü əlavə etmir.

## 5.3. Qeydiyyat

`POST /api/auth/register` → `RegisterRequest(username, email, password)`. Yeni hesab **həmişə USER rolu** ilə yaranır — client rol göndərə bilməz.

- username: `@NotBlank`, `@Size(min = 3, max = 50)`, `@Pattern` — yalnız hərf, rəqəm, nöqtə və alt xətt.

- email: `@NotBlank`, `@Email`.

- password: custom `@StrongPassword` — ən az 8 simvol, ən az 1 böyük hərf, 1 kiçik hərf və 1 rəqəm.

- Mesajlar `ValidationMessage.Keys`-dən gəlsin. Yeni açarlar: `USERNAME_PATTERN`, `PASSWORD_STRONG` — 3 bundle faylına əlavə edin.

- Cavab: **201** + `AccountResponse(id, username, email, role, enabled, createdAt)`. Şifrə (hətta hash) heç bir response-da olmamalıdır.

# 6. Tapşırıq 2 — JwtService və JwtAuthenticationFilter (20 bal)

## 6.1. Tokenin tərkibi (claim-lər)

| **Claim**     | **Access** | **Refresh** | **Təsvir**                                                                |
|---------------|------------|-------------|---------------------------------------------------------------------------|
| sub           | ✔          | ✔           | username                                                                  |
| jti           | ✔          | ✔           | UUID — blacklist bu dəyərlə işləyir                                       |
| sid           | ✔          | ✔           | session id — bir login-də verilən access və refresh eyni `sid`-ə malikdir |
| type          | ACCESS     | REFRESH     | Tokenin növü — refresh tokenlə API-yə müraciət etmək olmaz                |
| role          | ✔          | —           | ADMIN / MANAGER / USER (məlumat üçün; yoxlamada rol bazadan götürülür)    |
| iss, iat, exp | ✔          | ✔           | Issuer, verilmə və bitmə vaxtı                                            |

> **Diqqət!**
>
> JWT **şifrələnmir**, yalnız imzalanır — payload-ı jwt.io-da hər kəs oxuya bilər. Tokenə şifrə, email və ya digər şəxsi məlumat yazmayın.

## 6.2. JwtProperties və JwtService

1.  `JwtProperties` record-unu `@ConfigurationProperties(prefix = "app.jwt")` ilə yaradın, TTL-lər `Duration` tipində olsun. Main sinfə `@ConfigurationPropertiesScan` əlavə edin.

2.  `generate(Account, TokenType, sessionId)` hər dəfə yeni `jti` (UUID) yaradır və `IssuedToken` qaytarır — `jti` və `expiresAt` bazaya yazmaq üçün lazım olacaq.

3.  `parse(String token)` imzanı, issuer-i və müddəti yoxlayıb `Claims` qaytarır. Uğursuz olanda jjwt-nin öz exception-larını atır (onları filter tutacaq).

4.  Secret-dən yaranan `SecretKey`-i hər çağırışda yox, bir dəfə (konstruktorda) yaradın.

``` java
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer,
                            Duration accessTokenTtl, Duration refreshTokenTtl) {}

public record IssuedToken(String value, String jti, TokenType type,
                          Instant issuedAt, Instant expiresAt) {}

@Service
public class JwtService {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret()));
    }

    public IssuedToken generate(Account account, TokenType type, String sessionId) {
        Instant now = Instant.now();
        Duration ttl = type == TokenType.ACCESS
                ? props.accessTokenTtl()
                : props.refreshTokenTtl();
        Instant exp = now.plus(ttl);
        String jti = UUID.randomUUID().toString();

        JwtBuilder builder = Jwts.builder()
                .issuer(props.issuer())
                .subject(account.getUsername())
                .id(jti)
                .claim("sid", sessionId)
                .claim("type", type.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp));
        if (type == TokenType.ACCESS) {
            builder.claim("role", account.getRole().name());
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
```

## 6.3. JwtAuthenticationFilter

Filter `OncePerRequestFilter`-dən miras alır və `UsernamePasswordAuthenticationFilter`-dən əvvəl qoşulur. Hər sorğuda bu ardıcıllıqla yoxlayır:

| **\#** | **Yoxlama**                                      | **Uğursuz olanda**                                                                          |
|--------|--------------------------------------------------|---------------------------------------------------------------------------------------------|
| 1      | `Authorization` header-i `Bearer ` ilə başlayır? | Filter heç nə etmir, chain davam edir. URL qorunursa EntryPoint 401 **ERR-2700** qaytaracaq |
| 2      | İmza və issuer düzgündür?                        | 401 **ERR-2703** AUTH_TOKEN_INVALID                                                         |
| 3      | Müddəti bitməyib? (`ExpiredJwtException`)        | 401 **ERR-2702** AUTH_TOKEN_EXPIRED                                                         |
| 4      | `type` claim-i ACCESS-dir?                       | 401 **ERR-2705** AUTH_TOKEN_TYPE_MISMATCH                                                   |
| 5      | `jti` blacklist-də deyil? (DB)                   | 401 **ERR-2704** AUTH_TOKEN_REVOKED                                                         |
| 6      | Hesab mövcuddur və `enabled = true`?             | 403 **ERR-2708** AUTH_ACCOUNT_DISABLED                                                      |
| 7      | Hamısı uğurludur                                 | `SecurityContext`-ə `AuthPrincipal` + `ROLE_xxx` yazılır, MDC-yə `username`                 |

Skelet:

``` java
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

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
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            Claims claims = jwtService.parse(header.substring(BEARER.length()));
            // TODO: addim 4, 5, 6
            //       ugursuz olanda: throw new UnauthorizedException(...)
            // TODO: AuthPrincipal + authorities -> SecurityContextHolder
            MDC.put("username", claims.getSubject());
        } catch (ExpiredJwtException e) {     // JwtException-dan EVVEL
            fail(request, response,
                    new UnauthorizedException(ErrorCode.AUTH_TOKEN_EXPIRED));
            return;
        } catch (JwtException | IllegalArgumentException e) {
            fail(request, response,
                    new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID));
            return;
        } catch (BaseException e) {
            fail(request, response, e);
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("username");
        }
    }

    private void fail(HttpServletRequest req, HttpServletResponse res,
                      Exception ex) {
        SecurityContextHolder.clearContext();
        resolver.resolveException(req, res, null, ex); // -> GlobalExceptionHandler
    }
}
```

> **Diqqət!**
>
> Filter-də atılan exception `@RestControllerAdvice`-a **çatmır** — o yalnız controller-dən gələnləri tutur. Ona görə xəta `HandlerExceptionResolver`-ə ötürülür, o da `GlobalExceptionHandler`-i çağırır və cavab eyni formatda olur.
>
> Rolu tokendəki `role` claim-indən yox, **bazadakı** `Account`-dan götürün — stateful yanaşmanın üstünlüyü budur.
>
> `ExpiredJwtException` `JwtException`-ın alt sinfidir — catch ardıcıllığı səhv olsa, bitmiş token ERR-2703 kimi qayıdacaq.

# 7. Tapşırıq 3 — Stateful saxlama və DB blacklist (25 bal)

## 7.1. AuthToken entity-si (cədvəl: auth_tokens)

Verilmiş **hər** token (access və refresh) burada qeyd olunur. Bu cədvəl sessiyaların siyahısı, toplu ləğvetmə (logout-all) və audit üçündür.

| **Sahə**     | **Tip**      | **Sütun / Məhdudiyyət**                                                                 |
|--------------|--------------|-----------------------------------------------------------------------------------------|
| id           | Long         | PK, identity                                                                            |
| jti          | String       | not null, unique (`uk_auth_token_jti`), length 36                                       |
| sessionId    | String       | `session_id`, not null, length 36, index `idx_auth_token_session`                       |
| tokenType    | TokenType    | `token_type`, `@Enumerated(STRING)`, length 10                                          |
| account      | Account      | `@ManyToOne(fetch = LAZY, optional = false)`, `account_id`, FK: `fk_auth_token_account` |
| issuedAt     | Instant      | `issued_at`, not null                                                                   |
| expiresAt    | Instant      | `expires_at`, not null                                                                  |
| revoked      | boolean      | not null, default false                                                                 |
| revokedAt    | Instant      | `revoked_at`                                                                            |
| revokeReason | RevokeReason | `revoke_reason`, `@Enumerated(STRING)`, length 30                                       |

Cədvəl səviyyəsində index: `idx_auth_token_account_revoked (account_id, revoked)`.

> **Diqqət!**
>
> Tokenin özünü (string-i) bazada **saxlamayın** — yalnız `jti`. Baza sızsa belə, oradan işlək token götürmək mümkün olmamalıdır.

## 7.2. TokenBlacklist entity-si (cədvəl: token_blacklist)

Müddəti bitməmiş, amma ləğv edilmiş tokenlər. Filter hər sorğuda **yalnız bu cədvələ** baxır — ona görə kiçik və indeksli olmalıdır.

| **Sahə**      | **Tip**      | **Sütun / Məhdudiyyət**                                  |
|---------------|--------------|----------------------------------------------------------|
| id            | Long         | PK, identity                                             |
| jti           | String       | not null, unique (`uk_blacklist_jti`), length 36         |
| accountId     | Long         | `account_id`, not null, FK: `fk_blacklist_account`       |
| tokenType     | TokenType    | `token_type`, `@Enumerated(STRING)`                      |
| reason        | RevokeReason | not null, `@Enumerated(STRING)`, length 30               |
| expiresAt     | Instant      | `expires_at`, not null, index `idx_blacklist_expires_at` |
| blacklistedAt | Instant      | `blacklisted_at`, not null, `@CreationTimestamp`         |

## 7.3. RevokeReason enum-u

| **Dəyər**              | **Nə vaxt istifadə olunur**                        |
|------------------------|----------------------------------------------------|
| LOGOUT                 | `POST /api/auth/logout`                            |
| LOGOUT_ALL             | `POST /api/auth/logout-all`                        |
| SESSION_REVOKED        | `DELETE /api/auth/sessions/{sessionId}`            |
| REFRESH_ROTATION       | Refresh zamanı köhnə access + refresh cütü         |
| REFRESH_REUSE_DETECTED | Artıq istifadə olunmuş refresh token yenidən gəldi |
| PASSWORD_CHANGED       | İstifadəçi şifrəsini dəyişdi                       |
| ACCOUNT_DISABLED       | Admin hesabı söndürdü                              |
| ADMIN_REVOKE           | Admin tokenləri ləğv etdi və ya rolu dəyişdi       |

## 7.4. TokenBlacklistService

1.  `blacklist(AuthToken token, RevokeReason reason)`: `auth_tokens`-də `revoked = true`, `revokedAt`, `revokeReason` yazılır və `token_blacklist`-ə sətir əlavə olunur.

2.  Metod **idempotent** olmalıdır: token artıq blacklist-dədirsə heç nə etməsin.

3.  Müddəti artıq keçmiş tokeni blacklist-ə yazmayın — o onsuz da işləmir.

4.  `revokeSession(sessionId, reason)` — sessiyanın bütün aktiv tokenləri; `revokeAll(accountId, reason)` — hesabın bütün aktiv tokenləri.

5.  `isBlacklisted(jti)` — filter bunu çağırır (`existsByJti`, unique index sayəsində sürətli).

``` java
public interface TokenBlacklistRepository
        extends JpaRepository<TokenBlacklist, Long> {

    boolean existsByJti(String jti);

    Optional<TokenBlacklist> findByJti(String jti);

    long countByAccountId(Long accountId);

    @Modifying
    @Query("delete from TokenBlacklist b where b.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByJti(String jti);

    List<AuthToken> findAllBySessionIdAndRevokedFalse(String sessionId);

    List<AuthToken> findAllByAccountIdAndRevokedFalse(Long accountId);

    // Aktiv sessiya = revoked olmayan ve muddeti bitmeyen REFRESH token
    @Query("""
           select t from AuthToken t
           where t.account.id = :accountId
             and t.tokenType = :type
             and t.revoked = false
             and t.expiresAt > :now
           order by t.issuedAt desc
           """)
    List<AuthToken> findActive(@Param("accountId") Long accountId,
                               @Param("type") TokenType type,
                               @Param("now") Instant now);

    @Modifying
    @Query("delete from AuthToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
```

> **Diqqət!**
>
> İki paralel logout eyni `jti`-ni yazmağa çalışsa `uk_blacklist_jti` constraint-i `DataIntegrityViolationException` atacaq. Bu halı tutub udun (DEBUG log) — token onsuz da blacklist-dədir. Bu exception istifadəçiyə 409 kimi getməməlidir.

## 7.5. Təmizləmə job-u

1.  Main sinfə və ya ayrıca config-ə `@EnableScheduling` əlavə edin.

2.  Cron ifadəsi yaml-dan oxunsun: `app.security.token-cleanup-cron`.

3.  Həm `token_blacklist`-dən, həm `auth_tokens`-dən `expires_at < now` olan sətirlər silinsin, sayı INFO ilə loglansın.

4.  Job HTTP sorğusu olmadığı üçün traceId-siz işləyir. Başlanğıcda MDC-yə `traceId = "cleanup-" + UUID` qoyun, `finally`-də silin.

``` java
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupJob {

    private final TokenBlacklistRepository blacklistRepository;
    private final AuthTokenRepository authTokenRepository;

    @Transactional
    @Scheduled(cron = "${app.security.token-cleanup-cron}")
    public void cleanup() {
        // TODO: MDC traceId
        Instant now = Instant.now();
        int blacklist = blacklistRepository.deleteExpired(now);
        int tokens = authTokenRepository.deleteExpired(now);
        log.info("Token cleanup: token_blacklist={}, auth_tokens={} rows deleted",
                blacklist, tokens);
    }
}
```

**Bonus (+5):** `isBlacklisted(jti)` nəticəsini Caffeine cache-də saxlayın (TTL 1 dəqiqə). Token blacklist-ə düşəndə cache-də həmin açarı yeniləyin ki, logout dərhal təsir etsin. Niyə "olmayan" nəticəni uzun müddət cache-ləmək təhlükəlidir — README-də izah edin.

# 8. Tapşırıq 4 — Auth endpoint-ləri, rotation və sessiyalar (15 bal)

## 8.1. Endpoint-lər (/api/auth)

| **Metod** | **URL**                        | **Giriş** | **Təsvir**                                                                                 |
|-----------|--------------------------------|-----------|--------------------------------------------------------------------------------------------|
| POST      | /api/auth/register             | açıq      | Qeydiyyat, 201 + AccountResponse                                                           |
| POST      | /api/auth/login                | açıq      | `{login, password}` → access + refresh. Yeni `sid`, `auth_tokens`-ə 2 sətir                |
| POST      | /api/auth/refresh              | açıq      | `{refreshToken}` → yeni cüt (eyni `sid`), köhnə cüt blacklist-ə (REFRESH_ROTATION)         |
| POST      | /api/auth/logout               | token     | Cari sessiya (`sid`) ləğv olunur, 204                                                      |
| POST      | /api/auth/logout-all           | token     | Hesabın bütün sessiyaları ləğv olunur, 204                                                 |
| GET       | /api/auth/me                   | token     | Cari hesab — AccountResponse                                                               |
| GET       | /api/auth/sessions             | token     | Aktiv sessiyalar: `sessionId`, `loginAt`, `expiresAt`, `current`                           |
| DELETE    | /api/auth/sessions/{sessionId} | token     | Bir sessiyanı ləğv et (başqa cihazdan çıxış), 204. Başqasının sessiyası → 404 **ERR-2716** |
| PUT       | /api/auth/password             | token     | `{oldPassword, newPassword}` → bütün tokenlər ləğv olunur (PASSWORD_CHANGED), 204          |

Login cavabı (`AuthResponse`):

``` json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "accessExpiresIn": 900,
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshExpiresIn": 604800
}
```

1.  Login `AuthenticationManager.authenticate(new UsernamePasswordAuthenticationToken(login, password))` ilə olsun — şifrəni əl ilə müqayisə etməyin.

2.  Token cütünü yaradan və `auth_tokens`-ə yazan məntiq bir yerdə olsun: `TokenService.issuePair(account, sessionId)`. Login və refresh onu çağırır.

3.  Logout-da tokeni yenidən parse etməyin: controller-də `@AuthenticationPrincipal AuthPrincipal principal` alın — filter artıq `jti` və `sessionId`-ni ora yazıb.

4.  `/sessions` cavabında `current = true` cari sorğunun `sessionId`-si ilə üst-üstə düşən sessiyadır.

5.  Şifrə dəyişmə: köhnə şifrə səhvdirsə **ERR-2713**, yeni şifrə köhnə ilə eynidirsə **ERR-2714**. Uğurlu olanda `passwordChangedAt` yenilənir və `revokeAll(PASSWORD_CHANGED)`.

## 8.2. Refresh rotation və reuse detection

Hər refresh-də köhnə refresh token blacklist-ə düşür və yenisi verilir. Əgər blacklist-də `REFRESH_ROTATION` səbəbi ilə olan refresh token **yenidən** gəlirsə, deməli kimsə köhnə tokeni ələ keçirib istifadə edir. Belə halda kimin haqlı olduğunu bilmək mümkün olmadığı üçün hesabın **bütün** tokenləri ləğv olunur.

``` text
refresh(refreshToken):
    claims = jwtService.parse(refreshToken)   // ERR-2702 / ERR-2703
    type != REFRESH                -> throw ERR-2705
    jti blacklist-de var?
        sebeb == REFRESH_ROTATION  -> revokeAll(REFRESH_REUSE_DETECTED)
                                      throw ERR-2706
        basqa sebeb                -> throw ERR-2704
    hesab enabled deyil            -> throw ERR-2708
    revokeSession(sid, REFRESH_ROTATION)         // kohne access + refresh
    return tokenService.issuePair(account, sid)  // eyni sid, yeni jti-ler
```

> **Diqqət! (ən çox səhv olunan yer)**
>
> `revokeAll(...)` çağırıb dərhal exception atırsınız. Metod `@Transactional`-dırsa `RuntimeException` tranzaksiyanı **rollback** edəcək və ləğvetmə bazaya yazılmayacaq!
>
> Həll: `@Transactional(noRollbackFor = UnauthorizedException.class)` və ya ləğvetməni `Propagation.REQUIRES_NEW` ilə ayrıca tranzaksiyada edin. Hansını seçdiyinizi şərhdə izah edin və yoxlama siyahısındakı 7-ci addımla sübut edin.

**Bonus (+5):** Ardıcıl 5 uğursuz login-dən sonra hesab 15 dəqiqəlik bloklansın — 423 **ERR-2709** (`LockedException`). Uğurlu login sayğacı sıfırlayır. Sayğac və bloklanma vaxtı `accounts` cədvəlində saxlanılsın.

# 9. Tapşırıq 5 — Avtorizasiya: URL qaydaları və @PreAuthorize (10 bal)

| **Endpoint**                                  | **GET**             | **POST / PUT / PATCH** | **DELETE** |
|-----------------------------------------------|---------------------|------------------------|------------|
| `/api/auth/register`, `login`, `refresh`      | —                   | hamı (açıq)            | —          |
| Swagger (`/swagger-ui/**`, `/v3/api-docs/**`) | hamı                | —                      | —          |
| `/api/students/**`, `/api/courses/**`         | daxil olmuş hər kəs | ADMIN, MANAGER         | ADMIN      |
| `/api/authors/**`, `/api/books/**`            | daxil olmuş hər kəs | ADMIN, MANAGER         | ADMIN      |
| `/api/departments/**`, `/api/employees/**`    | daxil olmuş hər kəs | ADMIN                  | ADMIN      |
| `/api/users/**` (şəxsi məlumat)               | ADMIN, MANAGER      | ADMIN                  | ADMIN      |
| `/api/admin/**`                               | ADMIN               | ADMIN                  | ADMIN      |

1.  URL qaydalarını `SecurityConfig`-də `requestMatchers(HttpMethod.X, "...")` ilə yazın. Daha spesifik qayda yuxarıda, `anyRequest().authenticated()` ən sonda olmalıdır.

2.  Bütün DELETE controller metodlarını əlavə olaraq `@PreAuthorize("hasRole('ADMIN')")` ilə qoruyun (ən azı 3 metod). URL qaydası səhvən silinsə belə metod qorunmuş qalacaq.

3.  `hasRole("ADMIN")` avtomatik `ROLE_` prefiksi əlavə edir — authority-ni `ROLE_ADMIN` kimi yaradın.

**Admin endpoint-ləri** (`AdminAccountController`):

| **Metod** | **URL**                                | **Təsvir**                                                                                  |
|-----------|----------------------------------------|---------------------------------------------------------------------------------------------|
| GET       | /api/admin/accounts?role=MANAGER       | Hesabların siyahısı; role verilsə — filter                                                  |
| PATCH     | /api/admin/accounts/{id}/role          | `{role}` → rol dəyişir, hesabın tokenləri ləğv olunur (ADMIN_REVOKE) ki, yeni token alınsın |
| PATCH     | /api/admin/accounts/{id}/disable       | `enabled = false` + `revokeAll(ACCOUNT_DISABLED)`                                           |
| PATCH     | /api/admin/accounts/{id}/enable        | `enabled = true`                                                                            |
| POST      | /api/admin/accounts/{id}/revoke-tokens | `revokeAll(ADMIN_REVOKE)`, 204                                                              |

Admin özünü söndürə və öz rolunu dəyişə bilməz → 409 **ERR-2715**. Hesab tapılmasa → 404 **ERR-2710**.

# 10. Tapşırıq 6 — 401/403 xətaları: ErrorCode, i18n, traceId (10 bal)

## 10.1. Yeni ErrorCode-lar

Kod diapazonları cədvəlinə yeni sətir əlavə edin: **Auth / Account → ERR-2700 – 2799**. Enum-da ən azı bu elementlər olmalıdır:

| **Enum elementi**         | **Kod**  | **HTTP** | **Bundle açarı**                 |
|---------------------------|----------|----------|----------------------------------|
| AUTH_UNAUTHORIZED         | ERR-2700 | 401      | error.auth.unauthorized          |
| AUTH_BAD_CREDENTIALS      | ERR-2701 | 401      | error.auth.badCredentials        |
| AUTH_TOKEN_EXPIRED        | ERR-2702 | 401      | error.auth.tokenExpired          |
| AUTH_TOKEN_INVALID        | ERR-2703 | 401      | error.auth.tokenInvalid          |
| AUTH_TOKEN_REVOKED        | ERR-2704 | 401      | error.auth.tokenRevoked          |
| AUTH_TOKEN_TYPE_MISMATCH  | ERR-2705 | 401      | error.auth.tokenTypeMismatch     |
| AUTH_REFRESH_REUSED       | ERR-2706 | 401      | error.auth.refreshReused         |
| AUTH_ACCESS_DENIED        | ERR-2707 | 403      | error.auth.accessDenied          |
| AUTH_ACCOUNT_DISABLED     | ERR-2708 | 403      | error.auth.accountDisabled       |
| AUTH_ACCOUNT_LOCKED       | ERR-2709 | 423      | error.auth.accountLocked (bonus) |
| ACCOUNT_NOT_FOUND         | ERR-2710 | 404      | error.account.notFound           |
| ACCOUNT_USERNAME_EXISTS   | ERR-2711 | 409      | error.account.usernameExists     |
| ACCOUNT_EMAIL_EXISTS      | ERR-2712 | 409      | error.account.emailExists        |
| AUTH_OLD_PASSWORD_WRONG   | ERR-2713 | 400      | error.auth.oldPasswordWrong      |
| AUTH_PASSWORD_SAME        | ERR-2714 | 400      | error.auth.passwordSame          |
| ACCOUNT_SELF_MODIFICATION | ERR-2715 | 409      | error.account.selfModification   |
| SESSION_NOT_FOUND         | ERR-2716 | 404      | error.auth.sessionNotFound       |

## 10.2. Xəta harada yaranır, kim tutur

| **Harada yaranır**            | **Exception**                | **ErrorCode**   | **Kim cavab qaytarır**                            |
|-------------------------------|------------------------------|-----------------|---------------------------------------------------|
| JwtAuthenticationFilter       | UnauthorizedException        | 2702–2705, 2708 | HandlerExceptionResolver → GlobalExceptionHandler |
| Qorunan URL, token yoxdur     | AuthenticationException      | ERR-2700        | RestAuthenticationEntryPoint                      |
| URL qaydası, rol çatmır       | AccessDeniedException        | ERR-2707        | RestAccessDeniedHandler                           |
| @PreAuthorize                 | AuthorizationDeniedException | ERR-2707        | GlobalExceptionHandler                            |
| Login, şifrə və ya login səhv | BadCredentialsException      | ERR-2701        | GlobalExceptionHandler                            |
| Login, hesab söndürülüb       | DisabledException            | ERR-2708        | GlobalExceptionHandler                            |

EntryPoint və AccessDeniedHandler da cavabı özləri yazmır — xətanı resolver-ə ötürür ki, format və dil bir yerdə qalsın:

``` java
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver resolver;

    public RestAuthenticationEntryPoint(
            @Qualifier("handlerExceptionResolver")
            HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) {
        resolver.resolveException(request, response, null,
                new UnauthorizedException(ErrorCode.AUTH_UNAUTHORIZED));
    }
}

// GlobalExceptionHandler-e elave:
@ExceptionHandler(BadCredentialsException.class)
public ResponseEntity<ErrorResponse> handleBadCredentials(
        HttpServletRequest req, BadCredentialsException ex) {
    return build(req, ex, ErrorCode.AUTH_BAD_CREDENTIALS, null);
}

// AuthorizationDeniedException (@PreAuthorize) de bura dusur
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ErrorResponse> handleAccessDenied(
        HttpServletRequest req, AccessDeniedException ex) {
    return build(req, ex, ErrorCode.AUTH_ACCESS_DENIED, null);
}
```

> **Diqqət!**
>
> GlobalExceptionHandler-də `AccessDeniedException` üçün ayrıca handler olmasa, `@PreAuthorize` xətası ən sondakı `Exception` handler-ə düşür və **500 ERR-1001** qaytarır. Mütləq yoxlayın!
>
> Login-də "istifadəçi tapılmadı" və "şifrə səhvdir" üçün **eyni** cavab (ERR-2701) qaytarılır — hansının səhv olduğunu demək hücumçuya mövcud username-ləri tapmağa kömək edir (user enumeration).
>
> Filter-dən gələn xətada DispatcherServlet hələ işləməyib, ona görə `LocaleContextHolder` sizin `LanguageLocaleResolver`-i görməyib. `build(...)`-də dili `localeResolver.resolveLocale(request)` ilə götürün — onda `?lang=rus` 401 cavabında da işləyəcək.

401 cavabı nümunəsi (forma əvvəlki tapşırıqdakı ilə eynidir):

``` json
{
  "status": 401,
  "code": "ERR-2704",
  "message": "Token has been revoked. Please log in again.",
  "errors": {},
  "traceId": "test-12345",
  "timestamp": "2026-11-17T10:15:42.118204"
}
```

Bundle nümunəsi (3 fayl):

``` text
# messages.properties
error.auth.unauthorized=Bu əməliyyat üçün daxil olmalısınız
error.auth.tokenRevoked=Token ləğv edilib. Yenidən daxil olun.
error.auth.accessDenied=Bu əməliyyat üçün icazəniz yoxdur

# messages_en.properties
error.auth.unauthorized=Authentication is required
error.auth.tokenRevoked=Token has been revoked. Please log in again.
error.auth.accessDenied=You do not have permission for this operation

# messages_ru.properties
error.auth.unauthorized=Требуется авторизация
error.auth.tokenRevoked=Токен отозван. Войдите снова.
error.auth.accessDenied=У вас нет прав для этой операции
```

# 11. Tapşırıq 7 — Swagger və təhlükəsiz loglama (5 bal)

1.  `OpenApiConfig`-ə `bearerAuth` adlı SecurityScheme (HTTP, bearer, JWT) və qlobal SecurityRequirement əlavə edin — Swagger UI-da **Authorize** düyməsi görünsün.

2.  Açıq endpoint-lərdə (register, login, refresh) boş `@SecurityRequirements()` yazın ki, kilid ikonu görünməsin.

3.  Əvvəlki `OpenApiCustomizer` qorunan bütün endpoint-lərə 401 və 403 cavablarını `ErrorResponse` sxemi ilə əlavə etsin.

4.  `LoggingAspect`: `password`, `oldPassword`, `newPassword`, `refreshToken`, `accessToken` dəyərləri loga düşməməlidir — `***` ilə əvəz edin. Əvvəlki tapşırıqda bonus idi, indi **məcburidir**.

5.  Şifrə saxlayan request record-larında `toString()`-i override edin — record-un avtomatik `toString()`-i bütün sahələri, o cümlədən şifrəni yazır.

``` java
@Bean
public OpenAPI openAPI() {
    return new OpenAPI()
            .info(new Info().title("School API").version("v1"))
            .components(new Components().addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
}

public record LoginRequest(
        @NotBlank(message = ValidationMessage.Keys.REQUIRED) String login,
        @NotBlank(message = ValidationMessage.Keys.REQUIRED) String password) {

    @Override
    public String toString() {
        return "LoginRequest[login=" + login + ", password=***]";
    }
}
```

# 12. Yoxlama siyahısı (müəllim bunları yoxlayacaq)

| **\#** | **Yoxlama**                                             | **Gözlənilən nəticə**                                                            |
|--------|---------------------------------------------------------|----------------------------------------------------------------------------------|
| 1      | GET /api/students tokensiz, `?lang=rus` ilə             | 401, ERR-2700, rusca mesaj, traceId dolu                                         |
| 2      | Login səhv şifrə ilə; sonra mövcud olmayan username ilə | Hər ikisində 401 ERR-2701, eyni mesaj                                            |
| 3      | Düzgün login                                            | 200, iki token; `auth_tokens`-də eyni `session_id` ilə 2 sətir                   |
| 4      | Access token ilə GET /api/students                      | 200; `logs/app.log`-da sətirdə `[admin]` görünür                                 |
| 5      | Refresh tokeni Bearer kimi göndərmək                    | 401 ERR-2705                                                                     |
| 6      | Logout, sonra həmin access token ilə sorğu              | 204, sonra 401 ERR-2704; `token_blacklist`-də 2 sətir (LOGOUT)                   |
| 7      | Refresh → yeni cüt; köhnə refresh-i təkrar göndərmək    | 401 ERR-2706; hesabın bütün tokenləri bazada `revoked = true` (rollback olmayıb) |
| 8      | 2 ayrı login, GET /api/auth/sessions, birini DELETE     | 2 sessiya görünür; silinən sessiyanın tokeni 401 ERR-2704                        |
| 9      | USER rolu ilə DELETE /api/students/1                    | 403 ERR-2707 (500 deyil!)                                                        |
| 10     | Şifrə dəyişdikdən sonra köhnə token                     | 401 ERR-2704                                                                     |
| 11     | Admin hesabı disable edir                               | Həmin hesabın tokeni 401 ERR-2704; yenidən login → 403 ERR-2708                  |
| 12     | `access-token-ttl: 1m`, 1 dəqiqə gözləyin               | 401 ERR-2702                                                                     |
| 13     | Cleanup job (cron-u müvəqqəti hər dəqiqəyə qoyun)       | Logda "rows deleted" sətri, müddəti keçmiş sətirlər silinib                      |
| 14     | `grep -E 'Test1234\|eyJ' logs/app.log` (test şifrəniz)   | Nəticə yoxdur — şifrə və token loga düşmür                                       |
| 15     | Swagger: Authorize → token → qorunan endpoint           | İşləyir; register/login-də kilid yoxdur                                          |
| 16     | Access tokeni jwt.io-da açın                            | Payload-da şifrə və email yoxdur; `jti`, `sid`, `type` var                       |

# 13. Qiymətləndirmə

| **Bölmə**                                                | **Bal** |
|----------------------------------------------------------|---------|
| Tapşırıq 1 — Hesablar, rollar və SecurityConfig          | 15      |
| Tapşırıq 2 — JwtService və JwtAuthenticationFilter       | 20      |
| Tapşırıq 3 — Stateful saxlama və DB blacklist            | 25      |
| Tapşırıq 4 — Auth endpoint-ləri, rotation və sessiyalar  | 15      |
| Tapşırıq 5 — Avtorizasiya (URL + @PreAuthorize)          | 10      |
| Tapşırıq 6 — 401/403 xətaları (ErrorCode, i18n, traceId) | 10      |
| Tapşırıq 7 — Swagger və təhlükəsiz loglama               | 5       |
| **Cəmi**                                                 | **100** |
| Bonus: Caffeine cache ilə blacklist yoxlaması            | +5      |
| Bonus: Uğursuz login-lərdən sonra bloklama               | +5      |

# 14. Təhvil qaydası

- Hər tapşırıq ayrı branch-da: `feature/security-base`, `feature/jwt-filter`, `feature/token-blacklist`, `feature/auth-endpoints`, `feature/authorization`, `feature/security-errors`, `feature/security-swagger`.

- Hər branch üçün ayrıca Pull Request. PR təsvirində nə etdiyinizi 2–3 cümlə ilə yazın.

- Blacklist PR-ına `auth_tokens` və `token_blacklist` cədvəllərinin logout-dan sonrakı ekran görüntüsünü əlavə edin.

- README-yə `JWT_SECRET` və `ADMIN_PASSWORD` mühit dəyişənlərinin necə verildiyini yazın. Real dəyərlər repoda olmamalıdır.

- Tövsiyə olunan ardıcıllıq: **1 → 6 → 2 → 3 → 4 → 5 → 7** (əvvəl hesab və xəta kodları, sonra token, sonda dokumentasiya).

## Özünüzü yoxlayın

``` text
☐ Bazada heç bir şifrə açıq mətnlə saxlanmır, token string-i saxlanmır
☐ Logout-dan sonra həmin token ilə heç bir endpoint işləmir
☐ Köhnə refresh tokenin təkrar istifadəsi bütün sessiyaları bağlayır
☐ Bütün 401/403 cavablarında code, message və traceId var
☐ @PreAuthorize xətası 403 qaytarır, 500 yox
☐ Loglarda şifrə və token dəyəri yoxdur
```

**Uğurlar!**
