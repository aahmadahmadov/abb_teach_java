# SQL Dərinlik Modulu — Praktik Tapşırıq

**CTE (WITH) · Müvəqqəti cədvəllər · Tranzaksiyalar · ACID — prezentasiyanın 22 slaydını tam əhatə edən 51 tapşırıq**

Mühit: PostgreSQL 14+ (iki paralel bağlantı tələb olunur) · Tapşırıq sayı: 51 · Maksimal bal: 140 · Tövsiyə olunan vaxt: 180 dəqiqə (2 seans)

Ad, Soyad: ______________________________ Qrup: ____________ Tarix: ____________

**Qaydalar.**
- Hər tapşırığın cavabı işlək SQL skriptidir. Sintaktik xəta olan cavab 0 bal alır.
- Tapşırıqda konkret konstruksiya tələb olunubsa (`WITH`, `WITH RECURSIVE`, `TEMP TABLE`, `SAVEPOINT`, `FOR UPDATE` və s.), başqa üsulla alınmış düzgün nəticə də 0 bal alır.
- İki bağlantı tələb edən tapşırıqlarda hər sessiyanın skripti ayrıca yazılır və `-- Sessiya A` / `-- Sessiya B` şərhi ilə işarələnir. Müşahidə olunan nəticə (xəta mətni, gözləmə, oxunan dəyər) mütləq şərh blokunda qeyd olunmalıdır.
- `EXPLAIN ANALYZE` tələb olunan yerlərdə planın xülasəsi (node tipi, `rows=` gözləntisi, `actual time`) cavaba şərh kimi əlavə edilir.
- Sütunlara mənalı ləqəb (`AS`) verilməlidir. `SELECT *` yalnız açıq icazə verilən yerlərdə.
- Data dəyişdirən tapşırıqdan sonra cədvəl ilkin vəziyyətə qaytarılmalıdır (skriptin sonunda `ROLLBACK` və ya bərpa əmri).
- Cavablar tək .sql faylında, hər tapşırığın üstündə `-- 1-ci tapşırıq` şəklində şərhlə təhvil verilir.

## Hazırlıq — sxemi yaradın

Aşağıdakı skripti olduğu kimi icra edin. Bütün 51 tapşırıq bu altı cədvəl üzərində həll olunur.

```sql
DROP TABLE IF EXISTS kocurme_log;
DROP TABLE IF EXISTS satis;
DROP TABLE IF EXISTS hesab;
DROP TABLE IF EXISTS anbar;
DROP TABLE IF EXISTS isci;
DROP TABLE IF EXISTS kateqoriya;
DROP TABLE IF EXISTS qraf;

-- 1) Satış faktları — CTE, aqreqasiya, temp cədvəl bölmələri üçün
CREATE TABLE satis
(
 satis_id      INT PRIMARY KEY,
 tarix         DATE           NOT NULL,
 seher         VARCHAR(30),
 kateqoriya_id INT,
 satici_id     INT,
 musteri_id    INT,
 mebleg        NUMERIC(10, 2) NOT NULL
);

INSERT INTO satis
VALUES (1, DATE '2024-01-12', 'Bakı', 100, 4, 101, 2500.00),
       (2, DATE '2024-01-25', 'Gəncə', 20, 5, 102, 129.50),
       (3, DATE '2024-01-30', 'Bakı', 110, 7, 103, 1799.00),
       (4, DATE '2024-02-08', 'Sumqayıt', 101, 8, 104, 641.00),
       (5, DATE '2024-02-14', 'Bakı', 210, 4, 101, 349.90),
       (6, DATE '2024-02-22', 'Şəki', 100, 9, 105, 1250.00),
       (7, DATE '2024-03-05', 'Bakı', 110, 7, 106, 899.99),
       (8, DATE '2024-03-11', 'Gəncə', 100, 5, 102, 3598.00),
       (9, DATE '2024-03-19', 'Sumqayıt', 21, 8, 107, 89.90),
       (10, DATE '2024-03-28', 'Bakı', 101, 4, 103, 320.50),
       (11, DATE '2024-04-03', NULL, 100, 9, 108, 1799.00),
       (12, DATE '2024-04-16', 'Gəncə', 210, 5, 102, 699.80),
       (13, DATE '2024-04-21', 'Bakı', 110, 7, 101, 2699.97),
       (14, DATE '2024-04-29', 'Şəki', 20, 9, 105, 62.50),
       (15, DATE '2024-06-04', 'Bakı', 100, 4, 109, 4999.00),
       (16, DATE '2024-06-12', 'Sumqayıt', 101, 8, 104, 961.50),
       (17, DATE '2024-06-20', 'Gəncə', 21, 5, 110, 269.70),
       (18, DATE '2024-06-27', 'Bakı', 110, 7, 106, 1799.98),
       (19, DATE '2024-07-02', 'Bakı', 210, 4, 103, 174.95),
       (20, DATE '2024-07-15', 'Şəki', 100, 9, 105, 2500.00),
       (21, DATE '2024-07-23', 'Sumqayıt', 20, 8, 107, 37.50),
       (22, DATE '2024-08-05', 'Bakı', 101, 4, 101, 1282.00),
       (23, DATE '2024-08-14', 'Gəncə', 110, 5, 102, 899.99),
       (24, DATE '2024-08-27', 'Bakı', 100, 7, 109, 3750.00);

-- 2) İşçi ierarxiyası — rekursiv CTE üçün (özünə istinad)
CREATE TABLE isci
(
 isci_id   INT PRIMARY KEY,
 ad        VARCHAR(50),
 vezife    VARCHAR(40),
 rehber_id INT REFERENCES isci (isci_id),
 maas      NUMERIC(10, 2),
 ise_qebul DATE
);

INSERT INTO isci
VALUES (1, 'Aygün Məmmədova', 'Baş direktor', NULL, 9000.00, DATE '2018-02-01'),
       (2, 'Rauf Əliyev', 'Satış direktoru', 1, 6500.00, DATE '2019-03-15'),
       (3, 'Nigar Hüseynova', 'Texnologiya direktoru', 1, 6800.00, DATE '2019-05-20'),
       (4, 'Elvin Qasımov', 'Satış meneceri (Bakı)', 2, 4200.00, DATE '2020-01-10'),
       (5, 'Leyla Nəbiyeva', 'Satış meneceri (Gəncə)', 2, 4000.00, DATE '2020-06-01'),
       (6, 'Tural Səfərov', 'Komanda lideri', 3, 4800.00, DATE '2020-09-14'),
       (7, 'Kamran Vəliyev', 'Satıcı', 4, 2600.00, DATE '2021-02-11'),
       (8, 'Səbinə Quliyeva', 'Satıcı', 4, 2500.00, DATE '2021-04-05'),
       (9, 'Orxan Babayev', 'Satıcı', 5, 2400.00, DATE '2021-07-19'),
       (10, 'Günel Rzayeva', 'Developer', 6, 3800.00, DATE '2021-11-03'),
       (11, 'Anar Cəfərov', 'Developer', 6, 3600.00, DATE '2022-01-17'),
       (12, 'Nərmin Əliyeva', 'Stajçı', 10, 1200.00, DATE '2023-09-01');

-- 3) Kateqoriya ağacı — rekursiya və kompleks hesabat üçün
CREATE TABLE kateqoriya
(
 kateqoriya_id INT PRIMARY KEY,
 ad            VARCHAR(40),
 ust_id        INT
);

INSERT INTO kateqoriya
VALUES (1, 'Texnika', NULL),
       (2, 'Aksesuar', NULL),
       (10, 'Kompüter', 1),
       (11, 'Telefon', 1),
       (20, 'Kabel', 2),
       (21, 'Qulaqlıq', 2),
       (100, 'Noutbuk', 10),
       (101, 'Monitor', 10),
       (110, 'Smartfon', 11),
       (210, 'Simsiz qulaqlıq', 21);

-- 4) Dövrəli qraf — rekursiyanın təhlükəsi üçün (2 → 3 → 4 → 2)
CREATE TABLE qraf
(
 ust INT,
 alt INT
);
INSERT INTO qraf
VALUES (1, 2),
       (2, 3),
       (3, 4),
       (4, 2),
       (3, 5);

-- 5) Bank hesabları — tranzaksiya və ACID üçün
CREATE TABLE hesab
(
 hesab_id INT PRIMARY KEY,
 sahib    VARCHAR(50),
 balans   NUMERIC(12, 2) NOT NULL CHECK (balans >= 0),
 valyuta  CHAR(3) DEFAULT 'AZN'
);

INSERT INTO hesab
VALUES (1, 'Aysel Məmmədova', 5000.00, 'AZN'),
       (2, 'Rauf Əliyev', 1200.00, 'AZN'),
       (3, 'Nigar Hüseynova', 300.00, 'AZN'),
       (4, 'Elvin Qasımov', 0.00, 'AZN');

CREATE TABLE kocurme_log
(
 log_id    SERIAL PRIMARY KEY,
 hesab_id  INT,
 emeliyyat VARCHAR(20),
 mebleg    NUMERIC(12, 2),
 qeyd      TEXT,
 yaradildi TIMESTAMP DEFAULT now()
);

-- 6) Anbar — lock, deadlock və optimistic locking üçün
CREATE TABLE anbar
(
 mehsul_id INT PRIMARY KEY,
 ad        VARCHAR(40),
 qaliq     INT NOT NULL CHECK (qaliq >= 0),
 versiya   INT NOT NULL DEFAULT 1
);

INSERT INTO anbar
VALUES (1, 'Noutbuk Pro 15', 24, 1),
       (2, 'Monitor 27 düym', 40, 1),
       (3, 'Simsiz qulaqlıq', 65, 1),
       (4, 'USB-C kabel', 300, 1),
       (5, 'Smartfon X', 18, 1);
```

