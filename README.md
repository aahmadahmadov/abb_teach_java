# SQL Məhdudiyyətləri və İndekslər — Praktik Tapşırıq

**Constraints · Indexes · İcra planının təhlili — 40 tapşırıq**

Mühit: PostgreSQL 15+ · Tapşırıq sayı: 40 · Maksimal bal: 100 · Tövsiyə olunan vaxt: 120 dəqiqə

Ad, Soyad: ______________________________ Qrup: ____________ Tarix: ____________

**Qaydalar.** Hər tapşırığın cavabı işlək SQL kodudur. Yaratdığınız hər məhdudiyyətə açıq ad verin — prefikslər: pk_, uq_, chk_, fk_, indekslər üçün idx_. Sistem tərəfindən verilən ada güvənmək olmaz. G və H bölmələrində yalnız kod kifayət deyil: EXPLAIN nəticəsi və 1–2 cümləlik izah da tələb olunur; izahsız cavab yarım bal alır. Sintaktik xəta olan kod 0 bal alır. Cavabları tək .sql faylında, hər tapşırığın üstündə `-- 1-ci tapşırıq` şəklində şərhlə təhvil verin; ölçmə nəticələrini ayrıca hesabat faylına yazın.

## Hazırlıq — 1. İş sahəsi

A–F bölmələrində cədvəlləri özünüz yaradacaqsınız. Bunun üçün təmiz bir sxem açın.

```sql
CREATE SCHEMA IF NOT EXISTS magaza;
SET search_path TO magaza, public;
-- İcra vaxtını görmək üçün (psql-də):
\timing on
-- Planları oxunaqlı saxlamaq üçün paralelliyi söndürün:
SET max_parallel_workers_per_gather = 0;
```

## Hazırlıq — 2. İndeks bölmələri üçün cədvəl

Aşağıdakı skripti olduğu kimi icra edin. G və H bölmələri bu cədvəl üzərində həll olunur.

```sql
DROP TABLE IF EXISTS satis_log;
CREATE TABLE satis_log
(
 id INT,
 musteri_kodu INT,
 mehsul_adi VARCHAR(80),
 kateqoriya VARCHAR(30),
 seher VARCHAR(30),
 status VARCHAR(20),
 miqdar INT,
 mebleg NUMERIC(12, 2),
 tarix DATE
);
INSERT INTO satis_log
SELECT i,
 (random() * 20000)::int + 1,
 'Mehsul ' || (i % 5000),
 (ARRAY['Texnika','Aksesuar','Ofis','Mebel','Kitab'])[(i % 5) + 1],
 (ARRAY['Bakı','Gəncə','Sumqayıt','Şəki','Lənkəran'])[(i % 5) + 1],
 CASE WHEN i % 97 = 0 THEN 'legv' ELSE 'tamam' END,
 (random() * 10)::int + 1,
 (random() * 5000 + 10)::numeric(12, 2),
 DATE '2022-01-01' + (i % 1000)
FROM generate_series(1, 300000) AS i;
ANALYZE satis_log;
```

Bu cədvəlin üç xüsusiyyəti:
- satis_log-da heç bir məhdudiyyət və heç bir indeks yoxdur — PRIMARY KEY belə yoxdur.
- status sütununun təxminən 99%-i 'tamam'-dır — seçiciliyi (selectivity) çox aşağıdır.
- mehsul_adi cəmi 5 000 təkrarsız dəyərdən ibarətdir və 300 000 sətirə paylanıb.

## A. Cədvəl açarları və NOT NULL

*PRIMARY KEY · NOT NULL · IDENTITY / SERIAL · kompozit açar*

**1.** kateqoriya cədvəlini yaradın: id — avtomatik artan və cədvəlin açarı, ad — VARCHAR(50), boş ola bilməz. Açara açıq ad verin: pk_kateqoriya. (2 bal)
İpucu: `GENERATED ALWAYS AS IDENTITY` və `CONSTRAINT pk_kateqoriya PRIMARY KEY (id)`.

**2.** mehsul cədvəlini yaradın: id (açar), ad, kateqoriya_id, qiymet NUMERIC(10,2), anbarda_say INT, aktiv BOOLEAN. ad və qiymet boş ola bilməz. Hələlik yalnız PRIMARY KEY və NOT NULL yazın. (2 bal)

