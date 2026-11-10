# School API

Spring Boot + Spring Data JPA ilə entity əlaqələri: OneToOne, OneToMany, ManyToMany.

## Texnologiyalar

- Java 26, Gradle
- Spring Boot 4.1 (`webmvc`, `data-jpa`)
- PostgreSQL 16 (Docker Compose)
- Lombok, MapStruct 1.6.3
- springdoc-openapi (Swagger UI)

## İşə salma

```bash
docker compose down -v   # 1-ci tapşırığın köhnə bazasını silir, sxemi Liquibase yaradacaq
docker compose up -d
./gradlew bootRun
```

Base URL: `http://localhost:8080/test-demo`

## API Docs

- Swagger UI: http://localhost:8080/test-demo/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/test-demo/v3/api-docs

## Modul 1: User ↔ Passport (OneToOne)

| Metod | URL | Təsvir |
| --- | --- | --- |
| POST | `/api/users` | User yarat |
| GET | `/api/users/{id}` | User passport ilə birlikdə (JOIN FETCH) |
| GET | `/api/users/by-email?email=...` | Email ilə axtarış |
| GET | `/api/users?gender=MALE` | Hamısı, gender verilsə filter |
| GET | `/api/users/without-passport` | Passportu olmayan user-lər |
| PUT | `/api/users/{id}` | Yenilə |
| DELETE | `/api/users/{id}` | Sil (passport da silinir) |
| POST | `/api/users/{id}/passport` | Passport əlavə et |
| GET | `/api/users/{id}/passport` | User-in passportu |
| DELETE | `/api/users/{id}/passport` | Passportu sil (orphanRemoval) |

Nümunə request:

```http
POST /api/users
{
  "name": "Əli",
  "surname": "Məmmədov",
  "email": "ali@mail.com",
  "gender": "MALE"
}
```

## Modul 2: Department → Employee (OneToMany)

| Metod | URL | Təsvir |
| --- | --- | --- |
| POST | `/api/departments` | Department yarat (ad unikal) |
| GET | `/api/departments/{id}` | İşçilər ilə birlikdə (JOIN FETCH) |
| GET | `/api/departments` | Hamısı (işçilərsiz) |
| GET | `/api/departments/without-employees` | İşçisi olmayan departamentlər |
| PUT | `/api/departments/{id}` | Yenilə |
| DELETE | `/api/departments/{id}` | Sil (işçisi varsa olmaz) |
| POST | `/api/employees` | İşçi yarat |
| GET | `/api/employees/{id}` | İşçi |
| GET | `/api/employees/department/{departmentId}` | Departamentin işçiləri |
| GET | `/api/employees/salary-range?min=500&max=2000` | Maaş aralığına görə |
| GET | `/api/employees/department/{id}/average-salary` | Orta maaş |
| PUT | `/api/employees/{id}` | Yenilə (departament dəyişə bilər) |
| DELETE | `/api/employees/{id}` | İşçini sil |

## Modul 3: Student ↔ Course (ManyToMany, @JoinTable)

| Metod | URL | Təsvir |
| --- | --- | --- |
| POST | `/api/students` | Tələbə yarat |
| GET | `/api/students/{id}` | Kurslar ilə birlikdə (left join fetch) |
| GET | `/api/students` | Hamısı |
| GET | `/api/students/without-courses` | Heç bir kursa yazılmayanlar |
| PUT | `/api/students/{id}` | Yenilə |
| DELETE | `/api/students/{id}` | Sil (kurslar qalır) |
| POST | `/api/students/{studentId}/courses/{courseId}` | Kursa yaz |
| DELETE | `/api/students/{studentId}/courses/{courseId}` | Kursdan çıxar |
| POST | `/api/courses` | Kurs yarat (title unikal) |
| GET | `/api/courses` | Hamısı |
| GET | `/api/courses/{id}` | Biri |
| GET | `/api/courses/student/{studentId}` | Tələbənin kursları |
| GET | `/api/courses/{id}/students` | Kursun tələbələri |
| GET | `/api/courses/{id}/students/count` | Kursdakı tələbə sayı |
| PUT | `/api/courses/{id}` | Yenilə |
| DELETE | `/api/courses/{id}` | Sil (əvvəl tələbələrlə əlaqə qırılır) |

## Modul 4: Author ↔ AuthorBook ↔ Book (ManyToMany, 3-cü entity)

