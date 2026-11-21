---
name: java-project-conventions
description: House rules of the school_department Spring Boot project (package layout, ErrorCode + i18n, enums instead of hard-coded strings, DTO records, MapStruct, Liquibase, transactions, comment style, junior-level code). Load before writing, reviewing or fixing any Java, YAML or properties file in this project.
---

# school_department conventions

The owner is learning Java. Every change must be readable line by line by a beginner and must stay inside what the task spec asks.

## Stack

- Java 26, Gradle (`./gradlew`), Spring Boot 4.1 (Spring Security 7 when added), PostgreSQL 16 in `docker-compose.yaml`
- Lombok, MapStruct 1.6.3, springdoc-openapi, Liquibase (YAML changelogs), Ehcache (Hibernate L2)
- `server.servlet.context-path: /test-demo`, so every URL is `http://localhost:8080/test-demo/api/...`
- `spring.jpa.hibernate.ddl-auto: validate`: entity and Liquibase table must match exactly, or the app does not start

## Package layout (`com.spring_boot.school_department`)

| Package | What lives there |
| --- | --- |
| `config` | `@Configuration` classes (`OpenApiConfig`, `LocaleConfig`, `LanguageLocaleResolver`) |
| `controller/v1` | `@RestController`, `@RequestMapping("/api/...")`, Swagger `@Tag` + `@Operation` (summary/description in ASCII Azerbaijani, error codes listed in description) |
| `service` | `@Service @RequiredArgsConstructor @Transactional(readOnly = true)` on class, write methods get `@Transactional(propagation = Propagation.REQUIRED)` |
| `repository` | `JpaRepository` interfaces, derived queries first, `@Query` JPQL when needed |
| `entity` | `@Entity @Table(name = "...")`, Lombok `@Getter @Setter @NoArgsConstructor`, explicit `@Column(name = ...)` on every field |
| `enums` | `ErrorCode`, `ValidationMessage` (+ nested `Keys`), domain enums (`GenderEnum` with `dbCode` + `findByDbCode`) |
| `enums/converter` | `AttributeConverter<Enum, Integer>` like `GenderConverter` |
| `dto/request`, `dto/response` | Java `record`s. Requests carry Bean Validation annotations |
| `mapper` | MapStruct `@Mapper(componentModel = "spring")` interfaces |
| `exception` | `BaseException(ErrorCode, Object... args)` subclasses + `GlobalExceptionHandler` |
| `logging` | `TraceIdFilter` (MDC `traceId`), `LoggingAspect` |
| `validation` | custom constraint annotation + validator pair (`AzPhoneNumber`) |

A new top-level package is fine only when the task spec lists it (e.g. `security`, `scheduler`).

## Errors and messages

- Every new error is a new `ErrorCode` constant: `NAME("ERR-xxxx", "error.area.key", HttpStatus.X)`, grouped under the range comment `// ===== Area: xxxx-xxxx =====`.
- The message text never lives in Java. Add the key to all three bundles: `src/main/resources/i18n/messages.properties` (Azerbaijani, real letters ə ş ç ğ ı ö ü), `messages_en.properties`, `messages_ru.properties`. Placeholders are `{0}`, `{1}`.
- Throw `NotFoundException`, `AlreadyExistsException`, `BusinessException`, `InvalidRequestException` (or a new `BaseException` subclass the spec names) with the `ErrorCode` and args. `GlobalExceptionHandler.build(...)` turns it into `ErrorResponse(status, code, message, errors, traceId, timestamp)`.
- Validation messages: `message = ValidationMessage.Keys.X`. A new key needs a constant in `ValidationMessage` enum, one in `Keys` (`"{validation.x}"`), and the text in the three bundles.

## No hard-coded strings or magic values

If the spec shows a literal that is really a fixed set of values (roles, token types, reasons, claim names used in more than one place), use an enum or a `private static final` constant. Use enums for sets of values, constants for single names. Do not build a constants framework: one enum per concept, placed in `enums`.

## Database (Liquibase)

- New file `src/main/resources/db/changelog/changes/NNN-short-name.yaml`, next free number before `100-seed-data.yaml`, and include it in `db.changelog-master.yaml` in number order (seed stays last).
- changeSet ids `NNN-1`, `NNN-2`, ..., `author: Ahmad Ahmadov`, `preConditions: onFail: MARK_RAN, not tableExists` on createTable.
- Names: `pk_<table>`, `uk_<...>`, `fk_<child>_<parent>`, `idx_<...>`, `chk_<table>_<col>`. Use the exact names the spec gives.
- Types: `BIGINT autoIncrement` ids, `VARCHAR(n)`, `BOOLEAN`, `TIMESTAMP` (use `TIMESTAMP WITH TIME ZONE` only if the entity uses `Instant` and the spec does not say otherwise; keep entity and column type compatible for `validate`).

## Code style

- Constructor injection with `@RequiredArgsConstructor` and `private final` fields. Only write a constructor by hand when a `@Qualifier` is needed.
- Short methods, early `throw`, private `findX(id)` helpers like in `UserService`.
- No streams gymnastics, no generics tricks, no extra interfaces, no builders the codebase does not already use. Prefer the textbook version.
- Do not touch files the task does not mention. Do not refactor existing code "while you are there".
- No unit test files, even if a spec asks for them. Verification is done by running the app.

## Comments

Before writing any comment, read the existing comments in the same file (or a sibling file) and copy their style:

- ASCII Azerbaijani, no special letters: ə→e, ş→s, ç→c, ğ→g, ı→i, ö→o, ü→u
- lowercase, "biz" form (`yaziriq`, `saxlayiriq`, `tutur`), one or two short sentences, no trailing period
- explain only *why*, never restate the code
- no em dash or en dash, no emojis, no Javadoc blocks on new code
- write a comment only where the spec asks for one (e.g. "cavabi kodda serh kimi yazin") or where the why is not obvious

Examples from the codebase:

```java
// thread pool-a qayidan thread-de kohne id qalmasin deye silirik
// detallari yalniz loga yaziriq, cavaba yox
// annotation-in message-i yalniz compile-time sabit qebul edir, ona gore acarlari burda static final saxlayiriq
```

## Secrets

Secrets come from environment variables (`${JWT_SECRET}`, `${ADMIN_PASSWORD}`). `.env` is already in the repo root `.gitignore`. Never write a real secret into a tracked file, never log a password or token.
