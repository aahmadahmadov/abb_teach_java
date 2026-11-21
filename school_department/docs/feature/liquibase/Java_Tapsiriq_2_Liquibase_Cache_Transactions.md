# Praktiki Tapşırıq 2

*Spring Boot + Spring Data JPA: Liquibase, Second-Level Cache, Transaction Propagation və Isolation*

Bu tapşırıq birinci tapşırığın davamıdır: eyni **school-api** layihəsi üzərində işləyirsiniz, yeni layihə açılmır. Cədvəlləri artıq Hibernate yox, Liquibase yaradır; sxemin bütün tarixi changelog-da saxlanılır.

| **Tələbənin adı, soyadı:** |  |
|---|---|
| **Qrup:** |  |
| **Təhvil tarixi:** |  |
| **Layihə:** | school-api (paket: az.iktex.organization.structure.testders) – 1-ci tapşırığın reposu, yeni branch: `feature/liquibase-cache-tx` |

## 1. Tapşırığın məqsədi

Tapşırıq dörd hissədən ibarətdir:

- **Hissə A – Liquibase:** bütün cədvəllər, FK, unique və check constraint-lər changelog vasitəsilə yaradılır; `ddl-auto` artıq sxemi dəyişmir.
- **Hissə B – Second-Level Cache:** Hibernate L2 cache (JCache + Ehcache) inteqrasiya olunur; entity, collection və query cache ayrı-ayrılıqda göstərilir.
- **Hissə C – Propagation:** 3 fərqli propagation növü (`REQUIRED`, `REQUIRES_NEW`, `MANDATORY`) real biznes ssenarisində tətbiq olunur.
- **Hissə D – Isolation:** 3 fərqli isolation səviyyəsi (`READ_COMMITTED`, `REPEATABLE_READ`, `SERIALIZABLE`) paralel sorğularla sınaqdan keçirilir və fərq sübut olunur.

Tapşırığı bitirdikdən sonra siz sxem miqrasiyası, checksum, rollback, cache region və concurrency strategiyaları, tranzaksiya sərhədləri, lost update və write skew anlayışlarını real kodda görmüş olacaqsınız.

## 2. Yeni asılılıqlar və konfiqurasiya

Mövcud `build.gradle`-a aşağıdakı asılılıqları əlavə edin. Versiyaları Spring Boot BOM idarə edir; Ehcache-in `jakarta` classifier-i mütləqdir, əks halda Hibernate onu görməyəcək.

```
implementation 'org.springframework.boot:spring-boot-starter-liquibase'
implementation 'org.hibernate.orm:hibernate-jcache'
implementation 'org.ehcache:ehcache:3.10.8:jakarta'
```

`application.yaml` aşağıdakı kimi dəyişməlidir (yalnız dəyişən hissələr):

```
spring:
  jpa:
    hibernate:
      ddl-auto: validate          # update YOX! Sxemi yalnız Liquibase dəyişir
    properties:
      hibernate:
        generate_statistics: true
        cache:
          use_second_level_cache: true
          use_query_cache: true
          region:
            factory_class: jcache
        javax:
          cache:
            provider: org.ehcache.jsr107.EhcacheCachingProvider
            uri: classpath:ehcache.xml
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
    contexts: dev
 
logging:
  level:
    org.hibernate.cache: debug
    org.springframework.orm.jpa.JpaTransactionManager: debug
    org.springframework.transaction: trace
```

Paket strukturuna əlavə olunanlar:

| **Paket / fayl** | **Məzmun** |
|---|---|
| `resources/db/changelog/` | master fayl + `changes/` qovluğu |
| `resources/ehcache.xml` | cache region-ların konfiqurasiyası |
| `entity/AuditLog`, `enums/AuditStatus` | Hissə C üçün audit cədvəli |
| `service/AuditLogService`, `service/SalaryService`, `service/EnrollmentService` | propagation ssenariləri |
| `controller/IsolationDemoController`, `service/IsolationDemoService` | isolation ssenariləri |
| `controller/CacheController` | cache statistikası və təmizləmə |
| `exception/GlobalExceptionHandler` | `@RestControllerAdvice` – 404 / 400 / 409 cavabları |

**Diqqət!** 1-ci tapşırıqda cədvəllər `ddl-auto: update` ilə yaranıb. Başlamazdan əvvəl `docker compose down -v` ilə köhnə bazanı silin, cədvəlləri sıfırdan Liquibase yaratsın.

