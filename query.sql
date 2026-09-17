-- =====================================================================
-- SQL Məhdudiyyətləri və İndekslər — Praktik Tapşırıq (40 tapşırıq)
-- Ad, Soyad: Ahmad Ahmadov
-- Qrup: -
-- Tarix: 2026-09-15
-- Mühit: PostgreSQL 15+
-- Qeyd: Ölçmə nəticələri və izahlar hesabat.md faylındadır.
-- =====================================================================


-- =====================================================================
-- Hazırlıq — 1. İş sahəsi
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS magaza;
SET
search_path TO magaza, public;
-- İcra vaxtını görmək üçün (psql-də):
\timing
on
-- Planları oxunaqlı saxlamaq üçün paralelliyi söndürün:
SET max_parallel_workers_per_gather = 0;


-- =====================================================================
-- Hazırlıq — 2. İndeks bölmələri üçün cədvəl (G və H bölmələri)
-- =====================================================================

DROP TABLE IF EXISTS satis_log;
CREATE TABLE satis_log
(
    id           INT,
    musteri_kodu INT,
    mehsul_adi   VARCHAR(80),
    kateqoriya   VARCHAR(30),
    seher        VARCHAR(30),
    status       VARCHAR(20),
    miqdar       INT,
    mebleg       NUMERIC(12, 2),
    tarix        DATE
);

INSERT INTO satis_log
SELECT i,
       (random() * 20000)::int + 1, 'Mehsul ' || (i % 5000),
       (ARRAY['Texnika', 'Aksesuar', 'Ofis', 'Mebel', 'Kitab'])[(i % 5) + 1],
 (ARRAY['Bakı','Gəncə','Sumqayıt','Şəki','Lənkəran'])[(i % 5) + 1],
 CASE WHEN i % 97 = 0 THEN 'legv' ELSE 'tamam'
END
,
 (random() * 10)::int + 1,
 (random() * 5000 + 10)::numeric(12, 2),
 DATE '2022-01-01' + (i % 1000)
FROM generate_series(1, 300000) AS i;
ANALYZE
satis_log;


-- =====================================================================
-- A. Cədvəl açarları və NOT NULL (1–5, 10 bal)
-- =====================================================================

-- 1-ci tapşırıq (2 bal)
-- kateqoriya cədvəli: id avtomatik artan açar (pk_kateqoriya), ad VARCHAR(50) NOT NULL.



-- 2-ci tapşırıq (2 bal)
-- mehsul cədvəli: id (açar), ad, kateqoriya_id, qiymet, anbarda_say, aktiv.
-- Yalnız PRIMARY KEY və NOT NULL (ad, qiymet).



-- 3-cü tapşırıq (2 bal)
-- musteri cədvəli: id (açar), ad, soyad, email NOT NULL; telefon NULL ola bilər; qeydiyyat_tarixi DATE.



-- 4-cü tapşırıq (2 bal)
-- sifaris_detal cədvəli: kompozit açar (sifaris_id, mehsul_id); say, vahid_qiymet.



-- 5-ci tapşırıq (2 bal)
-- pg_indexes-dən magaza sxemindəki bütün indeksləri çıxaran sorğu.

-- İzah (nəticə niyə boş deyil + PK ilə UNIQUE+NOT NULL fərqi):


-- =====================================================================
-- B. Təkrarsızlıq — UNIQUE (6–9, 8 bal)
-- =====================================================================

-- 6-cı tapşırıq (2 bal)
-- musteri.email üçün ALTER TABLE ilə adlandırılmış UNIQUE; təkrar email ilə test.

-- Xəta mesajı:


-- 7-ci tapşırıq (2 bal)
-- mehsul: (kateqoriya_id, ad) cütü üzrə UNIQUE; hər iki halın testi.



-- 8-ci tapşırıq (2 bal)
-- musteri.telefon üzrə UNIQUE; iki NULL telefon testi + NULLS NOT DISTINCT variantı.

-- İzah:


