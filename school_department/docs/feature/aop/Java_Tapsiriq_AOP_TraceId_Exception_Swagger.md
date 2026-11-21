# Praktiki Tapşırıq

Spring Boot: AOP, Trace ID (MDC), Exception Handling, i18n Bundle, ErrorCode Enum və Swagger

| **Sahə** | **Doldurun** |
|---|---|
| Tələbənin adı, soyadı: |  |
| Qrup: |  |
| Təhvil tarixi: |  |
| Layihənin adı: | school-api (paket: `az.iktex.organization.structure.testders`) |

## 1. Tapşırığın məqsədi

Əvvəlki tapşırıqda yazdığınız layihəyə (Student, Course, Department, Employee, Author, Book, User/Passport) real proyektlərdə mütləq olan infrastruktur hissələrini əlavə edəcəksiniz.

Tapşırıq bitəndə API-nizdə bunlar olacaq:

- Hər metodun giriş/çıxışını avtomatik loglayan **AOP aspect**
- Hər sorğuya unikal **traceId** — bütün log sətirlərində və cavab header-ində (**MDC**)
- Bütün xətaların bir **ErrorCode enum**-unda saxlanması: unikal kod + bundle açarı + HTTP status
- 3 dildə (**az / en / ru**) xəta və validasiya mesajları (**i18n bundle**)
- Hər xəta üçün eyni formatda cavab qaytaran **Global Exception Handler**
- Bütün endpoint-ləri göstərən və test etməyə imkan verən **Swagger UI**

> **Əsas qayda**
>
> Kodda **heç bir yerdə** xəta mesajı string kimi yazılmır. `throw new NotFoundException("Student not found")` qadağandır.
>
> Hər xəta enum-dan gəlir: `throw new NotFoundException(ErrorCode.STUDENT_NOT_FOUND, id)`. Mesajın mətni isə bundle faylındadır.

## 2. İstifadə olunacaq texnologiyalar

| **Texnologiya** | **Qeyd** |
|---|---|
| Java 25, Spring Boot 4.x | Mövcud layihə |
| spring-boot-starter-aspectj | AOP (Boot 3-də: `spring-boot-starter-aop`) |
| spring-boot-starter-validation | `@NotBlank`, `@Size`, `@Email` və s. |
| SLF4J MDC | Spring Boot-un içində var, əlavə dependency lazım deyil |
| springdoc-openapi-starter-webmvc-ui 3.1.1 | Swagger UI (Boot 4 üçün 3.x, Boot 3 üçün 2.x) |

`build.gradle`-a əlavə edin:

```
// AOP: logging/LoggingAspect ucun
implementation 'org.springframework.boot:spring-boot-starter-aspectj'
implementation 'org.springframework.boot:spring-boot-starter-validation'
// Swagger UI + OpenAPI 3
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1'
```

## 3. Yaradılacaq paket strukturu

```
az.iktex.organization.structure.testders
├── config/
│   ├── LanguageLocaleResolver.java   (dil: ?lang= və ya Accept-Language)
│   ├── LocaleConfig.java
│   └── OpenApiConfig.java            (Swagger)
├── enums/
│   ├── ErrorCode.java                (bütün xətalar)
│   └── ValidationMessage.java        (validasiya mesajlarının açarları)
├── exception/
│   ├── BaseException.java
│   ├── NotFoundException.java
│   ├── AlreadyExistsException.java
│   ├── BusinessException.java
│   ├── InvalidRequestException.java
│   └── GlobalExceptionHandler.java
├── logging/
│   ├── TraceIdFilter.java
│   └── LoggingAspect.java
├── validation/
│   ├── AzPhoneNumber.java            (custom annotasiya)
│   └── AzPhoneNumberValidator.java
└── response/
    └── ErrorResponse.java
 
resources/i18n/
├── messages.properties               (az - default)
├── messages_en.properties
└── messages_ru.properties
```

## 4. Tapşırıq 1 — AOP: LoggingAspect (15 bal)

Controller və service metodlarının içinə əl ilə `log.info(...)` yazmaq əvəzinə bunu bir aspect avtomatik etməlidir.