**3.** musteri cədvəlini yaradın: id (açar), ad, soyad, email — üçü də boş ola bilməz; telefon boş qala bilər; qeydiyyat_tarixi DATE. (2 bal)

**4.** sifaris_detal cədvəlini yaradın. Açar tək sütun deyil: sifaris_id və mehsul_id birlikdə açarı təşkil etsin. Əlavə sütunlar: say, vahid_qiymet. (2 bal)
İpucu: Kompozit açar yalnız cədvəl səviyyəsində yazılır: `PRIMARY KEY (sifaris_id, mehsul_id)`.

**5.** Yaratdığınız cədvəllərin bütün indekslərini pg_indexes-dən çıxaran sorğu yazın. Siz heç bir indeks yaratmamısınız, amma nəticə boş deyil — niyə? PRIMARY KEY ilə UNIQUE + NOT NULL arasındakı fərqi bir cümlə ilə yazın. (2 bal)
İpucu: `WHERE schemaname = 'magaza'`. PK məhdudiyyət, indeks isə onun icra mexanizmidir.

## B. Təkrarsızlıq — UNIQUE

*UNIQUE · kompozit UNIQUE · NULL davranışı · partial unique index*

**6.** musteri.email təkrarlanmasın. Cədvəli yenidən yaratmadan, ALTER TABLE ilə adlandırılmış UNIQUE məhdudiyyət əlavə edin. Sonra eyni email ilə ikinci müştəri yazmağa çalışın və xəta mesajını qeyd edin. (2 bal)

**7.** mehsul cədvəlində eyni kateqoriyada eyni adlı iki məhsul olmasın, lakin fərqli kateqoriyalarda eyni ad işlənə bilsin. Məhdudiyyəti qurun və hər iki halı test edin. (2 bal)
İpucu: Tək sütuna deyil, sütun cütünə qoyulan UNIQUE.

**8.** musteri.telefon sütununa UNIQUE qoyun, sonra telefonu NULL olan iki müştəri əlavə edin. Sorğu keçirmi? Nəticəni izah edin və hər iki NULL-u da təkrar sayan variantı yazın. (2 bal)
İpucu: Standart davranışda NULL heç nəyə bərabər deyil, hətta özünə də. PostgreSQL 15+ üçün: `UNIQUE NULLS NOT DISTINCT`.

**9.** Hər kateqoriyada yalnız bir məhsul aktiv = true ola bilsin. Qeyri-aktiv məhsulların sayı isə məhdudlaşdırılmasın. Adi UNIQUE bunu həll etmir. (2 bal)
İpucu: `CREATE UNIQUE INDEX ... ON mehsul(kateqoriya_id) WHERE aktiv`.

## C. Dəyər yoxlamaları — CHECK

*CHECK · çoxsütunlu CHECK · CHECK və NULL · üç dəyərli məntiq*

**10.** mehsul cədvəlinə iki adlandırılmış CHECK əlavə edin: qiymet 0-dan böyük, anbarda_say mənfi olmasın. Hər ikisini pozan INSERT yazıb xəta mesajlarını qeyd edin. (2 bal)

**11.** musteri.email üçün qayda: tərkibində @ və nöqtə olsun, uzunluğu 5 simvoldan çox olsun, boşluq olmasın. (2 bal)
İpucu: POSITION, LENGTH və ya LIKE şablonları AND ilə birləşdirilir.

**12.** mehsul cədvəlinə endirimli_qiymet sütunu əlavə edin. Şərt: endirimli qiymət qiymet-dən böyük ola bilməz və mənfi olmamalıdır. (2 bal)
İpucu: İki sütunu eyni anda yoxlayan CHECK sütun səviyyəsində yazıla bilməz.

**13.** sifaris cədvəlini yaradın: status yalnız gozleyir, gonderilib, catdirilib, legv dəyərlərindən biri ola bilsin. Əlavə şərt: status legv olduqda legv_sebebi mütləq doldurulsun. (2 bal)
İpucu: `CHECK (status <> 'legv' OR legv_sebebi IS NOT NULL)` — şərti implikasiya kimi düşünün.

**14.** 12-ci tapşırıqdakı CHECK var, lakin endirimli_qiymet sütununa NULL yazanda sorğu keçir. Səbəbini üç dəyərli məntiqlə (TRUE / FALSE / UNKNOWN) izah edin və NULL-u da bloklayan düzgün həlli yazın. (2 bal)
İpucu: CHECK yalnız nəticə açıq-aydın FALSE olduqda sətri rədd edir.

