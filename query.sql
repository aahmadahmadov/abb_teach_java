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
SET search_path TO magaza, public;
-- İcra vaxtını görmək üçün (psql-də):
\timing on
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
       (random() * 20000)::int + 1,
       'Mehsul ' || (i % 5000),
       (ARRAY ['Texnika', 'Aksesuar', 'Ofis', 'Mebel', 'Kitab'])[(i % 5) + 1],
       (ARRAY ['Bakı','Gəncə','Sumqayıt','Şəki','Lənkəran'])[(i % 5) + 1],
       CASE
           WHEN i % 97 = 0 THEN 'legv'
           ELSE 'tamam'
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

-- 1-ci tapşırıq
-- kateqoriya cədvəlini yaradın: id — avtomatik artan və cədvəlin açarı, ad — VARCHAR(50), boş ola bilməz. Açara açıq ad verin: pk_kateqoriya. (2 bal)
-- İpucu: `GENERATED ALWAYS AS IDENTITY` və `CONSTRAINT pk_kateqoriya PRIMARY KEY (id)`.

DROP TABLE IF EXISTS kateqoriya;

CREATE TABLE kateqoriya
(
    id INT GENERATED ALWAYS AS IDENTITY,
    ad VARCHAR(50) NOT NULL,
    CONSTRAINT pk_kateqoriya PRIMARY KEY (id)
);

SELECT conname, contype
FROM pg_constraint
WHERE conrelid = 'kateqoriya'::regclass;

INSERT INTO kateqoriya (ad)
VALUES ('Texnika'),
       ('Aksesuar');

SELECT *
FROM kateqoriya;

-- Test: ad NOT NULL olduğu üçün bu sətir xəta verir.
-- INSERT INTO kateqoriya (ad) VALUES (NULL);
-- ERROR: null value in column "ad" of relation "kateqoriya" violates not-null constraint


-- 2-ci tapşırıq
-- mehsul cədvəlini yaradın: id (açar), ad, kateqoriya_id, qiymet NUMERIC(10,2), anbarda_say INT, aktiv BOOLEAN. ad və qiymet boş ola bilməz. Hələlik yalnız PRIMARY KEY və NOT NULL yazın. (2 bal)

drop table if exists mehsul;

create table mehsul
(
    id            int generated always as identity,
    ad            varchar(100)   not null,
    kateqoriya_id int,
    qiymet        numeric(10, 2) not null,
    anbarda_say   int,
    aktiv         boolean,
    constraint pk_mehsul primary key (id)
);


-- 3-cü tapşırıq
-- musteri cədvəlini yaradın: id (açar), ad, soyad, email — üçü də boş ola bilməz; telefon boş qala bilər; qeydiyyat_tarixi DATE. (2 bal)

drop table if exists musteri;

create table musteri
(
    id               int generated always as identity,
    ad               varchar(50)  not null,
    soyad            varchar(50)  not null,
    email            varchar(100) not null,
    telefon          varchar(20),
    qeydiyyat_tarixi date,
    constraint pk_musteri primary key (id)
);

-- 4-cü tapşırıq
-- sifaris_detal cədvəlini yaradın. Açar tək sütun deyil: sifaris_id və mehsul_id birlikdə açarı təşkil etsin. Əlavə sütunlar: say, vahid_qiymet. (2 bal)
-- İpucu: Kompozit açar yalnız cədvəl səviyyəsində yazılır: `PRIMARY KEY (sifaris_id, mehsul_id)`.

drop table if exists sifaris_detal;

create table sifaris_detal
(
    sifaris_id   int,
    mehsul_id    int,
    say          int,
    vahid_qiymet numeric(12, 2),
    constraint pk_sifaris_detal primary key (sifaris_id, mehsul_id)
);

-- 5-ci tapşırıq
-- Yaratdığınız cədvəllərin bütün indekslərini pg_indexes-dən çıxaran sorğu yazın. Siz heç bir indeks yaratmamısınız, amma nəticə boş deyil — niyə? PRIMARY KEY ilə UNIQUE + NOT NULL arasındakı fərqi bir cümlə ilə yazın. (2 bal)
-- İpucu: `WHERE schemaname = 'magaza'`. PK məhdudiyyət, indeks isə onun icra mexanizmidir.