1. `logging/LoggingAspect` sinfini `@Aspect` və `@Component` ilə yaradın.
2. İki `@Pointcut` yazın: `controller` paketindəki və `service` paketindəki bütün metodlar.
3. `@Around` advice ilə metodun adını, arqumentlərini və işləmə müddətini (ms) loglayın.
4. Controller səviyyəsini `INFO`, service səviyyəsini `DEBUG` ilə loglayın.
5. Metod exception atanda `<x-` işarəsi ilə metodun adını və exception-ın tipini yazın, amma stack trace yazmayın (onu handler yazacaq).

Gözlənilən log formatı:

```
INFO [3f2a...] LoggingAspect : --> StudentController.getById args=[5]
INFO [3f2a...] LoggingAspect : <-- StudentController.getById (12 ms)
INFO [3f2a...] LoggingAspect : <x- StudentController.getById (37 ms) NotFoundException: ERR-2000 error.student.notFound
```

Skelet:

```
@Slf4j
@Aspect
@Component
public class LoggingAspect {
 
    @Pointcut("within(az.iktex.organization.structure.testders.controller..*)")
    public void controllerLayer() {}
 
    @Pointcut("within(az.iktex.organization.structure.testders.service..*)")
    public void serviceLayer() {}
 
    @Around("controllerLayer() || serviceLayer()")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        // TODO: metod adi, args, vaxt; exception olanda "<x-" ile yaz ve yeniden at
    }
}
```

> **Diqqət!**
>
> Aspect exception-ı **udmamalıdır** — loglayıb yenidən `throw` etməlisiniz, yoxsa GlobalExceptionHandler xətanı görməyəcək.
>
> Bonus: şifrə kimi həssas parametrlər loga düşməsin.

## 5. Tapşırıq 2 — Trace ID sistemi (MDC) (15 bal)

Hər HTTP sorğusu unikal `traceId` almalıdır. Bu id həmin sorğunun **bütün** log sətirlərində görünməli və cavabın `X-Trace-Id` header-ində qayıtmalıdır. Beləliklə bir xətanın bütün loglarını bir əmrlə tapmaq olur.

1. `logging/TraceIdFilter` yaradın: `OncePerRequestFilter`-dən miras alsın, `@Order(Ordered.HIGHEST_PRECEDENCE)` ilə ən birinci işləsin.
2. Sorğuda `X-Trace-Id` header-i varsa onu götürün, yoxdursa `UUID` yaradın.
3. Kənardan gələn dəyəri yoxlayın: yalnız hərf, rəqəm və tire, 8–64 simvol (log injection-un qarşısını almaq üçün). Uyğun deyilsə yenisini yaradın.
4. `MDC.put("traceId", traceId)` edin və cavaba `X-Trace-Id` header-i əlavə edin.
5. `finally` blokunda `MDC.remove("traceId")` edin — thread pool-a qayıdan thread-də köhnə id qalmamalıdır.
6. `application.yaml`-da log pattern-ə traceId əlavə edin və logları fayla da yazın.

```
logging:
  pattern:
    level: "%5p [%X{traceId:-}]"
  file:
    name: logs/app.log
```

Yoxlama: `grep "<traceId>" logs/app.log` əmri həmin sorğunun aspect, service və exception handler loglarının hamısını göstərməlidir.

## 6. Tapşırıq 3 — ErrorCode enum-u və bundle mesajları (25 bal)

### 6.1. ErrorCode enum-u

Bütün xətalar `enums/ErrorCode` enum-unda saxlanır. Hər elementin 3 sahəsi var:

- `code` — unikal kod (`ERR-2000`). UI və support xətanı dildən asılı olmayaraq bununla tanıyır.
- `messageKey` — bundle faylındakı açar (`error.student.notFound`).
- `status` — qaytarılacaq HTTP status (`HttpStatus.NOT_FOUND`).

```
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
 
    // ===== Umumi: 1000-1099 =====
    VALIDATION_ERROR("ERR-1000", "error.validation", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("ERR-1001", "error.internal", HttpStatus.INTERNAL_SERVER_ERROR),
    // ...
 
    // ===== Student: 2000-2099 =====
    STUDENT_NOT_FOUND("ERR-2000", "error.student.notFound", HttpStatus.NOT_FOUND),
    // ...
    ;
 
    private final String code;
    private final String messageKey;
    private final HttpStatus status;
}
```

### 6.2. Kod diapazonları

Hər modul öz aralığında nömrələnir. Yeni xəta əlavə edəndə həmin modulun növbəti boş nömrəsini götürün.

