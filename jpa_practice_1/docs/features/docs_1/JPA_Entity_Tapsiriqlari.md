# JPA Entity Əlaqələri — Praktiki Tapşırıqlar

Oct 20, 2026 · @Ruslan

## Ümumi tələblər

Bu tapşırıqda 4 əlaqə növünü JPA entity-ləri ilə qurmalısınız: OneToOne, OneToMany/ManyToOne, ManyToMany (@JoinTable ilə) və ManyToMany (ara entity ilə). Hər tapşırıqda cədvəl adları, sütun adları və məhdudiyyətlər dəqiq verilib — onlara əməl edin.

Bütün entity-lər üçün qaydalar:

- Paket: az.iktex.organization.structure.testders.entity
- Yalnız jakarta.persistence annotasiyalarından istifadə edin (javax yox).
- Lombok: @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor. @Data və @ToString istifadə etməyin (iki tərəfli əlaqədə sonsuz dövrə və StackOverflowError yaradır).
- Hər entity-də @Table(name = "...") olsun, cədvəl adları cəm formada (users, books).
- id sahəsi: Long, @GeneratedValue(strategy = GenerationType.IDENTITY).
- Hər sahədə @Column(name = ...) yazılsın; nullable, length, unique lazım olduqda göstərilsin.
- Bütün əlaqələr FetchType.LAZY olsun.
- Foreign key-lərə ad verin: @ForeignKey(name = "fk_...").
- Kolleksiyalar elan zamanı initialize olunsun (= new ArrayList<>() və ya = new HashSet<>()).
- Hər əlaqənin üstündə bir sətirlik şərh yazın: cascade-i niyə belə seçdiniz.

## Tapşırıq 1 — OneToOne: User və Passport

Hər istifadəçinin ən çox bir pasportu var, hər pasport mütləq bir istifadəçiyə aiddir. Foreign key passports cədvəlində saxlanılır, yəni **Passport sahib (owner) tərəfdir**.

### 1.1. Gender enum və converter

- az.iktex.organization.structure.testders.enums paketində Gender enum-u yaradın: MALE (id = 1), FEMALE (id = 2). Enum-da id sahəsi və id-yə görə enum qaytaran static metod olsun.
- ...entity.converter paketində GenderConverter yazın: AttributeConverter<Gender, Integer> implement etsin. Bazaya 1/2 yazılsın, oxunanda enum-a çevrilsin. Naməlum id gəldikdə exception atılsın.

### 1.2. User entity — cədvəl users

| Sahə | Tip | Sütun | Məhdudiyyət |
| --- | --- | --- | --- |
| id | Long | id | PK, IDENTITY |
| name | String | name | not null, 50 |
| surname | String | surname | not null, 50 |
| email | String | email | not null, unique, 100 |
| gender | Gender | gender_id | not null, @Convert ilə |
| passport | Passport | — | inverse tərəf |

### 1.3. Passport entity — cədvəl passports

| Sahə | Tip | Sütun | Məhdudiyyət |
| --- | --- | --- | --- |
| id | Long | id | PK, IDENTITY |
| serialNumber | String | serial_number | not null, unique, 20 |
| issueDate | LocalDate | issue_date | not null |
| country | String | country | not null, 50 |
| user | User | user_id | not null, unique, FK fk_passport_user |

### 1.4. Əlaqə tələbləri

- User silinəndə onun pasportu da silinməlidir.
- user.setPassport(null) çağırılanda pasport bazadan silinməlidir.
- Passport silinəndə User silinməməlidir.
- User-də setPassport(Passport p) metodunu özünüz yazın: hər iki tərəfi sinxron saxlasın (null yoxlaması ilə).
- user_id sütunu niyə unique = true olmalıdır? Cavabı şərhdə yazın.

## Tapşırıq 2 — OneToMany / ManyToOne: Department və Employee

Bir departamentdə çox işçi işləyir, hər işçi mütləq bir departamentə aiddir. Foreign key employees cədvəlindədir, yəni **Employee sahib tərəfdir**.

### 2.1. Department entity — cədvəl departments

| Sahə | Tip | Sütun | Məhdudiyyət |
| --- | --- | --- | --- |
| id | Long | id | PK, IDENTITY |
| name | String | name | not null, unique, 50 |
| location | String | location | 100 |
| phone | String | phone | 20 |
| employees | List<Employee> | — | inverse tərəf |

### 2.2. Employee entity — cədvəl employees

| Sahə | Tip | Sütun | Məhdudiyyət |
| --- | --- | --- | --- |
| id | Long | id | PK, IDENTITY |
| name | String | name | not null, 50 |
| surname | String | surname | not null, 50 |
| salary | BigDecimal | salary | not null, precision 10, scale 2 |
| department | Department | department_id | not null, FK fk_employee_department |

### 2.3. Əlaqə tələbləri

- Departament silinəndə onun bütün işçiləri də silinməlidir.
- Siyahıdan çıxarılan işçi bazadan silinməlidir.
- İşçi silinəndə departament **silinməməlidir** — @ManyToOne tərəfində cascade olmamalıdır.
- employees cədvəlinə salary > 0 CHECK məhdudiyyəti əlavə edin: @Check(name = "chk_employee_salary", ...) (Hibernate annotasiyası).
- Department-də iki helper metod yazın: addEmployee(Employee e) və removeEmployee(Employee e). Hər ikisi əlaqənin iki tərəfini yeniləməlidir.
- Pul üçün niyə double yox, BigDecimal istifadə edirik? Şərhdə yazın.