SELECT *
FROM pg_indexes
WHERE schemaname = 'magaza';

-- İzah: PRIMARY KEY və UNIQUE məhdudiyyətləri PostgreSQL-də avtomatik unikal B-tree
-- indeks yaradır — indeks məhdudiyyətin icra mexanizmidir. Ona görə CREATE INDEX
-- yazmasam da nəticədə 4 PK indeksi görünür.
-- Fərq: UNIQUE + NOT NULL eyni yoxlamanı verir, amma PK cədvəlin əsas açarıdır —
-- cədvəldə yalnız bir PK ola bilər və xarici açarlar ona istinad edir.


-- =====================================================================
-- B. Təkrarsızlıq — UNIQUE (6–9, 8 bal)
-- =====================================================================

-- 6-cı tapşırıq
-- musteri.email təkrarlanmasın. Cədvəli yenidən yaratmadan, ALTER TABLE ilə adlandırılmış UNIQUE məhdudiyyət əlavə edin. Sonra eyni email ilə ikinci müştəri yazmağa çalışın və xəta mesajını qeyd edin. (2 bal)

alter table musteri
    add constraint uq_musteri_email unique (email);

insert into magaza.musteri (ad, soyad, email, telefon)
values ('Ali', 'Aliyev', 'ali@mail.ru', '0501112233');

-- Test: eyni email ilə ikinci müştəri — xəta verir.
insert into magaza.musteri (ad, soyad, email, telefon)
values ('Vali', 'Valiyev', 'ali@mail.ru', '0502223344');

-- Xəta mesajı:
-- ERROR: duplicate key value violates unique constraint "uq_musteri_email"
-- DETAIL: Key (email)=(ali@mail.ru) already exists.


-- 7-ci tapşırıq
-- mehsul cədvəlində eyni kateqoriyada eyni adlı iki məhsul olmasın, lakin fərqli kateqoriyalarda eyni ad işlənə bilsin. Məhdudiyyəti qurun və hər iki halı test edin. (2 bal)
-- İpucu: Tək sütuna deyil, sütun cütünə qoyulan UNIQUE.

alter table mehsul
    add constraint uq_mehsul_kateqoriya_ad unique (kateqoriya_id, ad);

insert into magaza.mehsul (ad, kateqoriya_id, qiymet)
values ('Yeni Mehsul', 1, 100.00);

-- 1-ci hal: eyni kateqoriyada eyni ad — xəta verir.
insert into magaza.mehsul (ad, kateqoriya_id, qiymet)
values ('Yeni Mehsul', 1, 150.00);

-- Xəta mesajı:
-- ERROR: duplicate key value violates unique constraint "uq_mehsul_kateqoriya_ad"
-- DETAIL: Key (kateqoriya_id, ad)=(1, Yeni Mehsul) already exists.

-- 2-ci hal: fərqli kateqoriyada eyni ad — keçir.
insert into magaza.mehsul (ad, kateqoriya_id, qiymet)
values ('Yeni Mehsul', 2, 120.00);

-- 8-ci tapşırıq
-- musteri.telefon sütununa UNIQUE qoyun, sonra telefonu NULL olan iki müştəri əlavə edin. Sorğu keçirmi? Nəticəni izah edin və hər iki NULL-u da təkrar sayan variantı yazın. (2 bal)
-- İpucu: Standart davranışda NULL heç nəyə bərabər deyil, hətta özünə də. PostgreSQL 15+ üçün: `UNIQUE NULLS NOT DISTINCT`.

alter table musteri
    add constraint uq_musteri_telefon unique (telefon);

insert into magaza.musteri (ad, soyad, email, telefon)
values ('Test', 'User1', 'email@mail.ru', NULL);

insert into magaza.musteri (ad, soyad, email, telefon)
values ('Test', 'User2', 'email2@mail.ru', NULL);

-- İzah: hər iki INSERT keçir. Standart UNIQUE NULL-ları müqayisə etmir — NULL heç nəyə,
-- hətta özünə də bərabər deyil, ona görə NULL telefonların sayı məhdudlaşmır.

