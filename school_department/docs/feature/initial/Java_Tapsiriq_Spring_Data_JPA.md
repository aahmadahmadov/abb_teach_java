# Praktiki Tapşırıq

*Spring Boot + Spring Data JPA: Entity əlaqələri (OneToOne, OneToMany, ManyToMany)*

| | |
| --- | --- |
| **Tələbənin adı, soyadı:** | |
| **Qrup:** | |
| **Təhvil tarixi:** | |
| **Layihənin adı:** | school-api (paket: `az.iktex.organization.structure.testders`) |

---

# 1. Tapşırığın məqsədi

Bu tapşırıqda siz məktəb/təşkilat məlumatlarını idarə edən REST API yazacaqsınız. Layihə JPA-da olan bütün əsas əlaqə növlərini praktikada öyrətmək üçün qurulub:

- **OneToOne:** User ↔ Passport (iki tərəfli, Gender enum + AttributeConverter ilə)
- **OneToMany / ManyToOne:** Department → Employee
- **ManyToMany – 1-ci üsul:** Student ↔ Course (`@JoinTable` ilə)
- **ManyToMany – 2-ci üsul:** Author ↔ AuthorBook ↔ Book (ayrıca 3-cü entity ilə)

Tapşırığı bitirdikdən sonra siz cascade, orphanRemoval, fetch tipləri, JOIN FETCH, derived query, JPQL, MapStruct və DTO (record) anlayışlarını real kodda tətbiq etmiş olacaqsınız.

---

# 2. İstifadə olunacaq texnologiyalar

| Texnologiya | Qeyd |
| --- | --- |
| Java 25 | Gradle toolchain ilə |
| Spring Boot 4.x | `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa` |
| PostgreSQL 16 | Docker Compose ilə qaldırılır |
| Lombok | `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor` |
| MapStruct 1.6.3 | + `lombok-mapstruct-binding` (Lombok ilə birlikdə işləməsi üçün) |
| Postman / curl | Endpoint-ləri yoxlamaq üçün |

---

# 3. Layihənin quraşdırılması

## 3.1. Spring Initializr

start.spring.io saytında Gradle (Groovy) layihəsi yaradın. Group: `az.iktex.organization.structure`, Artifact: `test-ders`. Asılılıqlar: Spring Web, Spring Data JPA, PostgreSQL Driver, Lombok, DevTools. MapStruct asılılıqlarını `build.gradle`-a əl ilə əlavə edin:

```groovy
implementation 'org.mapstruct:mapstruct:1.6.3'
annotationProcessor 'org.mapstruct:mapstruct-processor:1.6.3'
annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
```

## 3.2. docker-compose.yaml

```yaml
services:
  postgres:
    image: postgres:16
    container_name: school-postgres
    environment:
      POSTGRES_DB: school_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - school-data:/var/lib/postgresql/data
volumes:
  school-data:
```

## 3.3. application.yaml

```yaml
server:
  port: 8080
  servlet:
    context-path: /test-demo
spring:
  application:
    name: test-demo-api
  datasource:
    url: jdbc:postgresql://localhost:5432/school_db
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
```

> **Diqqət!** Qeyd: `open-in-view: false` olduğu üçün LAZY əlaqələr yalnız `@Transactional` metodun daxilində oxuna bilər. Bu, sizi JOIN FETCH yazmağa məcbur edəcək – tapşırığın vacib hissəsidir.

## 3.4. Paket strukturu

```text
az.iktex.organization.structure.testders
 ├── SchoolApplication.java
 ├── controller/     (7 controller)
 ├── service/        (7 service)
 ├── repository/     (9 repository)
 ├── entity/         (9 entity)
 │    └── converter/ (GenderConverter)
 ├── enums/          (Gender)
 ├── mapper/         (8 MapStruct mapper)
 ├── request/        (8 record)
 └── response/       (8 record)
```

---

# 4. Ümumi tələblər (bütün modullar üçün)