| Metod | URL | Təsvir |
| --- | --- | --- |
| POST | `/api/authors` | Müəllif yarat (eyni ad+soyad olmaz) |
| GET | `/api/authors/{id}` | Müəllif |
| GET | `/api/authors?country=Azerbaijan` | Hamısı, country verilsə filter |
| GET | `/api/authors/without-books` | Kitabı olmayan müəlliflər |
| GET | `/api/authors/{id}/books` | Müəllifin kitabları |
| PUT | `/api/authors/{id}` | Yenilə |
| DELETE | `/api/authors/{id}` | Sil (kitablar qalır) |
| POST | `/api/books` | Kitab yarat (title unikal) |
| GET | `/api/books/{id}` | Müəlliflər ilə birlikdə |
| GET | `/api/books?title=java` | Hamısı, title verilsə axtarış |
| GET | `/api/books/cheaper-than?price=20` | Qiyməti verilən məbləğə bərabər və ya aşağı olanlar |
| PUT | `/api/books/{id}` | Yenilə |
| DELETE | `/api/books/{id}` | Sil (müəlliflər qalır) |
| POST | `/api/books/{bookId}/authors/{authorId}` | Kitaba müəllif bağla |
| DELETE | `/api/books/{bookId}/authors/{authorId}` | Əlaqəni sil |

## Postman

Collection: [`postman/school-api.postman_collection.json`](postman/school-api.postman_collection.json)

Postman-da **Import** edin. `baseUrl` dəyişəni `http://localhost:8080/test-demo` olaraq təyin olunub.

## Liquibase

Cədvəlləri artıq Hibernate yox, Liquibase yaradır (`ddl-auto: validate`). Changelog: `src/main/resources/db/changelog/`.

| Fayl | Məzmun |
| --- | --- |
| `001`-`004` | Cədvəllər, FK, unique və check constraint-lər |
| `005-tag-v1.0` | `v1.0` teqi (1-ci tapşırığın son sxemi) |
| `006-add-course-capacity` | `courses.capacity` + `chk_course_capacity` |
| `007-create-audit-logs` | `audit_logs` cədvəli + `chk_audit_logs_status` |
| `100-seed-data` | CSV seed data (`data/` qovluğu), `context: dev` |

Əmrlər (Liquibase CLI, Docker ilə):

```bash
LB="docker run --rm --network host -v $PWD/src/main/resources:/liquibase/changelog liquibase/liquibase:4.31 \
  --search-path=/liquibase/changelog --changelog-file=db/changelog/db.changelog-master.yaml \
  --url=jdbc:postgresql://localhost:5432/school_db --username=postgres --password=postgres --contexts=dev"

$LB status --verbose        # icra olunmamış changeSet-lər
$LB update                  # bütün changeSet-ləri tətbiq et
$LB rollback --tag=v1.0     # v1.0-a qayıt: capacity, audit_logs və seed silinir
```

> zsh-də `$LB` əvəzinə `${=LB}` yazın.

**databasechangeloglock nədir?** Bir sətirlik kilid cədvəlidir. Liquibase işə düşəndə kilidi götürür, bitəndə buraxır. Beləliklə eyni anda iki instance (məs. iki server) eyni changeSet-i paralel icra etmir.

**Niyə komandada `ddl-auto: update` təhlükəlidir?** Hər developerin entity-si fərqli ola bilər və `update` bazanı ondan asılı olaraq dəyişir. O, sütun silmir, adı dəyişəndə yeni sütun yaradır, data miqrasiyası etmir, nə dəyişdiyinin tarixçəsi qalmır və geri qaytarmaq olmur. Liquibase-də hər dəyişiklik versiyalı changeSet-dir: git-də görünür, hər mühitdə eyni ardıcıllıqla bir dəfə icra olunur, checksum ilə dəyişdirilməkdən qorunur və rollback olunur.

## Second-Level Cache

Cache-lənən: Department, Course, Author, AuthorBook (`READ_WRITE`), Book (`NONSTRICT_READ_WRITE`), `Author.authorBooks`, `Book.authorBooks` collection-ları və `AuthorRepository.findAllByCountry` (query cache). Region-lar `src/main/resources/ehcache.xml`-dədir.

