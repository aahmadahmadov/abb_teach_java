**SPRING PRAKTİKA · TAPŞIRIQ VƏRƏQİ**

# Bean Scope-ları və Dependency Injection

Bu tapşırıqda dərsdə yazdığımız `UserInfoController` → `UserInfoService` → `UserRepository` layihəsini genişləndirəcək, hər scope-u öz gözünüzlə görəcək və DI növlərini praktikada tətbiq edəcəksiniz.
Mövzular: **IoC, DI, Singleton, Prototype, Request, Session, Lifecycle**.

| Yeni class-lar | Tapşırıq sayı | Texnologiya |
|---|---|---|
| `UserAuditLog` · `RequestContext` · `RecentlyViewedUsers` | 37 sual, 6 bölmə | Spring Boot 3 · Java 17 |

---

## Bölmə 1 · Singleton

*In-memory repository · thread-safety · lifecycle*

1. `UserRepository`-yə `Map<Long, UserInfoResponse>` tipində `storage` field-i əlavə et. Tip **thread-safe** olmalıdır.

   `ConcurrentHashMap`

2. ID-ni `ThreadLocalRandom` ilə yox, **ardıcıl sayacla** ver: 1, 2, 3...

   `AtomicLong.incrementAndGet()`

3. Tətbiq açılanda **3 user** avtomatik yüklənsin və konsola `UserRepository hazirdir, 3 user yuklendi` yazılsın.

   `@PostConstruct`

4. Tətbiq bağlananda `storage` təmizlənsin və log yazılsın.

   `@PreDestroy`

5. `getUserById` user tapılmasa xəta atsın.

   `NoSuchElementException`

6. Controller, service və repository konstruktorlarına `... yaradildi` log-u əlavə et. **10 sorğu** göndər — log neçə dəfə çıxdı? Cavabı şərh kimi yaz.

   `this.hashCode()`

7. `POST /create` ilə user yarat, qaytarılan `id` ilə `GET /{id}` çağır. Eyni user gəlirmi? `GET /all` neçə user qaytarır?

   `Postman`

8. Adi `HashMap` və `long counter` burda niyə **təhlükəlidir**? Singleton bean-i eyni anda neçə thread istifadə edir? Şərh kimi izah et.

   `thread-safety`

---

## Bölmə 2 · Prototype

*UserAuditLog · ObjectProvider*

9. `component` paketində `UserAuditLog` class-ı yarat: `auditId` (UUID), `createdAt` (`LocalDateTime`), `action` (String, setter ilə).

   `@Component + @Scope("prototype")`

10. Konstruktorda `UserAuditLog yaradildi:` + `auditId` log-u yaz.

11. **Səhv variant:** `UserAuditLog`-u `UserInfoService`-in konstruktoruna birbaşa inject et. 3 user yarat. Neçə fərqli `auditId` gördün? Log-u screenshot et.

    `gözlənilən nəticə: 1`

12. **Düzgün variant:** hər `createUser` çağırışında yeni obyekt al və `action = "CREATE_USER"` təyin et. Yenə 3 user yarat.

    `ObjectProvider<UserAuditLog>.getObject()`

13. `UserAuditLog`-a `@PreDestroy` əlavə et və tətbiqi bağla. Log çıxdımı? **Niyə?** Şərh kimi yaz.

    `prototype bean-lərin destroy-u`

---

## Bölmə 3 · Request

*RequestContext · scoped proxy*

14. `RequestContext` class-ı yarat: `requestId` (UUID-in ilk 8 simvolu), `startTime`.

    `@Component + @RequestScope`

15. `RequestContext`-i **üç qatın hamısına** inject et: controller, service, repository.

    `final field + constructor`

16. Hər qatın `getUserById` metodunda log yaz: `[Controller] requestId=...`, `[Service] ...`, `[Repository] ...`. Bir sorğuda üçü **eyni**, növbəti sorğuda **fərqli** olmalıdır.

17. Controller-də cavab qaytarmazdan əvvəl sorğunun neçə millisaniyə çəkdiyini log-a yaz.

    `System.currentTimeMillis() - startTime`

18. `@RequestScope`-u `@Scope("request")` ilə əvəz et (**proxyMode olmadan**). Tətbiq açıldımı? Xətanı şərh kimi yaz, sonra düzəlt.

    `proxyMode = ScopedProxyMode.TARGET_CLASS`

19. Controller-də `requestContext.getClass()` çap et. Nə çıxdı? Singleton-a request bean necə inject oluna bilir? İzah et.

    `$$SpringCGLIB$$`

---

## Bölmə 4 · Session

*RecentlyViewedUsers · JSESSIONID*