1. Controller heç vaxt Entity qaytarmamalı və qəbul etməməlidir – yalnız `XxxRequest` və `XxxResponse` record-ları.
2. Entity ↔ DTO çevrilməsi MapStruct ilə (`componentModel = SPRING`). Hər mapper-də: `toEntity`, `toResponse`, `toResponseList`, `updateEntity(@MappingTarget)`.
3. Service sinfinin üzərində `@Transactional(readOnly = true)`, yazma əməliyyatlarında (create/update/delete) `@Transactional`.
4. Update zamanı `save()` çağırmayın – managed entity-nin dəyişiklikləri tranzaksiya sonunda avtomatik flush olunur (dirty checking).
5. Tapılmayan obyekt üçün `EntityNotFoundException`, biznes qaydası pozulanda `IllegalStateException`, səhv parametr üçün `IllegalArgumentException` atın.
6. Hər entity-də `id` – `@GeneratedValue(strategy = GenerationType.IDENTITY)`, `Long` tipli.
7. Foreign key və unique constraint-lərə ad verin (məs. `fk_employee_department`, `uk_student_course`).
8. Dependency injection yalnız constructor ilə (`@RequiredArgsConstructor` + `private final`).
9. POST əməliyyatları `201 Created`, DELETE əməliyyatları `204 No Content` qaytarmalıdır.

---

# 5. Modul 1 – OneToOne: User ↔ Passport

## 5.1. Gender enum və Converter

Gender enum-u yaradın: `MALE(1)`, `FEMALE(2)`. Enum-da `Integer dbCode` sahəsi və `static Gender findByDbCode(Integer dbCode)` metodu olsun (Stream API ilə).

`entity/converter/GenderConverter` – `AttributeConverter<Gender, Integer>` implement etsin. Bazada `gender_id` sütununda 1 və ya 2 saxlanılmalıdır (enum adı yox!).

## 5.2. User entity (cədvəl: `users`)

| Sahə | Tip | Sütun / Məhdudiyyət |
| --- | --- | --- |
| id | Long | PK, identity |
| name | String | `name`, not null, length 50 |
| surname | String | `surname`, not null, length 50 |
| email | String | `email`, not null, unique, length 100 |
| gender | Gender | `gender_id`, not null, `@Convert(converter = GenderConverter.class)` |
| passport | Passport | `@OneToOne(mappedBy = "user", cascade = ALL, orphanRemoval = true, fetch = LAZY)` |

User-də `setPassport(Passport)` helper metodu yazın ki, iki tərəfi sinxron saxlasın (passport null deyilsə, `passport.setUser(this)`).

## 5.3. Passport entity (cədvəl: `passports`)

| Sahə | Tip | Sütun / Məhdudiyyət |
| --- | --- | --- |
| id | Long | PK, identity |
| serialNumber | String | `serial_number`, not null, unique, length 20 |
| issueDate | LocalDate | `issue_date`, not null |
| country | String | not null, length 50 |
| user | User | Owner tərəf: `@OneToOne(fetch = LAZY, optional = false)`, `@JoinColumn(name = "user_id", unique = true)`, FK: `fk_passport_user` |

## 5.4. Endpoint-lər (`/api/users`)

| Metod | URL | Təsvir |
| --- | --- | --- |
| POST | `/api/users` | User yarat. Email artıq varsa – xəta |
| GET | `/api/users/{id}` | User-i passport ilə birlikdə qaytar (JOIN FETCH) |
| GET | `/api/users/by-email?email=...` | Email ilə axtarış |
| GET | `/api/users?gender=MALE` | Hamısı; gender verilsə – filter |
| GET | `/api/users/without-passport` | Passportu olmayan user-lər |
| PUT | `/api/users/{id}` | Yenilə. Email başqa user-də varsa – xəta (`existsByEmailAndIdNot`) |
| DELETE | `/api/users/{id}` | Sil (cascade ilə passport da silinir) |
| POST | `/api/users/{id}/passport` | Passport əlavə et |
| GET | `/api/users/{id}/passport` | User-in passportu |
| DELETE | `/api/users/{id}/passport` | Passportu sil – `user.setPassport(null)` (orphanRemoval işləməlidir) |

## 5.5. Biznes qaydaları

- Bir user-in yalnız bir passportu ola bilər – ikincisini əlavə etmək xəta verməlidir.
- Passport seriya nömrəsi unikal olmalıdır.
- `UserResponse` daxilində `PassportResponse passport` olsun (`UserMapper` `uses = PassportMapper.class`).

---

# 6. Modul 2 – OneToMany: Department → Employee