| **Modul** | **Diapazon** |
|---|---|
| Ümumi / validasiya / sistem | ERR-1000 – 1099 |
| Student | ERR-2000 – 2099 |
| Course | ERR-2100 – 2199 |
| Department | ERR-2200 – 2299 |
| Employee | ERR-2300 – 2399 |
| Author | ERR-2400 – 2499 |
| Book | ERR-2500 – 2599 |
| User / Passport | ERR-2600 – 2699 |

### 6.3. Mütləq olmalı olan xəta kodları

Enum-da ən azı bu elementlər olmalıdır (kodlar dəqiq belə olsun, yoxlama zamanı bu kodlar gözlənilir):

| **Enum elementi** | **Kod** | **HTTP** | **Bundle açarı** |
|---|---|---|---|
| VALIDATION_ERROR | ERR-1000 | 400 | `error.validation` |
| INTERNAL_ERROR | ERR-1001 | 500 | `error.internal` |
| MALFORMED_REQUEST | ERR-1002 | 400 | `error.request.malformed` |
| TYPE_MISMATCH | ERR-1003 | 400 | `error.request.typeMismatch` |
| MISSING_PARAMETER | ERR-1004 | 400 | `error.request.missingParam` |
| METHOD_NOT_SUPPORTED | ERR-1005 | 405 | `error.request.methodNotSupported` |
| URL_NOT_FOUND | ERR-1006 | 404 | `error.request.urlNotFound` |
| DATA_INTEGRITY_VIOLATION | ERR-1007 | 409 | `error.data.integrity` |
| STUDENT_NOT_FOUND | ERR-2000 | 404 | `error.student.notFound` |
| STUDENT_ALREADY_ENROLLED | ERR-2001 | 409 | `error.student.alreadyEnrolled` |
| STUDENT_NOT_ENROLLED | ERR-2002 | 409 | `error.student.notEnrolled` |
| COURSE_NOT_FOUND | ERR-2100 | 404 | `error.course.notFound` |
| COURSE_ALREADY_EXISTS | ERR-2101 | 409 | `error.course.alreadyExists` |
| COURSE_TITLE_USED | ERR-2102 | 409 | `error.course.titleUsed` |
| DEPARTMENT_NOT_FOUND | ERR-2200 | 404 | `error.department.notFound` |
| DEPARTMENT_ALREADY_EXISTS | ERR-2201 | 409 | `error.department.alreadyExists` |
| DEPARTMENT_NAME_USED | ERR-2202 | 409 | `error.department.nameUsed` |
| DEPARTMENT_HAS_EMPLOYEES | ERR-2203 | 409 | `error.department.hasEmployees` |
| EMPLOYEE_NOT_FOUND | ERR-2300 | 404 | `error.employee.notFound` |
| EMPLOYEE_ALREADY_EXISTS | ERR-2301 | 409 | `error.employee.alreadyExists` |
| EMPLOYEE_SALARY_RANGE | ERR-2302 | 400 | `error.employee.salaryRange` |
| AUTHOR_NOT_FOUND | ERR-2400 | 404 | `error.author.notFound` |
| AUTHOR_ALREADY_EXISTS | ERR-2401 | 409 | `error.author.alreadyExists` |
| BOOK_NOT_FOUND | ERR-2500 | 404 | `error.book.notFound` |
| BOOK_ALREADY_EXISTS | ERR-2501 | 409 | `error.book.alreadyExists` |
| BOOK_AUTHOR_ALREADY_LINKED | ERR-2502 | 409 | `error.book.authorAlreadyLinked` |
| BOOK_AUTHOR_NOT_LINKED | ERR-2503 | 404 | `error.book.authorNotLinked` |
| USER_NOT_FOUND | ERR-2600 | 404 | `error.user.notFound` |
| USER_EMAIL_EXISTS | ERR-2601 | 409 | `error.user.emailExists` |
| USER_EMAIL_USED | ERR-2602 | 409 | `error.user.emailUsed` |
| USER_PASSPORT_EXISTS | ERR-2603 | 409 | `error.user.passportExists` |
| PASSPORT_NOT_FOUND | ERR-2604 | 404 | `error.passport.notFound` |
| PASSPORT_SERIAL_EXISTS | ERR-2605 | 409 | `error.passport.serialExists` |

### 6.4. Bundle faylları (i18n)