## 3. Hissə A – Liquibase ilə cədvəllərin yaradılması

Bazadakı hər cədvəl, sütun, FK, unique və check constraint changelog-dan gəlməlidir. Entity-lərdəki `@Table`, `@Column`, `@Check` annotasiyaları qalır, amma artıq yalnız `validate` üçün istifadə olunur: entity ilə sxem uyğun gəlməsə, tətbiq başlamamalıdır.

### 3.1. Changelog strukturu

Format: YAML. Hər modul ayrıca faylda, master fayl isə yalnız `include` edir:

```
resources/db/changelog/
 ├── db.changelog-master.yaml
 └── changes/
      ├── 001-create-users-passports.yaml
      ├── 002-create-departments-employees.yaml
      ├── 003-create-students-courses.yaml
      ├── 004-create-authors-books.yaml
      ├── 005-tag-v1.0.yaml
      ├── 006-add-course-capacity.yaml
      ├── 007-create-audit-logs.yaml
      └── 100-seed-data.yaml          (context: dev)
# db.changelog-master.yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/001-create-users-passports.yaml
  - include:
      file: db/changelog/changes/002-create-departments-employees.yaml
  # ... qalanları ardıcıllıqla
```

### 3.2. ChangeSet qaydaları

1. Hər changeSet-in `id`-si fayl nömrəsi ilə başlasın (`001-1`, `001-2`), `author` – sizin GitHub istifadəçi adınız.
2. Bir changeSet = bir məntiqi dəyişiklik (bir cədvəl, bir FK və ya bir sütun). Bütün sxemi tək changeSet-ə yığmaq olmaz.
3. Bütün constraint adları 1-ci tapşırıqdakı ilə eyni olmalıdır: `fk_passport_user`, `fk_employee_department`, `fk_sc_student`, `fk_sc_course`, `uk_student_course`, `fk_ab_author`, `fk_ab_book`, `uk_author_book`.
4. Check constraint-lər (`chk_employee_salary`, `chk_book_price`) `sql` change növü ilə yazılsın və onlar üçün `rollback` bloku əl ilə verilsin.
5. `users.gender_id` sütunu `INTEGER` olsun və check constraint ilə yalnız 1 və 2 qəbul etsin (`chk_users_gender`).
6. Hər `createTable` changeSet-inə `preConditions` əlavə edin: `onFail: MARK_RAN`, `not: tableExists`.
7. `005-tag-v1.0.yaml` faylında `tagDatabase` ilə `v1.0` teqi qoyun – bu, 1-ci tapşırığın sxeminin son vəziyyətidir.
8. **Tətbiq olunmuş changeSet-i heç vaxt dəyişməyin.** Hər yeni dəyişiklik yeni changeSet ilə gəlir.

Nümunə (yalnız istiqamət üçün, tam deyil):

```
databaseChangeLog:
  - changeSet:
      id: 002-1
      author: ali-mammadov
      preConditions:
        - onFail: MARK_RAN
        - not:
            - tableExists:
                tableName: departments
      changes:
        - createTable:
            tableName: departments
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    primaryKeyName: pk_departments
              - column:
                  name: name
                  type: VARCHAR(50)
                  constraints:
                    nullable: false
                    unique: true
                    uniqueConstraintName: uk_department_name
```

### 3.3. Yeni dəyişikliklər (v1.0-dan sonra)

| **Fayl** | **Dəyişiklik** | **Nə üçün lazımdır** |
|---|---|---|
| 006-add-course-capacity | `courses` cədvəlinə `capacity INTEGER NOT NULL DEFAULT 30` + `chk_course_capacity` (capacity > 0) | Hissə D – kurs tutumu ssenarisi |
| 007-create-audit-logs | `audit_logs` cədvəli (aşağıda) | Hissə C – REQUIRES_NEW ssenarisi |
| 100-seed-data | `loadData` (CSV) və ya `insert` ilə ən azı 3 departament, 10 işçi, 5 kurs, 10 tələbə, 5 müəllif, 8 kitab | Demo və cache testləri. `context: dev` |

`audit_logs` cədvəli:

| **Sütun** | **Tip** | **Məhdudiyyət** |
|---|---|---|
| id | BIGINT | PK, auto increment |
| action | VARCHAR(50) | not null (məs. `SALARY_RAISE`, `ENROLL`) |
| entity_name | VARCHAR(50) | not null |
| entity_id | BIGINT | null ola bilər |
| status | VARCHAR(20) | not null, `SUCCESS` və ya `FAILED` (check constraint) |
| message | VARCHAR(500) |  |
| created_at | TIMESTAMP | not null, default `CURRENT_TIMESTAMP` |