## 6.1. Department entity (cədvəl: `departments`)

| Sahə | Tip | Sütun / Məhdudiyyət |
| --- | --- | --- |
| id | Long | PK, identity |
| name | String | not null, unique, length 50 |
| location | String | length 100 |
| phone | String | length 20 |
| employees | `List<Employee>` | `@OneToMany(mappedBy = "department", cascade = ALL, orphanRemoval = true, fetch = LAZY)` |

Helper metodlar: `addEmployee(Employee)` və `removeEmployee(Employee)` – iki tərəfi sinxron saxlasın.

## 6.2. Employee entity (cədvəl: `employees`)

| Sahə | Tip | Sütun / Məhdudiyyət |
| --- | --- | --- |
| id | Long | PK, identity |
| name | String | not null, length 50 |
| surname | String | not null, length 50 |
| salary | BigDecimal | not null, precision 10, scale 2. `@Check(name = "chk_employee_salary", constraints = "salary > 0")` |
| department | Department | `@ManyToOne(fetch = LAZY, optional = false)`, `department_id`, FK: `fk_employee_department`. Cascade YOXDUR |

## 6.3. Endpoint-lər

| Metod | URL | Təsvir |
| --- | --- | --- |
| POST | `/api/departments` | Department yarat (ad unikal) |
| GET | `/api/departments/{id}` | İşçilər ilə birlikdə (JOIN FETCH) |
| GET | `/api/departments` | Hamısı (işçilərsiz) |
| GET | `/api/departments/without-employees` | İşçisi olmayan departamentlər |
| PUT | `/api/departments/{id}` | Yenilə (ad başqasında varsa – xəta) |
| DELETE | `/api/departments/{id}` | Sil – işçisi varsa silmək OLMAZ |
| POST | `/api/employees` | İşçi yarat (`departmentId` request-də gəlir) |
| GET | `/api/employees/{id}` | İşçi |
| GET | `/api/employees/department/{departmentId}` | Departamentin işçiləri |
| GET | `/api/employees/salary-range?min=500&max=2000` | Maaş aralığına görə |
| GET | `/api/employees/department/{id}/average-salary` | Orta maaş (JPQL `avg`) |
| PUT | `/api/employees/{id}` | Yenilə (departament dəyişə bilər) |
| DELETE | `/api/employees/{id}` | İşçini sil (departament qalmalıdır) |

## 6.4. Biznes qaydaları

- Eyni departamentdə eyni ad-soyadlı işçi təkrar yaradıla bilməz.
- `min > max` olarsa `IllegalArgumentException`.
- `EmployeeResponse` daxilində `departmentId` və `departmentName` olsun (MapStruct `@Mapping(source = "department.id", ...)`).
- Cascade ALL olduğu üçün departament silinəndə işçilər də silinərdi – service-də bunun qarşısını alın (`hasEmployees` sorğusu).

---

# 7. Modul 3 – ManyToMany (@JoinTable): Student ↔ Course

## 7.1. Student entity (cədvəl: `students`) – owner tərəf

| Sahə | Tip | Sütun / Məhdudiyyət |
| --- | --- | --- |
| id | Long | PK, identity |
| name | String | not null, length 50 |
| surname | String | not null, length 50 |
| age | Integer | not null |
| courses | `Set<Course>` | `@ManyToMany(cascade = {PERSIST, MERGE}, fetch = LAZY)` + `@JoinTable(name = "student_course")`, FK-lər: `fk_sc_student`, `fk_sc_course`, unique: `uk_student_course` |

Helper metodlar: `addCourse(Course)`, `removeCourse(Course)`.

**Sual:** Niyə `CascadeType.REMOVE` vermirik? Tələbə silinəndə kurslar silinməməlidir – yalnız `student_course` sətirləri silinir. Bu sualın cavabını kodda şərh kimi yazın.

## 7.2. Course entity (cədvəl: `courses`)

| Sahə | Tip | Sütun / Məhdudiyyət |
| --- | --- | --- |
| id | Long | PK, identity |
| title | String | not null, unique, length 100 |
| credit | Integer | not null |
| teacher | String | not null, length 50 |
| students | `Set<Student>` | `@ManyToMany(mappedBy = "courses", fetch = LAZY)` |

## 7.3. Endpoint-lər