1. `resources/i18n/` altında 3 fayl yaradın: `messages.properties` (az, default), `messages_en.properties`, `messages_ru.properties`.
2. Enum-dakı **hər** `messageKey` 3 faylın hamısında olmalıdır.
3. Parametrli mesajlarda `{0}`, `{1}` istifadə edin. Mətndə apostrof varsa iki dəfə yazın (`''`) — bu MessageFormat qaydasıdır.
4. `application.yaml`-da bundle-ı qoşun (aşağıda).
5. `LanguageLocaleResolver` yazın: dil əvvəlcə `?lang=` parametrindən, sonra `Accept-Language` header-indən oxunsun. `az/aze`, `en/eng`, `ru/rus` qəbul olunsun, tanınmayan dil → `az`. Bean adı mütləq `localeResolver` olmalıdır.

```
spring:
  messages:
    basename: i18n/messages
    encoding: UTF-8
    fallback-to-system-locale: false
```

Nümunə (3 fayl):

```
# messages.properties
error.student.notFound=Tələbə tapılmadı: {0}
validation.required=Bu sahə mütləqdir
validation.size.max=Ən çox {max} simvol ola bilər
 
# messages_en.properties
error.student.notFound=Student not found: {0}
validation.required=This field is required
validation.size.max=Must be at most {max} characters
 
# messages_ru.properties
error.student.notFound=Студент не найден: {0}
validation.required=Это поле обязательно
validation.size.max=Не более {max} символов
```

## 7. Tapşırıq 4 — Exception-lar və Global Exception Handler (25 bal)

### 7.1. Custom exception-lar

Exception-lar string qəbul etmir — yalnız `ErrorCode` və mesajın parametrlərini qəbul edir. HTTP status da enum-dan götürülür.

```
@Getter
public abstract class BaseException extends RuntimeException {
 
    private final ErrorCode errorCode;
    private final Object[] args;
 
    protected BaseException(ErrorCode errorCode, Object... args) {
        super(errorCode.getCode() + " " + errorCode.getMessageKey());
        this.errorCode = errorCode;
        // MessageFormat Long-u "1,234" kimi yazir - qarsisini almaq ucun String-e ceviririk
        this.args = Arrays.stream(args).map(String::valueOf).toArray();
    }
 
    public HttpStatus getStatus() {
        return errorCode.getStatus();
    }
}
 
public class NotFoundException extends BaseException {
    public NotFoundException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
```

`NotFoundException`, `AlreadyExistsException`, `BusinessException`, `InvalidRequestException` — dördü də `BaseException`-dan miras alsın.

Bütün service-lərdə xətaları enum ilə atın:

```
// Sehv - string mesaj
throw new RuntimeException("Student not found");
 
// Duzgun
return studentRepository.findById(id)
        .orElseThrow(() -> new NotFoundException(ErrorCode.STUDENT_NOT_FOUND, id));
 
if (studentRepository.existsByIdAndCoursesId(id, courseId)) {
    throw new AlreadyExistsException(ErrorCode.STUDENT_ALREADY_ENROLLED);
}
```

### 7.2. GlobalExceptionHandler

`@RestControllerAdvice` ilə yaradın. Mesajı `MessageSource` və `LocaleContextHolder.getLocale()` ilə seçilmiş dilə tərcümə edin. Aşağıdakı exception-lar tutulmalıdır:

| **Exception** | **ErrorCode** | **Qeyd** |
|---|---|---|
| `BaseException` | exception-ın öz kodu | NotFound, AlreadyExists, Business, InvalidRequest |
| `MethodArgumentNotValidException` | VALIDATION_ERROR | `errors` map-ı: sahə → mesaj |
| `HttpMessageNotReadableException` | MALFORMED_REQUEST | Yanlış JSON, yanlış enum/tarix |
| `MethodArgumentTypeMismatchException` | TYPE_MISMATCH | `/api/students/abc` |
| `MissingServletRequestParameterException` | MISSING_PARAMETER | Mütləq `@RequestParam` yoxdur |
| `HttpRequestMethodNotSupportedException` | METHOD_NOT_SUPPORTED | Məs. `PATCH` dəstəklənmir |
| `NoResourceFoundException` | URL_NOT_FOUND | Mövcud olmayan URL |
| `DataIntegrityViolationException` | DATA_INTEGRITY_VIOLATION | DB constraint pozuldu |
| `Exception` (ən sonda) | INTERNAL_ERROR | Detallar yalnız loga, cavaba yox |

Bütün handler-lər bir ümumi `build(...)` metodundan istifadə etsin. Status yalnız enum-dan götürülsün (`@ResponseStatus` ilə ikinci dəfə yazmayın):