## D. Standart və hesablanan dəyərlər

*DEFAULT · DEFAULT və açıq NULL · GENERATED ALWAYS AS ... STORED*

**15.** Standart dəyərləri qurun: musteri.qeydiyyat_tarixi → cari tarix, mehsul.anbarda_say → 0, mehsul.aktiv → true, sifaris.status → 'gozleyir'. (2 bal)

**16.** İki müştəri əlavə edin: birində qeydiyyat_tarixi sütununu ümumiyyətlə yazmayın, digərində isə açıq şəkildə NULL yazın. Nəticələr fərqlidir — səbəbini izah edin. (2 bal)
İpucu: DEFAULT yalnız sütun sorğuda iştirak etmədikdə işə düşür.

**17.** sifaris_detal cədvəlinə cemi sütunu əlavə edin — dəyəri say * vahid_qiymet kimi avtomatik hesablansın və saxlanılsın. Sonra bu sütuna əl ilə UPDATE etməyə çalışın və nəticəni qeyd edin. (2 bal)
İpucu: `GENERATED ALWAYS AS (say * vahid_qiymet) STORED`.

## E. Cədvəllərarası bağlar — FOREIGN KEY

*FOREIGN KEY · ON DELETE CASCADE / RESTRICT / SET NULL · self-reference · DEFERRABLE*

**18.** İki adlandırılmış xarici açar qurun: mehsul.kateqoriya_id → kateqoriya.id və sifaris.musteri_id → musteri.id. Mövcud olmayan kateqoriya_id ilə məhsul əlavə etməyə çalışın. (2 bal)

**19.** Üç fərqli silinmə davranışı qurun və hər birini ayrıca test edin: sifaris_detal.sifaris_id → CASCADE, sifaris_detal.mehsul_id → RESTRICT, mehsul.kateqoriya_id → SET NULL. Hər halda valideyn sətri silib nəticəni yazın. (2 bal)

**20.** musteri cədvəlinə devet_eden_id sütunu əlavə edin — həmin cədvələ istinad etsin (özünə istinad edən açar). Sonra bir-birini dəvət etmiş iki müştərini tək tranzaksiyada əlavə edin. (2 bal)
İpucu: Adi FK ilə mümkün deyil — DEFERRABLE INITIALLY DEFERRED lazımdır. Yoxlama COMMIT anına təxirə salınır.

## F. Məhdudiyyətlərin idarə olunması

*ALTER TABLE · DROP CONSTRAINT · NOT VALID · VALIDATE · pg_constraint*

**21.** mehsul.anbarda_say sütununu məcburi (NOT NULL) edin. Cədvəldə NULL dəyərlər varsa xəta alacaqsınız — əvvəlcə problemli sətirləri tapan sorğu yazın, onları düzəldin, sonra məhdudiyyəti tətbiq edin. (2 bal)

**22.** mehsul üzərindəki qiymət CHECK-ini silin və yerinə yenisini qoyun: qiymət 0-dan böyük və 100 000-dən kiçik olsun. Silmə və əlavə etmə eyni ALTER TABLE ifadəsində yazıla bilərmi? Yoxlayın. (2 bal)

**23.** Vəziyyət: cədvəldə qaydanı pozan köhnə sətirlər var, amma yeni sətirlərə qayda tətbiq olunmalıdır. Məhdudiyyəti mövcud sətirləri yoxlamadan əlavə edin, bir neçə səhv INSERT ilə onun işlədiyini sübut edin, sonra köhnə sətirləri düzəldib məhdudiyyəti təsdiqləyin. (2 bal)
İpucu: `ADD CONSTRAINT ... NOT VALID`, sonra `VALIDATE CONSTRAINT`. Fərqi izah edin.

**24.** Kütləvi məlumat yükləməsi üçün bir FK-nın yoxlanışını müvəqqəti dayandırmaq lazımdır. İki fərqli yol yazın və hər birinin risklərini bir cümlə ilə müqayisə edin. (2 bal)
İpucu: Birinci yol — məhdudiyyəti silib yükləmədən sonra geri qaytarmaq; ikinci yol — `SET CONSTRAINTS ALL DEFERRED` tranzaksiya daxilində.

