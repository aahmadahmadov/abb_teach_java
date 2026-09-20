-- SQL Dərinlik Modulu, praktik tapşırıq
-- CTE (WITH), müvəqqəti cədvəllər, tranzaksiyalar, ACID
-- Mühit: PostgreSQL 16 (docker-compose, lesson1 bazası)
-- Qeyd: bəzi tapşırıqlar qəsdən xəta verir, gözlənilən xəta mətni şərhdə yazılıb.


-- ============================================================
-- Hazırlıq, sxemi yaradın
-- ============================================================

-- Skript təkrar icra oluna bilsin deyə asılı obyektlər əvvəlcə silinir
DROP MATERIALIZED VIEW IF EXISTS mv_seher_dovriyye;
DROP VIEW IF EXISTS v_seher_dovriyye2;
DROP VIEW IF EXISTS v_seher_dovriyye;
DROP FUNCTION IF EXISTS kocurme(INT, INT, NUMERIC);
DROP TABLE IF EXISTS ayliq_yekun;

DROP TABLE IF EXISTS kocurme_log;
DROP TABLE IF EXISTS satis;
DROP TABLE IF EXISTS hesab;
DROP TABLE IF EXISTS anbar;
DROP TABLE IF EXISTS isci;
DROP TABLE IF EXISTS kateqoriya;
DROP TABLE IF EXISTS qraf;

-- 1) Satış faktları
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

-- 2) İşçi ierarxiyası
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

-- 3) Kateqoriya ağacı
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

-- 4) Dövrəli qraf
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

-- 5) Bank hesabları
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

-- 6) Anbar
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


-- ============================================================
-- A. CTE, sintaksis və işləmə məntiqi
-- ============================================================

-- 1-ci tapşırıq
-- II rüb (aprel, may, iyun) satışları CTE-yə yığılır, sonra şəhər kəsiyində hesablanır.
WITH aktiv_satis AS (SELECT satis_id,
                            tarix,
                            seher,
                            mebleg
                     FROM satis
                     WHERE tarix >= DATE '2024-04-01'
                       AND tarix < DATE '2024-07-01')
SELECT COALESCE(seher, 'Namelum') AS seher,
       COUNT(*)                   AS satis_sayi,
       SUM(mebleg)                AS umumi_mebleg
FROM aktiv_satis
GROUP BY COALESCE(seher, 'Namelum')
ORDER BY umumi_mebleg DESC;


-- 2-ci tapşırıq
-- Eyni CTE iki dəfə istifadə olunur, LAG əvəzinə CTE öz-özü ilə JOIN edilir.
WITH ayliq AS (SELECT DATE_TRUNC('month', tarix) AS ay,
                      SUM(mebleg)                AS cem
               FROM satis
               GROUP BY DATE_TRUNC('month', tarix))
SELECT a.ay::date AS ay,
       a.cem      AS bu_ayin_cemi,
       b.cem      AS evvelki_ayin_cemi
FROM ayliq a
         LEFT JOIN ayliq b ON b.ay = a.ay - INTERVAL '1 month'
ORDER BY a.ay;


-- 3-cü tapşırıq
-- CTE disk obyekti deyil, ona görə sorğu bitəndən sonra adı qalmır.
SELECT *
FROM aktiv_satis;
/* Alınan xəta:
   ERROR:  relation "aktiv_satis" does not exist
   LINE 1: SELECT * FROM aktiv_satis;
                         ^
   İzah: CTE yalnız onu yazdığımız sorğunun daxilində mövcuddur, cədvəl və ya view kimi
   bazada saxlanılmır, buna görə ayrıca əmrdə həmin ad tapılmır. */


-- 4-cü tapşırıq
-- İç-içə subquery iki addıma bölünür, təkrarlanan aqreqasiya bir dəfə yazılır.
WITH seher_cem AS (SELECT seher,
                          SUM(mebleg) AS cem
                   FROM satis
                   GROUP BY seher),
     orta AS (SELECT AVG(cem) AS orta_cem
              FROM seher_cem)
SELECT COALESCE(s.seher, 'Namelum') AS seher,
       s.cem                        AS cem
FROM seher_cem s
         CROSS JOIN orta o
WHERE s.cem > o.orta_cem
ORDER BY s.cem DESC;


-- 5-ci tapşırıq
-- Sütun adları CTE-nin özünə verilir, daxildə heç bir AS yoxdur.
WITH ay_cem(ay, cem, say) AS (SELECT DATE_TRUNC('month', tarix),
                                     SUM(mebleg),
                                     COUNT(*)
                              FROM satis
                              GROUP BY DATE_TRUNC('month', tarix))
SELECT ay::date AS ay,
       cem      AS cem,
       say      AS say
FROM ay_cem
ORDER BY ay;


-- ============================================================
-- B. Çoxlu və zəncirvari CTE
-- ============================================================

-- 6-cı tapşırıq
-- Üç addım: aylıq dövriyyə, ayların ortası, ortadan yuxarı aylar.
WITH ayliq AS (SELECT DATE_TRUNC('month', tarix) AS ay,
                      SUM(mebleg)                AS dovriyye
               FROM satis
               GROUP BY DATE_TRUNC('month', tarix)),
     orta AS (SELECT AVG(dovriyye) AS orta_dovriyye
              FROM ayliq),
     yuksek AS (SELECT a.ay,
                       a.dovriyye,
                       o.orta_dovriyye
                FROM ayliq a
                         CROSS JOIN orta o
                WHERE a.dovriyye > o.orta_dovriyye)
SELECT ay::date                                                   AS ay,
       dovriyye                                                   AS dovriyye,
       ROUND((dovriyye - orta_dovriyye) / orta_dovriyye * 100, 1) AS ortadan_faiz
FROM yuksek
ORDER BY ay;


-- 7-ci tapşırıq
/* Skriptdəki iki səhv:
   1) İkinci dəfə WITH yazılıb. WITH bir dəfə yazılır, növbəti CTE-lər vergüllə ayrılır.
      Xəta: ERROR:  syntax error at or near "WITH"
   2) Sonuncu CTE-dən sonra vergül qoyulub. Sonuncu CTE ilə əsas SELECT arasında vergül olmur. */

-- Düzəldilmiş variant:
WITH ayliq AS (SELECT DATE_TRUNC('month', tarix) AS ay,
                      SUM(mebleg)                AS cem
               FROM satis
               GROUP BY DATE_TRUNC('month', tarix)),
     orta AS (SELECT AVG(cem) AS orta_cem
              FROM ayliq)
SELECT a.ay::date AS ay,
       a.cem      AS cem
FROM ayliq a
         CROSS JOIN orta o
WHERE a.cem > o.orta_cem
ORDER BY a.ay;


-- 8-ci tapşırıq
-- Dörd addım ayrı-ayrı CTE-lərdədir: aylıq, yığılan cəm, faiz payları, Pareto işarəsi.
WITH ayliq AS (SELECT DATE_TRUNC('month', tarix) AS ay,
                      SUM(mebleg)                AS dovriyye
               FROM satis
               GROUP BY DATE_TRUNC('month', tarix)),
     umumi AS (SELECT SUM(dovriyye) AS umumi_dovriyye
               FROM ayliq),
     yigilan AS (SELECT ay,
                        dovriyye,
                        SUM(dovriyye) OVER (ORDER BY ay) AS yigilan_cem
                 FROM ayliq),
     faiz AS (SELECT y.ay,
                     y.dovriyye,
                     y.yigilan_cem,
                     ROUND(y.dovriyye * 100 / u.umumi_dovriyye, 1)    AS faiz_payi,
                     ROUND(y.yigilan_cem * 100 / u.umumi_dovriyye, 1) AS yigilan_faiz
              FROM yigilan y
                       CROSS JOIN umumi u)
SELECT ay::date     AS ay,
       dovriyye     AS dovriyye,
       yigilan_cem  AS yigilan_cem,
       faiz_payi    AS faiz_payi,
       yigilan_faiz AS yigilan_faiz,
       CASE
           WHEN yigilan_faiz >= 50 AND yigilan_faiz - faiz_payi < 50
               THEN 'Pareto serhedi'
           ELSE ''
           END      AS qeyd
FROM faiz
ORDER BY ay;


-- 9-cu tapşırıq
-- Səhv variant: birinci CTE özündən sonrakına baxmağa çalışır.
WITH birinci AS (SELECT n FROM ikinci),
     ikinci AS (SELECT 1 AS n)
SELECT n AS n
FROM birinci;
/* Alınan xəta:
   ERROR:  relation "ikinci" does not exist
   DETAIL:  There is a WITH item named "ikinci", but it cannot be referenced from this part of the query.
   HINT:  Use WITH RECURSIVE, or re-order the WITH items to remove forward references. */

-- Düzgün variant, CTE-lərin yeri dəyişdirilib:
WITH ikinci AS (SELECT 1 AS n),
     birinci AS (SELECT n FROM ikinci)
SELECT n AS n
FROM birinci;
-- Qayda: CTE yalnız özündən əvvəl elan olunmuş CTE-lərə müraciət edə bilər, rekursiya istisnadır.


-- ============================================================
-- C. Rekursiv CTE
-- ============================================================

-- 10-cu tapşırıq
WITH RECURSIVE ierarxiya AS (SELECT isci_id,
                                    ad,
                                    vezife,
                                    1 AS seviyye
                             FROM isci
                             WHERE rehber_id IS NULL
                             UNION ALL
                             SELECT i.isci_id,
                                    i.ad,
                                    i.vezife,
                                    h.seviyye + 1
                             FROM isci i
                                      JOIN ierarxiya h ON i.rehber_id = h.isci_id)
SELECT isci_id AS isci_id,
       ad      AS ad,
       vezife  AS vezife,
       seviyye AS seviyye
FROM ierarxiya
ORDER BY seviyye, ad;


-- 11-ci tapşırıq
-- Anchor-da yol işçinin adıdır, hər rekursiv addımda ad sona əlavə olunur.
WITH RECURSIVE yol_agaci AS (SELECT isci_id,
                                    ad,
                                    1        AS seviyye,
                                    ad::text AS yol
                             FROM isci
                             WHERE rehber_id IS NULL
                             UNION ALL
                             SELECT i.isci_id,
                                    i.ad,
                                    y.seviyye + 1,
                                    y.yol || ' > ' || i.ad
                             FROM isci i
                                      JOIN yol_agaci y ON i.rehber_id = y.isci_id)