Entity tərəfində `Course`-a `capacity` sahəsi, yeni `AuditLog` entity-si və `AuditStatus` enum-u (`@Enumerated(EnumType.STRING)`) əlavə edin.

### 3.4. Changelog-a nəzarət (yoxlanılacaq)

- Tətbiq işə düşdükdən sonra `databasechangelog` cədvəlində bütün changeSet-lərin `EXECUTED`, seed changeSet-in isə `contexts = dev` ilə yazıldığını göstərin.
- `databasechangeloglock` cədvəlinin nə üçün olduğunu README-də 2–3 cümlə ilə izah edin.
- **Checksum testi:** tətbiq olunmuş bir changeSet-də sütun uzunluğunu dəyişin, tətbiqi başladın, `ValidationFailedException` (checksum uyğunsuzluğu) xətasının ekran görüntüsünü çəkin, sonra dəyişikliyi geri qaytarın.
- **Validate testi:** entity-də bir sütunun adını müvəqqəti dəyişin – Hibernate `validate` səbəbindən tətbiqin başlamadığını göstərin.
- **Rollback testi:** Liquibase Gradle plugin və ya CLI ilə `rollback v1.0` icra edin – `capacity` sütununun və `audit_logs` cədvəlinin silindiyini, sonra yenidən `update` ilə qayıtdığını göstərin.

**Sual (README-də cavablayın):** Niyə komanda ilə işləyəndə `ddl-auto: update` təhlükəlidir və Liquibase hansı problemi həll edir?

## 4. Hissə B – Hibernate Second-Level Cache

L2 cache `EntityManager`-lər (tranzaksiyalar) arasında paylaşılır. Məqsəd: təkrar oxunan, nadir dəyişən məlumat üçün eyni `SELECT`-in ikinci dəfə bazaya getmədiyini sübut etmək.

### 4.1. Hansı entity cache-lənəcək

| **Entity / əlaqə** | **Cache növü** | **Strategiya** | **Region adı** |
|---|---|---|---|
| Department | Entity | `READ_WRITE` | `departments` |
| Course | Entity | `READ_WRITE` | `courses` |
| Author | Entity | `READ_WRITE` | `authors` |
| Book | Entity | `NONSTRICT_READ_WRITE` | `books` |
| AuthorBook | Entity | `READ_WRITE` | `author-books` |
| Author.authorBooks, Book.authorBooks | Collection | `READ_WRITE` | `author-authorBooks`, `book-authorBooks` |
| AuthorRepository.findAllByCountry | Query cache | – | default query results region |

User, Passport, Employee, Student və AuditLog **cache-lənmir**. Employee-nin niyə cache-lənmədiyini kodda şərh kimi izah edin (ipucu: maaş tez-tez dəyişir və Hissə D-də paralel yazılır).

Nümunə:

```
@Entity
@Table(name = "courses")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "courses")
public class Course { ... }
 
// collection cache – element entity-si (AuthorBook) də cache-lənməlidir
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "author-authorBooks")
@OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
private List<AuthorBook> authorBooks = new ArrayList<>();
 
// query cache
@QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
List<Author> findAllByCountry(String country);
```

### 4.2. ehcache.xml

Hər region üçün ayrıca `<cache alias="...">` yazın: `heap` ölçüsü (məs. 1000 entries) və `expiry` (TTL: Course/Department üçün 10 dəqiqə, Book üçün 2 dəqiqə). `default-update-timestamps-region` və `default-query-results-region` region-larını da konfiqurasiya edin. Region tapılmadıqda Hibernate xəbərdarlıq verir – log-da belə xəbərdarlıq olmamalıdır.

### 4.3. CacheController (/api/cache)

| **Metod** | **URL** | **Təsvir** |
|---|---|---|
| GET | /api/cache/stats | `SessionFactory.getStatistics()`-dən: `secondLevelCacheHitCount`, `MissCount`, `PutCount`, `queryCacheHitCount`, region-lar üzrə say. Response record ilə qaytarılsın |
| DELETE | /api/cache | Bütün L2 cache-i təmizlə (`entityManagerFactory.getCache().evictAll()`) |
| DELETE | /api/cache/{region} | Yalnız bir entity sinfini təmizlə (məs. `courses` → `evict(Course.class)`) |
| DELETE | /api/cache/stats | Statistikanı sıfırla (`statistics.clear()`) |