## Tapşırıq 3 — ManyToMany (@JoinTable): Student və Course

Bir tələbə çox kursa yazıla bilər, bir kursda çox tələbə oxuyur. Əlaqə ayrıca entity olmadan, @JoinTable ilə qurulur. **Student sahib tərəfdir**.

### 3.1. Student entity — cədvəl students

| Sahə | Tip | Sütun | Məhdudiyyət |
| --- | --- | --- | --- |
| id | Long | id | PK, IDENTITY |
| name | String | name | not null, 50 |
| surname | String | surname | not null, 50 |
| age | Integer | age | not null |
| courses | Set<Course> | — | sahib tərəf |

### 3.2. Course entity — cədvəl courses

| Sahə | Tip | Sütun | Məhdudiyyət |
| --- | --- | --- | --- |
| id | Long | id | PK, IDENTITY |
| title | String | title | not null, unique, 100 |
| credit | Integer | credit | not null |
| teacher | String | teacher | not null, 50 |
| students | Set<Student> | — | inverse tərəf (mappedBy) |

### 3.3. Ara cədvəl student_course

| Sütun | Haradan | FK adı |
| --- | --- | --- |
| student_id | students.id | fk_sc_student |
| course_id | courses.id | fk_sc_course |

Cüt (student_id, course_id) üçün uk_student_course adlı unique constraint olsun.

### 3.4. Əlaqə tələbləri

- Kolleksiya tipi List yox, Set olsun.
- Cascade yalnız PERSIST və MERGE. REMOVE olmamalıdır: tələbə silinəndə kurslar qalmalı, yalnız ara cədvəl sətirləri silinməlidir.
- Student-də addCourse(Course c) və removeCourse(Course c) helper metodları yazın, iki tərəfi sinxron saxlasın.
- Course tərəfində cascade yoxdur — niyə? Şərhdə yazın.

## Tapşırıq 4 — ManyToMany ara entity ilə: Author, AuthorBook, Book

Bir müəllifin çox kitabı, bir kitabın çox müəllifi ola bilər. Bu dəfə ara cədvəl üçün **ayrıca entity** (AuthorBook) yaradılır və ManyToMany iki ədəd OneToMany + ManyToOne-a bölünür. Bu üsul ara cədvələ sonradan əlavə sütun (məsələn, müəllifin rolu) qoymağa imkan verir.

### 4.1. Author entity — cədvəl authors

| Sahə | Tip | Sütun | Məhdudiyyət |
| --- | --- | --- | --- |
| id | Long | id | PK, IDENTITY |
| name | String | name | not null, 50 |
| surname | String | surname | not null, 50 |
| country | String | country | 50 |
| authorBooks | List<AuthorBook> | — | inverse tərəf |

### 4.2. Book entity — cədvəl books

| Sahə | Tip | Sütun | Məhdudiyyət |
| --- | --- | --- | --- |
| id | Long | id | PK, IDENTITY |
| title | String | title | not null, 150 |
| price | BigDecimal | price | not null, precision 8, scale 2 |
| pageCount | Integer | page_count | not null |
| authorBooks | List<AuthorBook> | — | inverse tərəf |

books cədvəlinə chk_book_price adlı CHECK əlavə edin: price >= 0.

### 4.3. AuthorBook entity — cədvəl author_book

| Sahə | Tip | Sütun | Məhdudiyyət |
| --- | --- | --- | --- |
| id | Long | id | PK, IDENTITY |
| author | Author | author_id | not null, FK fk_ab_author |
| book | Book | book_id | not null, FK fk_ab_book |

Cüt (author_id, book_id) üçün uk_author_book adlı unique constraint olsun.

### 4.4. Əlaqə tələbləri

- AuthorBook hər iki tərəf üçün sahib tərəfdir (@ManyToOne).
- Author silinəndə onun AuthorBook sətirləri silinməli, kitablar isə qalmalıdır. Eyni qayda Book üçün də keçərlidir.
- AuthorBook sətri silinəndə nə müəllif, nə də kitab silinməməlidir — @ManyToOne tərəflərində cascade olmamalıdır.
- Şərhdə yazın: Tapşırıq 3-dəki üsulla müqayisədə bu üsulun üstünlüyü və çatışmazlığı nədir?

## Son addım — cədvəlləri yaradın

Bütün entity-lər yazıldıqdan sonra layihəni işə salın və Hibernate-in cədvəlləri bazada yaratmasını təmin edin.

- application.properties (və ya .yml) faylında baza bağlantısını qurun.
- spring.jpa.hibernate.ddl-auto=create və spring.jpa.show-sql=true yazın.
- Layihəni işə salın — xəta olmadan başlamalıdır.
- Bazada aşağıdakı 10 cədvəlin yarandığını yoxlayın.

| Cədvəl | Entity | Tapşırıq |
| --- | --- | --- |
| users | User | 1 |
| passports | Passport | 1 |
| departments | Department | 2 |
| employees | Employee | 2 |
| students | Student | 3 |
| courses | Course | 3 |
| student_course | — (@JoinTable) | 3 |
| authors | Author | 4 |
| books | Book | 4 |
| author_book | AuthorBook | 4 |

Hər cədvəldə sütun adlarının, foreign key-lərin (fk_...), unique (uk_...) və check (chk_...) məhdudiyyətlərinin tapşırıqdakı kimi olduğunu yoxlayın. Təhvil: entity faylları + bazadakı cədvəllərin ekran görüntüsü.