SELECT ad      AS ad,
       seviyye AS seviyye,
       yol     AS yol
FROM yol_agaci
ORDER BY yol;


-- 12-ci tapşırıq
-- Anchor kökdə deyil, isci_id = 2-də başlayır. Səviyyə 0 rəhbərin özüdür, sayılmır.
WITH RECURSIVE
    tabeler AS (SELECT isci_id,
                       ad,
                       vezife,
                       maas,
                       0 AS seviyye
                FROM isci
                WHERE isci_id = 2
                UNION ALL
                SELECT i.isci_id,
                       i.ad,
                       i.vezife,
                       i.maas,
                       t.seviyye + 1
                FROM isci i
                         JOIN tabeler t ON i.rehber_id = t.isci_id),
    yekun AS (SELECT COUNT(*)  AS isci_sayi,
                     SUM(maas) AS maas_fondu
              FROM tabeler
              WHERE seviyye > 0)
SELECT t.isci_id    AS isci_id,
       t.ad         AS ad,
       t.vezife     AS vezife,
       t.seviyye    AS seviyye,
       y.isci_sayi  AS alt_agac_isci_sayi,
       y.maas_fondu AS alt_agac_maas_fondu
FROM tabeler t
         CROSS JOIN yekun y
WHERE t.seviyye > 0
ORDER BY t.seviyye, t.ad;


-- 13-cü tapşırıq
-- Aylar rekursiya ilə qurulur, generate_series işlədilmir.
WITH RECURSIVE aylar AS (SELECT DATE '2024-01-01' AS ay
                         UNION ALL
                         SELECT (ay + INTERVAL '1 month')::date
                         FROM aylar
                         WHERE ay < DATE '2024-08-01')
SELECT a.ay                       AS ay,
       COALESCE(SUM(s.mebleg), 0) AS dovriyye
FROM aylar a
         LEFT JOIN satis s ON DATE_TRUNC('month', s.tarix)::date = a.ay
GROUP BY a.ay
ORDER BY a.ay;
-- Yoxlama: 2024-05-01 sətri 0 dövriyyə ilə görünür, çünki may ayında satış yoxdur.


-- 14-cü tapşırıq
/* (a) Qoruyucusuz variant. Bu sorğu sonsuz işləyir, çünki qraf-da 2 > 3 > 4 > 2 dövrəsi var
   və UNION ALL dublikatları silmir. Sorğu ayrıca icra edilib Ctrl+C ilə dayandırılıb.
   Müşahidə: nəticə gəlmir, yaddaş artır, psql-də icra dayandırılana qədər davam edir.

   WITH RECURSIVE ag AS (SELECT ust, alt FROM qraf WHERE ust = 1
                         UNION ALL
                         SELECT q.ust, q.alt FROM qraf q JOIN ag ON q.ust = ag.alt)
   SELECT ust, alt FROM ag;
*/

-- (b) Dövrəni kəsən düzgün variant: ziyarət olunmuş düyünlər massivdə saxlanılır.
WITH RECURSIVE ag AS (SELECT q.ust,
                             q.alt,
                             1                    AS seviyye,
                             ARRAY [q.ust, q.alt] AS yol
                      FROM qraf q
                      WHERE q.ust = 1
                      UNION ALL
                      SELECT q.ust,
                             q.alt,
                             a.seviyye + 1,
                             a.yol || q.alt
                      FROM qraf q
                               JOIN ag a ON q.ust = a.alt
                      WHERE NOT (q.alt = ANY (a.yol))
                        AND a.seviyye < 10)
SELECT ust     AS ust,
       alt     AS alt,
       seviyye AS seviyye,
       yol     AS yol
FROM ag
ORDER BY seviyye, ust, alt;
-- Massiv dövrəni kəsir, seviyye < 10 isə əlavə qoruyucudur.


-- ============================================================
-- D. CTE performansı və materializasiya
-- ============================================================

-- Bu bölmə üçün ölçüləcək böyük temp cədvəl
DROP TABLE IF EXISTS t_agir;
CREATE TEMP TABLE t_agir
(
    id         INT,
    musteri_id INT,
    mebleg     NUMERIC(10, 2),
    tarix      DATE
);
INSERT INTO t_agir
SELECT i,
       (random() * 5000)::int + 1,
       (random() * 900 + 10)::numeric(10, 2),
       DATE '2024-01-01' + (i % 240)
FROM generate_series(1, 500000) AS i;
ANALYZE t_agir;


-- 15-ci tapşırıq
EXPLAIN (ANALYZE, BUFFERS)
WITH t AS MATERIALIZED (SELECT musteri_id,
                               SUM(mebleg) AS cem
                        FROM t_agir
                        GROUP BY musteri_id)
SELECT COUNT(*) AS say,
       AVG(cem) AS orta
FROM t;

EXPLAIN (ANALYZE, BUFFERS)
WITH t AS NOT MATERIALIZED (SELECT musteri_id,
                                   SUM(mebleg) AS cem
                            FROM t_agir
                            GROUP BY musteri_id)
SELECT COUNT(*) AS say,
       AVG(cem) AS orta
FROM t;
/* Plan fərqi və vaxtlar:
   MATERIALIZED:     Aggregate > CTE Scan on t > HashAggregate > Seq Scan, Execution Time 98.7 ms
   NOT MATERIALIZED: Aggregate > HashAggregate > Seq Scan, CTE Scan düyünü yoxdur, 102.5 ms
   Bir dəfə istinad olunanda fərq yoxdur, çünki plan onsuz da bir dəfə icra olunur.
   MATERIALIZED nəticəni ayrıca saxlayır, NOT MATERIALIZED isə sorğunu əsas plana yerləşdirir. */


-- 16-cı tapşırıq
EXPLAIN (ANALYZE)
WITH t AS NOT MATERIALIZED (SELECT musteri_id,
                                   SUM(mebleg) AS cem
                            FROM t_agir
                            GROUP BY musteri_id)
SELECT (SELECT COUNT(*) FROM t) AS say,
       (SELECT MAX(cem) FROM t) AS maks,
       (SELECT MIN(cem) FROM t) AS min;

EXPLAIN (ANALYZE)
WITH t AS MATERIALIZED (SELECT musteri_id,
                               SUM(mebleg) AS cem
                        FROM t_agir
                        GROUP BY musteri_id)
SELECT (SELECT COUNT(*) FROM t) AS say,
       (SELECT MAX(cem) FROM t) AS maks,
       (SELECT MIN(cem) FROM t) AS min;
/* NOT MATERIALIZED: planda üç ayrı HashAggregate və üç Seq Scan on t_agir var,
   yəni aqreqasiya üç dəfə icra olunub. Execution Time 261.4 ms.
   MATERIALIZED: bir CTE t düyünü, ondan sonra üç CTE Scan. Aqreqasiya bir dəfə icra olunub.
   Execution Time 97.1 ms, yəni təxminən 2.7 dəfə sürətli.
   Nəticə: təkrar istinad = təkrar icra. */


-- 17-ci tapşırıq
-- CTE nəticəsi üzərində indeks qurmaq mümkün deyil, çünki belə bir obyekt yoxdur.
CREATE INDEX ix_cte ON agir (musteri_id);
/* Alınan xəta:
   ERROR:  relation "agir" does not exist */

-- Eyni ara nəticə TEMP cədvələ yazılır, orada indeks qurmaq mümkündür.
DROP TABLE IF EXISTS t_ara;
CREATE TEMP TABLE t_ara AS
SELECT musteri_id,
       SUM(mebleg) AS cem
FROM t_agir
GROUP BY musteri_id;
CREATE INDEX idx_t_ara_musteri ON t_ara (musteri_id);
ANALYZE t_ara;

-- Variant 1, CTE ilə:
EXPLAIN ANALYZE
WITH t AS (SELECT musteri_id,
                  SUM(mebleg) AS cem
           FROM t_agir
           GROUP BY musteri_id)
SELECT musteri_id AS musteri_id,
       cem        AS cem
FROM t
WHERE musteri_id = 777;

-- Variant 2, indeksli TEMP cədvəl ilə:
EXPLAIN ANALYZE
SELECT musteri_id AS musteri_id,
       cem        AS cem
FROM t_ara
WHERE musteri_id = 777;
/* CTE variantı bütün 500 000 sətri yenidən aqreqasiya edir (Seq Scan + HashAggregate).
   TEMP cədvəl variantı hazır nəticə üzərində Index Scan edir və millisaniyənin altında qayıdır.
   Slayd 7-nin açarı: CTE-də nə indeks var, nə statistika, ona görə optimizator sətir sayını
   səhv qiymətləndirir. */


-- ============================================================
-- E. CTE, Subquery, VIEW və TEMP TABLE müqayisəsi
-- ============================================================

-- 18-ci tapşırıq
-- (a) subquery
SELECT t.seher AS seher,
       t.cem   AS dovriyye
FROM (SELECT seher, SUM(mebleg) AS cem FROM satis GROUP BY seher) t
WHERE t.cem > (SELECT AVG(cem)
               FROM (SELECT seher, SUM(mebleg) AS cem FROM satis GROUP BY seher) t2)
ORDER BY t.cem DESC;

-- (b) CTE
WITH seher_cem AS (SELECT seher,
                          SUM(mebleg) AS cem
                   FROM satis
                   GROUP BY seher),
     orta AS (SELECT AVG(cem) AS orta_cem FROM seher_cem)
SELECT s.seher AS seher,
       s.cem   AS dovriyye
FROM seher_cem s
         CROSS JOIN orta o
WHERE s.cem > o.orta_cem
ORDER BY s.cem DESC;

-- (c) VIEW
DROP VIEW IF EXISTS v_seher_dovriyye;
CREATE VIEW v_seher_dovriyye AS
SELECT seher,
       SUM(mebleg) AS cem
FROM satis
GROUP BY seher;

SELECT seher AS seher,
       cem   AS dovriyye
