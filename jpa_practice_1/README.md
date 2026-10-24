# JPA Entity Tapşırıqları

Tapşırıq: [JPA_Entity_Tapsiriqlari.md](docs/features/docs_1/JPA_Entity_Tapsiriqlari.md)

## Cədvəllər

| Cədvəl | Entity | Tapşırıq |
| --- | --- | --- |
| users | User | 1 |
| passports | Passport | 1 |
| departments | Department | 2 |
| employees | Employee | 2 |
| students | Student | 3 |
| courses | Course | 3 |
| student_course | @JoinTable | 3 |
| authors | Author | 4 |
| books | Book | 4 |
| author_book | AuthorBook | 4 |

## Tapşırıq 1: User və Passport (OneToOne)

- Sahib tərəf `Passport`-dur, FK `passports.user_id` (`fk_passport_user`).
- `User.passport`: `cascade = ALL` + `orphanRemoval = true`. User silinəndə pasport silinir, `setPassport(null)` pasportu bazadan silir.
- `Passport.user`-də cascade yoxdur: pasport silinəndə user qalır.
- `user_id` unique-dir, çünki bir userin yalnız bir pasportu ola bilər. Unique olmasa, cədvəl əslində OneToMany olardı.
- `GenderTypeEnum`: `MALE(1)`, `FEMALE(2)`, `fromId(id)` naməlum id-də `IllegalArgumentException` atır.
- `GenderConverter`: bazaya 1/2 yazır, oxuyanda `fromId` ilə enum-a çevirir.

## Tapşırıq 2: Department və Employee (OneToMany / ManyToOne)

- Sahib tərəf `Employee`-dir, FK `employees.department_id` (`fk_employee_department`).
- `Department.employees`: `cascade = ALL` + `orphanRemoval = true`. Departament silinəndə işçilər də silinir, siyahıdan çıxan işçi bazadan silinir.
- `Employee.department`-də cascade yoxdur.
- `chk_employee_salary`: `salary > 0`.
- Pul üçün `BigDecimal`: `double` dəqiq deyil (`0.1 + 0.2 = 0.30000000000000004`).
- Helper metodlar: `addEmployee`, `removeEmployee`.

## Tapşırıq 3: Student və Course (ManyToMany, @JoinTable)

- Sahib tərəf `Student`-dir, ara cədvəl `student_course`.
- FK-lar: `fk_sc_student`, `fk_sc_course`. Unique: `uk_student_course (student_id, course_id)`.
- Kolleksiya `Set`-dir, cascade yalnız `PERSIST` və `MERGE`. Tələbə silinəndə kurslar qalır, yalnız ara cədvəl sətirləri silinir.
- `Course`-da cascade yoxdur: əlaqəni sahib tərəf idarə edir, kurs silinəndə tələbələr silinməməlidir.
- Helper metodlar: `addCourse`, `removeCourse`.

## Tapşırıq 4: Author, AuthorBook, Book (ara entity)

- `AuthorBook` hər iki tərəf üçün sahib tərəfdir (`@ManyToOne`, cascade yoxdur).
- FK-lar: `fk_ab_author`, `fk_ab_book`. Unique: `uk_author_book (author_id, book_id)`.
- `Author.authorBooks` və `Book.authorBooks`: `cascade = ALL` + `orphanRemoval = true`. Müəllif və ya kitab silinəndə yalnız `author_book` sətirləri silinir.
- `chk_book_price`: `price >= 0`.
- Tapşırıq 3 ilə müqayisə:
  - Üstünlük: ara cədvələ əlavə sütun (məs. müəllifin rolu) qoymaq olur.
  - Çatışmazlıq: əlavə entity, daha çox kod, kitaba `getAuthorBooks()` üzərindən çatırıq.

## Kodda düzəldilənlər

| Fayl | Problem | Düzəliş |
| --- | --- | --- |
| GenderTypeEnum | `@AllArgsConstructor` + `@RequiredArgsConstructor` eyni konstruktoru iki dəfə yaradırdı, layihə compile olmurdu | `@RequiredArgsConstructor` silindi |
| GenderTypeEnum | Label-lər tərs idi (MALE = "Qadın") | Düzəldildi |
| GenderTypeEnum | id-yə görə static metod yox idi, sahə adı `code` idi | `id` sahəsi və `fromId` əlavə olundu |
| GenderConverter | Naməlum id-də `null` qaytarırdı | `fromId` ilə exception atır |
| Employee | `id` sahəsi yox idi, `@Id` `name`-in üstündə idi | `id` (IDENTITY) əlavə olundu |
| Employee | `chk_employee_age` olmayan `age` sütununa baxırdı (DDL xətası) | Silindi |
| Employee | CHECK `salary >= 0` idi | `salary > 0` |
| Department | `employees` initialize olunmamışdı (NPE) | `= new ArrayList<>()` |
| User | `passport`-da `FetchType.LAZY` yox idi | Əlavə olundu |
| Student | Ara cədvəl `student_courses`, FK/UK adları tapşırıqdan fərqli idi | `student_course`, `fk_sc_*`, `uk_student_course` |
| AuthorBook | `unk_author_book`, `fk_author_book_*` | `uk_author_book`, `fk_ab_author`, `fk_ab_book` |
| Author, Book, Student | İstifadə olunmayan importlar | Silindi |
| Bütün entity-lər | Cascade şərhləri, BigDecimal və müqayisə şərhləri yox idi | Əlavə olundu |

## Tapşırıqdan fərqli qalanlar

- Paket: tapşırıq `az.iktex.organization.structure.testders.entity` istəyir, layihədə `com.spring_boot.jpa_practice_1.entity` işlənir.
- Enum adı: tapşırıqda `Gender`, layihədə `GenderTypeEnum`.

## İşə salmaq

1. Bazada MariaDB işlədiyi üçün `mariadb-java-client` driver-i və `jdbc:mariadb://` URL-i istifadə olunur. `mysql-connector-j` MariaDB-də `INFORMATION_SCHEMA.KEYWORDS.RESERVED` sütununu tapmır və Hibernate "Unable to determine Dialect" xətası verir.
2. `./gradlew bootRun`
3. Bazada 10 cədvəlin, `fk_...`, `uk_...`, `chk_...` adlarının yarandığını yoxlayın:

```sql
SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'jpa_practice_1'
ORDER BY TABLE_NAME;
```
