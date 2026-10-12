# Bean Scope-ları və Dependency Injection

Tapşırıq vərəqi: [docs/features/mvp_version_1/Spring_tapsiriq_scope_DI.md](docs/features/mvp_version_1/Spring_tapsiriq_scope_DI.md)

Texnologiya: Spring Boot 4.1.1, Java 21.

## Layihənin quruluşu

| Paket | Class-lar | Scope |
|---|---|---|
| `controller` | `UserInfoController` | singleton |
| `service` | `UserInfoService`, `NotificationService`, `ConsoleNotificationService` | singleton |
| `repository` | `UserRepository` (interface), `InMemoryUserRepository`, `MockUserRepository` | singleton |
| `component` | `UserAuditLog` | prototype |
| `component` | `RequestContext` | request |
| `component` | `RecentlyViewedUsers` | session |

## Endpoint-lər

| Metod | URL |
|---|---|
| POST | `/api/v1/user-info/create` |
| GET | `/api/v1/user-info/{id}` |
| GET | `/api/v1/user-info/all` |
| GET | `/api/v1/user-info/recent` |

Swagger: `http://localhost:8080/swagger-ui/index.html`

---

## Bölmə 1: Singleton

### Tapşırıq 6

Tətbiq açılanda hər konstruktor log-u **1 dəfə** çıxır. 10 sorğu göndərəndən sonra yeni log çıxmır.
Səbəb: controller, service və repository singleton-dur. Spring onları başlanğıcda bir dəfə yaradır, bütün sorğular eyni obyekti istifadə edir. `hashCode` də hər yerdə eyni qalır.

```
InMemoryUserRepository yaradildi. Hash: 864361396
UserRepository hazirdir, 3 user yuklendi
UserInfoService yaradildi. Hash: 758403805
UserInfoController yaradildi. Hash: 1343015810
```

### Tapşırıq 7

`POST /create` ilə yaradılan user-in `id`-si ilə `GET /{id}` çağıranda eyni user gəlir. `GET /all` 3 başlanğıc user + yaradılan user-ləri qaytarır (məsələn 1 user yaratdıqdan sonra 4).
Bu nəticə `InMemoryUserRepository` üçündür. Bölmə 5-dən sonra service `MockUserRepository` istifadə etdiyi üçün `/all` artıq 2 `Mock User` qaytarır.

<!-- Postman screenshot-u bura əlavə et -->

### Tapşırıq 8

Singleton bean-i eyni anda **bütün sorğu thread-ləri** istifadə edir (Tomcat hər sorğunu ayrı thread-də işlədir).
Adi `HashMap` eyni anda iki thread yazanda pozula bilər, data itə bilər. `long counter++` atomik deyil: iki thread eyni dəyəri oxuyub eyni ID-ni verə bilər. `ConcurrentHashMap` və `AtomicLong` bu problemi həll edir.

---

## Bölmə 2: Prototype

### Tapşırıq 11

`UserAuditLog`-u service konstruktoruna birbaşa inject edəndə 3 user yaratsam da **1** `auditId` görürəm.
Səbəb: service singleton-dur, bir dəfə yaranır. Prototype bean də yalnız o an bir dəfə inject olunur və sonra həmişə eyni obyekt qalır.

<!-- Log screenshot-u bura əlavə et -->

### Tapşırıq 12

`ObjectProvider<UserAuditLog>.getObject()` ilə hər `createUser` çağırışında yeni obyekt alınır. 3 user üçün 3 fərqli `auditId`:

```
UserAuditLog yaradildi: 952c3fa3-a7df-4d48-8c0f-98117ae209ee
UserAuditLog yaradildi: 666bbc6a-84d7-4bb0-8262-d2e68b3e9d85
UserAuditLog yaradildi: a2f5c222-a7cd-44f5-9744-017ef1615b64
```

### Tapşırıq 13

Tətbiqi bağlayanda `UserAuditLog`-un `@PreDestroy` log-u **çıxmır**.
Səbəb: Spring prototype bean-i yaradıb verir və sonra onu izləmir. Obyektin həyatını idarə etmir, ona görə destroy metodunu çağırmır. Obyekti garbage collector silir.