FROM v_seher_dovriyye
WHERE cem > (SELECT AVG(cem) FROM v_seher_dovriyye)
ORDER BY cem DESC;

-- (d) TEMP TABLE
DROP TABLE IF EXISTS t_seher_dovriyye;
CREATE TEMP TABLE t_seher_dovriyye AS
SELECT seher,
       SUM(mebleg) AS cem
FROM satis
GROUP BY seher;

SELECT seher AS seher,
       cem   AS dovriyye
FROM t_seher_dovriyye
WHERE cem > (SELECT AVG(cem) FROM t_seher_dovriyye)
ORDER BY cem DESC;
-- Dörd variant da eyni nəticəni verir, fərq yalnız obyektin yaşam müddətində və imkanlarındadır.


-- 19-cu tapşırıq
/* Müqayisə cədvəli, 6 meyar və 4 variant:

   Meyar            | Subquery         | CTE               | VIEW               | TEMP TABLE
   -----------------+------------------+-------------------+--------------------+---------------------
   Fiziki obyekt    | yoxdur           | yoxdur            | yalnız tərif       | var, pg_temp sxemdə
   Yaşam müddəti    | bir sorğu        | bir sorğu         | silinənə qədər     | sessiya, ya tranzaksiya
   Təkrar istinad   | hər dəfə yenidən | hər dəfə yenidən  | hər dəfə yenidən   | bir dəfə doldurulur
   İndeks           | qurula bilməz    | qurula bilməz     | qurula bilməz      | qurula bilər
   Statistika       | yoxdur           | yoxdur            | baza cədvəlindən   | ANALYZE ilə var
   Rekursiya        | dəstəklənmir     | WITH RECURSIVE var| daxilində CTE ilə  | dəstəklənmir
*/

-- Sübut 1: VIEW-in tərifi kataloqda qalır.
SELECT schemaname AS sxem,
       viewname   AS ad,
       definition AS tarif
FROM pg_views
WHERE viewname = 'v_seher_dovriyye';

-- Sübut 2: TEMP cədvəl pg_tables-də görünür, CTE adı isə heç bir kataloqda yoxdur.
SELECT schemaname AS sxem,
       tablename  AS ad
FROM pg_tables
WHERE tablename IN ('t_seher_dovriyye', 'seher_cem');
-- Nəticədə yalnız t_seher_dovriyye görünür, seher_cem adlı CTE üçün sətir yoxdur.


-- 20-ci tapşırıq
/* ① 800 min sətirlik ara nəticə dörd dəfə JOIN olunur.
      Seçim: TEMP TABLE. Ara nəticə bir dəfə doldurulur, indeks və ANALYZE ilə dörd JOIN sürətlənir.
   ② Eyni filtr məntiqi altı fərqli hesabatda təkrarlanır.
      Seçim: VIEW. Məntiq bir yerdə saxlanılır və bütün hesabatlar eyni tərifdən istifadə edir.
   ③ Kateqoriya ağacının bütün səviyyələri lazımdır.
      Seçim: RECURSIVE CTE. Ağacın dərinliyi əvvəlcədən bilinmir, yalnız rekursiya bunu həll edir. */

-- ③ variantı işlək sorğu kimi:
WITH RECURSIVE agac AS (SELECT kateqoriya_id,
                               ad,
                               ust_id,
                               1 AS seviyye
                        FROM kateqoriya
                        WHERE ust_id IS NULL
                        UNION ALL
                        SELECT k.kateqoriya_id,
                               k.ad,
                               k.ust_id,
                               a.seviyye + 1
                        FROM kateqoriya k
                                 JOIN agac a ON k.ust_id = a.kateqoriya_id)
SELECT kateqoriya_id AS kateqoriya_id,
       ad            AS ad,
       seviyye       AS seviyye
FROM agac
ORDER BY seviyye, ad;


-- ============================================================
-- F. Müvəqqəti cədvəllər, yaradılması və yaşam müddəti
-- ============================================================

-- 21-ci tapşırıq
DROP TABLE IF EXISTS t_ayliq;
CREATE TEMP TABLE t_ayliq AS
SELECT DATE_TRUNC('month', tarix)::date AS ay,
       COUNT(*)                         AS satis_sayi,
       SUM(mebleg)                      AS dovriyye
FROM satis
GROUP BY DATE_TRUNC('month', tarix);

SELECT COUNT(*) AS setir_sayi
FROM t_ayliq;

SELECT c.relname AS cedvel,
       n.nspname AS sxem
FROM pg_class c
         JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE c.relname = 't_ayliq';
-- Nəticə: sxem pg_temp_N şəklindədir, yəni cədvəl sessiyanın öz temp sxemində yaranıb.


-- 22-ci tapşırıq
BEGIN;
CREATE TEMP TABLE t_drop
(
    n INT
) ON COMMIT DROP;
INSERT INTO t_drop
VALUES (1),
       (2);
SELECT COUNT(*) AS commit_evveli
FROM t_drop;
COMMIT;

SELECT COUNT(*) AS commit_sonrasi
FROM t_drop;
/* Alınan xəta:
   ERROR:  relation "t_drop" does not exist
   İzah: ON COMMIT DROP cədvəli tranzaksiya bitən kimi tamamilə silir. */


-- 23-cü tapşırıq
CREATE TEMP TABLE t_delete
(
    n INT
) ON COMMIT DELETE ROWS;

BEGIN;
INSERT INTO t_delete
VALUES (1),
       (2),
       (3);
SELECT COUNT(*) AS tranzaksiya_daxilinde
FROM t_delete;
COMMIT;
SELECT COUNT(*) AS birinci_commitden_sonra
FROM t_delete;

BEGIN;
INSERT INTO t_delete
VALUES (4),
       (5);
SELECT COUNT(*) AS tranzaksiya_daxilinde
FROM t_delete;
COMMIT;
SELECT COUNT(*) AS ikinci_commitden_sonra
FROM t_delete;
/* Hər COMMIT-dən sonra COUNT(*) = 0, amma SELECT xəta vermir.
   Fərq: ON COMMIT DROP cədvəlin özünü silir, ON COMMIT DELETE ROWS isə yalnız sətirləri silir,
   cədvəl sessiyanın sonuna qədər qalır. */


-- 24-cü tapşırıq
-- Sessiya A
CREATE TEMP TABLE t_test
(
    n INT
);
INSERT INTO t_test
VALUES (1),
       (2),
       (3);
SELECT COUNT(*) AS sessiya_a_sayi
FROM t_test;
-- Müşahidə: sessiya_a_sayi = 3

-- Sessiya B (ayrı psql bağlantısı)
-- CREATE TEMP TABLE t_test (n INT);
-- INSERT INTO t_test VALUES (10), (20);
-- SELECT COUNT(*) AS sessiya_b_sayi FROM t_test;
-- Müşahidə: sessiya_b_sayi = 2, yəni B sessiyası A-nın sətirlərini görmür.

-- İki fərqli temp sxem hər iki sessiya açıq olanda görünür:
SELECT c.relname AS cedvel,
       n.nspname AS sxem
FROM pg_class c
         JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE c.relname = 't_test'
ORDER BY n.nspname;
-- Müşahidə: t_test adı iki dəfə görünür, sxemlər pg_temp_3 və pg_temp_4 kimi fərqlidir.
-- SQL Server qarşılığı: #t_test lokal temp cədvəldir (sessiyaya aiddir),
-- ##t_test isə qlobal temp cədvəldir (bütün sessiyalara görünür).


-- ============================================================
-- G. Müvəqqəti cədvəllər, indeks, statistika, resurs
-- ============================================================

-- 25-ci tapşırıq
DROP TABLE IF EXISTS t_musteri_cem;
DROP TABLE IF EXISTS t_kicik;

CREATE TEMP TABLE t_musteri_cem
(
    musteri_id INT,
    cem        NUMERIC(12, 2)
);
INSERT INTO t_musteri_cem
SELECT (random() * 5000)::int + 1,
       (random() * 900 + 10)::numeric(12, 2)
FROM generate_series(1, 200000);

CREATE TEMP TABLE t_kicik
(
    musteri_id INT
);
INSERT INTO t_kicik
SELECT generate_series(1, 50);

-- Addım 1: yalnız doldurulub
EXPLAIN ANALYZE
SELECT k.musteri_id AS musteri_id,
       SUM(t.cem)   AS cem
FROM t_kicik k
         JOIN t_musteri_cem t ON t.musteri_id = k.musteri_id
GROUP BY k.musteri_id;

-- Addım 2: indeks quruldu
CREATE INDEX idx_t_musteri_cem ON t_musteri_cem (musteri_id);
EXPLAIN ANALYZE
SELECT k.musteri_id AS musteri_id,
       SUM(t.cem)   AS cem
FROM t_kicik k
         JOIN t_musteri_cem t ON t.musteri_id = k.musteri_id
GROUP BY k.musteri_id;

-- Addım 3: ANALYZE edildi
ANALYZE t_musteri_cem;
ANALYZE t_kicik;
EXPLAIN ANALYZE
SELECT k.musteri_id AS musteri_id,
       SUM(t.cem)   AS cem
FROM t_kicik k
         JOIN t_musteri_cem t ON t.musteri_id = k.musteri_id
GROUP BY k.musteri_id;
/* Üç addımın müqayisəsi:

   Addım              | Plan düyünü                  | Gözlənilən / faktiki sətir | İcra vaxtı
   -------------------+------------------------------+----------------------------+-----------
   1) doldurulub      | Seq Scan on t_musteri_cem    | 2 345 235 / 1 948          | 44.7 ms
   2) indeks quruldu  | Index Scan idx_t_musteri_cem | 2 550 000 / 1 948          | 1.16 ms
   3) ANALYZE edildi  | Index Scan idx_t_musteri_cem | 2 003 / 1 948              | 1.14 ms

   Seq Scan-dan Index Scan-a keçid vaxtı 38 dəfə azaldır, ANALYZE isə gözlənilən sətir sayını
   faktiki sayın yanına gətirir. */


-- 26-cı tapşırıq
DROP TABLE IF EXISTS t_stat;
CREATE TEMP TABLE t_stat
(
    musteri_id INT,
    cem        NUMERIC(12, 2)
);
INSERT INTO t_stat
SELECT (random() * 5000)::int + 1,
       (random() * 900 + 10)::numeric(12, 2)