20. `RecentlyViewedUsers` yarat: son **5** ID-ni saxlasın, təkrar olmasın, ən yenisi əvvəldə.

    `@Component + @SessionScope`

21. `getUserById` hər çağırılanda ID siyahıya əlavə olunsun.

22. `GET /api/v1/user-info/recent` endpoint-i yaz — ID-ləri yox, **tam** user məlumatını qaytarsın. Service-də yeni metod lazım olacaq.

    `List<UserInfoResponse>`

23. Session müddətini **1 dəqiqəyə** endir və `@PreDestroy` log-unun çıxdığını yoxla.

    `server.servlet.session.timeout: 1m`

24. Chrome-da `/1`, `/3`, `/2`, `/1` aç, sonra `/recent`. Sonra **incognito**-da `/recent` aç. Nəticələri müqayisə et. Postman-da niyə boş gələ bilər?

    `JSESSIONID cookie`

25. Bu siyahını request scope-da saxlasaydıq nə olardı? Şərh kimi yaz.

---

## Bölmə 5 · Dependency Injection

*interface · @Primary · @Qualifier · setter · test*

26. `UserRepository`-ni **interface**-ə çevir (3 metod). 1-ci bölmədəki kodu `InMemoryUserRepository`-yə köçür.

    `implements UserRepository`

27. `MockUserRepository` yaz — həmişə adı `Mock User` olan 2 user qaytarsın. Tətbiqi işə sal və xətanı qeyd et.

    `NoUniqueBeanDefinitionException`

28. Xətanı **birinci yolla** həll et: `InMemoryUserRepository` default olsun.

    `@Primary`

29. `@Primary`-ni sil, **ikinci yolla** həll et: service mock repository-ni seçsin. `/all` indi `Mock User` qaytarmalıdır.

    `@Qualifier("mockUserRepository")`

30. `NotificationService` interface-i yarat (`sendWelcome(String email)`) və `UserInfoService`-ə **optional** inject et. Implementasiyasız işə sal — xəta olmamalıdır.

    `@Autowired(required = false) setter`

31. `ConsoleNotificationService` əlavə et, `createUser`-də `Xoş gəldin` log-unu gör.

    `@Service`

32. `UserInfoService` üçün **Spring-siz** unit test yaz. Sonra service-i field injection-a keçir — test niyə işləmir? Şərh yaz və constructor injection-a qaytar.

    `Mockito.mock(...) + new UserInfoService(mock)`

33. Service konstruktoruna `List<UserRepository>` qəbul etdir. Neçə element gəldi? Yoxla.

    `List<T> injection`

---

## Bölmə 6 · Bonus

*məcburi deyil · əlavə bal*

34. `GET /api/v1/scope-demo` yaz: singleton `hashCode`, iki prototype `auditId`, `requestId`. 3 dəfə çağır, dəyişiklikləri cədvəl kimi README-yə yaz.

35. `UserStatistics` yarat: yaradılan user sayı və `getUserById` çağırış sayı. `GET /api/v1/stats` endpoint-i yaz. Fərqli brauzerlərdə sayğac **ümumi** olmalıdır.

    `@ApplicationScope + AtomicLong`

36. Repository seçimini `application.yml`-dan oxu: `app.repository-type: memory` və ya `mock`.

    `@ConditionalOnProperty`

37. Yalnız `az.iktex` paketindəki bean-lər üçün before/after initialization log-u yaz. Konsoldakı ardıcıllığı izah et.

    `BeanPostProcessor`

---

## Qaydalar

- Yalnız **constructor injection**: `final` field + `@RequiredArgsConstructor` və ya əl ilə konstruktor. Field üzərində `@Autowired` olmasın.
- Kodda `new UserInfoService(...)` və ya `new UserRepository()` olmasın — obyektləri Spring yaradır (yalnız unit test istisnadır).
- Yeni class-ları `az.iktex.organization.structure.testders.component` paketinə yaz.
- Hər bölmə ayrıca Git commit olsun: `task-1: singleton`, `task-2: prototype` və s.
- Şərh tələb olunan sualların cavablarını `README.md`-də `Tapşırıq N` başlığı ilə yaz, log və Postman screenshot-larını əlavə et.
- Təhvil: **GitHub repo linki**. Kod kompilyasiya olunmalıdır.

## Şifahi müdafiə üçün suallar

- IoC ilə Dependency Injection arasında fərq nədir?
- Constructor injection niyə field injection-dan yaxşıdır? Üç səbəb de.
- Prototype bean-in `@PreDestroy` metodu nə vaxt çağırılır?
- Request scope bean-i singleton-a inject edəndə arxada nə baş verir?
- Session scope ilə application scope arasında fərq nədir? Hər birinə real nümunə de.
- `@Primary` və `@Qualifier` eyni anda olanda hansı qalib gəlir?