-- Hər iki NULL-u təkrar sayan variant (PostgreSQL 15+).
-- Cədvəldə artıq iki NULL telefon var, yeni məhdudiyyət onları buraxmayacaq —
-- əvvəlcə birini silirik.
delete
from magaza.musteri
where soyad = 'User2';

alter table musteri
    drop constraint uq_musteri_telefon;

alter table musteri
    add constraint uq_musteri_telefon unique nulls not distinct (telefon);

-- Test: indi ikinci NULL telefon xəta verir.
insert into magaza.musteri (ad, soyad, email, telefon)
values ('Test', 'User3', 'email3@mail.ru', NULL);

-- Xəta mesajı:
-- ERROR: duplicate key value violates unique constraint "uq_musteri_telefon"
-- DETAIL: Key (telefon)=(null) already exists.


-- 9-cu tapşırıq
-- Hər kateqoriyada yalnız bir məhsul aktiv = true ola bilsin. Qeyri-aktiv məhsulların sayı isə məhdudlaşdırılmasın. Adi UNIQUE bunu həll etmir. (2 bal)
-- İpucu: `CREATE UNIQUE INDEX ... ON mehsul(kateqoriya_id) WHERE aktiv`.

-- Səbəb: ALTER TABLE ... ADD CONSTRAINT UNIQUE WHERE bəndi qəbul etmir, sütuna qoyulan
-- tam UNIQUE isə qeyri-aktiv məhsulların sayını da məhdudlaşdırardı.
-- Həll — partial unique index: yalnız aktiv = true sətirləri indeksə düşür.

create unique index uq_mehsul_kateqoriya_aktiv
    on mehsul (kateqoriya_id) where aktiv;

insert into magaza.mehsul (ad, kateqoriya_id, qiymet, aktiv)
values ('Aktiv Mehsul', 1, 200.00, true);

-- Eyni kateqoriyada ikinci aktiv məhsul — xəta verir.
insert into magaza.mehsul (ad, kateqoriya_id, qiymet, aktiv)
values ('Aktiv Mehsul 2', 1, 250.00, true);

-- Xəta mesajı:
-- ERROR: duplicate key value violates unique constraint "uq_mehsul_kateqoriya_aktiv"
-- DETAIL: Key (kateqoriya_id)=(1) already exists.

-- Qeyri-aktiv məhsulların sayı məhdud deyil — bu ikisi keçir.
insert into magaza.mehsul (ad, kateqoriya_id, qiymet, aktiv)
values ('Kohne 1', 1, 10.00, false),
       ('Kohne 2', 1, 20.00, false);


-- =====================================================================
-- C. Dəyər yoxlamaları — CHECK (10–14, 10 bal)
-- =====================================================================

-- 10-cu tapşırıq
-- mehsul cədvəlinə iki adlandırılmış CHECK əlavə edin: qiymet 0-dan böyük, anbarda_say mənfi olmasın. Hər ikisini pozan INSERT yazıb xəta mesajlarını qeyd edin. (2 bal)

alter table magaza.mehsul
    add constraint chk_mehsul_qiymet check (qiymet > 0);
alter table magaza.mehsul
    add constraint chk_mehsul_anbarda_say check (anbarda_say >= 0);

-- Hər iki şərti pozan INSERT.
insert into magaza.mehsul (ad, kateqoriya_id, qiymet, anbarda_say)
values ('Test Mehsul', 1, -10.00, -5);

-- Yalnız qiymet şərtini pozan INSERT.
insert into magaza.mehsul (ad, kateqoriya_id, qiymet, anbarda_say)
values ('Test Mehsul 2', 1, -10.00, 5);

-- Xəta mesajları:
-- 1) ERROR: new row for relation "mehsul" violates check constraint "chk_mehsul_anbarda_say"
--    DETAIL: Failing row contains (8, Test Mehsul, 1, -10.00, -5, null).
--    Qeyd: bu sətir hər iki şərti pozur, amma PostgreSQL yalnız ilk pozulan məhdudiyyəti göstərir.
-- 2) ERROR: new row for relation "mehsul" violates check constraint "chk_mehsul_qiymet"
--    DETAIL: Failing row contains (9, Test Mehsul 2, 1, -10.00, 5, null).