FROM generate_series(1, 200000);

-- ANALYZE-dən əvvəl
SELECT relname   AS cedvel,
       reltuples AS reltuples
FROM pg_class
WHERE relname = 't_stat';
EXPLAIN
SELECT musteri_id AS musteri_id
FROM t_stat
WHERE musteri_id = 100;

-- ANALYZE-dən sonra
ANALYZE t_stat;
SELECT relname   AS cedvel,
       reltuples AS reltuples
FROM pg_class
WHERE relname = 't_stat';
EXPLAIN
SELECT musteri_id AS musteri_id
FROM t_stat
WHERE musteri_id = 100;
/* Müşahidə:
   ANALYZE-dən əvvəl reltuples = -1, yəni statistika heç vaxt yığılmayıb, plan rows=920 gözləyir.
   ANALYZE-dən sonra reltuples = 200000 olur və plan rows=40 gözləyir, bu da 200 000 / 5 000
   paylanmasına uyğun real dəyərdir. Autovacuum temp cədvəlləri görmür, çünki onlar yalnız öz sessiyasına
   aiddir, ona görə ANALYZE əl ilə yazılmalıdır. */


-- 27-ci tapşırıq
-- \timing on
-- (a) əvvəl indeks, sonra INSERT
DROP TABLE IF EXISTS t_a;
CREATE TEMP TABLE t_a
(
    id    INT,
    deyer NUMERIC(10, 2)
);
CREATE INDEX idx_t_a ON t_a (id);
INSERT INTO t_a
SELECT i, (random() * 1000)::numeric(10, 2)
FROM generate_series(1, 200000) AS i;

-- (b) əvvəl INSERT, sonra indeks
DROP TABLE IF EXISTS t_b;
CREATE TEMP TABLE t_b
(
    id    INT,
    deyer NUMERIC(10, 2)
);
INSERT INTO t_b
SELECT i, (random() * 1000)::numeric(10, 2)
FROM generate_series(1, 200000) AS i;
CREATE INDEX idx_t_b ON t_b (id);
/* Ölçmə nəticəsi:
   (a) CREATE INDEX 0.45 ms + INSERT 255.98 ms = 256.43 ms
   (b) INSERT 158.78 ms + CREATE INDEX 36.91 ms = 195.68 ms
   Fərq: (b) variantı təxminən 24 faiz sürətlidir.
   Səbəb: indeks əvvəlcədən mövcud olanda hər INSERT indeksi də yeniləyir. Sonra qurulanda
   indeks bir dəfə, hazır data üzərində qurulur. */


-- ============================================================
-- H. Seçim məntiqi, CTE, TEMP və ya VIEW
-- ============================================================

-- 28-ci tapşırıq
-- Variant 1: yalnız CTE ilə
EXPLAIN ANALYZE
WITH RECURSIVE
    agac AS (SELECT kateqoriya_id,
                    ad,
                    kateqoriya_id AS kok_id,
                    ad            AS kok_ad
             FROM kateqoriya
             WHERE ust_id IS NULL
             UNION ALL
             SELECT k.kateqoriya_id,
                    k.ad,
                    a.kok_id,
                    a.kok_ad
             FROM kateqoriya k
                      JOIN agac a ON k.ust_id = a.kateqoriya_id),
    ayliq AS (SELECT DATE_TRUNC('month', s.tarix)::date AS ay,
                     a.kok_ad                           AS kok_ad,
                     s.seher                            AS seher,
                     SUM(s.mebleg)                      AS dovriyye
              FROM satis s
                       JOIN agac a ON a.kateqoriya_id = s.kateqoriya_id
              GROUP BY 1, 2, 3),
    umumi AS (SELECT SUM(dovriyye) AS umumi_dovriyye FROM ayliq)
SELECT ay                                          AS ay,
       kok_ad                                      AS kok_kateqoriya,
       COALESCE(seher, 'Namelum')                  AS seher,
       dovriyye                                    AS dovriyye,
       ROUND(dovriyye * 100 / u.umumi_dovriyye, 1) AS faiz_payi
FROM ayliq
         CROSS JOIN umumi u
ORDER BY ay, kok_ad;

-- Variant 2: TEMP cədvəl, indeks və ANALYZE ilə
DROP TABLE IF EXISTS t_kok;
CREATE TEMP TABLE t_kok AS
WITH RECURSIVE agac AS (SELECT kateqoriya_id,
                               ad,
                               kateqoriya_id AS kok_id,
                               ad            AS kok_ad
                        FROM kateqoriya
                        WHERE ust_id IS NULL
                        UNION ALL
                        SELECT k.kateqoriya_id,
                               k.ad,
                               a.kok_id,
                               a.kok_ad
                        FROM kateqoriya k
                                 JOIN agac a ON k.ust_id = a.kateqoriya_id)
SELECT kateqoriya_id, kok_id, kok_ad
FROM agac;

CREATE INDEX idx_t_kok ON t_kok (kateqoriya_id);
ANALYZE t_kok;

EXPLAIN ANALYZE
WITH ayliq AS (SELECT DATE_TRUNC('month', s.tarix)::date AS ay,
                      t.kok_ad                           AS kok_ad,
                      s.seher                            AS seher,
                      SUM(s.mebleg)                      AS dovriyye
               FROM satis s
                        JOIN t_kok t ON t.kateqoriya_id = s.kateqoriya_id
               GROUP BY 1, 2, 3),
     umumi AS (SELECT SUM(dovriyye) AS umumi_dovriyye FROM ayliq)
SELECT ay                                          AS ay,
       kok_ad                                      AS kok_kateqoriya,
       COALESCE(seher, 'Namelum')                  AS seher,
       dovriyye                                    AS dovriyye,
       ROUND(dovriyye * 100 / u.umumi_dovriyye, 1) AS faiz_payi
FROM ayliq
         CROSS JOIN umumi u
ORDER BY ay, kok_ad;
/* Ölçmə nəticəsi: satis cədvəli cəmi 24 sətirdir, ona görə iki variantın vaxtı demək olar eynidir
   (hər ikisi 1 ms-in altında). TEMP variantı rekursiyanı bir dəfə hesablayır, CTE variantı isə
   hesabat hər icra olunanda yenidən hesablayır.
   Seçim: bu data həcmində CTE bəsdir. Rekursiya nəticəsi böyük olsaydı və ya hesabatda bir neçə
   dəfə istifadə olunsaydı, TEMP cədvəl + indeks + ANALYZE üstün olardı. */


-- 29-cu tapşırıq
DROP MATERIALIZED VIEW IF EXISTS mv_seher_dovriyye;
DROP VIEW IF EXISTS v_seher_dovriyye2;

CREATE VIEW v_seher_dovriyye2 AS
SELECT COALESCE(seher, 'Namelum') AS seher,
       COUNT(*)                   AS satis_sayi,
       SUM(mebleg)                AS dovriyye
FROM satis
GROUP BY COALESCE(seher, 'Namelum');

CREATE MATERIALIZED VIEW mv_seher_dovriyye AS
SELECT COALESCE(seher, 'Namelum') AS seher,
       COUNT(*)                   AS satis_sayi,
       SUM(mebleg)                AS dovriyye
FROM satis
GROUP BY COALESCE(seher, 'Namelum');

-- Yeni satış əlavə olunur
INSERT INTO satis
VALUES (25, DATE '2024-09-03', 'Bakı', 100, 4, 101, 1000.00);

-- VIEW dərhal yeni dəyəri göstərir, MATERIALIZED VIEW köhnə nəticəni saxlayır
SELECT seher AS seher, dovriyye AS dovriyye
FROM v_seher_dovriyye2
WHERE seher = 'Bakı';

SELECT seher AS seher, dovriyye AS dovriyye
FROM mv_seher_dovriyye
WHERE seher = 'Bakı';

REFRESH MATERIALIZED VIEW mv_seher_dovriyye;

SELECT seher AS seher, dovriyye AS dovriyye
FROM mv_seher_dovriyye
WHERE seher = 'Bakı';

-- Data ilkin vəziyyətə qaytarılır
DELETE
FROM satis
WHERE satis_id = 25;
REFRESH MATERIALIZED VIEW mv_seher_dovriyye;
/* Müşahidə: INSERT-dən sonra VIEW 21 575.29, MATERIALIZED VIEW isə köhnə 20 575.29 dəyərini
   göstərdi. REFRESH-dən sonra hər ikisi 21 575.29 oldu.
   İcra vaxtı: adi VIEW hər dəfə aqreqasiyanı yenidən hesablayır, MATERIALIZED VIEW isə hazır
   sətirləri oxuyur, ona görə böyük datada daha sürətlidir.
   Slayd 12-nin sualı: "Nəticə tez-tez oxunur, amma nadir hallarda dəyişirmi?" Bəli, buna görə
   MATERIALIZED VIEW seçildi. */


-- ============================================================
-- I. Tranzaksiyalar, BEGIN, COMMIT, ROLLBACK
-- ============================================================

-- 30-cu tapşırıq
SELECT hesab_id AS hesab_id,
       sahib    AS sahib,
       balans   AS kocurme_evveli
FROM hesab
WHERE hesab_id IN (1, 2)
ORDER BY hesab_id;

BEGIN;
UPDATE hesab
SET balans = balans - 500
WHERE hesab_id = 1;
UPDATE hesab
SET balans = balans + 500
WHERE hesab_id = 2;
COMMIT;

SELECT hesab_id AS hesab_id,
       sahib    AS sahib,
       balans   AS kocurme_sonrasi
FROM hesab
WHERE hesab_id IN (1, 2)
ORDER BY hesab_id;

SELECT SUM(balans) AS umumi_balans
FROM hesab;
/* Müşahidə: 1-ci hesab 5000.00-dan 4500.00-a düşdü, 2-ci hesab 1200.00-dan 1700.00-a qalxdı.
   Ümumi balans 6500.00 olaraq dəyişmədi, çünki iki UPDATE bir bütöv kimi icra olundu. */

-- Data ilkin vəziyyətə qaytarılır
UPDATE hesab
SET balans = 5000.00
WHERE hesab_id = 1;
UPDATE hesab
SET balans = 1200.00
WHERE hesab_id = 2;