| Metod | URL | Təsvir |
| --- | --- | --- |
| GET | `/api/cache/stats` | Hit, miss, put, query cache hit və region-lar üzrə statistika |
| DELETE | `/api/cache` | Bütün L2 cache-i təmizlə |
| DELETE | `/api/cache/{region}` | Bir region-u təmizlə (`departments`, `courses`, `authors`, `books`, `author-books`) |
| DELETE | `/api/cache/stats` | Statistikanı sıfırla |
| PUT | `/api/courses/credits?amount=1` | Bulk update (`@Modifying`), bütün `courses` region-u təmizlənir |

**Query cache-də yeni müəllif əlavə edəndə niyə SQL yenidən görünür?** Hibernate `authors` cədvəlinin son dəyişmə vaxtını `default-update-timestamps-region`-da saxlayır. Insert-dən sonra bu vaxt yenilənir, query cache-dəki nəticə ondan köhnə olur və etibarsız sayılır.

**1. First-level və second-level cache fərqi?** First-level cache `EntityManager`-ə (Session) aiddir, söndürülmür və tranzaksiya/session bitəndə silinir. Second-level cache `EntityManagerFactory`-yə aiddir, bütün session-lar arasında paylaşılır, tətbiq işlədikcə yaşayır və ömrü TTL/eviction ilə idarə olunur.

**2. `READ_ONLY`, `NONSTRICT_READ_WRITE`, `READ_WRITE` fərqi?** `READ_ONLY` heç vaxt dəyişməyən data üçündür, update olsa exception atılır. `NONSTRICT_READ_WRITE` kilid qoymur, commit-dən sonra cache-dən silir; qısa müddət köhnə data görünə bilər. `READ_WRITE` soft lock ilə işləyir, köhnə data oxunmasının qarşısını alır, amma daha bahalıdır. Book nadir dəyişir və qiymətin bir neçə millisaniyə köhnə görünməsi kritik deyil, ona görə daha yüngül `NONSTRICT_READ_WRITE` kifayətdir.

**3. Query cache niyə tək işləmir?** Query cache yalnız nəticədəki entity id-lərini saxlayır. Entity cache olmasa, Hibernate hər id üçün bazaya ayrıca `SELECT` göndərir (N+1) və query cache faydasız, hətta zərərli olur.

## Transactions (Propagation)

| Metod | URL | Ssenari |
| --- | --- | --- |
| POST | `/api/employees/{id}/transfer` | C1 `REQUIRED` + C2 `REQUIRES_NEW` audit. Body: `{ "departmentId": 2, "raisePercent": 15 }` |
| POST | `/api/tx-demo/reserve-seat/{courseId}` | C3 `MANDATORY` tranzaksiyasız çağırış, 409 qaytarır |
| POST | `/api/students/{studentId}/courses/{courseId}` | `reserveSeat` tranzaksiya daxilindən çağırılır, kurs doludursa 409 |

Seed-də işçi 3 (maaş 9500) +15% ilə limiti (10 000) keçir: departament dəyişmir, `audit_logs`-da `FAILED` sətri qalır.

**1. `REQUIRES_NEW` connection pool-u nə vaxt tükəndirir?** Xarici tranzaksiya dayandırılır, amma connection-ı buraxmır, yeni tranzaksiya ikinci connection götürür. Çoxlu paralel sorğuda hər biri iki connection tutur; pool dolanda daxili tranzaksiyalar connection gözləyir, xaricilər də onları gözləyir və deadlock/timeout yaranır.

**2. Checked exception-da Spring rollback edirmi?** Xeyr. Default olaraq yalnız `RuntimeException` və `Error`-da rollback olur. Checked exception-da da rollback lazımdırsa `@Transactional(rollbackFor = Exception.class)` yazılır.

**3. Daxili metod exception atır, xarici onu udarsa?** Daxili metod eyni tranzaksiyada (`REQUIRED`) olduğu üçün tranzaksiyanı `rollback-only` işarələyir. Xarici metod normal bitib commit etməyə çalışanda Spring `UnexpectedRollbackException` atır və heç nə yazılmır.

## Isolation

| Ssenari | URL |
| --- | --- |
| D1 | `GET /api/isolation/departments/{id}/salary-report?isolation=...&delayMs=8000` |
| D2 | `POST /api/isolation/employees/{id}/raise?amount=100&isolation=...&delayMs=8000` |
| D3 | `POST /api/isolation/courses/{courseId}/enroll/{studentId}?isolation=...&delayMs=8000` |

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