**25.** Audit sorğusu. Sxeminizdəki bütün məhdudiyyətləri bir cədvəldə çıxarın: cədvəl adı, məhdudiyyət adı, tipi (p / u / c / f hərfləri Esas acar, Tekrarsiz, Yoxlama, Xarici acar kimi oxunaqlı yazılsın) və tam tərifi. Cədvəl adına görə sıralansın. (2 bal)
İpucu: `pg_constraint` + CASE + `pg_get_constraintdef(oid)` + `conrelid::regclass`.

## G. İndekslər — əsaslar

*EXPLAIN ANALYZE · B-tree · kompozit indeks · partial index · ifadə üzrə indeks*

Bu bölmədən etibarən bütün işlər satis_log cədvəli üzərində aparılır. Hər ölçmədən əvvəl `ANALYZE satis_log;` icra edin.

**26.** `WHERE mehsul_adi = 'Mehsul 4321'` sorğusunu `EXPLAIN (ANALYZE, BUFFERS)` ilə ölçün, sonra indeks qurub təkrar ölçün. İki cədvəldə müqayisə edin: icra vaxtı, plan növü (Seq Scan / Index Scan) və oxunan blok sayı (shared hit/read). (2 bal)

**27.** (kateqoriya, tarix) üzrə kompozit indeks qurun və üç sorğunu ayrı-ayrı yoxlayın: (a) yalnız kateqoriya üzrə, (b) yalnız tarix üzrə, (c) hər ikisi üzrə. Hansında indeks işə düşmədi? Kompozit indeksdə sütun sırasının niyə vacib olduğunu izah edin. (2 bal)
İpucu: Sol prefiks qaydası (leftmost prefix).

**28.** UNIQUE məhdudiyyət ilə UNIQUE INDEX arasındakı fərqi praktikada göstərin: ikisini də yaradın, pg_constraint və pg_indexes-də axtarın, sonra hər birini DROP CONSTRAINT ilə silməyə çalışın. Nəticəni izah edin. (2 bal)

**29.** `WHERE status = 'legv'` sorğusu üçün əvvəlcə tam indeks, sonra partial indeks qurun. İkisinin ölçüsünü və sorğu sürətini müqayisə edin. Partial indeks hansı halda məqsədəuyğundur? (2 bal)
İpucu: `pg_size_pretty(pg_relation_size('idx_ad'))`. Məlumatın 99%-i 'tamam'-dır.

**30.** `WHERE UPPER(mehsul_adi) = 'MEHSUL 100'` sorğusu 26-cı tapşırıqda qurduğunuz indeksdən istifadə etmir. Səbəbini izah edin və iki fərqli həll yazın, ikisini də ölçün. (2 bal)
İpucu: Sütunun üzərinə funksiya tətbiq olunduqda indeks açarı ilə uyğunluq itir. Həll yollarından biri — ifadə üzrə indeks.

## H. Çətin və qarışıq tapşırıqlar

*Index Only Scan · INCLUDE · GIN / pg_trgm · bloat · sistem kataloqları · yazma qiyməti*

**31.** `SELECT seher, tarix FROM satis_log WHERE seher = 'Gəncə'` sorğusunu Index Only Scan ilə işlətməyə nail olun. Planda Heap Fetches sətrini tapın, dəyərini sıfıra endirin və bunun nə demək olduğunu izah edin. (4 bal)
İpucu: INCLUDE bəndi və ya kompozit indeks; sonra `VACUUM satis_log;` — görünürlük xəritəsi yenilənməlidir.

**32.** `ORDER BY mebleg DESC LIMIT 20` sorğusunda sıralamanın indeks hesabına aparıldığını sübut edin — planda Sort düyünü olmamalıdır. Sonra eyni nəticəni `ORDER BY mebleg DESC NULLS LAST` üçün əldə edin. (4 bal)
İpucu: İndeks öz sıralama qaydası ilə yaradılır: `CREATE INDEX ... (mebleg DESC NULLS LAST)`.

**33.** Hesabat sorğusu: satis_log-un bütün indekslərini ölçüsü, ölçünün cədvələ nisbəti (faizlə, 1 rəqəm) və indeksin tərifi ilə birlikdə sadalayın. Ölçüyə görə azalan sıra. Ən «bahalı» indeks hansıdır? (4 bal)
İpucu: `pg_indexes` + `pg_relation_size()` + `pg_size_pretty()`.