### satis cədvəlinin məzmunu (24 sətir)

| satis_id | tarix | seher | kateqoriya_id | satici_id | musteri_id | mebleg |
|---|---|---|---|---|---|---|
| 1 | 2024-01-12 | Bakı | 100 | 4 | 101 | 2500.00 |
| 2 | 2024-01-25 | Gəncə | 20 | 5 | 102 | 129.50 |
| 3 | 2024-01-30 | Bakı | 110 | 7 | 103 | 1799.00 |
| 4 | 2024-02-08 | Sumqayıt | 101 | 8 | 104 | 641.00 |
| 5 | 2024-02-14 | Bakı | 210 | 4 | 101 | 349.90 |
| 6 | 2024-02-22 | Şəki | 100 | 9 | 105 | 1250.00 |
| 7 | 2024-03-05 | Bakı | 110 | 7 | 106 | 899.99 |
| 8 | 2024-03-11 | Gəncə | 100 | 5 | 102 | 3598.00 |
| 9 | 2024-03-19 | Sumqayıt | 21 | 8 | 107 | 89.90 |
| 10 | 2024-03-28 | Bakı | 101 | 4 | 103 | 320.50 |
| 11 | 2024-04-03 | NULL | 100 | 9 | 108 | 1799.00 |
| 12 | 2024-04-16 | Gəncə | 210 | 5 | 102 | 699.80 |
| 13 | 2024-04-21 | Bakı | 110 | 7 | 101 | 2699.97 |
| 14 | 2024-04-29 | Şəki | 20 | 9 | 105 | 62.50 |
| 15 | 2024-06-04 | Bakı | 100 | 4 | 109 | 4999.00 |
| 16 | 2024-06-12 | Sumqayıt | 101 | 8 | 104 | 961.50 |
| 17 | 2024-06-20 | Gəncə | 21 | 5 | 110 | 269.70 |
| 18 | 2024-06-27 | Bakı | 110 | 7 | 106 | 1799.98 |
| 19 | 2024-07-02 | Bakı | 210 | 4 | 103 | 174.95 |
| 20 | 2024-07-15 | Şəki | 100 | 9 | 105 | 2500.00 |
| 21 | 2024-07-23 | Sumqayıt | 20 | 8 | 107 | 37.50 |
| 22 | 2024-08-05 | Bakı | 101 | 4 | 101 | 1282.00 |
| 23 | 2024-08-14 | Gəncə | 110 | 5 | 102 | 899.99 |
| 24 | 2024-08-27 | Bakı | 100 | 7 | 109 | 3750.00 |

Sxemin beş xüsusiyyəti — tapşırıqların açarı burada gizlənib:
- satis cədvəlində 2024-05 ayı üçün heç bir sətir yoxdur. Boşluqsuz aylıq seriya yalnız rekursiv CTE ilə qurula bilər.
- qraf cədvəlində 2 → 3 → 4 → 2 dövrəsi var. Dayanma şərti olmayan rekursiya sonsuza gedəcək.
- hesab.balans üzərində `CHECK (balans >= 0)` var — Consistency zəmanətini canlı görmək üçün.
- isci.rehber_id özünə istinad edir; ağacın ən dərin qolu 5 səviyyədir (1 → 3 → 6 → 10 → 12).
- satis.seher bir sətirdə (`satis_id = 11`) NULL-dur; anbar.versiya optimistic locking üçün hazırlanıb.