-- 9-cu tapşırıq (2 bal)
-- Hər kateqoriyada yalnız bir aktiv məhsul — partial unique index.


-- =====================================================================
-- C. Dəyər yoxlamaları — CHECK (10–14, 10 bal)
-- =====================================================================

-- 10-cu tapşırıq (2 bal)
-- mehsul: iki adlandırılmış CHECK (qiymet > 0, anbarda_say >= 0) + pozan INSERT-lər.

-- Xəta mesajları:


-- 11-ci tapşırıq (2 bal)
-- musteri.email CHECK: @ və nöqtə var, uzunluq > 5, boşluq yoxdur.



-- 12-ci tapşırıq (2 bal)
-- mehsul.endirimli_qiymet sütunu + cədvəl səviyyəsində CHECK (0 <= endirimli_qiymet <= qiymet).



-- 13-cü tapşırıq (2 bal)
-- sifaris cədvəli: status yalnız 4 dəyərdən biri; status = 'legv' olduqda legv_sebebi NOT NULL.



-- 14-cü tapşırıq (2 bal)
-- endirimli_qiymet NULL olduqda CHECK niyə keçir; NULL-u da bloklayan həll.

-- İzah (üç dəyərli məntiq — TRUE / FALSE / UNKNOWN):


-- =====================================================================
-- D. Standart və hesablanan dəyərlər (15–17, 6 bal)
-- =====================================================================

-- 15-ci tapşırıq (2 bal)
-- DEFAULT: musteri.qeydiyyat_tarixi, mehsul.anbarda_say, mehsul.aktiv, sifaris.status.



-- 16-cı tapşırıq (2 bal)
-- İki INSERT: biri sütunsuz, biri açıq NULL ilə.

-- İzah (fərqin səbəbi):


-- 17-ci tapşırıq (2 bal)
-- sifaris_detal.cemi — GENERATED ALWAYS AS (say * vahid_qiymet) STORED + əl ilə UPDATE cəhdi.

-- Nəticə:


-- =====================================================================
-- E. Cədvəllərarası bağlar — FOREIGN KEY (18–20, 6 bal)
-- =====================================================================

-- 18-ci tapşırıq (2 bal)
-- İki adlandırılmış FK: mehsul.kateqoriya_id -> kateqoriya.id, sifaris.musteri_id -> musteri.id + səhv INSERT.



-- 19-cu tapşırıq (2 bal)
-- Üç silinmə davranışı: CASCADE, RESTRICT, SET NULL — hər biri üçün ayrıca test.

-- Nəticələr:


-- 20-ci tapşırıq (2 bal)
-- musteri.devet_eden_id — özünə istinad edən FK (DEFERRABLE INITIALLY DEFERRED);
-- bir-birini dəvət edən iki müştəri tək tranzaksiyada.


-- =====================================================================
-- F. Məhdudiyyətlərin idarə olunması (21–25, 10 bal)
-- =====================================================================

-- 21-ci tapşırıq (2 bal)
-- Problemli (NULL) sətirləri tapan sorğu -> düzəliş -> mehsul.anbarda_say NOT NULL.



-- 22-ci tapşırıq (2 bal)
-- Qiymət CHECK-ini silib yenisi ilə əvəz etmək (0 < qiymet < 100000);
-- eyni ALTER TABLE-də mümkündürmü — yoxlayın.

-- Nəticə:


-- 23-cü tapşırıq (2 bal)
-- NOT VALID ilə məhdudiyyət -> səhv INSERT-lərlə sübut -> köhnə sətirlərin düzəlişi -> VALIDATE CONSTRAINT.

-- İzah (NOT VALID ilə VALIDATE fərqi):


-- 24-cü tapşırıq (2 bal)
-- FK yoxlanışını müvəqqəti dayandırmağın iki yolu.

-- Risklərin müqayisəsi:


-- 25-ci tapşırıq (2 bal)
-- Audit sorğusu: cədvəl adı, məhdudiyyət adı, oxunaqlı tip, tam tərif; cədvəl adına görə sıralı.