**34.** Heç vaxt istifadə olunmayan indeksləri aşkarlayın: əvvəlcə statistikanı sıfırlayın, sonra 5–6 müxtəlif SELECT icra edin, sonra hər indeks üçün skan sayını göstərən hesabat çıxarın və `idx_scan = 0` olanları işarələyin. (4 bal)
İpucu: `SELECT pg_stat_reset();` + `pg_stat_user_indexes`.

**35.** `WHERE mehsul_adi LIKE '%hsul 4321%'` sorğusunu sürətləndirin. Adi B-tree indeks burada kömək etmir — səbəbini izah edin və işləyən həlli qurub ölçün. (4 bal)
İpucu: `CREATE EXTENSION pg_trgm;` + `USING GIN (mehsul_adi gin_trgm_ops)`.

**36.** İndeksin yazma əməliyyatına qiymətini ölçün: (a) bütün indeksləri silin, 100 000 sətir INSERT edin, vaxtı qeyd edin; (b) 5 indeks qurun, əlavə etdiyiniz sətirləri silin, eyni INSERT-i təkrarlayın. Fərqi faizlə göstərin və bir cümləlik nəticə yazın. (4 bal)

**37.** satis_log-a PRIMARY KEY əlavə edin, sonra ona FK ilə bağlı satis_qeyd cədvəli yaradıb 200 000 sətir doldurun. FK sütununa indeks qurmadan valideyn cədvəldən sətir silin və vaxtı ölçün; sonra indeks qurub təkrarlayın. PostgreSQL FK sütununa avtomatik indeks yaradırmı? (4 bal)
İpucu: Valideyn sətri silinəndə baza uşaq cədvəldə istinadları axtarmalıdır — indeks yoxdursa bu, tam skan deməkdir.

**38.** `WHERE kateqoriya = 'Ofis' AND seher = 'Bakı'` sorğusunu iki konfiqurasiyada müqayisə edin: (a) iki ayrı bir-sütunlu indeks, (b) bir kompozit indeks (kateqoriya, seher). Planda BitmapAnd görünürmü? Hansı variant daha sürətlidir və niyə? (4 bal)

**39.** İndeksin ölçüsünü qeyd edin, sonra cədvəlin təxminən 40%-ni UPDATE edin və ölçüyə yenidən baxın. Ölçü niyə artdı? n_dead_tup dəyərini göstərin, REINDEX icra edib fərqi cədvəldə təqdim edin. (4 bal)
İpucu: Şişmə (bloat), MVCC və ölü sətirlər. `pg_stat_user_tables`.

**40.** Yekun audit sorğusu. Sxeminizdəki hər cədvəl üçün bir sətir: cədvəl adı, təxmini sətir sayı, cədvəl ölçüsü, indeks sayı, indekslərin ümumi ölçüsü, PRIMARY KEY-in olub-olmaması (Var / Yoxdur) və status — PK yoxdursa Problemli, indekslərin ölçüsü cədvəlin 50%-dən çoxdursa Nezaret lazimdir, qalanı Normal. Cədvəl ölçüsünə görə azalan sıra. (4 bal)
İpucu: `pg_class` / `pg_stat_user_tables` + alt-sorğular + CASE + `pg_total_relation_size`. Məhdudiyyət, indeks, ölçü və şərti ifadələr — hamısı bir sorğuda.

## Qiymətləndirmə

| Bölmə | Tapşırıq | Bal |
|---|---|---|
| A. Cədvəl açarları və NOT NULL | 1–5 | 10 |
| B. Təkrarsızlıq — UNIQUE | 6–9 | 8 |
| C. Dəyər yoxlamaları — CHECK | 10–14 | 10 |
| D. Standart və hesablanan dəyərlər | 15–17 | 6 |
| E. FOREIGN KEY | 18–20 | 6 |
| F. Məhdudiyyətlərin idarəsi | 21–25 | 10 |
| G. İndekslər — əsaslar | 26–30 | 10 |
| H. Çətin və qarışıq tapşırıqlar | 31–40 | 40 |
| **Cəmi** | **1–40** | **100** |

| Bal | Qiymət |
|---|---|
| 90–100 | Əla |
| 75–89 | Yaxşı |
| 60–74 | Kafi |
| 60-dan aşağı | Təkrar |

| Bal azaldılır | Cəza |
|---|---|
| Kod icra olunmur / sintaktik xəta | 0 bal |
| G–H bölməsində izah yoxdur | –50% |
| Məhdudiyyətə ad verilməyib | –0.5 bal |
| Kopyalanmış iş | 0 bal |