| Metod | URL | Təsvir |
| --- | --- | --- |
| POST | `/api/students` | Tələbə yarat |
| GET | `/api/students/{id}` | Kurslar ilə birlikdə (left join fetch) |
| GET | `/api/students` | Hamısı |
| GET | `/api/students/without-courses` | Heç bir kursa yazılmayanlar (`s.courses is empty`) |
| PUT / DELETE | `/api/students/{id}` | Yenilə / sil |
| POST | `/api/students/{studentId}/courses/{courseId}` | Kursa yaz (enroll) |
| DELETE | `/api/students/{studentId}/courses/{courseId}` | Kursdan çıxar (unenroll) |
| POST | `/api/courses` | Kurs yarat (title unikal) |
| GET | `/api/courses`, `/api/courses/{id}` | Hamısı / biri |
| GET | `/api/courses/student/{studentId}` | Tələbənin kursları |
| GET | `/api/courses/{id}/students` | Kursun tələbələri |
| GET | `/api/courses/{id}/students/count` | Kursdakı tələbə sayı |
| PUT / DELETE | `/api/courses/{id}` | Yenilə / sil |

## 7.4. Biznes qaydaları

- Tələbə eyni kursa iki dəfə yazıla bilməz (`existsByIdAndCoursesId`).
- Yazılmadığı kursdan çıxarmaq xəta verməlidir.
- `StudentResponse.courses` yalnız detail sorğuda doldurulur. Mapper-də iki metod olsun: `toResponse` (courses ignore) və `@Named("withCourses") toDetailResponse`.
- Kurs silinərkən əvvəlcə tələbələrlə əlaqəni qırın (Course inverse tərəfdir – niyə lazım olduğunu izah edin).

---

# 8. Modul 4 – ManyToMany (3-cü entity): Author ↔ AuthorBook ↔ Book

## 8.1. Author entity (cədvəl: `authors`)

| Sahə | Tip | Sütun / Məhdudiyyət |
| --- | --- | --- |
| id | Long | PK, identity |
| name | String | not null, length 50 |
| surname | String | not null, length 50 |
| country | String | length 50 |
| authorBooks | `List<AuthorBook>` | `@OneToMany(mappedBy = "author", cascade = ALL, orphanRemoval = true, fetch = LAZY)` |

## 8.2. Book entity (cədvəl: `books`)

| Sahə | Tip | Sütun / Məhdudiyyət |
| --- | --- | --- |
| id | Long | PK, identity |
| title | String | not null, length 150 |
| price | BigDecimal | not null, precision 8, scale 2. `@Check(name = "chk_book_price", constraints = "price >= 0")` |
| pageCount | Integer | `page_count`, not null |
| authorBooks | `List<AuthorBook>` | `@OneToMany(mappedBy = "book", cascade = ALL, orphanRemoval = true, fetch = LAZY)` |

## 8.3. AuthorBook entity (cədvəl: `author_book`)

| Sahə | Tip | Sütun / Məhdudiyyət |
| --- | --- | --- |
| id | Long | PK, identity |
| author | Author | `@ManyToOne(fetch = LAZY, optional = false)`, `author_id`, FK: `fk_ab_author` |
| book | Book | `@ManyToOne(fetch = LAZY, optional = false)`, `book_id`, FK: `fk_ab_book` |

Cədvəl səviyyəsində unique constraint: `uk_author_book (author_id, book_id)`. Cascade YOXDUR.

## 8.4. Endpoint-lər

| Metod | URL | Təsvir |
| --- | --- | --- |
| POST | `/api/authors` | Müəllif yarat (eyni ad+soyad varsa – xəta) |
| GET | `/api/authors/{id}` | Müəllif |
| GET | `/api/authors?country=Azerbaijan` | Hamısı; country verilsə – filter |
| GET | `/api/authors/without-books` | Kitabı olmayan müəlliflər |
| GET | `/api/authors/{id}/books` | Müəllifin kitabları |
| PUT / DELETE | `/api/authors/{id}` | Yenilə / sil (kitablar qalmalıdır) |
| POST | `/api/books` | Kitab yarat (title unikal yoxlanılır) |
| GET | `/api/books/{id}` | Müəlliflər ilə birlikdə |
| GET | `/api/books?title=java` | Hamısı; title verilsə – axtarış (`ContainingIgnoreCase`) |
| GET | `/api/books/cheaper-than?price=20` | Qiyməti verilən məbləğə bərabər və ya aşağı olanlar (qiymətə görə artan sıra) |
| PUT / DELETE | `/api/books/{id}` | Yenilə / sil |
| POST | `/api/books/{bookId}/authors/{authorId}` | Kitaba müəllif bağla (AuthorBook yarat) |
| DELETE | `/api/books/{bookId}/authors/{authorId}` | Əlaqəni sil |