### 4.4. Biznes qaydaları və diqqət ediləcək məqamlar

- `findById` L2 cache-dən oxuya bilir, amma `JOIN FETCH` olan JPQL sorğuları həmişə bazaya gedir. `GET /api/courses/{id}` üçün `findById` istifadə olunan ayrıca bir yol saxlayın və fərqi log-da göstərin.
- `@Modifying` JPQL bulk update/delete həmin entity-nin bütün region-unu təmizləyir. Bunu bir bulk sorğu ilə sınayın və `/api/cache/stats`-da region-un boşaldığını göstərin.
- Book update edildikdən sonra növbəti `GET`-in yeni qiyməti qaytardığını yoxlayın (`NONSTRICT_READ_WRITE` cache-i commit-dən sonra invalidate edir).

**Suallar (README-də cavablayın):**

1. First-level və second-level cache arasındakı fərq nədir? Hər birinin ömrü nə qədərdir?
2. `READ_ONLY`, `NONSTRICT_READ_WRITE`, `READ_WRITE` arasındakı fərq nədir? Book üçün niyə `NONSTRICT_READ_WRITE` seçilə bilər?
3. Query cache niyə tək başına işləmir və entity cache olmadan nə baş verir?

## 5. Hissə C – Transaction Propagation

Üç fərqli propagation növü bir biznes axınında birlikdə işləməlidir: işçinin başqa departamentə köçürülməsi və maaş artımı. Hər ssenari üçün log-da (`JpaTransactionManager` debug) tranzaksiyanın yaradıldığını, qoşulduğunu və ya dayandırıldığını göstərin.

### 5.1. Ssenari C1 – REQUIRED (ortaq tranzaksiya)

`POST /api/employees/{id}/transfer` – body: `{ "departmentId": 2, "raisePercent": 15 }`

- `EmployeeService.transfer(...)` – `@Transactional` (default `REQUIRED`): işçinin departamentini dəyişir, sonra `SalaryService.raise(employee, percent)` çağırır.
- `SalaryService.raise(...)` – `@Transactional(propagation = Propagation.REQUIRED)`: yeni maaşı hesablayır. Yeni maaş **10 000-dən çox** olarsa `IllegalStateException` atır.
- **Gözlənilən nəticə:** maaş limiti keçəndə departament dəyişikliyi də geri qaytarılır (hər ikisi eyni tranzaksiyadadır). Bazada işçinin köhnə departamentdə qaldığını göstərin.

### 5.2. Ssenari C2 – REQUIRES_NEW (müstəqil audit)

- `AuditLogService.log(action, entityName, entityId, status, message)` – `@Transactional(propagation = Propagation.REQUIRES_NEW)`.
- `transfer` uğurla bitəndə `SUCCESS`, xəta olanda `FAILED` audit yazılır. Xəta halında `catch` blokunda audit yazılır və exception yenidən atılır.
- **Gözlənilən nəticə:** əsas tranzaksiya rollback olsa da, `audit_logs` cədvəlində `FAILED` sətri qalır.
- **Müqayisə:** `AuditLogService.log`-u müvəqqəti `REQUIRED` edin və eyni sorğunu göndərin – `FAILED` sətrinin itdiyini göstərin. Sonra `REQUIRES_NEW`-a qaytarın.

### 5.3. Ssenari C3 – MANDATORY (tranzaksiyasız çağırış qadağandır)

- `EnrollmentService.reserveSeat(Long courseId)` – `@Transactional(propagation = Propagation.MANDATORY)`: kursdakı tələbə sayını `capacity` ilə müqayisə edir; yer yoxdursa `IllegalStateException`.
- Bu metod yalnız `StudentService.enroll(...)` daxilindən (yəni mövcud tranzaksiyanın içindən) çağırılır.
- Demo endpoint: `POST /api/tx-demo/reserve-seat/{courseId}` – controller bu metodu **birbaşa** çağırır (controller-də `@Transactional` yoxdur).
- **Gözlənilən nəticə:** `IllegalTransactionStateException` ("No existing transaction found for transaction marked with propagation 'mandatory'"). `GlobalExceptionHandler` bunu 500 deyil, aydın mesajla 409 kimi qaytarsın.

### 5.4. Ümumi tələblər