-- 11-ci tapşırıq
-- musteri.email üçün qayda: tərkibində @ və nöqtə olsun, uzunluğu 5 simvoldan çox olsun, boşluq olmasın. (2 bal)
-- İpucu: POSITION, LENGTH və ya LIKE şablonları AND ilə birləşdirilir.

alter table magaza.musteri
    add constraint chk_musteri_email check (
        position('@' in email) > 0 and
        position('.' in email) > 0 and
        length(email) > 5 and
        email not like '% %'
        );


-- 12-ci tapşırıq
-- mehsul cədvəlinə endirimli_qiymet sütunu əlavə edin. Şərt: endirimli qiymət qiymet-dən böyük ola bilməz və mənfi olmamalıdır. (2 bal)
-- İpucu: İki sütunu eyni anda yoxlayan CHECK sütun səviyyəsində yazıla bilməz.

alter table magaza.mehsul
    add column endirimli_qiymet numeric(10, 2);

-- İki sütunu yoxladığı üçün CHECK cədvəl səviyyəsində yazılır və açıq ad alır.
alter table magaza.mehsul
    add constraint chk_mehsul_endirimli_qiymet
        check (endirimli_qiymet >= 0 and endirimli_qiymet <= qiymet);

-- Test: endirimli qiymət qiymətdən böyükdür — xəta verir.
insert into magaza.mehsul (ad, kateqoriya_id, qiymet, endirimli_qiymet)
values ('Endirim Test', 3, 100.00, 150.00);

-- Xəta mesajı:
-- ERROR: new row for relation "mehsul" violates check constraint "chk_mehsul_endirimli_qiymet"

-- 13-cü tapşırıq
-- sifaris cədvəlini yaradın: status yalnız gozleyir, gonderilib, catdirilib, legv dəyərlərindən biri ola bilsin. Əlavə şərt: status legv olduqda legv_sebebi mütləq doldurulsun. (2 bal)
-- İpucu: `CHECK (status <> 'legv' OR legv_sebebi IS NOT NULL)` — şərti implikasiya kimi düşünün.

drop table if exists sifaris;

create table sifaris
(
    id          int generated always as identity,
    musteri_id  int,
    status      varchar(20),
    legv_sebebi varchar(100),
    constraint pk_sifaris primary key (id),
    constraint chk_sifaris_status check (status in ('gozleyir', 'gonderilib', 'catdirilib', 'legv')),
    constraint chk_sifaris_legv check (status <> 'legv' or legv_sebebi is not null)
);

-- 14-cü tapşırıq
-- 12-ci tapşırıqdakı CHECK var, lakin endirimli_qiymet sütununa NULL yazanda sorğu keçir. Səbəbini üç dəyərli məntiqlə (TRUE / FALSE / UNKNOWN) izah edin və NULL-u da bloklayan düzgün həlli yazın. (2 bal)
-- İpucu: CHECK yalnız nəticə açıq-aydın FALSE olduqda sətri rədd edir.

-- Test: endirimli_qiymet NULL olanda sətir keçir.
insert into magaza.mehsul (ad, kateqoriya_id, qiymet, endirimli_qiymet)
values ('Null Endirim', 4, 100.00, NULL);

-- İzah (üç dəyərli məntiq — TRUE / FALSE / UNKNOWN):
-- endirimli_qiymet NULL olanda NULL >= 0 və NULL <= qiymet müqayisələri UNKNOWN qaytarır,
-- UNKNOWN and UNKNOWN da UNKNOWN olur. CHECK sətri yalnız nəticə FALSE olduqda rədd edir,
-- UNKNOWN isə FALSE deyil — ona görə sətir keçir.

-- Həll: CHECK-ə IS NOT NULL şərtini əlavə etmək. Mövcud sətirlərdə NULL var,
-- əvvəlcə onları doldururuq, yoxsa yeni məhdudiyyət tətbiq olunmaz.

update magaza.mehsul
set endirimli_qiymet = 0
where endirimli_qiymet is null;