```
private ResponseEntity<ErrorResponse> build(HttpServletRequest request, Exception ex,
                                            ErrorCode errorCode, Map<String, String> errors,
                                            Object... args) {
    HttpStatus status = errorCode.getStatus();
    logError(request, ex, errorCode, errors);
    String key = errorCode.getMessageKey();
    String message = messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
    return ResponseEntity.status(status).body(new ErrorResponse(status.value(), errorCode.getCode(),
            message, errors, MDC.get("traceId"), LocalDateTime.now()));
}
```

Loglama qaydası: 4xx xətalar `WARN` ilə (stack trace olmadan), 5xx xətalar `ERROR` ilə (stack trace ilə). Log sətrində metod, URL, status, kod, açar və exception tipi olsun:

```
WARN [test-12345] GlobalExceptionHandler : Request failed: GET /test-demo/api/students/99999
     -> 404 ERR-2000 (error.student.notFound) [NotFoundException] args=[99999]
```

### 7.3. ErrorResponse — cavab formatı

```
public record ErrorResponse(
        int status,
        String code,                 // ERR-2000
        String message,              // secilmis dilde
        Map<String, String> errors,  // yalniz validasiyada dolu
        String traceId,              // X-Trace-Id ile eyni
        LocalDateTime timestamp
) {}
```

Hər xəta üçün cavab eyni formada olmalıdır:

```
{
  "status": 404,
  "code": "ERR-2000",
  "message": "Student not found: 99999",
  "errors": {},
  "traceId": "test-12345",
  "timestamp": "2026-11-03T09:58:08.290767"
}
```

## 8. Tapşırıq 5 — Validasiyalarda enum mesajları (10 bal)

Validasiya mesajlarının açarları da bir enum-da saxlanır. Annotasiyanın `message` atributu yalnız **compile-time sabit** qəbul edir — `ValidationMessage.REQUIRED.getKey()` yazmaq olmaz. Ona görə açarlar enum-un içindəki `Keys` sinfində `static final` sabit kimi saxlanır.

```
@Getter
@RequiredArgsConstructor
public enum ValidationMessage {
 
    REQUIRED(Keys.REQUIRED),
    SIZE_MAX(Keys.SIZE_MAX),
    EMAIL(Keys.EMAIL),
    AZ_PHONE(Keys.AZ_PHONE);
    // ... MIN, MAX, POSITIVE, NOT_NEGATIVE, DIGITS, PAST_OR_PRESENT, SERIAL_NUMBER_PATTERN
 
    private final String key;
 
    public static final class Keys {
        public static final String REQUIRED = "{validation.required}";
        public static final String SIZE_MAX = "{validation.size.max}";
        public static final String EMAIL    = "{validation.email}";
        public static final String AZ_PHONE = "{validation.phone.az}";
 
        private Keys() {}
    }
}
```

Request DTO-larda istifadə:

```
public record StudentRequest(
        @NotBlank(message = ValidationMessage.Keys.REQUIRED)
        @Size(max = 50, message = ValidationMessage.Keys.SIZE_MAX)
        String firstName,
 
        @Email(message = ValidationMessage.Keys.EMAIL)
        String email
) {}
```

1. Bütün `request/*Request` siniflərindəki string mesajları `ValidationMessage.Keys.*` ilə əvəz edin.
2. Bundle-da annotasiyanın öz parametrlərindən istifadə edin: `{max}`, `{value}`, `{integer}`, `{fraction}`.
3. Custom validator yazın: `@AzPhoneNumber` — format `+994` + operator kodu (50, 51, 55, 70, 77, 99) + 7 rəqəm. Boşluqlar nəzərə alınmasın. Default mesaj: `ValidationMessage.Keys.AZ_PHONE`.
4. Validasiya xətası cavabında `code: ERR-1000` olsun, `errors`-da isə hər sahə üçün seçilmiş dildə mesaj.
5. Service səviyyəsindəki biznes yoxlamaları (məs. minimum maaş > maksimum maaş) `throw new InvalidRequestException(ErrorCode.EMPLOYEE_SALARY_RANGE)` ilə atın.

## 9. Tapşırıq 6 — Swagger (springdoc-openapi) (10 bal)

Bütün endpoint-lər Swagger UI-da görünməli və oradan test edilə bilməlidir: `http://localhost:8080/test-demo/swagger-ui.html`