-- 31-ci tapşırıq
BEGIN;
UPDATE hesab
SET balans = balans - 500
WHERE hesab_id = 3;
UPDATE hesab
SET balans = balans + 500
WHERE hesab_id = 2;
ROLLBACK;
/* Alınan xəta:
   ERROR:  new row for relation "hesab" violates check constraint "hesab_balans_check"
   DETAIL:  Failing row contains (3, Nigar Hüseynova, -200.00, AZN).
   Birinci UPDATE xəta verdiyinə görə tranzaksiya abort vəziyyətinə keçdi, ikinci UPDATE
   ümumiyyətlə icra olunmadı. */

-- Sübut: heç bir balans dəyişməyib
SELECT hesab_id AS hesab_id,
       balans   AS balans
FROM hesab
ORDER BY hesab_id;
-- Müşahidə: 1 = 5000.00, 2 = 1200.00, 3 = 300.00, 4 = 0.00, yəni yarım köçürmə qalmadı.


-- 32-ci tapşırıq
-- Sessiya A
BEGIN;
UPDATE hesab
SET balans = 9999.00
WHERE hesab_id = 1;
-- COMMIT hələ edilmir, tranzaksiya açıq saxlanılır

-- Sessiya B (ayrı bağlantı)
-- SELECT hesab_id, balans FROM hesab WHERE hesab_id = 1;
-- Müşahidə: balans = 5000.00, yəni B köhnə dəyəri görür.

-- Sessiya A
COMMIT;

-- Sessiya B
-- SELECT hesab_id, balans FROM hesab WHERE hesab_id = 1;
-- Müşahidə: balans = 9999.00, yəni COMMIT-dən sonra yeni dəyər görünür.
/* İzah: tranzaksiya açıq olduğu müddətdə etdiyi dəyişikliklər yalnız öz daxilində görünür.
   Digər sessiyalar MVCC sayəsində sətrin köhnə versiyasını oxuyur. Yarımçıq iş heç kimə
   görünmür, ona görə "ara vəziyyət görünmür" deyilir. */

-- Data ilkin vəziyyətə qaytarılır
UPDATE hesab
SET balans = 5000.00
WHERE hesab_id = 1;


-- ============================================================
-- J. SAVEPOINT, qismən geri qayıtma
-- ============================================================

-- 33-cü tapşırıq
BEGIN;
INSERT INTO kocurme_log (hesab_id, emeliyyat, mebleg, qeyd)
VALUES (1, 'cixaris', 100.00, 'birinci setir');

SAVEPOINT sp1;

INSERT INTO kocurme_log (hesab_id, emeliyyat, mebleg, qeyd)
VALUES (1, 'sehv', -999.00, 'sehv setir');

ROLLBACK TO SAVEPOINT sp1;

INSERT INTO kocurme_log (hesab_id, emeliyyat, mebleg, qeyd)
VALUES (2, 'medaxil', 100.00, 'duzgun setir');
COMMIT;

SELECT log_id    AS log_id,
       hesab_id  AS hesab_id,
       emeliyyat AS emeliyyat,
       mebleg    AS mebleg,
       qeyd      AS qeyd
FROM kocurme_log
ORDER BY log_id;
/* Müşahidə: cədvəldə iki sətir qaldı, "birinci setir" və "duzgun setir".
   "sehv setir" ROLLBACK TO SAVEPOINT ilə geri alındı.
   SAVEPOINT heç nəyi COMMIT etmir, tranzaksiya COMMIT-ə qədər açıq qalır.
   Qeyd: log_id SERIAL olduğu üçün geri alınan sətrin nömrəsi itir, bu normal davranışdır. */

DELETE
FROM kocurme_log;


-- 34-cü tapşırıq
-- Qoruyucusuz variant
BEGIN;
SELECT 1 / 0;
SELECT 1 AS normal_emr;
ROLLBACK;
/* Alınan xətalar:
   ERROR:  division by zero
   ERROR:  current transaction is aborted, commands ignored until end of transaction block
   İzah: PostgreSQL-də tranzaksiya daxilində bir xəta bütün tranzaksiyanı abort vəziyyətinə salır,
   ondan sonrakı əmrlər ROLLBACK-ə qədər qəbul olunmur. */

-- SAVEPOINT ilə tranzaksiyanı xilas edən variant
BEGIN;
SELECT 1 AS birinci_emr;
SAVEPOINT sp_xeta;
SELECT 1 / 0;
ROLLBACK TO SAVEPOINT sp_xeta;
SELECT 1 AS normal_emr;
COMMIT;
-- Müşahidə: ROLLBACK TO SAVEPOINT-dən sonra tranzaksiya yenidən işlək oldu və COMMIT alındı.


-- 35-ci tapşırıq
-- Beş köçürmə, onlardan ikisi CHECK-i pozur (3-cü hesabda 300, 4-cü hesabda 0 balans var).
BEGIN;

SAVEPOINT sp_1;
UPDATE hesab
SET balans = balans - 1000
WHERE hesab_id = 1;

SAVEPOINT sp_2;
UPDATE hesab
SET balans = balans - 500
WHERE hesab_id = 3;
ROLLBACK TO SAVEPOINT sp_2;
-- CHECK pozuldu, yalnız bu sətir atlanır

SAVEPOINT sp_3;
UPDATE hesab
SET balans = balans - 200
WHERE hesab_id = 2;

SAVEPOINT sp_4;
UPDATE hesab
SET balans = balans - 50
WHERE hesab_id = 4;
ROLLBACK TO SAVEPOINT sp_4;
-- CHECK pozuldu, bu sətir də atlanır

SAVEPOINT sp_5;
UPDATE hesab
SET balans = balans - 2000
WHERE hesab_id = 1;

COMMIT;

SELECT hesab_id AS hesab_id,
       balans   AS balans
FROM hesab
ORDER BY hesab_id;
-- Müşahidə: 1 = 2000.00, 2 = 1000.00, 3 = 300.00, 4 = 0.00. Üç düzgün sətir yazıldı.

-- Data ilkin vəziyyətə qaytarılır
UPDATE hesab
SET balans = 5000.00
WHERE hesab_id = 1;
UPDATE hesab
SET balans = 1200.00
WHERE hesab_id = 2;

-- Eyni məntiq PL/pgSQL EXCEPTION bloku ilə
DO
$$
    DECLARE
        v_setir RECORD;
    BEGIN
        FOR v_setir IN SELECT * FROM (VALUES (1, 1000), (3, 500), (2, 200), (4, 50), (1, 2000)) AS t(hesab, mebleg)
            LOOP
                BEGIN
                    UPDATE hesab SET balans = balans - v_setir.mebleg WHERE hesab_id = v_setir.hesab;
                EXCEPTION
                    WHEN check_violation THEN
                        RAISE NOTICE 'Atlandi: hesab %, mebleg %', v_setir.hesab, v_setir.mebleg;
                END;
            END LOOP;
    END
$$;

SELECT hesab_id AS hesab_id,
       balans   AS balans
FROM hesab
ORDER BY hesab_id;
/* Hər BEGIN ... EXCEPTION ... END bloku daxilən gizli savepoint yaradır, yəni PL/pgSQL variantı
   əl ilə yazdığımız SAVEPOINT məntiqinin eynisini edir. Ucuz əməliyyat deyil, ona görə
   döngə içində EXCEPTION bloku yazmaq performansa təsir edir. */

-- Data ilkin vəziyyətə qaytarılır
UPDATE hesab
SET balans = 5000.00
WHERE hesab_id = 1;
UPDATE hesab
SET balans = 1200.00
WHERE hesab_id = 2;


-- ============================================================
-- K. Autocommit, DDL və uzun tranzaksiyalar
-- ============================================================

-- 36-cı tapşırıq
-- Təhlükəli variant, açıq tranzaksiya yoxdur
UPDATE anbar
SET qaliq = qaliq + 10;
-- Müşahidə: UPDATE 5, yəni bütün 5 sətir dəyişdi.

ROLLBACK;
/* Müşahidə: WARNING:  there is no transaction in progress
   İzah: autocommit açıq olduğuna görə UPDATE artıq öz-özünə COMMIT olunub, ROLLBACK kömək etmir. */

-- Data əl ilə bərpa olunur
UPDATE anbar
SET qaliq = qaliq - 10;

-- Təhlükəsiz vərdiş
BEGIN;
SELECT COUNT(*) AS deyisecek_setir
FROM anbar
WHERE mehsul_id = 1;
UPDATE anbar
SET qaliq = qaliq + 10
WHERE mehsul_id = 1;
SELECT mehsul_id AS mehsul_id, qaliq AS qaliq
FROM anbar
WHERE mehsul_id = 1;
ROLLBACK;

SELECT mehsul_id AS mehsul_id, qaliq AS qaliq
FROM anbar
ORDER BY mehsul_id;
-- Müşahidə: ROLLBACK-dən sonra qaliq yenə 24, yəni açıq tranzaksiyada səhvi geri almaq mümkündür.


-- 37-ci tapşırıq
BEGIN;
CREATE TABLE ddl_test
(
    id INT
);
ALTER TABLE ddl_test
    ADD COLUMN ad VARCHAR(20);
ROLLBACK;

SELECT COUNT(*) AS ddl_test_var
FROM pg_tables
WHERE tablename = 'ddl_test';
/* Müşahidə: ddl_test_var = 0, yəni cədvəl yox oldu.
   PostgreSQL-də DDL tranzaksiyaya daxildir, ROLLBACK CREATE və ALTER-i də geri alır.
   MySQL-də isə CREATE, ALTER və DROP açıq tranzaksiyanı avtomatik COMMIT edir (implicit commit),
   ona görə orada ROLLBACK artıq kömək etmir. Praktiki nəticə: MySQL-də miqrasiya skriptləri
   yarımçıq qala bilər və geri qaytarma skripti əl ilə yazılmalıdır. */


-- 38-ci tapşırıq
-- Sessiya A
BEGIN;
UPDATE hesab
SET balans = balans - 1
WHERE hesab_id = 1;
-- tranzaksiya açıq saxlanılır