---

## Bölmə 3: Request

### Tapşırıq 16

Bir sorğuda 3 qatda eyni `requestId`, növbəti sorğuda fərqli:

```
[Controller] requestId=50dc7a37
[Service] requestId=50dc7a37
[Repository] requestId=50dc7a37
Sorgu 9 ms cekdi
[Controller] requestId=9d9a8b5d
[Service] requestId=9d9a8b5d
[Repository] requestId=9d9a8b5d
Sorgu 1 ms cekdi
```

### Tapşırıq 18

`@Scope("request")` (proxyMode olmadan) yazanda tətbiq **açılmır**. Xəta:

```
ScopeNotActiveException: Scope 'request' is not active for the current thread
```

Səbəb: controller, service və repository singleton-dur və tətbiq açılanda yaranır. O an heç bir HTTP sorğu yoxdur, ona görə Spring request bean-i yarada bilmir.
Həll: `@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)`. Bu `@RequestScope` ilə eynidir.

### Tapşırıq 19

```
RequestContext class: class ...component.RequestContext$$SpringCGLIB$$0
```

Singleton-a əsl `RequestContext` yox, onun **proxy**-si inject olunur (CGLIB ilə yaradılmış alt class). Proxy-nin metodunu çağıranda (`getRequestId()`) o, cari sorğuya aid əsl obyekti tapır və çağırışı ona ötürür. Ona görə singleton bir dəfə yaransa da hər sorğuda fərqli `requestId` görür.

---

## Bölmə 4: Session

### Tapşırıq 23

`application.properties`-də `server.servlet.session.timeout=1m`. Sorğu göndərib 1 dəqiqə gözləyəndən sonra konsolda:

```
RecentlyViewedUsers silindi, session bitdi
```

### Tapşırıq 24

Chrome-da `/1`, `/3`, `/2`, `/1` açıb `/recent` çağıranda siyahı `[1, 2, 3]` olur (ən yenisi əvvəldə, `1` təkrar olmur). Incognito-da `/recent` boş siyahı `[]` qaytarır, çünki incognito yeni session-dur.
Session `JSESSIONID` cookie-si ilə tanınır. Postman cookie-ni saxlamasa hər sorğu yeni session açır və `/recent` boş gəlir.

<!-- Chrome və incognito screenshot-ları bura əlavə et -->

### Tapşırıq 25

Request scope-da saxlasaydıq siyahı hər sorğuda yenidən yaranardı. `/recent` ayrıca sorğu olduğu üçün həmişə boş qayıdardı. Əvvəlki baxışları yadda saxlamaq üçün session scope lazımdır.

---

## Bölmə 5: Dependency Injection

### Tapşırıq 27

`MockUserRepository` əlavə edəndən sonra tətbiq açılmır:

```
NoUniqueBeanDefinitionException: expected single matching bean but found 2:
inMemoryUserRepository, mockUserRepository
```

Səbəb: service `UserRepository` istəyir, amma bu interface-in 2 implementasiyası var. Spring hansını seçəcəyini bilmir.

### Tapşırıq 28 və 29

- Birinci yol: `InMemoryUserRepository`-yə `@Primary` yazmaq. Başqa göstəriş yoxdursa Spring onu seçir.
- İkinci yol: `@Primary`-ni silib service konstruktorunda `@Qualifier("mockUserRepository")` yazmaq. Kodda indi bu variant var, `/all` 2 `Mock User` qaytarır.

### Tapşırıq 30 və 31

`NotificationService` setter ilə inject olunur: `@Autowired(required = false)`. Implementasiya yoxdursa Spring setter-i çağırmır, field `null` qalır və tətbiq xətasız açılır. `ConsoleNotificationService` əlavə edəndən sonra `createUser`-də `Xoş gəldin` log-u çıxır.

### Tapşırıq 33

```
UserRepository sayi: 2
```

`List<UserRepository>` istəyəndə Spring bu tipdə olan bütün bean-ləri siyahıya yığır: `InMemoryUserRepository` və `MockUserRepository`.