alter table magaza.mehsul
    drop constraint chk_mehsul_endirimli_qiymet;

alter table magaza.mehsul
    add constraint chk_mehsul_endirimli_qiymet
        check (endirimli_qiymet is not null and
               endirimli_qiymet >= 0 and
               endirimli_qiymet <= qiymet);

-- Test: artıq NULL keçmir.
insert into magaza.mehsul (ad, kateqoriya_id, qiymet, endirimli_qiymet)
values ('Null Endirim 2', 4, 100.00, NULL);

-- Xəta mesajı:
-- ERROR: new row for relation "mehsul" violates check constraint "chk_mehsul_endirimli_qiymet"
-- Alternativ həll: alter column endirimli_qiymet set not null;


-- =====================================================================
-- D. Standart və hesablanan dəyərlər (15–17, 6 bal)
-- =====================================================================

-- 15-ci tapşırıq
-- Standart dəyərləri qurun: musteri.qeydiyyat_tarixi → cari tarix, mehsul.anbarda_say → 0, mehsul.aktiv → true, sifaris.status → 'gozleyir'. (2 bal)

-- Cədvəllər artıq yaradılıb, ona görə mövcud sütunlara DEFAULT təyin olunur.
alter table magaza.musteri
    alter column qeydiyyat_tarixi set default current_date;

alter table magaza.mehsul
    alter column anbarda_say set default 0;

alter table magaza.mehsul
    alter column aktiv set default true;

alter table magaza.sifaris
    alter column status set default 'gozleyir';

-- Yoxlama: DEFAULT dəyərləri information_schema-dan görünür.
select table_name, column_name, column_default
from information_schema.columns
where table_schema = 'magaza'
  and column_default is not null
order by table_name, column_name;

-- 16-cı tapşırıq
-- İki müştəri əlavə edin: birində qeydiyyat_tarixi sütununu ümumiyyətlə yazmayın, digərində isə açıq şəkildə NULL yazın. Nəticələr fərqlidir — səbəbini izah edin. (2 bal)
-- İpucu: DEFAULT yalnız sütun sorğuda iştirak etmədikdə işə düşür.

-- Birinci müştəri: qeydiyyat_tarixi sütunu sorğuda yoxdur.
insert into magaza.musteri (ad, soyad, email, telefon)
values ('Default', 'User', 'email1@mail.ru', '0503334455');

-- İkinci müştəri: qeydiyyat_tarixi sütununa açıq NULL yazılır.
insert into magaza.musteri (ad, soyad, email, telefon, qeydiyyat_tarixi)
values ('Explicit', 'Null', 'email2@mail.ru', '0504445566', NULL);

select ad, soyad, qeydiyyat_tarixi
from magaza.musteri
where soyad in ('User', 'Null');

-- Nəticə: birinci sətirdə qeydiyyat_tarixi cari tarixdir, ikincidə NULL.

-- İzah (fərqin səbəbi): DEFAULT yalnız sütun INSERT-də iştirak etmədikdə işə düşür.
-- Açıq NULL yazdıqda bu, verilmiş dəyər sayılır — baza onu DEFAULT ilə əvəz etmir.


-- 17-ci tapşırıq
-- sifaris_detal cədvəlinə cemi sütunu əlavə edin — dəyəri say * vahid_qiymet kimi avtomatik hesablansın və saxlanılsın. Sonra bu sütuna əl ilə UPDATE etməyə çalışın və nəticəni qeyd edin. (2 bal)
-- İpucu: `GENERATED ALWAYS AS (say * vahid_qiymet) STORED`.

alter table magaza.sifaris_detal
    add column cemi numeric(10, 2) generated always as (say * vahid_qiymet) stored;

-- Test: cemi sütununu yazmırıq, özü hesablanır.
insert into magaza.sifaris_detal (sifaris_id, mehsul_id, say, vahid_qiymet)
values (1, 1, 3, 250.00);

select sifaris_id, mehsul_id, say, vahid_qiymet, cemi
from magaza.sifaris_detal;

-- Əl ilə UPDATE cəhdi.
update magaza.sifaris_detal
set cemi = 1000.00
where sifaris_id = 1;