## 8.5. Biznes qaydaları

- Eyni müəllifi eyni kitaba ikinci dəfə bağlamaq xəta verməlidir (`existsByAuthorIdAndBookId`).
- Bağlı olmayan əlaqəni silmək `EntityNotFoundException` verməlidir.
- `BookResponse.authors` yalnız detail sorğuda doldurulur.
- Müəllif silinəndə yalnız `author_book` sətirləri silinir, kitablar qalır.

---

# 9. Repository tələbləri

Hər repository `JpaRepository<Entity, Long>`-dan extend etsin. Hər birində iki bölmə olsun:

- **ORM (derived query):** metod adından yaranan sorğular, məs. `existsByEmail`, `findAllByGender`, `findAllByPassportIsNull`, `existsByIdAndCoursesId`, `countByCoursesId`, `findAllBySalaryBetween`.
- **JPQL:** `@Query` ilə, məs. JOIN FETCH sorğuları, `is empty`, `size(...)`, `avg(...)`, `@Modifying` bulk delete.

Hər repository-də ən azı 3 derived query və 2 JPQL sorğu olmalıdır.

---

# 10. Yoxlama ssenarisi (Postman)

Bütün URL-lərin əvvəlinə `http://localhost:8080/test-demo` əlavə edin. Nümunə request:

```http
POST /api/users
{
  "name": "Əli",
  "surname": "Məmmədov",
  "email": "ali@mail.com",
  "gender": "MALE"
}
```

Aşağıdakıları yoxlayın və nəticələri ekran görüntüsü ilə hesabata əlavə edin:

1. User yaradın, passport əlavə edin, `GET /api/users/{id}` ilə passportun gəldiyini göstərin.
2. Bazada `users.gender_id` sütununda 1/2 yazıldığını pgAdmin/DBeaver ilə göstərin.
3. Passportu silin – `passports` cədvəlində sətrin silindiyini göstərin (orphanRemoval).
4. İşçisi olan departamenti silməyə çalışın – xəta alındığını göstərin.
5. Tələbəni 2 kursa yazın, sonra tələbəni silin – kursların qaldığını göstərin.
6. Kitaba 2 müəllif bağlayın, eyni müəllifi təkrar bağlamağa çalışın – xəta alındığını göstərin.
7. Konsolda `show-sql` çıxışında JOIN FETCH sorğusunun tək SELECT ilə işlədiyini göstərin (N+1 yoxdur).

---

# 11. Qiymətləndirmə meyarları

| Meyar | Bal |
| --- | --- |
| Layihə quraşdırılması (Docker, yaml, Gradle, paket strukturu) | 10 |
| Modul 1 – OneToOne + Gender Converter | 20 |
| Modul 2 – OneToMany / ManyToOne | 20 |
| Modul 3 – ManyToMany (`@JoinTable`) | 20 |
| Modul 4 – ManyToMany (3-cü entity) | 20 |
| Kod keyfiyyəti (DTO, MapStruct, `@Transactional`, adlandırma, şərhlər) | 10 |
| **Cəmi** | **100** |

---

# 12. Təhvil qaydası

- Layihəni GitHub-a yükləyin, linki müəllimə göndərin. `build/`, `.gradle/`, `.idea/` qovluqları repoda olmamalıdır.
- Repoda `README.md` olsun: layihəni necə işə salmaq (`docker compose up -d`, `./gradlew bootRun`) və bütün endpoint-lərin siyahısı.
- Postman collection-ı (`.json`) repoya əlavə edin.
- Kodda vacib yerlərdə (cascade, orphanRemoval, mappedBy seçimi) qısa şərhlər yazın – niyə belə seçdiyinizi izah edin.

**Uğurlar!**