## A. CTE — sintaksis və işləmə məntiqi

*Slayd 4 · 1.1 — WITH · adlandırılmış nəticə çoxluğu · əhatə dairəsi · obyekt yaratmaması*

**1.** aktiv_satis adlı CTE yaradın: yalnız 2024-cü ilin II rübü (aprel–iyun) satışları. Əsas sorğuda həmin CTE üzərindən şəhər kəsiyində satış sayı və ümumi məbləği çıxarın; şəhər NULL olduqda Namelum yazılsın. Məbləğə görə azalan sıra. (2 bal)
İpucu: `WITH ad AS ( ... ) SELECT ... FROM ad;` — CTE əsas sorğudan ƏVVƏL yazılır.

**2.** Eyni CTE-yə bir sorğuda iki dəfə müraciət edin: aylıq cəmləri hesablayan CTE-ni özü ilə birləşdirib hər ayın cəmini əvvəlki ayın cəmi ilə yan-yana göstərin. Pəncərə funksiyası (`LAG`) istifadə etmək qadağandır. (2 bal)
İpucu: `FROM ayliq a LEFT JOIN ayliq b ON b.ay = a.ay - INTERVAL '1 month'` — bu, subquery-nin edə bilmədiyi şeydir.

**3.** CTE obyekt yaratmır. 1-ci tapşırığın sorğusunu icra etdikdən sonra ayrıca əmr kimi `SELECT * FROM aktiv_satis;` yazın. Alınan xəta mətnini olduğu kimi şərh blokunda qeyd edin və nəyin baş verdiyini 1 cümlə ilə izah edin. (2 bal)
İpucu: Gözlənilən xəta `relation "aktiv_satis" does not exist`. CTE — disk obyekti deyil, sorğu mətninə verilmiş addır.

**4.** Aşağıdakı iç-içə subquery-ni eyni nəticəni verən CTE-yə çevirin və məntiqi 2 addıma bölün. Təkrarlanan alt-sorğu yalnız bir dəfə yazılmalıdır. (2 bal)

```sql
SELECT seher, cem
FROM (SELECT seher, SUM(mebleg) AS cem FROM satis GROUP BY seher) t
WHERE cem > (SELECT AVG(cem)
             FROM (SELECT seher, SUM(mebleg) AS cem FROM satis GROUP BY seher) t2);
```

İpucu: CTE-nin əsas dəyəri budur — iç-içə subquery-ni oxunaqlı addımlara bölür.

**5.** CTE-yə açıq sütun siyahısı verin: `WITH ay_cem(ay, cem, say) AS ( ... )`. CTE-nin daxilində heç bir `AS` ləqəbi yazmadan, xaricdə sütunlara bu adlarla müraciət edin. (2 bal)
İpucu: Sütun sayı CTE-nin SELECT siyahısı ilə dəqiq üst-üstə düşməlidir, əks halda xəta alacaqsınız.

## B. Çoxlu və zəncirvari CTE

*Slayd 5 · 1.2 — bir WITH, vergüllə ayırma · istiqamət qaydası · tipik səhvlər*

**6.** Üç addımlı zəncir qurun: ayliq (ay üzrə dövriyyə) → orta (ayların orta dövriyyəsi) → yuksek (ortadan yuxarı aylar). Nəticə xronoloji sıra ilə; hər sətirdə ayın dövriyyəsi və ortadan neçə faiz yuxarı olduğu görünsün. (2 bal)
İpucu: WITH yalnız bir dəfə yazılır, CTE-lər vergüllə ayrılır. Hər CTE bir məntiqi addımı təmsil etməlidir.

**7.** Aşağıdakı skriptdə iki tipik səhv var. Skripti işlək hala salın və hər səhvi şərhdə adlandırın (nə səhvdir və niyə). (2 bal)

```sql
WITH ayliq AS (
    SELECT DATE_TRUNC('month', tarix) AS ay, SUM(mebleg) AS cem
    FROM satis GROUP BY 1
)
WITH orta AS (
    SELECT AVG(cem) AS orta_cem FROM ayliq
),
SELECT a.ay, a.cem FROM ayliq a CROSS JOIN orta o WHERE a.cem > o.orta_cem;
```

İpucu: Slayd 5-dəki "Tipik səhv" bloku — təkrar WITH və sonuncu CTE-dən sonrakı vergül.

**8.** Dörd addımlı zəncir yazın: (1) aylıq dövriyyə → (2) xronoloji yığılan (running) cəm → (3) hər ayın ümumi dövriyyədəki faiz payı və yığılan faiz → (4) CASE ilə yığılan cəmin 50%-i ilk dəfə keçdiyi ayı Pareto sərhədi kimi işarələyin. Hər addım ayrıca CTE olmalıdır. (3 bal)
İpucu: Zəncir = addım-addım düşünmək. 4 addımı bir CTE-yə yığmaq bal itkisidir.

**9.** İstiqamət qaydasını sübut edin. Elə bir WITH yazın ki, birinci CTE ikinciyə müraciət etsin. Alınan xətanı qeyd edin, sonra CTE-lərin yerini dəyişib işlək variantı yazın və qaydanı 1 cümlə ilə formulə edin. (3 bal)
İpucu: Sonrakı CTE əvvəlkinə baxa bilər — əksi mümkün deyil (rekursiya istisna olmaqla).

## C. Rekursiv CTE

*Slayd 6 · 1.3 — anchor + recursive · UNION ALL · ierarxiya, ağac, seriya · sonsuz dövrə təhlükəsi*

**10.** isci cədvəli üzrə rekursiv CTE yazın: anchor kök işçidir (`rehber_id IS NULL`), hər addımda səviyyə 1 artır. Nəticədə isci_id, ad, vəzifə və səviyyə göstərilsin; səviyyə, sonra ad üzrə sıralansın. (2 bal)
İpucu: PostgreSQL-də RECURSIVE açar sözü məcburidir, SQL Server-də yoxdur. İki hissə UNION ALL ilə birləşir.

**11.** Hər işçi üçün ierarxiya yolunu mətn kimi qurun — məsələn `Aygün Məmmədova > Rauf Əliyev > Elvin Qasımov > Kamran Vəliyev`. Nəticədə ad, səviyyə və yol sütunları olsun, yol üzrə sıralansın. (3 bal)
İpucu: Anchor-da yol = ad; rekursiv hissədə `h.yol || ' > ' || i.ad`.

