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