```
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
    try-it-out-enabled: true
```

1. `config/OpenApiConfig` yaradın: API adı, versiyası və təsviri olan `OpenAPI` bean.
2. Hər controller-ə `@Tag(name = "Student", description = "...")` əlavə edin.
3. Hər endpoint-ə `@Operation(summary = "...", description = "...")` əlavə edin. Təsvirdə hansı xəta kodunu qaytara biləcəyini yazın (məs. "Tapılmasa 404 (ERR-2200)").
4. `OpenApiCustomizer` bean-i ilə **bütün** endpoint-lərə avtomatik əlavə edin: `Accept-Language` (az / eng / rus) və `X-Trace-Id` header parametrləri.
5. Eyni customizer ilə bütün endpoint-lərə 400, 404, 409, 500 cavablarını `ErrorResponse` sxemi ilə əlavə edin.
6. `ErrorResponse` sahələrinə `@Schema(description = "...", example = "...")` yazın.

> **Diqqət!**
>
> `ErrorResponse` heç bir controller-in return tipi olmadığı üçün springdoc onu özü sxemlərə əlavə etmir. Customizer-in içində əl ilə əlavə edin:
>
> `ModelConverters.getInstance().read(ErrorResponse.class).forEach(openApi.getComponents()::addSchemas);`

**Bonus (+5):** API təsvirində `ErrorCode.values()`-dan avtomatik qurulan xəta kodları cədvəli göstərin. Belə olanda enum-a yeni kod əlavə ediləndə Swagger özü yenilənir.

## 10. Yoxlama siyahısı (müəllim bunları yoxlayacaq)

| **#** | **Yoxlama** | **Gözlənilən nəticə** |
|---|---|---|
| 1 | `GET /api/students/99999` + `Accept-Language: eng` | 404, `code: ERR-2000`, "Student not found: 99999", traceId dolu |
| 2 | Eyni sorğu `?lang=rus` ilə | Eyni kod, rusca mesaj |
| 3 | Dil göndərilmədən | Azərbaycanca mesaj |
| 4 | `POST /api/departments` + `{"name": ""}` | 400, `ERR-1000`, `errors.name` dolu |
| 5 | `GET /api/students/abc` | 400, `ERR-1003` |
| 6 | Sorğu `X-Trace-Id: test-12345` ilə | Cavab header-ində və cavab body-sində `test-12345` |
| 7 | `grep test-12345 logs/app.log` | Aspect (`-->`, `<x-`) və handler (`WARN`) sətirləri görünür |
| 8 | Swagger UI açılır | 7 tag, hər endpoint-də header-lər və xəta cavabları |
| 9 | `grep -rn 'Exception("' src/main` | Nəticə yoxdur (string mesaj qalmayıb) |

## 11. Qiymətləndirmə

| **Bölmə** | **Bal** |
|---|---|
| Tapşırıq 1 — AOP LoggingAspect | 15 |
| Tapşırıq 2 — Trace ID (MDC) | 15 |
| Tapşırıq 3 — ErrorCode enum və bundle mesajları | 25 |
| Tapşırıq 4 — Exception-lar və Global Exception Handler | 25 |
| Tapşırıq 5 — Validasiyada enum mesajları | 10 |
| Tapşırıq 6 — Swagger | 10 |
| **Cəmi** | **100** |
| Bonus: Swagger-də ErrorCode cədvəli | +5 |

## 12. Təhvil qaydası

- Hər tapşırıq ayrı branch-da: `feature/aop`, `feature/trace-id`, `feature/error-code`, `feature/exception-handler`, `feature/validation`, `feature/swagger`.
- Hər branch üçün ayrıca Pull Request. PR təsvirində nə etdiyinizi 2–3 cümlə ilə yazın.
- Swagger PR-ına 404 və 400 cavablarının screenshot-unu əlavə edin.
- Tövsiyə olunan ardıcıllıq: **3 → 4 → 5 → 2 → 1 → 6** (əvvəl xəta infrastrukturu, sonra loglama, sonda dokumentasiya).

### Özünüzü yoxlayın

☐  Kodda string ilə atılan heç bir xəta qalmayıb

☐  Enum-dakı hər açar 3 bundle faylında var

☐  Bütün xəta cavablarında `code`, `message` və `traceId` var

☐  Bir sorğunun bütün logları eyni traceId ilə tapılır

☐  Swagger UI açılır və "Try it out" işləyir