**12.** isci_id = 2 (Satış direktoru) rəhbərinin bütün tabeliyində olanları istənilən dərinlikdə çıxarın. Əlavə olaraq həmin alt-ağacdakı ümumi işçi sayını və ümumi maaş fondunu da göstərin. (3 bal)
İpucu: Anchor-u kökdə deyil, konkret isci_id-də başladın; sayı ikinci CTE-də aqreqasiya ilə alın.

**13.** 2024-01-01 – 2024-08-31 aralığındakı bütün ayların boşluqsuz siyahısını rekursiv CTE ilə generasiya edin (`generate_series` istifadə etmək qadağandır), sonra satışlarla birləşdirib satış olmayan ayı 0 dövriyyə ilə göstərin. (3 bal)
İpucu: Anchor: `DATE '2024-01-01'`; rekursiv addım: `ay + INTERVAL '1 month'`, dayanma şərti `ay < DATE '2024-08-01'`. Yoxlama: 2024-05 sıfır olmalıdır.

**14.** Dövrəli data. qraf cədvəlində 2 → 3 → 4 → 2 dövrəsi var. (a) Qoruyucusuz rekursiv sorğu yazın, icranı dayandırın və müşahidəni şərhdə qeyd edin. (b) Ziyarət olunmuş düyünlərin massivini saxlayaraq dövrəni kəsən düzgün variantı yazın; həm də səviyyə limiti qoyun. (4 bal)
İpucu: UNION ALL dublikatı silmir. Yol massivi (`yol || q.alt`) və `NOT (q.alt = ANY(yol))` şərti — və əlavə qoruyucu kimi `seviyye < 10`.

## D. CTE performansı və materializasiya

*Slayd 7 · 1.4 — təkrar istinad = təkrar icra · MATERIALIZED / NOT MATERIALIZED · indeks və statistikanın olmaması*

**15.** Eyni ağır aqreqasiyanı iki variantda yazın: `WITH t AS MATERIALIZED (...)` və `WITH t AS NOT MATERIALIZED (...)`. Hər ikisini `EXPLAIN (ANALYZE, BUFFERS)` ilə icra edin; plan fərqini (CTE Scan düyünü var / yoxdur) və icra vaxtlarını şərhdə müqayisə edin. (3 bal)
İpucu: PostgreSQL 12-yə qədər CTE həmişə materializasiya olunurdu (optimallaşdırma bariyeri); 12+ artıq inline edir.

**16.** Bir CTE-yə əsas sorğuda üç dəfə müraciət edən sorğu yazın. Plandan alt-sorğunun neçə dəfə icra olunduğunu göstərin (`NOT MATERIALIZED`), sonra `MATERIALIZED` ilə bunun bir dəfəyə düşdüyünü sübut edin. (3 bal)
İpucu: "Təkrar istinad = təkrar icra" — SQL Server-də bu davranış default-dur və nəticə heç yerdə saxlanmır.

**17.** CTE nəticəsi üzərində `CREATE INDEX` qurmağa cəhd edin və xətanı qeyd edin. Sonra eyni ara nəticəni TEMP cədvələ yazıb indeks qurun; hər iki variantın planını və vaxtını müqayisə edin. (3 bal)
İpucu: Slayd 7-nin 3-cü və 4-cü açarı — indeks qurmaq mümkün deyil, statistika yoxdur, ona görə optimizator sətir sayını səhv qiymətləndirir.

## E. CTE · Subquery · VIEW · TEMP TABLE müqayisəsi

*Slayd 8 · 1.5 — fiziki obyekt · yaşam müddəti · təkrar istinad · indeks · statistika · rekursiya*

**18.** Eyni məntiqi dörd variantda yazın: şəhər üzrə dövriyyəni hesablayıb yalnız orta dövriyyədən yuxarı şəhərləri qaytarın — (a) subquery, (b) CTE, (c) VIEW, (d) TEMP TABLE. Dördü də eyni nəticə çoxluğunu verməlidir. (2 bal)
İpucu: VIEW üçün `CREATE VIEW v_seher_dovriyye AS ...`, sonra ondan SELECT.

**19.** Slayd 8-dəki müqayisə cədvəlini SQL şərh bloku şəklində doldurun (6 meyar × 4 variant). Ən azı iki sətri real sorğu ilə sübut edin — məsələn VIEW-in tərifinin pg_views-də qalması, CTE-nin isə heç bir kataloqda görünməməsi. (2 bal)
İpucu: `SELECT * FROM pg_views WHERE viewname = ...`, `SELECT * FROM pg_tables WHERE tablename = ...`.

**20.** Aşağıdakı üç ssenariyə CTE / VIEW / MATERIALIZED VIEW / TEMP TABLE / RECURSIVE CTE-dən birini seçin və hər birini 1 cümlə ilə əsaslandırın; seçdiyiniz variantlardan birini işlək sorğu kimi yazın. (2 bal)
① 800 min sətirlik ara nəticə dörd dəfə JOIN olunur · ② Eyni filtr məntiqi altı fərqli hesabatda təkrarlanır · ③ Kateqoriya ağacının bütün səviyyələri lazımdır
İpucu: Seçim qaydası — kiçik data + bir dəfə oxunur → CTE; böyük data + indeks lazımdır → TEMP; məntiq komandaya lazımdır → VIEW.

## F. Müvəqqəti cədvəllər — yaradılması və yaşam müddəti

*Slayd 10 · 2.1 — CREATE TEMP TABLE · sessiya səviyyəsi · ON COMMIT DROP / DELETE ROWS · ad toqquşması*

**21.** `CREATE TEMP TABLE t_ayliq AS SELECT ...` ilə aylıq hesabatı (ay, satış sayı, dövriyyə) temp cədvələ yazın. Sətir sayını yoxlayın və cədvəlin hansı sxemdə yarandığını sorğu ilə göstərin. (2 bal)
İpucu: `SELECT c.relname, n.nspname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE c.relname = 't_ayliq';` — nəticə pg_temp_N olmalıdır.

**22.** `ON COMMIT DROP` ilə tranzaksiya daxilində temp cədvəl yaradın, içinə sətir yazın, `COMMIT` edin, sonra SELECT cəhdi edin. Xəta mətnini qeyd edin. (2 bal)
İpucu: ON COMMIT DROP yalnız açıq tranzaksiya daxilində mənalıdır — tranzaksiya bitən kimi cədvəl tamamilə yox olur.