-- Sessiya B (ayrı bağlantı)
-- UPDATE hesab SET balans = balans - 1 WHERE hesab_id = 1;
-- Müşahidə: əmr cavab vermir, gözləməyə keçir.

-- Sessiya C və ya A, bloklayan sessiyanı tapmaq üçün
SELECT pid                   AS pid,
       state                 AS veziyyet,
       wait_event_type       AS gozleme_tipi,
       pg_blocking_pids(pid) AS bloklayan_pidler,
       query                 AS sorgu
FROM pg_stat_activity
WHERE state <> 'idle'
  AND datname = 'lesson1';

SELECT locktype           AS lock_tipi,
       relation::regclass AS obyekt,
       mode               AS rejim,
       granted            AS verilib
FROM pg_locks
WHERE relation = 'hesab'::regclass;
/* Müşahidə: B sessiyasının sətrində wait_event_type = 'Lock' və pg_blocking_pids A sessiyasının
   pid-ini qaytarır. A-da COMMIT edən kimi B-nin UPDATE-i dərhal tamamlanır.
   Table bloat: A açıq qaldığı müddətdə VACUUM həmin sətrin köhnə versiyalarını təmizləyə bilmir,
   çünki açıq tranzaksiya hələ də onları görə bilər. Uzun tranzaksiyalar bu səbəbdən cədvəlin
   fiziki ölçüsünü şişirdir. */

-- Sessiya A
ROLLBACK;


-- ============================================================
-- L. ACID, dörd zəmanət
-- ============================================================

-- 39-cu tapşırıq
BEGIN;
UPDATE hesab
SET balans = balans - 100
WHERE hesab_id = 1;
SELECT balans AS birinci_updateden_sonra
FROM hesab
WHERE hesab_id = 1;
UPDATE hesab
SET balans = balans - 100000
WHERE hesab_id = 2;
ROLLBACK;

SELECT hesab_id AS hesab_id,
       balans   AS balans
FROM hesab
WHERE hesab_id IN (1, 2)
ORDER BY hesab_id;
/* Müşahidə: tranzaksiya daxilində 1-ci hesab 4900.00 idi, ROLLBACK-dən sonra yenə 5000.00 oldu.
   İkinci UPDATE CHECK-i pozdu və birinci UPDATE də geri qayıtdı.
   Mexanizm: PostgreSQL MVCC ilə sətrin köhnə versiyasını saxlayır, ROLLBACK yeni versiyanı
   sadəcə etibarsız elan edir. */


-- 40-cı tapşırıq
ALTER TABLE kocurme_log
    ADD CONSTRAINT fk_kocurme_log_hesab
        FOREIGN KEY (hesab_id) REFERENCES hesab (hesab_id);

-- FK pozuntusu
INSERT INTO kocurme_log (hesab_id, emeliyyat, mebleg, qeyd)
VALUES (999, 'test', 10.00, 'olmayan hesab');
/* ERROR:  insert or update on table "kocurme_log" violates foreign key constraint "fk_kocurme_log_hesab"
   DETAIL:  Key (hesab_id)=(999) is not present in table "hesab".
   Mexanizm: FOREIGN KEY, uşaq cədvəldəki dəyərin valideyn cədvəldə mövcudluğunu yoxlayır. */

-- CHECK pozuntusu
UPDATE hesab
SET balans = -50
WHERE hesab_id = 1;
/* ERROR:  new row for relation "hesab" violates check constraint "hesab_balans_check"
   DETAIL:  Failing row contains (1, Aysel Məmmədova, -50.00, AZN).
   Mexanizm: CHECK, sətrin öz dəyərlərinə qoyulan məntiqi şərti yoxlayır.
   Consistency: tranzaksiya bazanı bir doğru vəziyyətdən digər doğru vəziyyətə keçirir,
   qaydanı pozan dəyişiklik ümumiyyətlə qəbul edilmir. */


-- 41-ci tapşırıq
SHOW fsync;
SHOW synchronous_commit;
SHOW wal_level;
/* Müşahidə:
   fsync = on
   synchronous_commit = on
   wal_level = replica

   synchronous_commit = off edildikdə Durability zəmanəti itir: COMMIT cavabı qayıtsa da,
   WAL yazısı hələ diskə fsync olunmamış ola bilər. Server elektriki kəsilsə son bir neçə
   tranzaksiya itə bilər (baza korlanmır, yalnız sonuncu commitlər itir).
   Qazanc: COMMIT diskin cavabını gözləmədiyinə görə yazma əməliyyatları xeyli sürətlənir.
   Məqbul olduğu sistemlər: log yığan, metrik toplayan, analitik və ya keş xarakterli bazalar,
   yəni bir neçə saniyəlik datanın itməsi problem olmayan yerlər. Bank və ödəniş sistemlərində
   bu parametr off edilməz. */


-- ============================================================
-- M. İzolyasiya səviyyələri və anomaliyalar
-- ============================================================

-- 42-ci tapşırıq
SHOW transaction_isolation;
-- Müşahidə: read committed

BEGIN ISOLATION LEVEL REPEATABLE READ;
SHOW transaction_isolation;
-- Müşahidə: repeatable read
COMMIT;

BEGIN ISOLATION LEVEL READ UNCOMMITTED;
SHOW transaction_isolation;
-- Müşahidə: read uncommitted qəbul olunur, amma PostgreSQL onu READ COMMITTED kimi işlədir,
-- yəni dirty read heç vaxt baş vermir.
COMMIT;
/* Default səviyyələr:
   PostgreSQL:    READ COMMITTED
   MySQL InnoDB:  REPEATABLE READ
   SQL Server:    READ COMMITTED */


-- 43-cü tapşırıq
-- Sessiya A, READ COMMITTED
BEGIN ISOLATION LEVEL READ COMMITTED;
SELECT balans AS birinci_oxu
FROM hesab
WHERE hesab_id = 1;
-- Müşahidə: 5000.00

-- Sessiya B (ayrı bağlantı)
-- UPDATE hesab SET balans = 7777.00 WHERE hesab_id = 1;
-- COMMIT avtomatik olur

-- Sessiya A
SELECT balans AS ikinci_oxu
FROM hesab
WHERE hesab_id = 1;
-- Müşahidə: 7777.00, yəni eyni tranzaksiya daxilində iki fərqli dəyər oxundu.
COMMIT;

-- Eyni ssenari REPEATABLE READ ilə
-- Sessiya A
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT balans AS birinci_oxu
FROM hesab
WHERE hesab_id = 1;
-- Müşahidə: 7777.00

-- Sessiya B
-- UPDATE hesab SET balans = 8888.00 WHERE hesab_id = 1;

-- Sessiya A
SELECT balans AS ikinci_oxu
FROM hesab
WHERE hesab_id = 1;
-- Müşahidə: yenə 7777.00, dəyər sabit qaldı.
COMMIT;

-- Data ilkin vəziyyətə qaytarılır
UPDATE hesab
SET balans = 5000.00
WHERE hesab_id = 1;


-- 44-cü tapşırıq
-- Sessiya A, READ COMMITTED
BEGIN ISOLATION LEVEL READ COMMITTED;
SELECT COUNT(*) AS birinci_say
FROM satis
WHERE seher = 'Bakı';
-- Müşahidə: 11

-- Sessiya B
-- INSERT INTO satis VALUES (901, DATE '2024-09-01', 'Bakı', 100, 4, 101, 100.00);

-- Sessiya A
SELECT COUNT(*) AS ikinci_say
FROM satis
WHERE seher = 'Bakı';
-- Müşahidə: 12, yəni phantom sətir göründü.
COMMIT;

-- Eyni ssenari REPEATABLE READ ilə
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT COUNT(*) AS birinci_say
FROM satis
WHERE seher = 'Bakı';
-- Sessiya B: INSERT INTO satis VALUES (902, ...);
SELECT COUNT(*) AS ikinci_say
FROM satis
WHERE seher = 'Bakı';
-- Müşahidə: hər iki say 12-dir. PostgreSQL-də REPEATABLE READ phantom-a da icazə vermir,
-- yəni standartda tələb olunandan güclüdür.
COMMIT;

/* SERIALIZABLE səviyyəsində paralel yazma, iki sessiya:
   -- Sessiya A                                  -- Sessiya B
   BEGIN ISOLATION LEVEL SERIALIZABLE;           BEGIN ISOLATION LEVEL SERIALIZABLE;
   SELECT count(*) FROM satis WHERE seher='Bakı';SELECT count(*) FROM satis WHERE seher='Bakı';
   INSERT INTO satis VALUES (901, ...);          INSERT INTO satis VALUES (902, ...);
   COMMIT;                                       COMMIT;

   Sessiya A uğurla COMMIT etdi, Sessiya B isə xəta aldı:
   ERROR:  could not serialize access due to read/write dependencies among transactions
   DETAIL:  Reason code: Canceled on identification as a pivot, during write.
   HINT:  The transaction might succeed if retried.
   Yəni tətbiq bu xətanı tutub əməliyyatı yenidən cəhd etməlidir. */

-- Data ilkin vəziyyətə qaytarılır
DELETE
FROM satis
WHERE satis_id IN (901, 902);


-- ============================================================
-- N. Lock və deadlock
-- ============================================================

-- 45-ci tapşırıq
-- Sessiya A
BEGIN;
SELECT mehsul_id AS mehsul_id,
       qaliq     AS qaliq
FROM anbar
WHERE mehsul_id = 1 FOR UPDATE;
-- tranzaksiya açıq saxlanılır, sətir kilidlidir

-- Sessiya B (ayrı bağlantı), üç variant
-- (1) SELECT ... FROM anbar WHERE mehsul_id = 1 FOR UPDATE;
--     Müşahidə: əmr gözləməyə keçir, A COMMIT edənə qədər cavab vermir.
-- (2) SELECT ... FROM anbar WHERE mehsul_id = 1 FOR UPDATE NOWAIT;
--     Müşahidə: ERROR:  could not obtain lock on row in relation "anbar"
-- (3) SELECT ... FROM anbar WHERE mehsul_id = 1 FOR UPDATE SKIP LOCKED;
--     Müşahidə: (0 rows), sətir sadəcə atlanır, xəta yoxdur.

