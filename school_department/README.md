# School API

Spring Boot + Spring Data JPA ilə entity əlaqələri (OneToOne, OneToMany, ManyToMany), Liquibase, second-level cache, tranzaksiyalar, AOP loglama və JWT ilə Spring Security.

## Texnologiyalar

- Java 26, Gradle
- Spring Boot 4.1.2 (`webmvc`, `data-jpa`, `validation`, `aspectj`, `security`)
- PostgreSQL 16 (Docker Compose)
- Liquibase (YAML changelog-lar, `ddl-auto: validate`)
- Hibernate L2 cache: `hibernate-jcache` + Ehcache 3.10.8
- AOP loglama (`LoggingAspect`) və `X-Trace-Id` (MDC)
- Spring Security + JWT (jjwt 0.12.6), tokenlər bazada saxlanır, logout blacklist ilə
- Bean Validation, xəta mesajları i18n bundle-larda (az, en, ru)
- Lombok, MapStruct 1.6.3
- springdoc-openapi 3.2.0 (Swagger UI)
- Unit testlər: JUnit 5, Mockito, AssertJ
- JaCoCo 0.8.16 (test coverage)

## İşə salma

```bash
docker compose up -d
./gradlew bootRun
```

Bazanı sıfırlamaq lazımdırsa:

```bash
docker compose down -v   # school-data volume-u silinir, bütün data gedir
docker compose up -d     # boş baza, sxemi və seed-i Liquibase yenidən yaradır
```

Base URL: `http://localhost:8080/test-demo`

### Mühit dəyişənləri

`JWT_SECRET` və `ADMIN_PASSWORD` repoda yazılmır, `application.yaml` onları mühit dəyişənlərindən oxuyur (`${JWT_SECRET}`, `${ADMIN_PASSWORD}`). Verilməsə tətbiq işə düşmür.

- `JWT_SECRET`: tokenləri imzalayan Base64 açar, ən az 256 bit (32 bayt).
- `ADMIN_PASSWORD`: ilk dəfə yaradılan `admin` hesabının şifrəsi.

Secret yaratmaq:

```bash
openssl rand -base64 32
```

`./gradlew bootRun`-dan əvvəl:

```bash
export JWT_SECRET='<openssl-in-verdiyi-dəyər>'
export ADMIN_PASSWORD='<öz-şifrəniz>'
```

Bu `export` sətirlərini `.env` faylında saxlayıb `source .env` ilə yükləmək də olar. `.env` `.gitignore`-dadır və repoya düşmür.

### Admin hesabı

Bazada ADMIN rolu olan hesab yoxdursa, tətbiq başlayanda `AdminSeeder` onu yaradır:

- username: `admin`
- email: `admin@school.az`
- şifrə: `ADMIN_PASSWORD`-un dəyəri

`docker compose down -v`-dən sonra admin növbəti işə salmada yenidən yaranır.

## API Docs

- Swagger UI: http://localhost:8080/test-demo/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/test-demo/v3/api-docs

Swagger-də **Authorize** düyməsinə login-dən alınan access tokeni yazın.

## Endpoint-lər haqqında