**23.** `ON COMMIT DELETE ROWS` variantını yazın: iki ardıcıl tranzaksiyada cədvələ sətir əlavə edin və hər `COMMIT`-dən sonra cədvəlin qaldığını, sətirlərin isə silindiyini `COUNT(*)` ilə sübut edin. `ON COMMIT DROP` ilə fərqi 1 cümlə ilə yazın. (3 bal)
İpucu: Hər tranzaksiyadan sonra `COUNT(*) = 0`, amma SELECT xəta vermir — fərq budur.

**24.** İki ayrı bağlantı açın. Hər ikisində eyni adlı t_test temp cədvəli yaradıb fərqli sətirlər yazın. Hər sessiyadan `COUNT(*)` götürüb bir-birini görmədiklərini sübut edin; pg_class-da iki fərqli pg_temp_N sxemini göstərin. Şərhdə SQL Server-dəki #lokal və ##qlobal qarşılığını yazın. (3 bal)
İpucu: Slayd 10-un 4-cü açarı — ad toqquşması yoxdur, hər sessiya öz temp sxeminə sahibdir.

## G. Müvəqqəti cədvəllər — indeks, statistika, resurs

*Slayd 11 · 2.2 — doldur → indeksləşdir → ANALYZE → istifadə et · autovacuum temp cədvələ toxunmur · tempdb yükü*

**25.** Slayddakı düzgün ardıcıllığı tətbiq edin: doldur → indeks → ANALYZE → istifadə et. Hər addımdan sonra eyni JOIN sorğusunun `EXPLAIN ANALYZE` nəticəsini götürün və üç göstəricini (plan düyünü, gözlənilən/faktiki sətir, icra vaxtı) cədvəl şəklində şərhdə müqayisə edin. (3 bal)
İpucu: Seq Scan → Index Scan keçidi və `rows=` gözləntisinin dəqiqləşməsi aydın görünməlidir.

**26.** PostgreSQL temp cədvəl üçün statistikanı avtomatik yığmır — autovacuum ona toxunmur. Bunu sübut edin: `pg_class.reltuples` dəyərini və plandakı `rows=` gözləntisini `ANALYZE`-dən əvvəl və sonra müqayisə edin. (3 bal)
İpucu: Doldurduqdan dərhal sonra reltuples adətən -1 və ya 0 olur; `ANALYZE t_...;` yazandan sonra real dəyər görünür.

**27.** Qızıl qaydanı ölçün. 200 000 sətirlik temp cədvəli iki üsulla doldurun: (a) əvvəl indeks, sonra `INSERT`; (b) əvvəl `INSERT`, sonra indeks. `\timing` ilə hər iki vaxtı ölçüb fərqi faizlə şərhdə yazın. (3 bal)
İpucu: Süni data üçün `generate_series(1, 200000)`. Hər INSERT mövcud indeksi yeniləyir — yavaşlamanın səbəbi budur.

## H. Seçim məntiqi — CTE, TEMP və ya VIEW

*Slayd 12 · 2.3 — dörd sual, dörd cavab · MATERIALIZED VIEW · qərar ağacı*

**28.** Eyni hesabatı (kateqoriya ağacı + aylıq dövriyyə + şəhər üzrə pay) iki variantda yazın: yalnız CTE ilə, və TEMP cədvəl + indeks + ANALYZE ilə. İcra vaxtlarını ölçüb hansının niyə üstün olduğunu ölçü nəticələrinə istinadən əsaslandırın. (3 bal)
İpucu: "Hər variantı EXPLAIN ANALYZE ilə ölçüb qərar vermək" — slayd 21-dəki düzgün vərdiş.

**29.** Ağır aqreqasiya üçün MATERIALIZED VIEW yaradın, yeni satış əlavə edib `REFRESH MATERIALIZED VIEW` edin və adi VIEW ilə fərqini həm nəticə, həm icra vaxtı üzərində göstərin. Şərhdə slayd 12-dəki dörd sualdan hansına cavab verdiyinizi yazın. (3 bal)
İpucu: Adi VIEW hər dəfə yenidən hesablayır; MATERIALIZED VIEW nəticəni saxlayır — köhnəlmə riski buradan doğur.

## I. Tranzaksiyalar — BEGIN · COMMIT · ROLLBACK

*Slayd 14 · 3.1 — məntiqi iş vahidi · bölünməzlik · ara vəziyyət görünmür · xəta = tam geri dönüş*

**30.** Bank köçürməsi: 1 nömrəli hesabdan 2 nömrəli hesaba 500 AZN köçürün. Tək tranzaksiya, iki `UPDATE`, sonda `COMMIT`. Köçürmədən əvvəl və sonra hər iki balansı göstərin və cəmin dəyişmədiyini sübut edin. (2 bal)
İpucu: Slayd 14-ün klassik nümunəsi. İki UPDATE bir bütövdür.

**31.** Eyni köçürməni 3 nömrəli hesabdan (balans 300) 500 AZN üçün yazın — CHECK pozulacaq. Xəta mətnini qeyd edin, `ROLLBACK` edin və heç bir balansın dəyişmədiyini sorğu ilə sübut edin. (3 bal)
İpucu: "Bir UPDATE icra olunub, digəri olmayıbsa — data korlanmış sayılır." Burada yarım köçürmə qalmamalıdır.

**32.** İki sessiya. A-da `BEGIN` + `UPDATE` edin (COMMIT etmədən), B-də həmin sətri SELECT edin — hansı dəyər görünür? Sonra A-da `COMMIT` edib B-də yenidən oxuyun. Hər iki nəticəni qeyd edib "ara vəziyyət görünmür" prinsipini öz sözlərinizlə izah edin. (3 bal)
İpucu: Slayd 14-ün canlı demosu. B-nin gördüyü köhnə dəyər MVCC-nin nəticəsidir.

## J. SAVEPOINT — qismən geri qayıtma

*Slayd 15 · 3.2 — işarə nöqtəsi · ROLLBACK TO · aborted transaction · SAVEPOINT COMMIT etmir*

**33.** kocurme_log üzərində slayddakı ssenarini təkrarlayın: sətir yazın → `SAVEPOINT sp1` → səhv sətir yazın → `ROLLBACK TO SAVEPOINT sp1` → düzgün sətir yazın → `COMMIT`. Sonda cədvəldə hansı sətirlərin qaldığını göstərin. (2 bal)
İpucu: SAVEPOINT heç nəyi COMMIT etmir — tranzaksiya hələ də açıqdır.