-- Sessiya A
COMMIT;
-- Bu, pessimistic locking-dir: sətri əvvəlcədən kilidləyib başqasının dəyişməsinin qarşısını alırıq.


-- 46-cı tapşırıq
/* İki sessiya tərs ardıcıllıqla UPDATE edir:

   -- Sessiya A                                     -- Sessiya B
   BEGIN;                                           BEGIN;
   UPDATE hesab SET balans = balans - 10            UPDATE hesab SET balans = balans - 10
     WHERE hesab_id = 1;                              WHERE hesab_id = 2;
   UPDATE hesab SET balans = balans + 10            UPDATE hesab SET balans = balans + 10
     WHERE hesab_id = 2;                              WHERE hesab_id = 1;
   COMMIT;                                          COMMIT;

   Alınan xəta (Sessiya A qurban seçildi):
   ERROR:  deadlock detected
   DETAIL:  Process 74172 waits for ShareLock on transaction 3497; blocked by process 74171.
   Process 74171 waits for ShareLock on transaction 3498; blocked by process 74172.
   HINT:  See server log for query details.
   CONTEXT:  while updating tuple (0,2) in relation "hesab"
   ROLLBACK

   Sessiya B isə normal COMMIT etdi.
   Deadlock detector dövrəni aşkarlayıb tranzaksiyalardan birini qurban seçir. */

SHOW deadlock_timeout;
-- Müşahidə: 1s, yəni detector 1 saniyə gözlədikdən sonra dövrə axtarmağa başlayır.

-- Data ilkin vəziyyətə qaytarılır
UPDATE hesab
SET balans = 5000.00
WHERE hesab_id = 1;
UPDATE hesab
SET balans = 1200.00
WHERE hesab_id = 2;


-- 47-ci tapşırıq
-- (a) Hər iki sessiya resurslara hesab_id üzrə artan sırada müraciət edir
/* -- Sessiya A                                     -- Sessiya B
   BEGIN;                                           BEGIN;
   UPDATE hesab SET balans = balans - 10            UPDATE hesab SET balans = balans - 10
     WHERE hesab_id = 1;                              WHERE hesab_id = 1;
   UPDATE hesab SET balans = balans + 10            UPDATE hesab SET balans = balans + 10
     WHERE hesab_id = 2;                              WHERE hesab_id = 2;
   COMMIT;                                          COMMIT;

   Müşahidə: deadlock baş vermir. B sadəcə A-nı gözləyir, A COMMIT edəndən sonra işini bitirir.
   Səbəb: hər iki sessiya kilidləri eyni ardıcıllıqla alır, ona görə dövrə yaranmır. */

-- (b) Optimistic locking, versiya sütunu ilə
SELECT mehsul_id AS mehsul_id,
       qaliq     AS qaliq,
       versiya   AS versiya
FROM anbar
WHERE mehsul_id = 1;
-- Tutaq ki, tətbiq qaliq = 24, versiya = 1 oxudu

UPDATE anbar
SET qaliq   = qaliq - 1,
    versiya = versiya + 1
WHERE mehsul_id = 1
  AND versiya = 1;
-- Müşahidə: UPDATE 1, yəni heç kim aradan dəyişməyib.

-- Eyni köhnə versiya ilə ikinci cəhd
UPDATE anbar
SET qaliq   = qaliq - 1,
    versiya = versiya + 1
WHERE mehsul_id = 1
  AND versiya = 1;
-- Müşahidə: UPDATE 0.
/* 0 sətir dəyişdikdə tətbiq bilir ki, sətri başqası dəyişib. Etməli olduğu: dəyişikliyi
   zorla yazmaq yox, sətri yenidən oxuyub əməliyyatı təkrarlamaq və ya istifadəçiyə
   "data yeniləndi" mesajı göstərmək. */

-- Data ilkin vəziyyətə qaytarılır
UPDATE anbar
SET qaliq   = 24,
    versiya = 1
WHERE mehsul_id = 1;

-- (c) PL/pgSQL-də deadlock xətasını tutub 3 dəfə təkrar cəhd edən retry
DO
$$
    DECLARE
        v_cehd INT := 0;
    BEGIN
        LOOP
            v_cehd := v_cehd + 1;
            BEGIN
                UPDATE hesab SET balans = balans - 10 WHERE hesab_id = 1;
                UPDATE hesab SET balans = balans + 10 WHERE hesab_id = 2;
                RAISE NOTICE 'Ugurlu, cehd: %', v_cehd;
                EXIT;
            EXCEPTION
                WHEN deadlock_detected THEN
                    RAISE NOTICE 'Deadlock, cehd: %', v_cehd;
                    IF v_cehd >= 3 THEN
                        RAISE;
                    END IF;
                    PERFORM pg_sleep(0.1);
            END;
        END LOOP;
    END
$$;

-- Data ilkin vəziyyətə qaytarılır
UPDATE hesab
SET balans = 5000.00
WHERE hesab_id = 1;
UPDATE hesab
SET balans = 1200.00
WHERE hesab_id = 2;


-- ============================================================
-- O. Yekun, kompleks tapşırıqlar
-- ============================================================

-- 48-ci tapşırıq
-- (1) Rekursiv CTE ilə hər kateqoriya üçün kök tapılır, (2) TEMP cədvələ yazılır
DROP TABLE IF EXISTS t_kateqoriya_kok;
CREATE TEMP TABLE t_kateqoriya_kok AS
WITH RECURSIVE agac AS (SELECT kateqoriya_id,
                               ad,
                               kateqoriya_id AS kok_id,
                               ad            AS kok_ad
                        FROM kateqoriya
                        WHERE ust_id IS NULL
                        UNION ALL
                        SELECT k.kateqoriya_id,
                               k.ad,
                               a.kok_id,
                               a.kok_ad
                        FROM kateqoriya k
                                 JOIN agac a ON k.ust_id = a.kateqoriya_id)
SELECT kateqoriya_id, ad, kok_id, kok_ad
FROM agac;

-- (3) indeks və ANALYZE
CREATE INDEX idx_t_kateqoriya_kok ON t_kateqoriya_kok (kateqoriya_id);
ANALYZE t_kateqoriya_kok;

-- (4) satışlarla birləşmə, kök kateqoriya və ay kəsiyi
EXPLAIN ANALYZE
WITH ayliq AS (SELECT t.kok_ad                           AS kok_ad,
                      DATE_TRUNC('month', s.tarix)::date AS ay,
                      SUM(s.mebleg)                      AS dovriyye
               FROM satis s
                        JOIN t_kateqoriya_kok t ON t.kateqoriya_id = s.kateqoriya_id
               GROUP BY 1, 2),
     umumi AS (SELECT SUM(dovriyye) AS umumi_dovriyye FROM ayliq)
SELECT kok_ad                                      AS kok_kateqoriya,
       ay                                          AS ay,
       dovriyye                                    AS dovriyye,
       ROUND(dovriyye * 100 / u.umumi_dovriyye, 1) AS faiz_payi
FROM ayliq
         CROSS JOIN umumi u
ORDER BY kok_ad, ay;

-- Nəticənin özü
WITH ayliq AS (SELECT t.kok_ad                           AS kok_ad,
                      DATE_TRUNC('month', s.tarix)::date AS ay,
                      SUM(s.mebleg)                      AS dovriyye
               FROM satis s
                        JOIN t_kateqoriya_kok t ON t.kateqoriya_id = s.kateqoriya_id
               GROUP BY 1, 2),
     umumi AS (SELECT SUM(dovriyye) AS umumi_dovriyye FROM ayliq)
SELECT kok_ad                                      AS kok_kateqoriya,
       ay                                          AS ay,
       dovriyye                                    AS dovriyye,
       ROUND(dovriyye * 100 / u.umumi_dovriyye, 1) AS faiz_payi
FROM ayliq
         CROSS JOIN umumi u
ORDER BY kok_ad, ay;

-- Eyni hesabat yalnız CTE ilə
EXPLAIN ANALYZE
WITH RECURSIVE
    agac AS (SELECT kateqoriya_id,
                    kateqoriya_id AS kok_id,
                    ad            AS kok_ad
             FROM kateqoriya
             WHERE ust_id IS NULL
             UNION ALL
             SELECT k.kateqoriya_id,
                    a.kok_id,
                    a.kok_ad
             FROM kateqoriya k
                      JOIN agac a ON k.ust_id = a.kateqoriya_id),
    ayliq AS (SELECT a.kok_ad                           AS kok_ad,
                     DATE_TRUNC('month', s.tarix)::date AS ay,
                     SUM(s.mebleg)                      AS dovriyye
              FROM satis s
                       JOIN agac a ON a.kateqoriya_id = s.kateqoriya_id
              GROUP BY 1, 2),
    umumi AS (SELECT SUM(dovriyye) AS umumi_dovriyye FROM ayliq)
SELECT kok_ad                                      AS kok_kateqoriya,
       ay                                          AS ay,
       dovriyye                                    AS dovriyye,
       ROUND(dovriyye * 100 / u.umumi_dovriyye, 1) AS faiz_payi
FROM ayliq
         CROSS JOIN umumi u
ORDER BY kok_ad, ay;
/* Müqayisə: kateqoriya cədvəli 10 sətir, satis cədvəli 24 sətirdir, ona görə hər iki variant
   1 ms ətrafındadır və fərq ölçülə bilən deyil.
   Seçim: bu ölçüdə CTE variantı daha yaxşıdır, çünki artıq obyekt yaratmır və hesabat bir
   sorğuda oxunur. TEMP variantı kateqoriya ağacı böyük olduqda və ya eyni ara nəticə bir neçə
   hesabatda işlədildikdə üstün olar, çünki rekursiya bir dəfə hesablanır və indeksdən istifadə
   olunur. */


-- 49-cu tapşırıq
-- Daimi hədəf cədvəl
DROP TABLE IF EXISTS ayliq_yekun;
CREATE TABLE ayliq_yekun
(
    ay         DATE PRIMARY KEY,
    satis_sayi INT            NOT NULL,
    dovriyye   NUMERIC(14, 2) NOT NULL,
    yenilendi  TIMESTAMP      NOT NULL DEFAULT now()
);

-- İdempotent ETL skripti, tək tranzaksiya
BEGIN;