- Propagation göstərilən metodlar **mütləq ayrı bean-də** olmalıdır. Eyni sinif daxilində `this.raise(...)` çağırışında propagation işləmir (self-invocation, proxy keçilmir). Bunu bir dəfə sınayın: `raise`-i `EmployeeService`-in içinə köçürüb `REQUIRES_NEW` verin, log-da yeni tranzaksiyanın açılmadığını göstərin, sonra geri qaytarın.
- Service sinfinin üzərindəki `@Transactional(readOnly = true)` qalır; yazma metodlarında propagation açıq yazılsın, hətta default olsa belə (`REQUIRED`), oxuyan görsün.
- **Bonus (+5):** `NESTED` ilə savepoint ssenarisi – toplu kursa yazılmada (`POST /api/students/{id}/courses/bulk`) dolu kurs atılır, qalanları yazılır.

**Suallar (README-də cavablayın):**

1. `REQUIRES_NEW` hansı halda connection pool-u tükəndirə bilər?
2. Checked exception atılanda Spring default olaraq rollback edirmi? `rollbackFor` nə vaxt lazımdır?
3. C1-də daxili metod exception atıb, xarici metod onu `catch` edib udarsa nə baş verir? (`UnexpectedRollbackException`)

## 6. Hissə D – Isolation Level

Üç isolation səviyyəsi üç fərqli anomaliya üzərində sınanır. Hər ssenari iki paralel sorğu ilə göstərilir: biri `delayMs` parametri ilə gözləyir (`Thread.sleep`), digəri bu müddətdə məlumatı dəyişir. Hər endpoint `isolation` parametrini qəbul edir və servisdə müvafiq `@Transactional(isolation = ...)` metodu çağırılır.

| **Ssenari** | **Səviyyə** | **Anomaliya** | **Endpoint** |
|---|---|---|---|
| D1 | `READ_COMMITTED` vs `REPEATABLE_READ` | Non-repeatable read | `GET /api/isolation/departments/{id}/salary-report?isolation=...&delayMs=8000` |
| D2 | `READ_COMMITTED` vs `REPEATABLE_READ` | Lost update | `POST /api/isolation/employees/{id}/raise?amount=100&isolation=...&delayMs=8000` |
| D3 | `REPEATABLE_READ` vs `SERIALIZABLE` | Write skew (kurs tutumunun aşılması) | `POST /api/isolation/courses/{courseId}/enroll/{studentId}?isolation=...&delayMs=8000` |

### 6.1. D1 – Non-repeatable read

- Metod departamentin orta maaşını JPQL `avg` ilə oxuyur, `delayMs` gözləyir, sonra **eyni sorğunu yenidən** icra edir və iki dəyəri qaytarır: `{ "firstAvg": ..., "secondAvg": ..., "isolation": "..." }`.
- Gözləmə zamanı ikinci terminaldan `PUT /api/employees/{id}` ilə həmin departamentdən bir işçinin maaşını dəyişin.
- **Gözlənilən:** `READ_COMMITTED`-də iki dəyər fərqlidir, `REPEATABLE_READ`-də eynidir.
- Qeyd: entity yox, aggregate sorğu istifadə edin – entity ikinci dəfə first-level cache-dən gələcək və fərqi gizlədəcək. Bunu kodda şərh kimi yazın.

### 6.2. D2 – Lost update

- Metod `employeeRepository.findById` ilə işçini oxuyur, gözləyir, `salary = salary + amount` edir (dirty checking ilə).
- Eyni işçi üçün eyni anda iki sorğu göndərin (hər biri +100).
- **Gözlənilən:** `READ_COMMITTED`-də son maaş yalnız +100 artır (bir yeniləmə itir). `REPEATABLE_READ`-də ikinci tranzaksiya PostgreSQL xətası ilə düşür: *could not serialize access due to concurrent update*.
- `GlobalExceptionHandler` `ConcurrencyFailureException`-ı tutub **409 Conflict** qaytarsın.

### 6.3. D3 – Write skew

- Kursun `capacity`-sini seed data-da 1 edin. Metod kursdakı tələbə sayını `countByCoursesId` ilə yoxlayır, gözləyir, yer varsa tələbəni əlavə edir.
- Eyni kursa iki fərqli tələbəni eyni anda yazın.
- **Gözlənilən:** `REPEATABLE_READ`-də hər iki tələbə yazılır (kurs tutumu aşılır, çünki iki tranzaksiya fərqli sətirlərə yazır). `SERIALIZABLE`-də biri 409 alır.
- **Bonus (+5):** 409 alanda əməliyyatı avtomatik 3 dəfəyə qədər təkrarlayan sadə retry mexanizmi (ayrıca bean, hər cəhd yeni tranzaksiya).