**34.** Aborted vəziyyəti. Tranzaksiya daxilində qəsdən xəta yaradın (məsələn `SELECT 1/0;`), sonra tamam normal bir əmr icra etməyə cəhd edin. `current transaction is aborted` xətasını qeyd edin. Sonra eyni ssenarini SAVEPOINT ilə yazıb tranzaksiyanı xilas edin. (3 bal)
İpucu: PostgreSQL nüansı — MySQL və Oracle bu davranışı göstərmir. SAVEPOINT bu vəziyyətdən çıxmağın yeganə yoludur.

**35.** ETL ssenarisi. Beş sətirlik köçürmə siyahısını hesab cədvəlinə tətbiq edin; onlardan ikisi `CHECK`-i pozur. Hər sətirdən əvvəl SAVEPOINT qoyub yalnız səhv sətirləri atlayın, qalanları isə bir `COMMIT` ilə yazın. Eyni məntiqi PL/pgSQL EXCEPTION bloku ilə də yazın və hansının daxilən savepoint işlətdiyini şərhdə qeyd edin. (3 bal)
İpucu: PL/pgSQL-də hər `BEGIN ... EXCEPTION ... END` bloku gizli savepoint yaradır — bu, ucuz əməliyyat deyil.

## K. Autocommit, DDL və uzun tranzaksiyalar

*Slayd 16 · 3.3 — autocommit default açıqdır · MySQL-də DDL implicit COMMIT · uzun tranzaksiya = uzun lock · bloat*

**36.** Autocommit tələsi. Açıq tranzaksiya olmadan `UPDATE anbar SET qaliq = qaliq + 10;` icra edin (WHERE yazmadan). Neçə sətrin dəyişdiyini göstərin, `ROLLBACK` yazıb kömək etmədiyini sübut edin, sonra datanı bərpa edin. Ardınca eyni əməliyyatı təhlükəsiz vərdişlə təkrarlayın: `BEGIN` → yoxlayıcı `SELECT count(*)` → `UPDATE` → nəticəni yoxla → `COMMIT` və ya `ROLLBACK`. (2 bal)
İpucu: "UPDATE etdim, WHERE yazmadım" faciəsi məhz buradan doğur.

**37.** PostgreSQL-də DDL tranzaksiyaya daxildir. `BEGIN` → `CREATE TABLE` → `ALTER TABLE` → `ROLLBACK` ardıcıllığını icra edin və cədvəlin yox olduğunu sübut edin. Şərhdə MySQL-in implicit COMMIT davranışını və bunun praktiki nəticəsini yazın. (3 bal)
İpucu: MySQL-də CREATE / ALTER / DROP açıq tranzaksiyanı avtomatik commit edir — ROLLBACK artıq kömək etmir.

**38.** Uzun tranzaksiya = uzun lock. A-da `BEGIN` + `UPDATE hesab ... WHERE hesab_id = 1` edib tranzaksiyanı açıq saxlayın; B-də eyni sətrə `UPDATE` yazın — gözləmə başlayacaq. `pg_stat_activity`, `pg_locks` və `pg_blocking_pids()` ilə bloklayan sessiyanı tapın və nəticəni qeyd edin. Sonra A-da `COMMIT` edib B-nin açıldığını göstərin. (3 bal)
İpucu: `SELECT pid, state, wait_event_type, query, pg_blocking_pids(pid) FROM pg_stat_activity WHERE state <> 'idle';` Şərhdə VACUUM-un köhnə sətirləri təmizləyə bilməməsini (table bloat) də izah edin.

## L. ACID — dörd zəmanət

*Slayd 18 · 4.1 — Atomicity (undo log) · Consistency (PK/FK/CHECK) · Isolation (lock/MVCC) · Durability (WAL, fsync)*

**39.** Atomicity. Elə bir tranzaksiya yazın ki, birinci `UPDATE` uğurlu olsun, ikincisi qəsdən xəta versin. `ROLLBACK`-dən sonra birincinin də geri qayıtdığını sübut edin. Mexanizmi (undo log / MVCC köhnə versiya) 1 cümlə ilə adlandırın. (2 bal)
İpucu: "Tranzaksiya ya tam icra olunur, ya da heç olmur."

**40.** Consistency. `kocurme_log.hesab_id` üzərinə `hesab(hesab_id)`-ə FOREIGN KEY əlavə edin, sonra mövcud olmayan hesab id-si ilə log yazmağa cəhd edin. Həm FK, həm də CHECK pozuntusunu ayrıca göstərin və hər birinin hansı mexanizmlə işlədiyini adlandırın. (3 bal)
İpucu: Mexanizm: PK, FK, CHECK, trigger. Tranzaksiya bazanı bir doğru vəziyyətdən digər doğru vəziyyətə keçirir.

**41.** Durability. `SHOW fsync;`, `SHOW synchronous_commit;`, `SHOW wal_level;` nəticələrini qeyd edin. `synchronous_commit = off` edildikdə hansı zəmanətin itdiyini, hansı qazancın əldə edildiyini və hansı sistemlərdə bunun məqbul olduğunu izah edin. (3 bal)
İpucu: Mexanizm: WAL / redo log + fsync. "COMMIT dedikdən sonra serverin fişini çəksək nə olar?"

## M. İzolyasiya səviyyələri və anomaliyalar

*Slayd 19 · 4.2 — READ UNCOMMITTED · READ COMMITTED · REPEATABLE READ · SERIALIZABLE · dirty / non-repeatable / phantom*

**42.** Cari izolyasiya səviyyəsini göstərin (`SHOW transaction_isolation;`), sonra tranzaksiyanı `BEGIN ISOLATION LEVEL REPEATABLE READ;` ilə açıb yenidən yoxlayın. Şərhdə PostgreSQL, MySQL InnoDB və SQL Server-in default səviyyələrini yazın. (2 bal)
İpucu: PostgreSQL `READ UNCOMMITTED`-i qəbul edir, amma əslində `READ COMMITTED` kimi işlədir — bunu da yoxlayıb qeyd edin.

**43.** Non-repeatable read. A-da `READ COMMITTED` tranzaksiyası eyni sətri iki dəfə oxusun; aralarında B həmin sətri dəyişib `COMMIT` etsin. İki fərqli dəyəri qeyd edin. Sonra eyni ssenarini `REPEATABLE READ` ilə təkrarlayıb dəyərin sabit qaldığını göstərin. (3 bal)
İpucu: Non-repeatable — eyni sətri iki dəfə oxuyub fərqli DƏYƏR almaq.