-- Nəticə: hesablanan sütuna əl ilə dəyər yazmaq mümkün deyil.
-- ERROR: column "cemi" can only be updated to DEFAULT
-- DETAIL: Column "cemi" is a generated column.


-- =====================================================================
-- E. Cədvəllərarası bağlar — FOREIGN KEY (18–20, 6 bal)
-- =====================================================================

-- 18-ci tapşırıq
-- İki adlandırılmış xarici açar qurun: mehsul.kateqoriya_id → kateqoriya.id və sifaris.musteri_id → musteri.id. Mövcud olmayan kateqoriya_id ilə məhsul əlavə etməyə çalışın. (2 bal)


-- 19-cu tapşırıq
-- Üç fərqli silinmə davranışı qurun və hər birini ayrıca test edin: sifaris_detal.sifaris_id → CASCADE, sifaris_detal.mehsul_id → RESTRICT, mehsul.kateqoriya_id → SET NULL. Hər halda valideyn sətri silib nəticəni yazın. (2 bal)

-- Nəticələr:


-- 20-ci tapşırıq
-- musteri cədvəlinə devet_eden_id sütunu əlavə edin — həmin cədvələ istinad etsin (özünə istinad edən açar). Sonra bir-birini dəvət etmiş iki müştərini tək tranzaksiyada əlavə edin. (2 bal)
-- İpucu: Adi FK ilə mümkün deyil — DEFERRABLE INITIALLY DEFERRED lazımdır. Yoxlama COMMIT anına təxirə salınır.


-- =====================================================================
-- F. Məhdudiyyətlərin idarə olunması (21–25, 10 bal)
-- =====================================================================

-- 21-ci tapşırıq
-- mehsul.anbarda_say sütununu məcburi (NOT NULL) edin. Cədvəldə NULL dəyərlər varsa xəta alacaqsınız — əvvəlcə problemli sətirləri tapan sorğu yazın, onları düzəldin, sonra məhdudiyyəti tətbiq edin. (2 bal)


-- 22-ci tapşırıq
-- mehsul üzərindəki qiymət CHECK-ini silin və yerinə yenisini qoyun: qiymət 0-dan böyük və 100 000-dən kiçik olsun. Silmə və əlavə etmə eyni ALTER TABLE ifadəsində yazıla bilərmi? Yoxlayın. (2 bal)

-- Nəticə:


-- 23-cü tapşırıq
-- Vəziyyət: cədvəldə qaydanı pozan köhnə sətirlər var, amma yeni sətirlərə qayda tətbiq olunmalıdır. Məhdudiyyəti mövcud sətirləri yoxlamadan əlavə edin, bir neçə səhv INSERT ilə onun işlədiyini sübut edin, sonra köhnə sətirləri düzəldib məhdudiyyəti təsdiqləyin. (2 bal)
-- İpucu: `ADD CONSTRAINT ... NOT VALID`, sonra `VALIDATE CONSTRAINT`. Fərqi izah edin.

-- İzah (NOT VALID ilə VALIDATE fərqi):


-- 24-cü tapşırıq
-- Kütləvi məlumat yükləməsi üçün bir FK-nın yoxlanışını müvəqqəti dayandırmaq lazımdır. İki fərqli yol yazın və hər birinin risklərini bir cümlə ilə müqayisə edin. (2 bal)
-- İpucu: Birinci yol — məhdudiyyəti silib yükləmədən sonra geri qaytarmaq; ikinci yol — `SET CONSTRAINTS ALL DEFERRED` tranzaksiya daxilində.

-- Risklərin müqayisəsi:


-- 25-ci tapşırıq
-- Audit sorğusu. Sxeminizdəki bütün məhdudiyyətləri bir cədvəldə çıxarın: cədvəl adı, məhdudiyyət adı, tipi (p / u / c / f hərfləri Esas acar, Tekrarsiz, Yoxlama, Xarici acar kimi oxunaqlı yazılsın) və tam tərifi. Cədvəl adına görə sıralansın. (2 bal)
-- İpucu: `pg_constraint` + CASE + `pg_get_constraintdef(oid)` + `conrelid::regclass`.


