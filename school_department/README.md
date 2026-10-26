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

![Swagger UI](docs/screenshots/swagger-ui.png)

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

### Yoxlama

User yaradıldı, passport əlavə olundu, `GET /api/users/{id}` passportu qaytarır:

![User passport ilə](docs/screenshots/user-with-passport.png)

Bazada `users.gender_id` sütununda 1/2 saxlanılır:

![gender_id](docs/screenshots/users-gender-id.png)

Passport silindikdən sonra `passports` cədvəlində sətir yoxdur (orphanRemoval):

![Passport silindi](docs/screenshots/passport-deleted.png)

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

### Yoxlama

İşçisi olan departamenti silmək xəta verir:

![Department silinmir](docs/screenshots/department-delete-error.png)

`GET /api/departments/{id}` tək SELECT ilə işləyir (JOIN FETCH, N+1 yoxdur):

![JOIN FETCH](docs/screenshots/department-join-fetch-sql.png)

## Modul 3: Student ↔ Course (ManyToMany, @JoinTable)

Hazırlanır.

## Modul 4: Author ↔ AuthorBook ↔ Book (ManyToMany, 3-cü entity)

Hazırlanır.