**44.** Phantom read. Eyni WHERE şərti ilə iki dəfə `COUNT(*)` alın; aralarında B yeni sətir `INSERT` edib `COMMIT` etsin. Nəticəni həm `READ COMMITTED`, həm `REPEATABLE READ`-də müqayisə edin. Sonra `SERIALIZABLE` səviyyəsində paralel yazma cəhdi edib `could not serialize access` xətasını alın. (3 bal)
İpucu: Phantom — eyni şərtlə iki dəfə oxuyub fərqli SAY almaq. PostgreSQL-də REPEATABLE READ phantom-a da icazə vermir — standartdan güclüdür; bunu şərhdə qeyd edin.

## N. Lock və deadlock

*Slayd 20 · 4.3 — deadlock detector · qurban tranzaksiya · eyni ardıcıllıq · qısa tranzaksiya · retry · FOR UPDATE*

**45.** `SELECT ... FOR UPDATE` ilə anbar cədvəlinin bir sətrini kilidləyin. İkinci sessiyada eyni sətri üç variantda oxuyun: adi `FOR UPDATE`, `FOR UPDATE NOWAIT` və `FOR UPDATE SKIP LOCKED`. Üç fərqli davranışı (gözləmə / dərhal xəta / sətrin atlanması) qeyd edin. (2 bal)
İpucu: Bu, pessimistic locking-dir — sətri əvvəlcədən kilidləmək.

**46.** Deadlock yaradın. Slayd 20-dəki ssenarini təkrarlayın: A `hesab_id=1` → `hesab_id=2`, B isə `hesab_id=2` → `hesab_id=1` ardıcıllığı ilə `UPDATE` etsin. Deadlock xətasının tam mətnini, qurban seçilən sessiyanı və `SHOW deadlock_timeout;` dəyərini qeyd edin. (3 bal)
İpucu: Deadlock detector dövrəni aşkarlayıb tranzaksiyalardan birini "qurban" seçir. Deadlock səhv deyil — normal hal kimi idarə olunmalıdır.

**47.** Deadlock-u aradan qaldırın — üç addım. (a) Hər iki sessiyanı resurslara hesab_id üzrə artan sırada müraciət edəcək şəkildə yenidən yazın və toqquşmanın baş vermədiyini göstərin. (b) anbar.versiya sütunu ilə optimistic locking yazın (`UPDATE ... WHERE mehsul_id = ? AND versiya = ?`) və 0 sətir dəyişdikdə tətbiqin nə etməli olduğunu izah edin. (c) PL/pgSQL blokunda deadlock xətasını tutub əməliyyatı 3 dəfə təkrar cəhd edən retry məntiqi qurun. (4 bal)
İpucu: Retry üçün `EXCEPTION WHEN deadlock_detected THEN ...` və sayğac; hər cəhd arasında qısa gözləmə.

## O. Yekun — kompleks tapşırıqlar

*Slayd 21–22 · Modulun bütün bölmələrini bir skriptdə birləşdirir · tipik səhvlərin auditi*

**48.** Tam pipeline. (1) Rekursiv CTE ilə hər yarpaq kateqoriya üçün kök kateqoriyanı tapın → (2) nəticəni TEMP cədvələ yazın → (3) indeks qurub ANALYZE edin → (4) satışlarla birləşdirib kök kateqoriya × ay kəsiyində dövriyyəni və ümumi dövriyyədəki faiz payını çıxarın. Eyni hesabatı yalnız CTE ilə də yazıb icra vaxtlarını müqayisə edin və seçiminizi əsaslandırın. (4 bal)
İpucu: Rekursiya + zəncirvari CTE + temp cədvəl + indeks + statistika — modulun 1-ci və 2-ci bölməsi tam şəkildə.

**49.** İdempotent ETL. Aylıq yekunları ayliq_yekun daimi cədvəlinə köçürən skript yazın. Şərtlər: tək tranzaksiya · ara nəticə üçün `ON COMMIT DROP` temp cədvəli · mövcud ayların yenilənməsi (`INSERT ... ON CONFLICT DO UPDATE`) · istənilən xətada tam `ROLLBACK`. Skripti iki dəfə icra edib nəticənin dəyişmədiyini sübut edin. (4 bal)
İpucu: İdempotentlik = ikinci icra nə yeni sətir yaradır, nə də dublikat. Tranzaksiya sərhədlərini düzgün seçin.

**50.** Köçürmə funksiyası. `kocurme(gonderen INT, alan INT, mebleg NUMERIC)` adlı PL/pgSQL funksiyası yazın. Tələblər: balans yoxlaması · iki `UPDATE` · kocurme_log-a qeyd · vəsait çatmadıqda mənalı xəta (`RAISE EXCEPTION`) və tam geri qayıtma · deadlock xətasında 3 dəfə retry · hesablara həmişə hesab_id üzrə artan sırada müraciət. Bir uğurlu və bir uğursuz çağırış üçün nəticəni göstərin. (4 bal)
İpucu: Sıralamanı LEAST/GREATEST ilə təmin edin — deadlock-un qarşısını alan əsas priyomdur.

**51.** Audit. Aşağıdakı skriptdə slayd 21-dəki "belə etməyin" siyahısından altı səhv var. Hamısını tapın, hər birini bir sətirlə adlandırın və skripti düzgün variantda yenidən yazın. (4 bal)

```sql
CREATE TEMP TABLE t_boyuk (musteri_id INT, cem NUMERIC);
CREATE INDEX ix_t ON t_boyuk (musteri_id);

INSERT INTO t_boyuk
SELECT musteri_id, SUM(mebleg) FROM satis GROUP BY musteri_id;

WITH agir AS (
    SELECT s.satis_id, s.mebleg, t.cem
    FROM satis s JOIN t_boyuk t USING (musteri_id)
)
SELECT (SELECT COUNT(*)    FROM agir) AS say,
       (SELECT SUM(mebleg) FROM agir) AS cem,
       (SELECT MAX(cem)    FROM agir) AS maks;

WITH RECURSIVE ag AS (
    SELECT ust, alt FROM qraf WHERE ust = 1
    UNION ALL
    SELECT q.ust, q.alt FROM qraf q JOIN ag ON q.ust = ag.alt
)
SELECT * FROM ag;

UPDATE hesab SET balans = balans * 1.05;

BEGIN;
  UPDATE anbar SET qaliq = qaliq - 1 WHERE mehsul_id = 1;
  -- burada tətbiq xarici API-nin cavabını gözləyir (~30 saniyə)
  UPDATE anbar SET qaliq = qaliq - 1 WHERE mehsul_id = 2;
COMMIT;
```