-- =====================================================================
-- G. İndekslər — əsaslar (26–30, 10 bal)
-- Bütün işlər satis_log üzərində. Hər ölçmədən əvvəl: ANALYZE satis_log;
-- Ölçmə cədvəlləri hesabat.md faylındadır.
-- =====================================================================

-- 26-cı tapşırıq
-- `WHERE mehsul_adi = 'Mehsul 4321'` sorğusunu `EXPLAIN (ANALYZE, BUFFERS)` ilə ölçün, sonra indeks qurub təkrar ölçün. İki cədvəldə müqayisə edin: icra vaxtı, plan növü (Seq Scan / Index Scan) və oxunan blok sayı (shared hit/read). (2 bal)

-- İzah:


-- 27-ci tapşırıq
-- (kateqoriya, tarix) üzrə kompozit indeks qurun və üç sorğunu ayrı-ayrı yoxlayın: (a) yalnız kateqoriya üzrə, (b) yalnız tarix üzrə, (c) hər ikisi üzrə. Hansında indeks işə düşmədi? Kompozit indeksdə sütun sırasının niyə vacib olduğunu izah edin. (2 bal)
-- İpucu: Sol prefiks qaydası (leftmost prefix).

-- İzah (sol prefiks qaydası):


-- 28-ci tapşırıq
-- UNIQUE məhdudiyyət ilə UNIQUE INDEX arasındakı fərqi praktikada göstərin: ikisini də yaradın, pg_constraint və pg_indexes-də axtarın, sonra hər birini DROP CONSTRAINT ilə silməyə çalışın. Nəticəni izah edin. (2 bal)

-- İzah:


-- 29-cu tapşırıq
-- `WHERE status = 'legv'` sorğusu üçün əvvəlcə tam indeks, sonra partial indeks qurun. İkisinin ölçüsünü və sorğu sürətini müqayisə edin. Partial indeks hansı halda məqsədəuyğundur? (2 bal)
-- İpucu: `pg_size_pretty(pg_relation_size('idx_ad'))`. Məlumatın 99%-i 'tamam'-dır.

-- İzah (partial indeks nə vaxt məqsədəuyğundur):


-- 30-cu tapşırıq
-- `WHERE UPPER(mehsul_adi) = 'MEHSUL 100'` sorğusu 26-cı tapşırıqda qurduğunuz indeksdən istifadə etmir. Səbəbini izah edin və iki fərqli həll yazın, ikisini də ölçün. (2 bal)
-- İpucu: Sütunun üzərinə funksiya tətbiq olunduqda indeks açarı ilə uyğunluq itir. Həll yollarından biri — ifadə üzrə indeks.

-- İzah:


-- =====================================================================
-- H. Çətin və qarışıq tapşırıqlar (31–40, 40 bal)
-- =====================================================================

-- 31-ci tapşırıq
-- `SELECT seher, tarix FROM satis_log WHERE seher = 'Gəncə'` sorğusunu Index Only Scan ilə işlətməyə nail olun. Planda Heap Fetches sətrini tapın, dəyərini sıfıra endirin və bunun nə demək olduğunu izah edin. (4 bal)
-- İpucu: INCLUDE bəndi və ya kompozit indeks; sonra `VACUUM satis_log;` — görünürlük xəritəsi yenilənməlidir.

-- İzah:


-- 32-ci tapşırıq
-- `ORDER BY mebleg DESC LIMIT 20` sorğusunda sıralamanın indeks hesabına aparıldığını sübut edin — planda Sort düyünü olmamalıdır. Sonra eyni nəticəni `ORDER BY mebleg DESC NULLS LAST` üçün əldə edin. (4 bal)
-- İpucu: İndeks öz sıralama qaydası ilə yaradılır: `CREATE INDEX ... (mebleg DESC NULLS LAST)`.

-- İzah:


-- 33-cü tapşırıq
-- Hesabat sorğusu: satis_log-un bütün indekslərini ölçüsü, ölçünün cədvələ nisbəti (faizlə, 1 rəqəm) və indeksin tərifi ilə birlikdə sadalayın. Ölçüyə görə azalan sıra. Ən «bahalı» indeks hansıdır? (4 bal)
-- İpucu: `pg_indexes` + `pg_relation_size()` + `pg_size_pretty()`.