### 6.4. Paralel sorğuların göndərilməsi

Postman-də iki tab kifayət etmir – sorğular ardıcıl gedə bilər. Aşağıdakı kimi skript və ya JUnit testi (`ExecutorService` + `CountDownLatch`) istifadə edin:

```
BASE=http://localhost:8080/test-demo/api/isolation
curl -s -X POST "$BASE/employees/1/raise?amount=100&isolation=READ_COMMITTED&delayMs=5000" &
curl -s -X POST "$BASE/employees/1/raise?amount=100&isolation=READ_COMMITTED&delayMs=5000" &
wait
curl -s http://localhost:8080/test-demo/api/employees/1
```

Skripti `scripts/isolation-demo.sh` adı ilə repoya əlavə edin; hər ssenari üçün iki variantı (zəif və güclü səviyyə) ardıcıl işlətsin.

**Suallar (README-də cavablayın):**

1. PostgreSQL-də `READ_UNCOMMITTED` niyə əslində `READ_COMMITTED` kimi işləyir? Dirty read-i niyə göstərə bilmirik?
2. PostgreSQL-in `REPEATABLE_READ`-i ANSI standartından nə ilə güclüdür (phantom read)?
3. D2-ni isolation dəyişmədən necə həll etmək olar? (`@Version` optimistic lock, `PESSIMISTIC_WRITE`) – qısa müqayisə.

## 7. Yoxlama ssenarisi

Aşağıdakıların ekran görüntülərini hesabata əlavə edin:

1. Boş bazada tətbiqin işə düşməsi və log-da bütün changeSet-lərin icra olunması.
2. `databasechangelog` cədvəli (DBeaver/pgAdmin) – id, author, filename, contexts, tag sütunları görünsün.
3. Checksum xətası (`ValidationFailedException`) və `validate` xətası.
4. `rollback v1.0` əvvəli və sonrası: `courses.capacity` və `audit_logs`.
5. `GET /api/courses/{id}` iki dəfə: birinci dəfə konsolda `SELECT` var, ikinci dəfə yoxdur; `/api/cache/stats`-da hit sayının artması.
6. Query cache: `GET /api/authors?country=Azerbaijan` iki dəfə – ikincidə SQL yoxdur. Sonra yeni müəllif əlavə edin – növbəti sorğuda SQL yenidən görünür (niyə? izah edin).
7. C1: limit keçən transfer – departamentin dəyişmədiyi; C2: `audit_logs`-da `FAILED` sətri; C3: `IllegalTransactionStateException` cavabı.
8. D1, D2, D3 – hər biri üçün zəif və güclü səviyyənin nəticəsi yan-yana.

## 8. Qiymətləndirmə meyarları

| **Meyar** | **Bal** |
|---|---|
| Hissə A – Liquibase (struktur, constraint adları, preconditions, seed, tag, rollback, checksum testi) | 30 |
| Hissə B – Second-Level Cache (ehcache.xml, entity/collection/query cache, CacheController, statistika) | 25 |
| Hissə C – Propagation (REQUIRED, REQUIRES_NEW, MANDATORY, self-invocation testi) | 20 |
| Hissə D – Isolation (D1, D2, D3, 409 handler, demo skript) | 20 |
| README sualları və kod şərhləri | 5 |
| **Cəmi** | **100** |
| Bonus: NESTED savepoint (+5), retry mexanizmi (+5) | +10 |

## 9. Təhvil qaydası

- İş 1-ci tapşırığın reposunda `feature/liquibase-cache-tx` branch-ında aparılır; təhvil Pull Request linki ilə olur.
- README-yə yeni bölmələr əlavə edin: Liquibase əmrləri (update, rollback, status), cache endpoint-ləri, `scripts/isolation-demo.sh`-ın necə işlədilməsi və bütün sualların cavabları.
- Postman collection-ı yeniləyin: `Cache`, `Transactions`, `Isolation` qovluqları olsun.
- `ddl-auto: update` və ya `create` kodda heç yerdə qalmamalıdır – tapılarsa Hissə A-dan 10 bal çıxılır.
- Tətbiq olunmuş changeSet-in sonradan dəyişdirildiyi görünərsə (git history), Hissə A-dan 10 bal çıxılır.

**Uğurlar!**