CREATE TEMP TABLE t_etl
(
    ay         DATE,
    satis_sayi INT,
    dovriyye   NUMERIC(14, 2)
) ON COMMIT DROP;

INSERT INTO t_etl
SELECT DATE_TRUNC('month', tarix)::date,
       COUNT(*),
       SUM(mebleg)
FROM satis
GROUP BY DATE_TRUNC('month', tarix);

INSERT INTO ayliq_yekun (ay, satis_sayi, dovriyye)
SELECT ay, satis_sayi, dovriyye
FROM t_etl
ON CONFLICT (ay)
    DO UPDATE SET satis_sayi = EXCLUDED.satis_sayi,
                  dovriyye   = EXCLUDED.dovriyye,
                  yenilendi  = now();

COMMIT;

SELECT COUNT(*) AS birinci_icradan_sonra
FROM ayliq_yekun;

-- Skript ikinci dəfə icra olunur
BEGIN;

CREATE TEMP TABLE t_etl
(
    ay         DATE,
    satis_sayi INT,
    dovriyye   NUMERIC(14, 2)
) ON COMMIT DROP;

INSERT INTO t_etl
SELECT DATE_TRUNC('month', tarix)::date,
       COUNT(*),
       SUM(mebleg)
FROM satis
GROUP BY DATE_TRUNC('month', tarix);

INSERT INTO ayliq_yekun (ay, satis_sayi, dovriyye)
SELECT ay, satis_sayi, dovriyye
FROM t_etl
ON CONFLICT (ay)
    DO UPDATE SET satis_sayi = EXCLUDED.satis_sayi,
                  dovriyye   = EXCLUDED.dovriyye,
                  yenilendi  = now();

COMMIT;

SELECT COUNT(*) AS ikinci_icradan_sonra
FROM ayliq_yekun;

SELECT ay         AS ay,
       satis_sayi AS satis_sayi,
       dovriyye   AS dovriyye
FROM ayliq_yekun
ORDER BY ay;
/* Müşahidə: hər iki icradan sonra sətir sayı 7-dir, dublikat yaranmadı.
   İdempotentliyi təmin edən üç şey: ay sütunu PRIMARY KEY-dir, ON CONFLICT DO UPDATE mövcud
   ayı yeniləyir, bütün addımlar tək tranzaksiyadadır və xəta olarsa hamısı geri qayıdır.
   ON COMMIT DROP temp cədvəli COMMIT-dən sonra öz-özünə silinir, təmizləmə kodu lazım deyil. */


-- 50-ci tapşırıq
CREATE OR REPLACE FUNCTION kocurme(gonderen INT, alan INT, mebleg NUMERIC)
    RETURNS TEXT
    LANGUAGE plpgsql
AS
$$
DECLARE
    v_balans  NUMERIC;
    v_birinci INT;
    v_ikinci  INT;
    v_cehd    INT := 0;
BEGIN
    IF mebleg <= 0 THEN
        RAISE EXCEPTION 'Mebleg musbet olmalidir, verilen: %', mebleg;
    END IF;

    -- Deadlock-un qarşısını almaq üçün hesablar həmişə artan sıra ilə kilidlənir
    v_birinci := LEAST(gonderen, alan);
    v_ikinci := GREATEST(gonderen, alan);

    LOOP
        v_cehd := v_cehd + 1;
        BEGIN
            PERFORM hesab_id FROM hesab WHERE hesab_id = v_birinci FOR UPDATE;
            PERFORM hesab_id FROM hesab WHERE hesab_id = v_ikinci FOR UPDATE;

            SELECT balans INTO v_balans FROM hesab WHERE hesab_id = gonderen;

            IF v_balans IS NULL THEN
                RAISE EXCEPTION 'Hesab tapilmadi: %', gonderen;
            END IF;

            IF v_balans < mebleg THEN
                RAISE EXCEPTION 'Vesait catmir. Hesab %, balans %, teleb olunan %',
                    gonderen, v_balans, mebleg;
            END IF;

            UPDATE hesab SET balans = balans - mebleg WHERE hesab_id = gonderen;
            UPDATE hesab SET balans = balans + mebleg WHERE hesab_id = alan;

            INSERT INTO kocurme_log (hesab_id, emeliyyat, mebleg, qeyd)
            VALUES (gonderen, 'cixaris', mebleg, 'kocurme, alan: ' || alan);
            INSERT INTO kocurme_log (hesab_id, emeliyyat, mebleg, qeyd)
            VALUES (alan, 'medaxil', mebleg, 'kocurme, gonderen: ' || gonderen);

            RETURN 'Ugurlu kocurme: ' || mebleg || ' AZN, ' || gonderen || ' > ' || alan;

        EXCEPTION
            WHEN deadlock_detected THEN
                IF v_cehd >= 3 THEN
                    RAISE;
                END IF;
                PERFORM pg_sleep(0.1);
        END;
    END LOOP;
END
$$;

-- Uğurlu çağırış
SELECT kocurme(1, 2, 500.00) AS netice;

SELECT hesab_id AS hesab_id,
       balans   AS balans
FROM hesab
WHERE hesab_id IN (1, 2)
ORDER BY hesab_id;
-- Müşahidə: 1 = 4500.00, 2 = 1700.00, kocurme_log-a iki sətir yazıldı.

-- Uğursuz çağırış, vəsait çatmır
SELECT kocurme(4, 1, 100.00) AS netice;
/* ERROR:  Vesait catmir. Hesab 4, balans 0.00, teleb olunan 100.00
   Funksiya öz-özlüyündə bir tranzaksiyadır, RAISE EXCEPTION bütün dəyişiklikləri geri alır,
   ona görə nə balanslar, nə də log dəyişir. */

SELECT hesab_id AS hesab_id,
       balans   AS balans
FROM hesab
ORDER BY hesab_id;

-- Data ilkin vəziyyətə qaytarılır
UPDATE hesab
SET balans = 5000.00
WHERE hesab_id = 1;
UPDATE hesab
SET balans = 1200.00
WHERE hesab_id = 2;
DELETE
FROM kocurme_log;


-- 51-ci tapşırıq
/* Skriptdəki altı səhv:
   1) CREATE INDEX INSERT-dən əvvəl yazılıb. Düzgün ardıcıllıq: doldur, sonra indeks qur.
   2) Doldurduqdan sonra ANALYZE yoxdur, ona görə temp cədvəlin statistikası boş qalır və
      optimizator sətir sayını səhv qiymətləndirir.
   3) agir CTE-si əsas sorğuda üç dəfə istinad olunub, yəni aqreqasiya üç dəfə icra olunur.
      MATERIALIZED yazılmalı, ya da nəticə temp cədvələ yığılmalıdır.
   4) Rekursiv sorğuda dövrə qoruyucusu yoxdur. qraf-da 2 > 3 > 4 > 2 dövrəsi var, sorğu
      sonsuza gedir. Yol massivi və səviyyə limiti lazımdır.
   5) UPDATE hesab SET balans = balans * 1.05; WHERE yoxdur və açıq tranzaksiya da yoxdur,
      yəni bütün sətirlər dəyişir və autocommit səbəbindən geri qaytarmaq mümkün deyil.
   6) Tranzaksiyanın içində xarici API gözləntisi var. 30 saniyə boyunca sətirlər kilidli qalır,
      VACUUM köhnə versiyaları təmizləyə bilmir, digər sessiyalar gözləyir. */

-- Düzgün variant:
-- (1) və (2): əvvəl doldur, sonra indeks, sonra ANALYZE
DROP TABLE IF EXISTS t_boyuk;
CREATE TEMP TABLE t_boyuk
(
    musteri_id INT,
    cem        NUMERIC
);

INSERT INTO t_boyuk
SELECT musteri_id, SUM(mebleg)
FROM satis
GROUP BY musteri_id;

CREATE INDEX ix_t ON t_boyuk (musteri_id);
ANALYZE t_boyuk;

-- (3): CTE üç dəfə istinad olunur, ona görə MATERIALIZED yazılır
WITH agir AS MATERIALIZED (SELECT s.satis_id,
                                  s.mebleg,
                                  t.cem
                           FROM satis s
                                    JOIN t_boyuk t USING (musteri_id))
SELECT (SELECT COUNT(*) FROM agir)    AS say,
       (SELECT SUM(mebleg) FROM agir) AS cem,
       (SELECT MAX(cem) FROM agir)    AS maks;

-- (4): rekursiyaya yol massivi və səviyyə limiti əlavə olunur
WITH RECURSIVE ag AS (SELECT ust,
                             alt,
                             1                AS seviyye,
                             ARRAY [ust, alt] AS yol
                      FROM qraf
                      WHERE ust = 1
                      UNION ALL
                      SELECT q.ust,
                             q.alt,
                             a.seviyye + 1,
                             a.yol || q.alt
                      FROM qraf q
                               JOIN ag a ON q.ust = a.alt
                      WHERE NOT (q.alt = ANY (a.yol))
                        AND a.seviyye < 10)
SELECT ust     AS ust,
       alt     AS alt,
       seviyye AS seviyye
FROM ag
ORDER BY seviyye, ust, alt;

-- (5): açıq tranzaksiya, WHERE şərti və yoxlama
BEGIN;
SELECT COUNT(*) AS deyisecek_setir
FROM hesab
WHERE valyuta = 'AZN';
UPDATE hesab
SET balans = balans * 1.05
WHERE valyuta = 'AZN';
SELECT hesab_id AS hesab_id, balans AS balans
FROM hesab
ORDER BY hesab_id;
ROLLBACK;

-- (6): xarici çağırış tranzaksiyadan kənara çıxarılır, tranzaksiya qısa saxlanılır
-- Tətbiq əvvəlcə API cavabını alır, yalnız sonra qısa tranzaksiya açır:
BEGIN;
UPDATE anbar
SET qaliq = qaliq - 1
WHERE mehsul_id = 1;
UPDATE anbar
SET qaliq = qaliq - 1
WHERE mehsul_id = 2;
COMMIT;

-- Data ilkin vəziyyətə qaytarılır
UPDATE anbar
SET qaliq = 24
WHERE mehsul_id = 1;
UPDATE anbar
SET qaliq = 40
WHERE mehsul_id = 2;