-- Ən «bahalı» indeks:


-- 34-cü tapşırıq
-- Heç vaxt istifadə olunmayan indeksləri aşkarlayın: əvvəlcə statistikanı sıfırlayın, sonra 5–6 müxtəlif SELECT icra edin, sonra hər indeks üçün skan sayını göstərən hesabat çıxarın və `idx_scan = 0` olanları işarələyin. (4 bal)
-- İpucu: `SELECT pg_stat_reset();` + `pg_stat_user_indexes`.

-- İzah:


-- 35-ci tapşırıq
-- `WHERE mehsul_adi LIKE '%hsul 4321%'` sorğusunu sürətləndirin. Adi B-tree indeks burada kömək etmir — səbəbini izah edin və işləyən həlli qurub ölçün. (4 bal)
-- İpucu: `CREATE EXTENSION pg_trgm;` + `USING GIN (mehsul_adi gin_trgm_ops)`.

-- İzah:


-- 36-cı tapşırıq
-- İndeksin yazma əməliyyatına qiymətini ölçün: (a) bütün indeksləri silin, 100 000 sətir INSERT edin, vaxtı qeyd edin; (b) 5 indeks qurun, əlavə etdiyiniz sətirləri silin, eyni INSERT-i təkrarlayın. Fərqi faizlə göstərin və bir cümləlik nəticə yazın. (4 bal)

-- Fərq (%) və nəticə:


-- 37-ci tapşırıq
-- satis_log-a PRIMARY KEY əlavə edin, sonra ona FK ilə bağlı satis_qeyd cədvəli yaradıb 200 000 sətir doldurun. FK sütununa indeks qurmadan valideyn cədvəldən sətir silin və vaxtı ölçün; sonra indeks qurub təkrarlayın. PostgreSQL FK sütununa avtomatik indeks yaradırmı? (4 bal)
-- İpucu: Valideyn sətri silinəndə baza uşaq cədvəldə istinadları axtarmalıdır — indeks yoxdursa bu, tam skan deməkdir.

-- İzah (PostgreSQL FK sütununa avtomatik indeks yaradırmı):


-- 38-ci tapşırıq
-- `WHERE kateqoriya = 'Ofis' AND seher = 'Bakı'` sorğusunu iki konfiqurasiyada müqayisə edin: (a) iki ayrı bir-sütunlu indeks, (b) bir kompozit indeks (kateqoriya, seher). Planda BitmapAnd görünürmü? Hansı variant daha sürətlidir və niyə? (4 bal)

-- İzah (BitmapAnd, hansı daha sürətli və niyə):


-- 39-cu tapşırıq
-- İndeksin ölçüsünü qeyd edin, sonra cədvəlin təxminən 40%-ni UPDATE edin və ölçüyə yenidən baxın. Ölçü niyə artdı? n_dead_tup dəyərini göstərin, REINDEX icra edib fərqi cədvəldə təqdim edin. (4 bal)
-- İpucu: Şişmə (bloat), MVCC və ölü sətirlər. `pg_stat_user_tables`.

-- İzah (bloat, MVCC, ölü sətirlər):


-- 40-cı tapşırıq
-- Yekun audit sorğusu. Sxeminizdəki hər cədvəl üçün bir sətir: cədvəl adı, təxmini sətir sayı, cədvəl ölçüsü, indeks sayı, indekslərin ümumi ölçüsü, PRIMARY KEY-in olub-olmaması (Var / Yoxdur) və status — PK yoxdursa Problemli, indekslərin ölçüsü cədvəlin 50%-dən çoxdursa Nezaret lazimdir, qalanı Normal. Cədvəl ölçüsünə görə azalan sıra. (4 bal)
-- İpucu: `pg_class` / `pg_stat_user_tables` + alt-sorğular + CASE + `pg_total_relation_size`. Məhdudiyyət, indeks, ölçü və şərti ifadələr — hamısı bir sorğuda.