İpucu: Səhvləri bu dörd yerdə axtarın — temp cədvəlin hazırlanma ardıcıllığı, CTE-yə istinad sayı, rekursiyanın qoruyucusu, tranzaksiyanın sərhədləri və uzunluğu.

## Əhatə xəritəsi — slayd → tapşırıq

*Prezentasiyanın hər başlıq və alt başlığının hansı tapşırıqlarla yoxlandığı*

| Slayd | Başlıq / alt başlıq | Tapşırıqlar | Yoxlanılan bacarıq |
|---|---|---|---|
| 1–2 | Giriş və məzmun | — | Modulun çərçivəsi |
| 3 | Bölmə 01 — WITH (CTE) | 1–20 | Bölmə bütövlükdə |
| 4 | 1.1 CTE nədir və necə işləyir | 1–5 | Sintaksis, əhatə dairəsi, obyekt yaratmaması |
| 5 | 1.2 Çoxlu və zəncirvari CTE | 6–9 | Zəncir qurma, istiqamət qaydası, tipik səhvlər |
| 6 | 1.3 Rekursiv CTE — ierarxiyalar | 10–14 | Anchor/recursive, yol, seriya, dövrə qoruyucusu |
| 7 | 1.4 CTE nə vaxt performansı öldürür | 15–17 | Materializasiya, təkrar icra, indeks/statistika |
| 8 | 1.5 CTE · Subquery · VIEW · TEMP müqayisəsi | 18–20 | Seçim qaydası, 6 meyar üzrə fərqlər |
| 9 | Bölmə 02 — Müvəqqəti cədvəllər | 21–29 | Bölmə bütövlükdə |
| 10 | 2.1 Yaradılması və yaşam müddəti | 21–24 | TEMP növləri, ON COMMIT, sessiya təcridi |
| 11 | 2.2 Temp cədvəlin gücü və qiyməti | 25–27 | İndeks, ANALYZE, doldurma ardıcıllığı |
| 12 | 2.3 CTE, TEMP və ya VIEW — hansını seçim | 28–29 | Qərar ağacı, MATERIALIZED VIEW |
| 13 | Bölmə 03 — Tranzaksiyalar | 30–38 | Bölmə bütövlükdə |
| 14 | 3.1 Məntiqi iş vahidi: ya hamısı, ya heç biri | 30–32 | BEGIN/COMMIT/ROLLBACK, ara vəziyyət |
| 15 | 3.2 SAVEPOINT — qismən geri qayıtma | 33–35 | ROLLBACK TO, aborted state, ETL |
| 16 | 3.3 Autocommit, DDL və uzun tranzaksiyalar | 36–38 | Gizli commit, DDL fərqi, lock və bloat |
| 17 | Bölmə 04 — ACID prinsipləri | 39–47 | Bölmə bütövlükdə |
| 18 | 4.1 Dörd zəmanət və praktiki qarşılığı | 39–41 | Atomicity, Consistency, Durability + mexanizmlər |
| 19 | 4.2 İzolyasiya səviyyələri və anomaliyalar | 42–44 | Dirty / non-repeatable / phantom, SERIALIZABLE |
| 20 | 4.3 Lock və deadlock | 45–47 | FOR UPDATE, deadlock, ardıcıllıq, retry |
| 21 | Yekun · tipik səhvlər və düzgün vərdişlər | 51 | Skript auditi — 6 səhvin aşkarlanması |
| 22 | Yekun mənzərə · praktiki tapşırıq | 48–50 | Bütün bölmələrin bir pipeline-da birləşməsi |

## Qiymətləndirmə

| Bölmə | Slayd | Tapşırıqlar | Bal |
|---|---|---|---|
| A. CTE — sintaksis və işləmə məntiqi | 4 | 1–5 | 10 |
| B. Çoxlu və zəncirvari CTE | 5 | 6–9 | 10 |
| C. Rekursiv CTE | 6 | 10–14 | 15 |
| D. CTE performansı və materializasiya | 7 | 15–17 | 9 |
| E. CTE · Subquery · VIEW · TEMP müqayisəsi | 8 | 18–20 | 6 |
| F. Müvəqqəti cədvəllər — yaradılması və yaşam müddəti | 10 | 21–24 | 10 |
| G. Müvəqqəti cədvəllər — indeks, statistika, resurs | 11 | 25–27 | 9 |
| H. Seçim məntiqi — CTE, TEMP və ya VIEW | 12 | 28–29 | 6 |
| I. Tranzaksiyalar — BEGIN · COMMIT · ROLLBACK | 14 | 30–32 | 8 |
| J. SAVEPOINT — qismən geri qayıtma | 15 | 33–35 | 8 |
| K. Autocommit, DDL və uzun tranzaksiyalar | 16 | 36–38 | 8 |
| L. ACID — dörd zəmanət | 18 | 39–41 | 8 |
| M. İzolyasiya səviyyələri və anomaliyalar | 19 | 42–44 | 8 |
| N. Lock və deadlock | 20 | 45–47 | 9 |
| O. Yekun — kompleks tapşırıqlar | 21–22 | 48–51 | 16 |
| **Cəmi** | **1–22** | **1–51** | **140** |

| Bal | Qiymət | Nə deməkdir |
|---|---|---|
| 126–140 | Əla | Bütün dörd mövzu, o cümlədən nüanslar mənimsənilib |
| 105–125 | Yaxşı | Əsas konstruksiyalar işlək, nüanslarda boşluq var |
| 84–104 | Kafi | Sintaksis var, davranış və performans izahı zəifdir |
| 84-dən aşağı | Təkrar | Modul yenidən keçilməlidir |

**Qiymətləndirmə meyarları.**
- Tam bal: sorğu işləkdir, tələb olunan konstruksiya istifadə olunub, nəticə düzgündür və tələb olunan müşahidə (xəta mətni, plan, ölçmə) şərhdə var.
- Yarım bal: nəticə düzgündür, lakin tələb olunan müşahidə/izah yoxdur və ya lazımsız alt-sorğu ilə şişirdilib.
- Sıfır bal: sintaktik xəta · tələb olunan konstruksiyanın əvəzlənməsi · iki sessiya tələb olunan yerdə tək sessiya · data bərpa edilməyib.