- Bütün URL-lər Base URL-ə əlavə olunur: `http://localhost:8080/test-demo/api/...`
- Siyahı və yaratma endpoint-ləri `/` ilə bitir (`POST /api/users/`, `GET /api/users/`). Sondakı `/` mütləqdir.
- `/api/auth/register`, `/login`, `/refresh` və Swagger-dən başqa hər sorğuya `Authorization: Bearer <accessToken>` lazımdır. Kimin nəyə icazəsi olduğu [Təhlükəsizlik](#təhlükəsizlik) bölməsindədir.

## Modul 1: User ↔ Passport (OneToOne)

| Metod | URL | Status | Təsvir |
| --- | --- | --- | --- |
| POST | `/api/users/` | 201 | User yarat |
| GET | `/api/users/{id}` | 200 | User passport ilə birlikdə (JOIN FETCH) |
| GET | `/api/users/by-email?email=...` | 200 | Email ilə axtarış |
| GET | `/api/users/?gender=MALE` | 200 | Hamısı, gender verilsə filter |
| GET | `/api/users/without-passport` | 200 | Passportu olmayan user-lər |
| PUT | `/api/users/{id}` | 200 | Yenilə |
| DELETE | `/api/users/{id}` | 204 | Sil (passport da silinir) |
| POST | `/api/users/{id}/passport` | 201 | Passport əlavə et |
| GET | `/api/users/{id}/passport` | 200 | User-in passportu |
| DELETE | `/api/users/{id}/passport` | 204 | Passportu sil (orphanRemoval) |

Nümunə request:

```http
POST /api/users/
{
  "name": "Əli",
  "surname": "Məmmədov",
  "email": "ali@mail.com",
  "gender": "MALE"
}
```

## Modul 2: Department → Employee (OneToMany)

| Metod | URL | Status | Təsvir |
| --- | --- | --- | --- |
| POST | `/api/departments/` | 201 | Department yarat (ad unikal) |
| GET | `/api/departments/{id}` | 200 | İşçilər ilə birlikdə (JOIN FETCH) |
| GET | `/api/departments/` | 200 | Hamısı (işçilərsiz) |
| GET | `/api/departments/without-employees` | 200 | İşçisi olmayan departamentlər |
| PUT | `/api/departments/{id}` | 200 | Yenilə |
| DELETE | `/api/departments/{id}` | 204 | Sil (işçisi varsa olmaz) |
| POST | `/api/employees/` | 201 | İşçi yarat |
| GET | `/api/employees/{id}` | 200 | İşçi |
| GET | `/api/employees/department/{departmentId}` | 200 | Departamentin işçiləri |
| GET | `/api/employees/salary-range?min=500&max=2000` | 200 | Maaş aralığına görə |
| GET | `/api/employees/department/{departmentId}/average-salary` | 200 | Orta maaş |
| PUT | `/api/employees/{id}` | 200 | Yenilə (departament dəyişə bilər) |
| POST | `/api/employees/{id}/transfer` | 200 | Başqa departamentə köçür + maaş artımı (bax: [Transactions](#transactions-propagation)) |
| DELETE | `/api/employees/{id}` | 204 | İşçini sil |

## Modul 3: Student ↔ Course (ManyToMany, @JoinTable)

| Metod | URL | Status | Təsvir |
| --- | --- | --- | --- |
| POST | `/api/students/` | 201 | Tələbə yarat |
| GET | `/api/students/{id}` | 200 | Kurslar ilə birlikdə (left join fetch) |
| GET | `/api/students/` | 200 | Hamısı |
| GET | `/api/students/without-courses` | 200 | Heç bir kursa yazılmayanlar |
| PUT | `/api/students/{id}` | 200 | Yenilə |
| DELETE | `/api/students/{id}` | 204 | Sil (kurslar qalır) |
| POST | `/api/students/{studentId}/courses/{courseId}` | 201 | Kursa yaz (kurs doludursa 409) |
| DELETE | `/api/students/{studentId}/courses/{courseId}` | 204 | Kursdan çıxar |
| POST | `/api/courses/` | 201 | Kurs yarat (title unikal) |
| GET | `/api/courses/` | 200 | Hamısı |
| GET | `/api/courses/{id}` | 200 | Biri |
| GET | `/api/courses/student/{studentId}` | 200 | Tələbənin kursları |
| GET | `/api/courses/{id}/students` | 200 | Kursun tələbələri |
| GET | `/api/courses/{id}/students/count` | 200 | Kursdakı tələbə sayı |
| PUT | `/api/courses/{id}` | 200 | Yenilə |
| PUT | `/api/courses/credits?amount=1` | 200 | Bütün kursların kreditini artır (bulk update) |
| DELETE | `/api/courses/{id}` | 204 | Sil (əvvəl tələbələrlə əlaqə qırılır) |

## Modul 4: Author ↔ AuthorBook ↔ Book (ManyToMany, 3-cü entity)

| Metod | URL | Status | Təsvir |
| --- | --- | --- | --- |
| POST | `/api/authors/` | 201 | Müəllif yarat (eyni ad+soyad olmaz) |
| GET | `/api/authors/{id}` | 200 | Müəllif |
| GET | `/api/authors/?country=Azerbaijan` | 200 | Hamısı, country verilsə filter |
| GET | `/api/authors/without-books` | 200 | Kitabı olmayan müəlliflər |
| GET | `/api/authors/{id}/books` | 200 | Müəllifin kitabları |
| PUT | `/api/authors/{id}` | 200 | Yenilə |
| DELETE | `/api/authors/{id}` | 204 | Sil (kitablar qalır) |
| POST | `/api/books/` | 201 | Kitab yarat (title unikal) |
| GET | `/api/books/{id}` | 200 | Müəlliflər ilə birlikdə |
| GET | `/api/books/?title=java` | 200 | Hamısı, title verilsə axtarış |
| GET | `/api/books/cheaper-than?price=20` | 200 | Qiyməti verilən məbləğə bərabər və ya aşağı olanlar |
| PUT | `/api/books/{id}` | 200 | Yenilə |
| DELETE | `/api/books/{id}` | 204 | Sil (müəlliflər qalır) |
| POST | `/api/books/{bookId}/authors/{authorId}` | 201 | Kitaba müəllif bağla |
| DELETE | `/api/books/{bookId}/authors/{authorId}` | 204 | Əlaqəni sil |

## Auth

| Metod | URL | Status | Giriş | Təsvir |
| --- | --- | --- | --- | --- |
| POST | `/api/auth/register` | 201 | açıq | `{username, email, password}`, hesab həmişə USER rolu ilə yaranır |
| POST | `/api/auth/login` | 200 | açıq | `{login, password}`, `login` username və ya email ola bilər. Access + refresh token qaytarır |
| POST | `/api/auth/refresh` | 200 | açıq | `{refreshToken}`, yeni token cütü (eyni sessiya) |
| POST | `/api/auth/logout` | 204 | token | Cari sessiyanı ləğv et |
| POST | `/api/auth/logout-all` | 204 | token | Hesabın bütün sessiyalarını ləğv et |
| GET | `/api/auth/me` | 200 | token | Cari hesab |
| GET | `/api/auth/sessions` | 200 | token | Aktiv sessiyalar (`current = true` cari sessiyadır) |
| DELETE | `/api/auth/sessions/{sessionId}` | 204 | token | Bir sessiyanı ləğv et |
| PUT | `/api/auth/password` | 204 | token | `{oldPassword, newPassword}`, bütün tokenlər ləğv olunur |

Şifrə qaydası (`@StrongPassword`): ən az 8 simvol, ən az 1 böyük hərf, 1 kiçik hərf və 1 rəqəm.

Login cavabı:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "accessExpiresIn": 900,
  "refreshExpiresIn": 604800,
  "tokenType": "Bearer"
}
```

## Admin

Hamısı yalnız ADMIN üçündür.

| Metod | URL | Status | Təsvir |
| --- | --- | --- | --- |
| GET | `/api/admin/accounts?role=MANAGER` | 200 | Hesablar, role verilsə filter |
| PATCH | `/api/admin/accounts/{id}/role` | 200 | `{role}`, rol dəyişir və hesabın tokenləri ləğv olunur |
| PATCH | `/api/admin/accounts/{id}/disable` | 200 | Hesabı söndür, bütün tokenləri ləğv olunur |
| PATCH | `/api/admin/accounts/{id}/enable` | 200 | Hesabı aç |
| POST | `/api/admin/accounts/{id}/revoke-tokens` | 204 | Hesabın bütün tokenlərini ləğv et |

Admin özünü söndürə və öz rolunu dəyişə bilməz (409 `ERR-2715`).

## Təhlükəsizlik

Axın:

1. `POST /api/auth/login` access və refresh token qaytarır. Hər ikisi `auth_tokens` cədvəlinə yazılır (tokenin özü yox, yalnız `jti`).
2. Sonrakı sorğularda `Authorization: Bearer <accessToken>` header-i göndərilir. `JwtAuthenticationFilter` imzanı, müddəti, token tipini, blacklist-i və hesabın aktiv olduğunu yoxlayır.
3. Access tokenin vaxtı bitəndə `POST /api/auth/refresh` çağırılır. Köhnə cüt blacklist-ə düşür, yeni cüt verilir (rotation). Artıq istifadə olunmuş refresh token yenidən gəlsə, hesabın bütün tokenləri ləğv olunur (401 `ERR-2706`).
4. Logout, şifrə dəyişmə, hesabı söndürmə və rol dəyişmə tokenləri `token_blacklist`-ə yazır, növbəti sorğu 401 alır.
5. `TokenCleanupJob` hər saat (`app.security.token-cleanup-cron`) müddəti keçmiş sətirləri `token_blacklist` və `auth_tokens`-dən silir.

Token müddətləri (`app.jwt.*`):

| Token | Müddət |
| --- | --- |
| Access | 15 dəqiqə (`access-token-ttl: 15m`) |
| Refresh | 7 gün (`refresh-token-ttl: 7d`) |

Rollar: `ADMIN`, `MANAGER`, `USER`. İcazələr (`SecurityConfig`):

| Endpoint | GET | POST / PUT / PATCH | DELETE |
| --- | --- | --- | --- |
| `/api/auth/register`, `login`, `refresh` | | hamı (açıq) | |
| Swagger (`/swagger-ui/**`, `/v3/api-docs/**`) | hamı | | |
| `/api/students/**`, `/api/courses/**` | daxil olmuş hər kəs | ADMIN, MANAGER | ADMIN |
| `/api/authors/**`, `/api/books/**` | daxil olmuş hər kəs | ADMIN, MANAGER | ADMIN |
| `/api/departments/**`, `/api/employees/**` | daxil olmuş hər kəs | ADMIN | ADMIN |
| `/api/users/**` | ADMIN, MANAGER | ADMIN | ADMIN |
| `/api/admin/**` | ADMIN | ADMIN | ADMIN |
| Qalan hamısı (`/api/auth/**`, `/api/cache/**`, `/api/tx-demo/**`, `/api/isolation/**`) | daxil olmuş hər kəs | daxil olmuş hər kəs | daxil olmuş hər kəs |

DELETE controller metodları əlavə olaraq `@PreAuthorize("hasRole('ADMIN')")` ilə qorunur. Buna `/api/cache` DELETE-ləri də daxildir.

Token yoxdursa və ya etibarsızdırsa 401 (`ERR-2700`...`ERR-2706`), rol çatmırsa 403 (`ERR-2707`).

## Xətalar

Bütün xətalar eyni formatda qayıdır (`ErrorResponse`):

```json
{
  "status": 404,
  "code": "ERR-2000",
  "message": "Tələbə tapılmadı: 99999",
  "errors": {},
  "traceId": "test-12345",
  "timestamp": "2026-11-03T09:58:08.290767"
}
```

`errors` yalnız validasiya xətasında (`ERR-1000`) dolur: sahə adı və mesaj.

Kod diapazonları (`ErrorCode`):

| Modul | Diapazon |
| --- | --- |
| Ümumi / validasiya / sistem | ERR-1000 - 1099 |
| Student | ERR-2000 - 2099 |
| Course | ERR-2100 - 2199 |
| Department | ERR-2200 - 2299 |
| Employee | ERR-2300 - 2399 |
| Author | ERR-2400 - 2499 |
| Book | ERR-2500 - 2599 |
| User / Passport | ERR-2600 - 2699 |
| Auth / Account | ERR-2700 - 2799 |

Dil: `?lang=` parametri və ya `Accept-Language` header-i. `az` (default), `en` / `eng`, `ru` / `rus`. Mesajlar `src/main/resources/i18n/messages*.properties`-dədir.

Trace ID: sorğuda `X-Trace-Id` header-i göndərilsə (8-64 simvol, hərf, rəqəm, tire) o istifadə olunur, yoxsa yeni UUID yaranır. Eyni id cavab header-ində, `ErrorResponse.traceId`-də və log sətirlərində olur. `LoggingAspect` controller və service metodlarının giriş/çıxışını və müddətini loglayır, şifrə və token parametrləri `***` ilə gizlədilir. Log faylı: `logs/app.log`.

## Postman

Collection: [`postman/school-api.postman_collection.json`](postman/school-api.postman_collection.json)

Postman-da **Import** edin. `baseUrl` dəyişəni `http://localhost:8080/test-demo` olaraq təyin olunub.

## Unit testlər

`service` paketindəki hər class unit testlə yoxlanır: JUnit 5 + Mockito + AssertJ. Spring context və baza qalxmır, repository-lər, mapper-lər və digər service-lər mock olunur.

| Service | Test class |
| --- | --- |
| `AccountService` | `AccountServiceTest` |
| `AuditLogService` | `AuditLogServiceTest` |
| `AuthService` | `AuthServiceTest` |
| `AuthorService` | `AuthorServiceTest` |
| `BookService` | `BookServiceTest` |
| `CacheService` | `CacheServiceTest` |
| `CourseService` | `CourseServiceTest` |
| `DepartmentService` | `DepartmentServiceTest` |
| `EmployeeService` | `EmployeeServiceTest` |
| `EnrollmentService` | `EnrollmentServiceTest` |
| `IsolationDemoService` | `IsolationDemoServiceTest` |
| `SalaryService` | `SalaryServiceTest` |
| `StudentService` | `StudentServiceTest` |
| `TokenBlacklistService` | `TokenBlacklistServiceTest` |
| `TokenService` | `TokenServiceTest` |
| `UserService` | `UserServiceTest` |

Testlər `src/test/java/com/spring_boot/school_department/service/`-dədir.

Yalnız service testləri (baza və mühit dəyişənləri lazım deyil):

```bash
./gradlew test --tests 'com.spring_boot.school_department.service.*'
```

Bütün testlər, `SchoolDepartmentApplicationTests` də daxil. Bu test tam context qaldırır, ona görə baza və [mühit dəyişənləri](#mühit-dəyişənləri) lazımdır:

```bash
docker compose up -d
export JWT_SECRET='<openssl-in-verdiyi-dəyər>'
export ADMIN_PASSWORD='<öz-şifrəniz>'
./gradlew test
```

### Coverage (JaCoCo)

```bash
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

- HTML hesabat: `build/reports/jacoco/test/html/index.html`
- `jacocoTestCoverageVerification` `service` paketinin line və ya branch coverage-i 100%-dən aşağı düşsə build-i dayandırır.

## Liquibase

Cədvəlləri artıq Hibernate yox, Liquibase yaradır (`ddl-auto: validate`). Changelog: `src/main/resources/db/changelog/`.

| Fayl | Məzmun |
| --- | --- |
| `001`-`004` | Cədvəllər, FK, unique və check constraint-lər |
| `005-tag-v1.0` | `v1.0` teqi (1-ci tapşırığın son sxemi) |
| `006-add-course-capacity` | `courses.capacity` + `chk_course_capacity` |
| `007-create-audit-logs` | `audit_logs` cədvəli + `chk_audit_logs_status` |
| `008-create-auth-tables` | `accounts`, `auth_tokens`, `token_blacklist` + `chk_accounts_role` |
| `100-seed-data` | CSV seed data (`data/` qovluğu), `context: dev` |

Əmrlər (Liquibase CLI, Docker ilə):

```bash
LB="docker run --rm --network host -v $PWD/src/main/resources:/liquibase/changelog liquibase/liquibase:4.31 \
  --search-path=/liquibase/changelog --changelog-file=db/changelog/db.changelog-master.yaml \
  --url=jdbc:postgresql://localhost:5432/school_db --username=postgres --password=postgres --contexts=dev"

$LB status --verbose        # icra olunmamış changeSet-lər
$LB update                  # bütün changeSet-ləri tətbiq et
$LB rollback --tag=v1.0     # v1.0-a qayıt: capacity, audit_logs, auth cədvəlləri və seed silinir
```

> zsh-də `$LB` əvəzinə `${=LB}` yazın.

**databasechangeloglock nədir?** Bir sətirlik kilid cədvəlidir. Liquibase işə düşəndə kilidi götürür, bitəndə buraxır. Beləliklə eyni anda iki instance (məs. iki server) eyni changeSet-i paralel icra etmir.

**Niyə komandada `ddl-auto: update` təhlükəlidir?** Hər developerin entity-si fərqli ola bilər və `update` bazanı ondan asılı olaraq dəyişir. O, sütun silmir, adı dəyişəndə yeni sütun yaradır, data miqrasiyası etmir, nə dəyişdiyinin tarixçəsi qalmır və geri qaytarmaq olmur. Liquibase-də hər dəyişiklik versiyalı changeSet-dir: git-də görünür, hər mühitdə eyni ardıcıllıqla bir dəfə icra olunur, checksum ilə dəyişdirilməkdən qorunur və rollback olunur.

## Second-level cache

Cache-lənən: Department, Course, Author, AuthorBook (`READ_WRITE`), Book (`NONSTRICT_READ_WRITE`), `Author.authorBooks`, `Book.authorBooks` collection-ları və `AuthorRepository.findAllByCountry` (query cache). Region-lar `src/main/resources/ehcache.xml`-dədir.

| Metod | URL | Status | Təsvir |
| --- | --- | --- | --- |
| GET | `/api/cache/stats` | 200 | Hit, miss, put, query cache hit və region-lar üzrə statistika |
| DELETE | `/api/cache` | 204 | Bütün L2 cache-i təmizlə |
| DELETE | `/api/cache/{region}` | 204 | Bir region-u təmizlə (`departments`, `courses`, `authors`, `books`, `author-books`), tapılmasa 404 `ERR-1011` |
| DELETE | `/api/cache/stats` | 204 | Statistikanı sıfırla |
| PUT | `/api/courses/credits?amount=1` | 200 | Bulk update (`@Modifying`), bütün `courses` region-u təmizlənir |

**Query cache-də yeni müəllif əlavə edəndə niyə SQL yenidən görünür?** Hibernate `authors` cədvəlinin son dəyişmə vaxtını `default-update-timestamps-region`-da saxlayır. Insert-dən sonra bu vaxt yenilənir, query cache-dəki nəticə ondan köhnə olur və etibarsız sayılır.

**1. First-level və second-level cache fərqi?** First-level cache `EntityManager`-ə (Session) aiddir, söndürülmür və tranzaksiya/session bitəndə silinir. Second-level cache `EntityManagerFactory`-yə aiddir, bütün session-lar arasında paylaşılır, tətbiq işlədikcə yaşayır və ömrü TTL/eviction ilə idarə olunur.

**2. `READ_ONLY`, `NONSTRICT_READ_WRITE`, `READ_WRITE` fərqi?** `READ_ONLY` heç vaxt dəyişməyən data üçündür, update olsa exception atılır. `NONSTRICT_READ_WRITE` kilid qoymur, commit-dən sonra cache-dən silir; qısa müddət köhnə data görünə bilər. `READ_WRITE` soft lock ilə işləyir, köhnə data oxunmasının qarşısını alır, amma daha bahalıdır. Book nadir dəyişir və qiymətin bir neçə millisaniyə köhnə görünməsi kritik deyil, ona görə daha yüngül `NONSTRICT_READ_WRITE` kifayətdir.

**3. Query cache niyə tək işləmir?** Query cache yalnız nəticədəki entity id-lərini saxlayır. Entity cache olmasa, Hibernate hər id üçün bazaya ayrıca `SELECT` göndərir (N+1) və query cache faydasız, hətta zərərli olur.

## Transactions (propagation)

| Metod | URL | Status | Ssenari |
| --- | --- | --- | --- |
| POST | `/api/employees/{id}/transfer` | 200 | C1 `REQUIRED` + C2 `REQUIRES_NEW` audit. Body: `{ "departmentId": 2, "raisePercent": 15 }` |
| POST | `/api/tx-demo/reserve-seat/{courseId}` | 409 | C3 `MANDATORY` tranzaksiyasız çağırış (`ERR-1009`) |
| POST | `/api/students/{studentId}/courses/{courseId}` | 201 | `reserveSeat` tranzaksiya daxilindən çağırılır, kurs doludursa 409 (`ERR-2103`) |

Seed-də işçi 3 (maaş 9500) +15% ilə limiti (10 000) keçir: departament dəyişmir, `audit_logs`-da `FAILED` sətri qalır.

**1. `REQUIRES_NEW` connection pool-u nə vaxt tükəndirir?** Xarici tranzaksiya dayandırılır, amma connection-ı buraxmır, yeni tranzaksiya ikinci connection götürür. Çoxlu paralel sorğuda hər biri iki connection tutur; pool dolanda daxili tranzaksiyalar connection gözləyir, xaricilər də onları gözləyir və deadlock/timeout yaranır.

**2. Checked exception-da Spring rollback edirmi?** Xeyr. Default olaraq yalnız `RuntimeException` və `Error`-da rollback olur. Checked exception-da da rollback lazımdırsa `@Transactional(rollbackFor = Exception.class)` yazılır.

**3. Daxili metod exception atır, xarici onu udarsa?** Daxili metod eyni tranzaksiyada (`REQUIRED`) olduğu üçün tranzaksiyanı `rollback-only` işarələyir. Xarici metod normal bitib commit etməyə çalışanda Spring `UnexpectedRollbackException` atır və heç nə yazılmır.

## Isolation

| Ssenari | Metod | URL | Status | İcazəli isolation |
| --- | --- | --- | --- | --- |
| D1 non-repeatable read | GET | `/api/isolation/departments/{id}/salary-report?isolation=...&delayMs=8000` | 200 | `READ_COMMITTED`, `REPEATABLE_READ` |
| D2 lost update | POST | `/api/isolation/employees/{id}/raise?amount=100&isolation=...&delayMs=8000` | 200 | `READ_COMMITTED`, `REPEATABLE_READ` |
| D3 write skew | POST | `/api/isolation/courses/{courseId}/enroll/{studentId}?isolation=...&delayMs=8000` | 200 | `REPEATABLE_READ`, `SERIALIZABLE` |

Başqa isolation göndərilsə 400 (`ERR-1010`). `delayMs` verilməsə 0-dır.

Demo skript (tətbiq seed data ilə işləməlidir):

```bash
./scripts/isolation-demo.sh
# başqa ünvan üçün: BASE=http://localhost:8080/test-demo ./scripts/isolation-demo.sh
```

Skript hər ssenarini əvvəl zəif, sonra güclü səviyyə ilə işlədir. D3 üçün seed-də 5 nömrəli kursun `capacity`-si 1-dir.

**1. PostgreSQL-də `READ_UNCOMMITTED` niyə `READ_COMMITTED` kimi işləyir?** PostgreSQL MVCC istifadə edir: hər sorğu yalnız commit olunmuş sətir versiyalarını görür. Commit olunmamış versiyanı oxumaq mexanizmi yoxdur, ona görə `READ_UNCOMMITTED` avtomatik `READ_COMMITTED` olur və dirty read göstərmək mümkün deyil.

**2. PostgreSQL-in `REPEATABLE_READ`-i ANSI-dən nə ilə güclüdür?** ANSI `REPEATABLE_READ`-də phantom read mümkündür. PostgreSQL-də isə tranzaksiya başlanğıcdakı snapshot-u görür, yeni əlavə olunan sətirlər də görünmür, yəni phantom read olmur. Eyni sətri paralel yeniləmədə isə serialization xətası verir.

**3. D2-ni isolation dəyişmədən necə həll etmək olar?**
- `@Version` (optimistic lock): kilid yoxdur, `UPDATE ... WHERE version = ?` 0 sətir qaytarsa exception atılır. Toqquşma az olanda yaxşıdır, xəta halında retry lazımdır.
- `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`): sətir oxunanda kilidlənir, ikinci tranzaksiya gözləyir və artıq yenilənmiş dəyəri oxuyur. Xəta yoxdur, amma gözləmə və deadlock riski var.

> Qeyd: `BaseEntity`-də `@Version` 1-ci tapşırıqdan qalıb və onu bilərəkdən saxlamışam. Ona görə D2-də `READ_COMMITTED`-də də update itmir, ikinci sorğu optimistic lock xətası ilə 409 alır. Yəni lost update-in qarşısını elə `@Version` alır.