-- =====================================================================
-- G. İndekslər — əsaslar (26–30, 10 bal)
-- Bütün işlər satis_log üzərində. Hər ölçmədən əvvəl: ANALYZE satis_log;
-- Ölçmə cədvəlləri hesabat.md faylındadır.
-- =====================================================================

-- 26-cı tapşırıq (2 bal)
-- WHERE mehsul_adi = 'Mehsul 4321' — EXPLAIN (ANALYZE, BUFFERS) indekssiz və indeksli.

-- İzah:


-- 27-ci tapşırıq (2 bal)
-- (kateqoriya, tarix) kompozit indeksi; üç sorğu: (a) kateqoriya, (b) tarix, (c) hər ikisi.

-- İzah (sol prefiks qaydası):


-- 28-ci tapşırıq (2 bal)
-- UNIQUE məhdudiyyət vs UNIQUE INDEX: yaratmaq, pg_constraint/pg_indexes-də axtarmaq, DROP CONSTRAINT cəhdi.

-- İzah:


-- 29-cu tapşırıq (2 bal)
-- WHERE status = 'legv' — tam indeks vs partial indeks; ölçü və sürət müqayisəsi.

-- İzah (partial indeks nə vaxt məqsədəuyğundur):


-- 30-cu tapşırıq (2 bal)
-- WHERE UPPER(mehsul_adi) = 'MEHSUL 100' — indeks niyə işləmir; iki fərqli həll və ölçmə.

-- İzah:


-- =====================================================================
-- H. Çətin və qarışıq tapşırıqlar (31–40, 40 bal)
-- =====================================================================

-- 31-ci tapşırıq (4 bal)
-- SELECT seher, tarix ... WHERE seher = 'Gəncə' — Index Only Scan, Heap Fetches = 0.

-- İzah:


-- 32-ci tapşırıq (4 bal)
-- ORDER BY mebleg DESC LIMIT 20 — planda Sort olmamalı; sonra NULLS LAST variantı.

-- İzah:


-- 33-cü tapşırıq (4 bal)
-- Hesabat: satis_log indeksləri — ölçü, cədvələ nisbət (%), tərif; ölçüyə görə azalan.

-- Ən «bahalı» indeks:


-- 34-cü tapşırıq (4 bal)
-- pg_stat_reset() -> 5–6 SELECT -> indeks skan sayları; idx_scan = 0 olanlar.

-- İzah:


-- 35-ci tapşırıq (4 bal)
-- WHERE mehsul_adi LIKE '%hsul 4321%' — B-tree niyə kömək etmir; GIN + pg_trgm həlli və ölçmə.

-- İzah:


-- 36-cı tapşırıq (4 bal)
-- İndeksin yazma qiyməti: (a) indekssiz 100 000 INSERT, (b) 5 indekslə eyni INSERT.

-- Fərq (%) və nəticə:


-- 37-ci tapşırıq (4 bal)
-- satis_log-a PRIMARY KEY; satis_qeyd cədvəli (FK, 200 000 sətir);
-- FK sütununda indekssiz və indeksli silinmə vaxtı.

-- İzah (PostgreSQL FK sütununa avtomatik indeks yaradırmı):


-- 38-ci tapşırıq (4 bal)
-- WHERE kateqoriya = 'Ofis' AND seher = 'Bakı' — (a) iki ayrı indeks, (b) kompozit indeks.

-- İzah (BitmapAnd, hansı daha sürətli və niyə):


-- 39-cu tapşırıq (4 bal)
-- İndeks ölçüsü -> cədvəlin ~40%-i UPDATE -> ölçü yenidən; n_dead_tup; REINDEX.

-- İzah (bloat, MVCC, ölü sətirlər):


-- 40-cı tapşırıq (4 bal)
-- Yekun audit: cədvəl adı, təxmini sətir sayı, cədvəl ölçüsü, indeks sayı, indekslərin ümumi ölçüsü,
-- PK var/yoxdur, status (Problemli / Nezaret lazimdir / Normal); cədvəl ölçüsünə görə azalan.

