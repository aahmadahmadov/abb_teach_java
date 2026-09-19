-- =====================================================================
-- SQL məhdudiyyətləri və indekslər: praktik tapşırıq (40 tapşırıq)
-- Ad, Soyad: Ahmad Ahmadov
-- Qrup: -
-- Tarix: 2026-09-15
-- Mühit: PostgreSQL 15+
-- Qeyd: Ölçmə nəticələri və izahlar hesabat.md faylındadır.
-- Qeyd: Məhdudiyyəti qəsdən pozan (test məqsədli) INSERT/UPDATE/DELETE ifadələri şərhə
-- alınıb və aldıqları xəta mesajı altlarında yazılıb. Belə olanda fayl əvvəldən sonuna
-- qədər xətasız icra olunur. Onları yoxlamaq üçün şərhdən çıxarmaq kifayətdir.
-- =====================================================================


-- =====================================================================
-- Hazırlıq 1: iş sahəsi
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS magaza;
SET search_path TO magaza, public;
-- İcra vaxtını görmək üçün (yalnız psql-də işləyir, IDE-də xəta verir):
-- \timing on
-- Planları oxunaqlı saxlamaq üçün paralelliyi söndürün:
SET max_parallel_workers_per_gather = 0;


-- =====================================================================
-- Hazırlıq 2: indeks bölmələri üçün cədvəl (G və H bölmələri)
-- =====================================================================

DROP TABLE IF EXISTS magaza.satis_qeyd;
DROP TABLE IF EXISTS magaza.satis_log;
CREATE TABLE magaza.satis_log
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

INSERT INTO magaza.satis_log
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
ANALYZE magaza.satis_log;


-- =====================================================================
-- A. Cədvəl açarları və NOT NULL (1–5, 10 bal)
-- =====================================================================

-- 1-ci tapşırıq
-- kateqoriya cədvəlini yaradın: id — avtomatik artan və cədvəlin açarı, ad — VARCHAR(50), boş ola bilməz. Açara açıq ad verin: pk_kateqoriya. (2 bal)
-- İpucu: `GENERATED ALWAYS AS IDENTITY` və `CONSTRAINT pk_kateqoriya PRIMARY KEY (id)`.

DROP TABLE IF EXISTS magaza.kateqoriya CASCADE;

CREATE TABLE magaza.kateqoriya
(
    id INT GENERATED ALWAYS AS IDENTITY,
    ad VARCHAR(50) NOT NULL,
    CONSTRAINT pk_kateqoriya PRIMARY KEY (id)
);

SELECT conname, contype
FROM pg_constraint
WHERE conrelid = 'kateqoriya'::regclass;

INSERT INTO magaza.kateqoriya (ad)
VALUES ('Texnika'),
       ('Aksesuar');

SELECT *
FROM magaza.kateqoriya;

-- Test: ad NOT NULL olduğu üçün bu sətir xəta verir.
-- INSERT INTO magaza.kateqoriya (ad) VALUES (NULL);
-- ERROR: null value in column "ad" of relation "kateqoriya" violates not-null constraint


-- 2-ci tapşırıq
-- mehsul cədvəlini yaradın: id (açar), ad, kateqoriya_id, qiymet NUMERIC(10,2), anbarda_say INT, aktiv BOOLEAN. ad və qiymet boş ola bilməz. Hələlik yalnız PRIMARY KEY və NOT NULL yazın. (2 bal)

drop table if exists magaza.mehsul cascade;

create table magaza.mehsul
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

drop table if exists magaza.musteri cascade;

create table magaza.musteri
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

drop table if exists magaza.sifaris_detal;

create table magaza.sifaris_detal
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
-- indeks yaradır, yəni indeks məhdudiyyətin icra mexanizmidir. Ona görə CREATE INDEX
-- yazmasam da nəticədə 4 PK indeksi görünür.
-- Fərq: UNIQUE + NOT NULL eyni yoxlamanı verir, amma PK cədvəlin əsas açarıdır:
-- cədvəldə yalnız bir PK ola bilər və xarici açarlar ona istinad edir.


-- =====================================================================
-- B. Təkrarsızlıq — UNIQUE (6–9, 8 bal)
-- =====================================================================

-- 6-cı tapşırıq
-- musteri.email təkrarlanmasın. Cədvəli yenidən yaratmadan, ALTER TABLE ilə adlandırılmış UNIQUE məhdudiyyət əlavə edin. Sonra eyni email ilə ikinci müştəri yazmağa çalışın və xəta mesajını qeyd edin. (2 bal)

alter table magaza.musteri
    add constraint uq_musteri_email unique (email);

insert into magaza.musteri (ad, soyad, email, telefon)
values ('Ali', 'Aliyev', 'ali@mail.ru', '0501112233');

-- Test: eyni email ilə ikinci müştəri xəta verir.
-- insert into magaza.musteri (ad, soyad, email, telefon)
-- values ('Vali', 'Valiyev', 'ali@mail.ru', '0502223344');

-- Xəta mesajı:
-- ERROR: duplicate key value violates unique constraint "uq_musteri_email"
-- DETAIL: Key (email)=(ali@mail.ru) already exists.


-- 7-ci tapşırıq
-- mehsul cədvəlində eyni kateqoriyada eyni adlı iki məhsul olmasın, lakin fərqli kateqoriyalarda eyni ad işlənə bilsin. Məhdudiyyəti qurun və hər iki halı test edin. (2 bal)
-- İpucu: Tək sütuna deyil, sütun cütünə qoyulan UNIQUE.

alter table magaza.mehsul
    add constraint uq_mehsul_kateqoriya_ad unique (kateqoriya_id, ad);

insert into magaza.mehsul (ad, kateqoriya_id, qiymet)
values ('Yeni Mehsul', 1, 100.00);

-- 1-ci hal: eyni kateqoriyada eyni ad xəta verir.
-- insert into magaza.mehsul (ad, kateqoriya_id, qiymet)
-- values ('Yeni Mehsul', 1, 150.00);

-- Xəta mesajı:
-- ERROR: duplicate key value violates unique constraint "uq_mehsul_kateqoriya_ad"
-- DETAIL: Key (kateqoriya_id, ad)=(1, Yeni Mehsul) already exists.

-- 2-ci hal: fərqli kateqoriyada eyni ad keçir.
insert into magaza.mehsul (ad, kateqoriya_id, qiymet)
values ('Yeni Mehsul', 2, 120.00);

-- 8-ci tapşırıq
-- musteri.telefon sütununa UNIQUE qoyun, sonra telefonu NULL olan iki müştəri əlavə edin. Sorğu keçirmi? Nəticəni izah edin və hər iki NULL-u da təkrar sayan variantı yazın. (2 bal)
-- İpucu: Standart davranışda NULL heç nəyə bərabər deyil, hətta özünə də. PostgreSQL 15+ üçün: `UNIQUE NULLS NOT DISTINCT`.

alter table magaza.musteri
    add constraint uq_musteri_telefon unique (telefon);

insert into magaza.musteri (ad, soyad, email, telefon)
values ('Test', 'User1', 'email@mail.ru', NULL);

insert into magaza.musteri (ad, soyad, email, telefon)
values ('Test', 'User2', 'email2@mail.ru', NULL);

-- İzah: hər iki INSERT keçir. Standart UNIQUE NULL-ları müqayisə etmir: NULL heç nəyə,
-- hətta özünə də bərabər deyil, ona görə NULL telefonların sayı məhdudlaşmır.

-- Hər iki NULL-u təkrar sayan variant (PostgreSQL 15+).
-- Cədvəldə artıq iki NULL telefon var, yeni məhdudiyyət onları buraxmayacaq, ona görə
-- əvvəlcə birini silirik.
delete
from magaza.musteri
where soyad = 'User2';

alter table magaza.musteri
    drop constraint uq_musteri_telefon;

alter table magaza.musteri
    add constraint uq_musteri_telefon unique nulls not distinct (telefon);

-- Test: indi ikinci NULL telefon xəta verir.
-- insert into magaza.musteri (ad, soyad, email, telefon)
-- values ('Test', 'User3', 'email3@mail.ru', NULL);

-- Xəta mesajı:
-- ERROR: duplicate key value violates unique constraint "uq_musteri_telefon"
-- DETAIL: Key (telefon)=(null) already exists.


-- 9-cu tapşırıq
-- Hər kateqoriyada yalnız bir məhsul aktiv = true ola bilsin. Qeyri-aktiv məhsulların sayı isə məhdudlaşdırılmasın. Adi UNIQUE bunu həll etmir. (2 bal)
-- İpucu: `CREATE UNIQUE INDEX ... ON mehsul(kateqoriya_id) WHERE aktiv`.

-- Səbəb: ALTER TABLE ... ADD CONSTRAINT UNIQUE WHERE bəndi qəbul etmir, sütuna qoyulan
-- tam UNIQUE isə qeyri-aktiv məhsulların sayını da məhdudlaşdırardı.
-- Həll partial unique index-dir: yalnız aktiv = true sətirləri indeksə düşür.

create unique index uq_mehsul_kateqoriya_aktiv
    on mehsul (kateqoriya_id) where aktiv;

insert into magaza.mehsul (ad, kateqoriya_id, qiymet, aktiv)
values ('Aktiv Mehsul', 1, 200.00, true);

-- Eyni kateqoriyada ikinci aktiv məhsul xəta verir.
-- insert into magaza.mehsul (ad, kateqoriya_id, qiymet, aktiv)
-- values ('Aktiv Mehsul 2', 1, 250.00, true);

-- Xəta mesajı:
-- ERROR: duplicate key value violates unique constraint "uq_mehsul_kateqoriya_aktiv"
-- DETAIL: Key (kateqoriya_id)=(1) already exists.

-- Qeyri-aktiv məhsulların sayı məhdud deyil, bu ikisi keçir.
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
-- insert into magaza.mehsul (ad, kateqoriya_id, qiymet, anbarda_say)
-- values ('Test Mehsul', 1, -10.00, -5);

-- Yalnız qiymet şərtini pozan INSERT.
-- insert into magaza.mehsul (ad, kateqoriya_id, qiymet, anbarda_say)
-- values ('Test Mehsul 2', 1, -10.00, 5);

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

-- Test: endirimli qiymət qiymətdən böyükdür, ona görə xəta verir.
-- insert into magaza.mehsul (ad, kateqoriya_id, qiymet, endirimli_qiymet)
-- values ('Endirim Test', 2, 100.00, 150.00);

-- Xəta mesajı:
-- ERROR: new row for relation "mehsul" violates check constraint "chk_mehsul_endirimli_qiymet"

-- 13-cü tapşırıq
-- sifaris cədvəlini yaradın: status yalnız gozleyir, gonderilib, catdirilib, legv dəyərlərindən biri ola bilsin. Əlavə şərt: status legv olduqda legv_sebebi mütləq doldurulsun. (2 bal)
-- İpucu: `CHECK (status <> 'legv' OR legv_sebebi IS NOT NULL)` — şərti implikasiya kimi düşünün.

drop table if exists magaza.sifaris;

create table magaza.sifaris
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
values ('Null Endirim', 2, 100.00, NULL);

-- İzah (üç dəyərli məntiq: TRUE / FALSE / UNKNOWN):
-- endirimli_qiymet NULL olanda NULL >= 0 və NULL <= qiymet müqayisələri UNKNOWN qaytarır,
-- UNKNOWN and UNKNOWN da UNKNOWN olur. CHECK sətri yalnız nəticə FALSE olduqda rədd edir,
-- UNKNOWN isə FALSE deyil, ona görə sətir keçir.

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
-- insert into magaza.mehsul (ad, kateqoriya_id, qiymet, endirimli_qiymet)
-- values ('Null Endirim 2', 2, 100.00, NULL);

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
-- Açıq NULL yazdıqda bu, verilmiş dəyər sayılır və baza onu DEFAULT ilə əvəz etmir.


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
-- update magaza.sifaris_detal
-- set cemi = 1000.00
-- where sifaris_id = 1;

-- Nəticə: hesablanan sütuna əl ilə dəyər yazmaq mümkün deyil.
-- ERROR: column "cemi" can only be updated to DEFAULT
-- DETAIL: Column "cemi" is a generated column.


-- =====================================================================
-- E. Cədvəllərarası bağlar — FOREIGN KEY (18–20, 6 bal)
-- =====================================================================

-- 18-ci tapşırıq
-- İki adlandırılmış xarici açar qurun: mehsul.kateqoriya_id → kateqoriya.id və sifaris.musteri_id → musteri.id. Mövcud olmayan kateqoriya_id ilə məhsul əlavə etməyə çalışın. (2 bal)

alter table magaza.mehsul
    add constraint fk_mehsul_kateqoriya foreign key (kateqoriya_id) references magaza.kateqoriya (id);

alter table magaza.sifaris
    add constraint fk_sifaris_musteri foreign key (musteri_id) references magaza.musteri (id);

-- Mövcud olmayan kateqoriya_id ilə məhsul.
-- endirimli_qiymet 14-cü tapşırıqdan sonra NULL qəbul etmir, ona görə dəyər veririk,
-- yoxsa sətir FK-ya çatmadan CHECK-də dayanar.
-- insert into magaza.mehsul (ad, kateqoriya_id, qiymet, endirimli_qiymet)
-- values ('Invalid Kateqoriya', 999, 100.00, 50.00);

-- Mövcud olmayan musteri_id ilə sifariş.
-- insert into magaza.sifaris (musteri_id, status)
-- values (999, 'gozleyir');

-- Xəta mesajları:
-- 1) ERROR: insert or update on table "mehsul" violates foreign key constraint "fk_mehsul_kateqoriya"
--    DETAIL: Key (kateqoriya_id)=(999) is not present in table "kateqoriya".
-- 2) ERROR: insert or update on table "sifaris" violates foreign key constraint "fk_sifaris_musteri"
--    DETAIL: Key (musteri_id)=(999) is not present in table "musteri".

-- 19-cu tapşırıq
-- Üç fərqli silinmə davranışı qurun və hər birini ayrıca test edin: sifaris_detal.sifaris_id → CASCADE, sifaris_detal.mehsul_id → RESTRICT, mehsul.kateqoriya_id → SET NULL. Hər halda valideyn sətri silib nəticəni yazın. (2 bal)

-- 17-ci tapşırıqda əlavə edilmiş detal sətri valideynsizdir (o vaxt FK yox idi),
-- FK qurmaq üçün əvvəlcə onu silirik.
delete
from magaza.sifaris_detal
where sifaris_id = 1
  and mehsul_id = 1;

alter table magaza.sifaris_detal
    add constraint fk_sifaris_detal_sifaris foreign key (sifaris_id) references magaza.sifaris (id) on delete cascade;

alter table magaza.sifaris_detal
    add constraint fk_sifaris_detal_mehsul foreign key (mehsul_id) references magaza.mehsul (id) on delete restrict;

-- fk_mehsul_kateqoriya 18-ci tapşırıqda yaradılıb, eyni adla ikinci dəfə yaratmaq olmaz, ona görə
-- əvvəlcə silib, sonra SET NULL davranışı ilə yenidən qururuq.
alter table magaza.mehsul
    drop constraint fk_mehsul_kateqoriya;

alter table magaza.mehsul
    add constraint fk_mehsul_kateqoriya foreign key (kateqoriya_id) references magaza.kateqoriya (id) on delete set null;


-- (a) CASCADE testi: sifariş silinəndə onun detalları da silinir.
insert into magaza.sifaris (musteri_id, status)
values ((select id from magaza.musteri where email = 'ali@mail.ru'), 'gozleyir');

insert into magaza.sifaris_detal (sifaris_id, mehsul_id, say, vahid_qiymet)
values ((select max(id) from magaza.sifaris),
        (select id from magaza.mehsul where ad = 'Yeni Mehsul' and kateqoriya_id = 1),
        2, 100.00);

delete
from magaza.sifaris
where id = (select max(id) from magaza.sifaris);

-- Detal sətri də silindi: nəticə 0.
select count(*) as qalan_detal
from magaza.sifaris_detal;


-- (b) RESTRICT testi: detalı olan məhsulu silmək mümkün deyil.
insert into magaza.sifaris (musteri_id, status)
values ((select id from magaza.musteri where email = 'ali@mail.ru'), 'gozleyir');

insert into magaza.sifaris_detal (sifaris_id, mehsul_id, say, vahid_qiymet)
values ((select max(id) from magaza.sifaris),
        (select id from magaza.mehsul where ad = 'Yeni Mehsul' and kateqoriya_id = 1),
        2, 100.00);

-- delete
-- from magaza.mehsul
-- where ad = 'Yeni Mehsul'
--   and kateqoriya_id = 1;


-- (c) SET NULL testi: kateqoriya silinəndə məhsulun kateqoriya_id-si NULL olur.
delete
from magaza.kateqoriya
where ad = 'Aksesuar';

select ad, kateqoriya_id
from magaza.mehsul
where kateqoriya_id is null;


-- Nəticələr:
-- (a) CASCADE: valideyn sifariş silindi, sifaris_detal sətri avtomatik silindi (qalan_detal = 0).
-- (b) RESTRICT: silinmə baş tutmadı.
--     ERROR: update or delete on table "mehsul" violates foreign key constraint
--            "fk_sifaris_detal_mehsul" on table "sifaris_detal"
--     DETAIL: Key (id)=(1) is still referenced from table "sifaris_detal".
-- (c) SET NULL: kateqoriya sətri silindi, ona istinad edən məhsulların kateqoriya_id-si NULL oldu.


-- 20-ci tapşırıq
-- musteri cədvəlinə devet_eden_id sütunu əlavə edin — həmin cədvələ istinad etsin (özünə istinad edən açar). Sonra bir-birini dəvət etmiş iki müştərini tək tranzaksiyada əlavə edin. (2 bal)
-- İpucu: Adi FK ilə mümkün deyil — DEFERRABLE INITIALLY DEFERRED lazımdır. Yoxlama COMMIT anına təxirə salınır.

alter table magaza.musteri
    add column devet_eden_id int;

-- Özünə istinad edən FK. Yoxlama COMMIT anına təxirə salınır.
alter table magaza.musteri
    add constraint fk_musteri_devet_eden foreign key (devet_eden_id) references magaza.musteri (id)
        deferrable initially deferred;

-- Bir-birini dəvət etmiş iki müştəri tək tranzaksiyada.
-- id GENERATED ALWAYS olduğu üçün açıq id yazmaq üçün OVERRIDING SYSTEM VALUE lazımdır.
begin;

insert into magaza.musteri (id, ad, soyad, email, telefon, devet_eden_id)
    overriding system value
values (9001, 'Aygun', 'Mammadova', 'aygun@mail.ru', '0505556677', 9002);

insert into magaza.musteri (id, ad, soyad, email, telefon, devet_eden_id)
    overriding system value
values (9002, 'Kamran', 'Hesenov', 'kamran@mail.ru', '0506667788', 9001);

commit;

select id, ad, devet_eden_id
from magaza.musteri
where id in (9001, 9002);

-- Nəticə: birinci INSERT hələ mövcud olmayan 9002-yə istinad edir, amma FK DEFERRABLE
-- INITIALLY DEFERRED olduğu üçün yoxlama COMMIT anında aparılır və hər iki sətir keçir.
-- Adi FK-da birinci INSERT dərhal xəta verərdi.

-- =====================================================================
-- F. Məhdudiyyətlərin idarə olunması (21–25, 10 bal)
-- =====================================================================

-- 21-ci tapşırıq
-- mehsul.anbarda_say sütununu məcburi (NOT NULL) edin. Cədvəldə NULL dəyərlər varsa xəta alacaqsınız — əvvəlcə problemli sətirləri tapan sorğu yazın, onları düzəldin, sonra məhdudiyyəti tətbiq edin. (2 bal)

-- Əvvəlcə problemli sətirləri tapırıq.
select id, ad, anbarda_say
from magaza.mehsul
where anbarda_say is null;

-- Düzəliş: NULL yerinə 0 yazırıq (15-ci tapşırıqdakı default da 0-dır,
-- amma default yalnız yeni sətirlərə işləyir, köhnələrə yox).
update magaza.mehsul
set anbarda_say = 0
where anbarda_say is null;

alter table magaza.mehsul
    alter column anbarda_say set not null;

-- Sütun səviyyəsindəki NOT NULL-a ad vermək mümkün deyil, ona görə eyni qaydanı
-- adlandırılmış CHECK kimi də yazırıq.
alter table magaza.mehsul
    add constraint chk_mehsul_anbarda_say_not_null check (anbarda_say is not null);

-- 22-ci tapşırıq
-- mehsul üzərindəki qiymət CHECK-ini silin və yerinə yenisini qoyun: qiymət 0-dan böyük və 100 000-dən kiçik olsun. Silmə və əlavə etmə eyni ALTER TABLE ifadəsində yazıla bilərmi? Yoxlayın. (2 bal)

alter table magaza.mehsul
    drop constraint chk_mehsul_qiymet,
    add constraint chk_mehsul_qiymet check (qiymet > 0 and qiymet < 100000);

-- Nəticə: bəli, DROP və ADD eyni ALTER TABLE ifadəsində vergüllə yazıla bilər.
-- Hər ikisi tək əmr kimi, tək tranzaksiyada icra olunur, aralıqda cədvəl
-- məhdudiyyətsiz qalmır.


-- 23-cü tapşırıq
-- Vəziyyət: cədvəldə qaydanı pozan köhnə sətirlər var, amma yeni sətirlərə qayda tətbiq olunmalıdır. Məhdudiyyəti mövcud sətirləri yoxlamadan əlavə edin, bir neçə səhv INSERT ilə onun işlədiyini sübut edin, sonra köhnə sətirləri düzəldib məhdudiyyəti təsdiqləyin. (2 bal)
-- İpucu: `ADD CONSTRAINT ... NOT VALID`, sonra `VALIDATE CONSTRAINT`. Fərqi izah edin.

-- Qaydanı pozan köhnə sətir. Minimum qiymət qaydası hələ yoxdur, ona görə keçir.
-- aktiv = false yazırıq, çünki 9-cu tapşırıqdakı partial unique index kateqoriyada
-- yalnız bir aktiv məhsula icazə verir.
insert into magaza.mehsul (ad, kateqoriya_id, qiymet, endirimli_qiymet, anbarda_say, aktiv)
values ('Kohne Ucuz Mehsul', 1, 0.50, 0.50, 5, false);

-- Məhdudiyyət NOT VALID ilə əlavə olunur, mövcud sətirlər yoxlanmır, ona görə
-- yuxarıdakı səhv sətrə baxmayaraq əmr uğurla keçir.
alter table magaza.mehsul
    add constraint chk_mehsul_min_qiymet check (qiymet >= 1) not valid;

-- Test: yeni sətirlərə qayda işləyir, bu iki INSERT xəta verir.
-- insert into magaza.mehsul (ad, kateqoriya_id, qiymet, endirimli_qiymet, anbarda_say, aktiv)
-- values ('Yeni Ucuz Mehsul', 1, 0.80, 0.80, 5, false);

-- insert into magaza.mehsul (ad, kateqoriya_id, qiymet, endirimli_qiymet, anbarda_say, aktiv)
-- values ('Yeni Pulsuz Mehsul', 1, 0.10, 0.10, 5, false);

-- Xəta mesajı:
-- ERROR: new row for relation "mehsul" violates check constraint "chk_mehsul_min_qiymet"
-- DETAIL: Failing row contains (..., 0.80, ...).

-- İndi köhnə sətri düzəldirik və məhdudiyyəti təsdiqləyirik.
update magaza.mehsul
set qiymet           = 1.00,
    endirimli_qiymet = 1.00
where ad = 'Kohne Ucuz Mehsul';

alter table magaza.mehsul
    validate constraint chk_mehsul_min_qiymet;

-- İzah (NOT VALID ilə VALIDATE fərqi):
-- NOT VALID məhdudiyyəti mövcud sətirləri yoxlamadan əlavə edir: yalnız yeni INSERT
-- və UPDATE-lərə tətbiq olunur, ona görə cədvəl uzun müddət kilidlənmir.
-- VALIDATE CONSTRAINT isə sonradan mövcud sətirləri bir dəfə yoxlayır; hamısı qaydaya
-- uyğundursa məhdudiyyət tam etibarlı olur (pg_constraint.convalidated = true).
-- Səhv sətir qalıbsa VALIDATE xəta verir və məhdudiyyət NOT VALID vəziyyətində qalır.


-- 24-cü tapşırıq
-- Kütləvi məlumat yükləməsi üçün bir FK-nın yoxlanışını müvəqqəti dayandırmaq lazımdır. İki fərqli yol yazın və hər birinin risklərini bir cümlə ilə müqayisə edin. (2 bal)
-- İpucu: Birinci yol — məhdudiyyəti silib yükləmədәn sonra geri qaytarmaq; ikinci yol — `SET CONSTRAINTS ALL DEFERRED` tranzaksiya daxilindә.

-- 1-ci yol: FK-nı silmək, yükləməni etmək, sonra geri qaytarmaq.
alter table magaza.sifaris
    drop constraint fk_sifaris_musteri;

-- ... burada kütləvi yükləmə gedir ...
insert into magaza.sifaris (musteri_id, status)
values ((select id from magaza.musteri where email = 'ali@mail.ru'), 'gozleyir');

alter table magaza.sifaris
    add constraint fk_sifaris_musteri foreign key (musteri_id) references magaza.musteri (id);

-- 2-ci yol: FK DEFERRABLE olmalıdır, yoxlama tranzaksiyanın sonuna keçir.
-- 20-ci tapşırıqdakı fk_musteri_devet_eden məhz belə qurulub.
begin;

set constraints all deferred;

insert into magaza.musteri (id, ad, soyad, email, telefon, devet_eden_id)
    overriding system value
values (9003, 'Nigar', 'Aliyeva', 'nigar@mail.ru', '0507778899', 9004);

insert into magaza.musteri (id, ad, soyad, email, telefon, devet_eden_id)
    overriding system value
values (9004, 'Rauf', 'Quliyev', 'rauf@mail.ru', '0508889900', 9003);

commit;

-- Risklərin müqayisəsi:
-- 1-ci yolda yükləmə vaxtı FK ümumiyyətlə yoxdur, ona görə istənilən səhv sətir cədvələ
--   düşür; geri qaytaranda baza bütün cədvəli yenidən yoxlayır və bir səhv sətir tapılsa
--   ADD CONSTRAINT xəta verir, cədvəl isə FK-sız qalır.
-- 2-ci yolda yoxlama itmir, sadəcə COMMIT anına təxirə salınır: səhv sətir varsa bütün
--   tranzaksiya geri qayıdır, yəni məlumat təhlükəsizdir, amma yükləmə tək tranzaksiyada
--   getdiyi üçün kilidlər uzun saxlanılır və WAL həcmi böyüyür.


-- 25-ci tapşırıq
-- Audit sorğusu. Sxeminizdəki bütün məhdudiyyətləri bir cədvəldə çıxarın: cədvəl adı, məhdudiyyət adı, tipi (p / u / c / f hərfləri Esas acar, Tekrarsiz, Yoxlama, Xarici acar kimi oxunaqlı yazılsın) və tam tərifi. Cədvəl adına görə sıralansın. (2 bal)
-- İpucu: `pg_constraint` + CASE + `pg_get_constraintdef(oid)` + `conrelid::regclass`.

SELECT conrelid::regclass        AS table_name,
       conname                   AS constraint_name,
       CASE contype
           WHEN 'p' THEN 'Əsas açar'
           WHEN 'u' THEN 'Təkrarsız'
           WHEN 'c' THEN 'Yoxlama'
           WHEN 'f' THEN 'Xarici açar'
           END                   AS constraint_type,
       pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE connamespace = 'magaza'::regnamespace
ORDER BY conrelid::regclass, conname;

-- =====================================================================
-- G. İndekslər — əsaslar (26–30, 10 bal)
-- Bütün işlər satis_log üzərində. Hər ölçmədən əvvəl: ANALYZE magaza.satis_log;
-- Ölçmə cədvəlləri hesabat.md faylındadır.
-- =====================================================================

-- 26-cı tapşırıq
-- `WHERE mehsul_adi = 'Mehsul 4321'` sorğusunu `EXPLAIN (ANALYZE, BUFFERS)` ilə ölçün, sonra indeks qurub təkrar ölçün. İki cədvəldə müqayisə edin: icra vaxtı, plan növü (Seq Scan / Index Scan) və oxunan blok sayı (shared hit/read). (2 bal)

analyze magaza.satis_log;

-- İndekssiz ölçmə.
explain (analyze, buffers)
select *
from magaza.satis_log
where mehsul_adi = 'Mehsul 4321';

create index idx_satis_log_mehsul_adi on magaza.satis_log (mehsul_adi);

analyze magaza.satis_log;

-- İndekslə ölçmə.
explain (analyze, buffers)
select *
from magaza.satis_log
where mehsul_adi = 'Mehsul 4321';

-- İzah: indekssiz plan Seq Scan-dır, 300 000 sətrin hamısı oxunur (shared hit=3258),
-- filtrdən sonra 60 sətir qalır, icra 16.5 ms. İndeksdən sonra plan Bitmap Index Scan +
-- Bitmap Heap Scan olur, cəmi 63 blok oxunur (hit=60, read=3) və icra 0.08 ms-ə düşür.
-- Təqribən 200 dəfə sürətlidir. Ölçmə cədvəli hesabat.md-dədir.


-- 27-ci tapşırıq
-- (kateqoriya, tarix) üzrə kompozit indeks qurun və üç sorğunu ayrı-ayrı yoxlayın: (a) yalnız kateqoriya üzrə, (b) yalnız tarix üzrə, (c) hər ikisi üzrə. Hansında indeks işə düşmədi? Kompozit indeksdə sütun sırasının niyə vacib olduğunu izah edin. (2 bal)
-- İpucu: Sol prefiks qaydası (leftmost prefix).

create index idx_satis_log_kateqoriya_tarix on magaza.satis_log (kateqoriya, tarix);

analyze magaza.satis_log;

-- (a) yalnız kateqoriya üzrə, indeksin sol sütunu.
explain (analyze, buffers)
select *
from magaza.satis_log
where kateqoriya = 'Texnika';

-- (b) yalnız tarix üzrə, indeksin ikinci sütunu.
explain (analyze, buffers)
select *
from magaza.satis_log
where tarix = date '2022-05-01';

-- (c) hər iki sütun üzrə.
explain (analyze, buffers)
select *
from magaza.satis_log
where kateqoriya = 'Texnika'
  and tarix = date '2022-05-01';

-- İzah (sol prefiks qaydası): (a) və (c) indeksi düzgün işlədir: Index Cond-da indeksin
-- sol sütunu (kateqoriya) var, Bitmap Index Scan qiyməti 663 və 5.02-dir.
-- (b)-də indeks yenə seçilir, amma sol sütun şərtdə olmadığı üçün baza bütün indeksi
-- başdan-sona oxuyur: qiymət 3266, yəni (c)-dəkindən ~650 dəfə baha.
-- B-tree indeks açarları soldan sağa sıralayır. Birinci sütun şərtdə yoxdursa,
-- hədəflənmiş axtarış mümkün deyil. Ona görə kompozit indeksdə ən çox filtrlənən
-- sütun birinci yazılır.


-- 28-ci tapşırıq
-- UNIQUE məhdudiyyət ilə UNIQUE INDEX arasındakı fərqi praktikada göstərin: ikisini də yaradın, pg_constraint və pg_indexes-də axtarın, sonra hər birini DROP CONSTRAINT ilə silməyə çalışın. Nəticəni izah edin. (2 bal)

-- satis_log.id dəyərləri təkrarsızdır, ona görə hər iki obyekti onun üzərində qururuq.
alter table magaza.satis_log
    add constraint uq_satis_log_id unique (id);

create unique index uq_satis_log_id_musteri on magaza.satis_log (id, musteri_kodu);

-- pg_constraint-də yalnız məhdudiyyət görünür.
select conname, contype
from pg_constraint
where conrelid = 'magaza.satis_log'::regclass
  and conname like 'uq%';

-- pg_indexes-də hər ikisi görünür, çünki məhdudiyyət də özünə indeks yaradıb.
select indexname
from pg_indexes
where schemaname = 'magaza'
  and tablename = 'satis_log'
  and indexname like 'uq%';

-- Məhdudiyyəti DROP CONSTRAINT silir (indeksi də özü ilə aparır).
alter table magaza.satis_log
    drop constraint uq_satis_log_id;

-- İndeksi isə DROP CONSTRAINT silmir, xəta verir.
-- alter table magaza.satis_log
--     drop constraint uq_satis_log_id_musteri;

-- Xəta mesajı:
-- ERROR: constraint "uq_satis_log_id_musteri" of relation "satis_log" does not exist

-- İndeks yalnız DROP INDEX ilə silinir.
drop index magaza.uq_satis_log_id_musteri;

-- İzah: UNIQUE məhdudiyyət həm pg_constraint-də, həm pg_indexes-də görünür, çünki baza onu
-- icra etmək üçün özü unikal indeks yaradır və DROP CONSTRAINT o indeksi də aparır.
-- Əl ilə yaradılan UNIQUE INDEX isə yalnız pg_indexes-dədir; pg_constraint onu tanımır,
-- ona görə DROP CONSTRAINT xəta verir və yalnız DROP INDEX işləyir.
-- Fərq: məhdudiyyət məntiqi qaydadır, indeks onun texniki icrasıdır. Xarici açar yalnız
-- məhdudiyyətə istinad edə bilər, təkbaşına unikal indeksə yox.


-- 29-cu tapşırıq
-- `WHERE status = 'legv'` sorğusu üçün əvvəlcə tam indeks, sonra partial indeks qurun. İkisinin ölçüsünü və sorğu sürətini müqayisə edin. Partial indeks hansı halda məqsədəuyğundur? (2 bal)
-- İpucu: `pg_size_pretty(pg_relation_size('idx_ad'))`. Məlumatın 99%-i 'tamam'-dır.

-- Tam indeks: bütün 300 000 sətir indeksə düşür.
create index idx_satis_log_status on magaza.satis_log (status);

analyze magaza.satis_log;

select pg_size_pretty(pg_relation_size('magaza.idx_satis_log_status')) as tam_indeks_olcusu;

explain (analyze, buffers)
select *
from magaza.satis_log
where status = 'legv';

-- Partial indeks: yalnız status = 'legv' sətirləri indeksə düşür.
drop index magaza.idx_satis_log_status;

create index idx_satis_log_status_legv on magaza.satis_log (status) where status = 'legv';

analyze magaza.satis_log;

select pg_size_pretty(pg_relation_size('magaza.idx_satis_log_status_legv')) as partial_indeks_olcusu;

explain (analyze, buffers)
select *
from magaza.satis_log
where status = 'legv';

-- İzah (partial indeks nə vaxt məqsədəuyğundur): tam indeks 2056 kB, partial indeks
-- 40 kB, yəni 50 dəfə kiçik, çünki məlumatın yalnız ~1%-i 'legv'-dir. Hər iki halda plan
-- Index Scan-dır və icra vaxtı eyni səviyyədə qalır (1.4 ms / 2.2 ms, fərq ölçmə
-- səs-küyüdür), yəni sürət eyni, qiymət isə dəfələrlə ucuzdur.
-- Partial indeks sorğuların yalnız kiçik və seçici hissəyə baxdığı hallarda
-- məqsədəuyğundur: həm yer, həm də hər INSERT/UPDATE-in qiyməti azalır.


-- 30-cu tapşırıq
-- `WHERE UPPER(mehsul_adi) = 'MEHSUL 100'` sorğusu 26-cı tapşırıqda qurduğunuz indeksdən istifadə etmir. Səbəbini izah edin və iki fərqli həll yazın, ikisini də ölçün. (2 bal)
-- İpucu: Sütunun üzərinə funksiya tətbiq olunduqda indeks açarı ilə uyğunluq itir. Həll yollarından biri — ifadə üzrə indeks.

-- 26-cı tapşırığın indeksi ilə plan yenə Seq Scan olur.
analyze magaza.satis_log;

explain (analyze, buffers)
select *
from magaza.satis_log
where upper(mehsul_adi) = 'MEHSUL 100';

-- Həll 1: ifadə üzrə indeks. İndeksə sütunun özü yox, upper(sütun) yazılır.
create index idx_satis_log_mehsul_adi_upper on magaza.satis_log (upper(mehsul_adi));

analyze magaza.satis_log;

explain (analyze, buffers)
select *
from magaza.satis_log
where upper(mehsul_adi) = 'MEHSUL 100';

-- Həll 2: hesablanan sütun + adi indeks. Sorğu artıq funksiya çağırmır.
drop index magaza.idx_satis_log_mehsul_adi_upper;

alter table magaza.satis_log
    add column mehsul_adi_boyuk varchar(80) generated always as (upper(mehsul_adi)) stored;

create index idx_satis_log_mehsul_adi_boyuk on magaza.satis_log (mehsul_adi_boyuk);

analyze magaza.satis_log;

explain (analyze, buffers)
select *
from magaza.satis_log
where mehsul_adi_boyuk = 'MEHSUL 100';

-- İzah: B-tree indeksdə sütunun xam dəyərləri saxlanılır, upper(mehsul_adi) isə
-- funksiyanın nəticəsidir və indeks açarı ilə uyğun gəlmir, ona görə plan Seq Scan-a
-- düşür (57.9 ms). Həll 1 (ifadə üzrə indeks) 0.089 ms, Həll 2 (hesablanan sütun +
-- adi indeks) 0.137 ms. İkisi də işləyir: ifadə üzrə indeks sorğunu dəyişmədən həll
-- verir, hesablanan sütun isə əlavə yer tutur, amma sorğunu sadələşdirir.


-- =====================================================================
-- H. Çətin və qarışıq tapşırıqlar (31–40, 40 bal)
-- =====================================================================

-- 31-ci tapşırıq
-- `SELECT seher, tarix FROM magaza.satis_log WHERE seher = 'Gəncə'` sorğusunu Index Only Scan ilə işlətməyə nail olun. Planda Heap Fetches sətrini tapın, dəyərini sıfıra endirin və bunun nə demək olduğunu izah edin. (4 bal)
-- İpucu: INCLUDE bəndi və ya kompozit indeks; sonra `VACUUM satis_log;` — görünürlük xəritəsi yenilənməlidir.

-- tarix sütunu INCLUDE ilə indeksə əlavə olunur, beləcə sorğunun bütün sütunları indeksdədir.
create index idx_satis_log_seher_tarix on magaza.satis_log (seher) include (tarix);

analyze magaza.satis_log;

-- 30-cu tapşırıqdakı ALTER TABLE cədvəli yenidən yazıb, görünürlük xəritəsi boşdur.
-- Planı Index Only Scan-a məcbur edirik ki, Heap Fetches sətri görünsün.
set enable_bitmapscan = off;
set enable_seqscan = off;

explain (analyze, buffers)
select seher, tarix
from magaza.satis_log
where seher = 'Gəncə';

-- VACUUM görünürlük xəritəsini yeniləyir.
vacuum magaza.satis_log;

explain (analyze, buffers)
select seher, tarix
from magaza.satis_log
where seher = 'Gəncə';

reset enable_bitmapscan;
reset enable_seqscan;

-- İzah (Heap Fetches = 0 nə deməkdir): sorğunun hər iki sütunu indeksdədir (seher açar,
-- tarix INCLUDE), ona görə plan Index Only Scan ola bilir. VACUUM-dan əvvəl görünürlük
-- xəritəsi boş idi və baza hər sətrin görünən olduğunu yoxlamaq üçün cədvələ getdi:
-- Heap Fetches 60 000, 3704 blok, 12.3 ms. VACUUM xəritəni yeniləyəndən sonra
-- Heap Fetches = 0, yəni cədvəl ümumiyyətlə oxunmur (234 blok, 5.6 ms).
-- Yəni Heap Fetches = 0 o deməkdir ki, sorğu tam olaraq indeksdən cavablanır.


-- 32-ci tapşırıq
-- `ORDER BY mebleg DESC LIMIT 20` sorğusunda sıralamanın indeks hesabına aparıldığını sübut edin — planda Sort düyünü olmamalıdır. Sonra eyni nəticəni `ORDER BY mebleg DESC NULLS LAST` üçün əldə edin. (4 bal)
-- İpucu: İndeks öz sıralama qaydası ilə yaradılır: `CREATE INDEX ... (mebleg DESC NULLS LAST)`.

-- İndekssiz plan: Sort + Limit.
analyze magaza.satis_log;

explain (analyze, buffers)
select *
from magaza.satis_log
order by mebleg desc
limit 20;

-- DESC indeks (standart NULLS FIRST).
create index idx_satis_log_mebleg_desc on magaza.satis_log (mebleg desc);

analyze magaza.satis_log;

explain (analyze, buffers)
select *
from magaza.satis_log
order by mebleg desc
limit 20;

-- NULLS LAST sıralaması yuxarıdakı indekslə uyğun gəlmir, ayrıca indeks lazımdır.
explain (analyze, buffers)
select *
from magaza.satis_log
order by mebleg desc nulls last
limit 20;

create index idx_satis_log_mebleg_desc_nulls_last on magaza.satis_log (mebleg desc nulls last);

analyze magaza.satis_log;

explain (analyze, buffers)
select *
from magaza.satis_log
order by mebleg desc nulls last
limit 20;

-- İzah: indekssiz plan Sort düyünü saxlayır (top-N heapsort) və 300 000 sətri oxuyur:
-- 37.2 ms. (mebleg DESC) indeksi sətirləri artıq lazımi sırada saxlayır, ona görə plan
-- Index Scan + Limit olur, Sort yoxdur: 0.031 ms.
-- NULLS LAST isə başqa sıralamadır: DESC indeksin standartı NULLS FIRST-dür, ona görə
-- köhnə indeks yaramır və Sort geri qayıdır (35.4 ms). Ayrıca (mebleg DESC NULLS LAST)
-- indeksindən sonra Sort yenidən itir: 0.049 ms.


-- 33-cü tapşırıq
-- Hesabat sorğusu: satis_log-un bütün indekslərini ölçüsü, ölçünün cədvələ nisbəti (faizlə, 1 rəqəm) və indeksin tərifi ilə birlikdə sadalayın. Ölçüyə görə azalan sıra. Ən «bahalı» indeks hansıdır? (4 bal)
-- İpucu: `pg_indexes` + `pg_relation_size()` + `pg_size_pretty()`.

select indexname                                                        as indeks,
       pg_size_pretty(pg_relation_size(schemaname || '.' || indexname)) as olcu,
       round(100.0 * pg_relation_size(schemaname || '.' || indexname)
                 / pg_relation_size(schemaname || '.' || tablename), 1) as cedvele_nisbeti_faiz,
       indexdef                                                         as terif
from pg_indexes
where schemaname = 'magaza'
  and tablename = 'satis_log'
order by pg_relation_size(schemaname || '.' || indexname) desc;

-- Ən bahalı indeks idx_satis_log_seher_tarix-dir: 9272 kB, cədvəlin 31.2%-i.
-- Səbəb: INCLUDE bəndi ilə tarix sütunu da indeksə yazılır, yəni indeks iki sütunun
-- məlumatını saxlayır. Ondan sonra iki mebleg indeksi gəlir (6608 kB, 22.3%),
-- ən ucuzu isə partial indeksdir: 40 kB (0.1%).


-- 34-cü tapşırıq
-- Heç vaxt istifadə olunmayan indeksləri aşkarlayın: əvvəlcə statistikanı sıfırlayın, sonra 5–6 müxtəlif SELECT icra edin, sonra hər indeks üçün skan sayını göstərən hesabat çıxarın və `idx_scan = 0` olanları işarələyin. (4 bal)
-- İpucu: `SELECT pg_stat_reset();` + `pg_stat_user_indexes`.

select pg_stat_reset();

-- 5 müxtəlif SELECT. mehsul_adi_boyuk sütununa heç bir sorğu getmir, ona görə onun indeksinin
-- skan sayı sıfır qalmalıdır.
select count(*)
from magaza.satis_log
where mehsul_adi = 'Mehsul 4321';
select count(*)
from magaza.satis_log
where kateqoriya = 'Texnika';
select count(*)
from magaza.satis_log
where status = 'legv';
select seher, tarix
from magaza.satis_log
where seher = 'Gəncə'
limit 10;
select id, mebleg
from magaza.satis_log
order by mebleg desc
limit 20;

-- Statistika bir qədər gecikmə ilə yazılır, ona görə hesabatdan əvvəl gözləyirik.
select pg_sleep(1);

-- Skan sayı hesabatı.
select indexrelname                                                      as indeks,
       idx_scan                                                          as skan_sayi,
       case when idx_scan = 0 then 'İstifadə olunmur' else 'İşləyir' end as qeyd
from pg_stat_user_indexes
where schemaname = 'magaza'
  and relname = 'satis_log'
order by idx_scan, indexrelname;

-- İzah: 5 SELECT-dən sonra altı indeksin skan sayı ≥ 1-dir, idx_satis_log_mehsul_adi_boyuk
-- isə 0 qalıb, çünki heç bir sorğu o sütuna baxmadı. idx_scan = 0 olan indeks yer tutur və
-- hər INSERT/UPDATE-i yavaşladır, faydası isə yoxdur, yəni silinməyə namizəddir.
-- Qeyd: statistika pg_stat_reset()-dən sonrakı dövrü göstərir, ona görə qərar verməzdən
-- əvvəl real yük altında kifayət qədər uzun müddət yığılmalıdır.


-- 35-ci tapşırıq
-- `WHERE mehsul_adi LIKE '%hsul 4321%'` sorğusunu sürətləndirin. Adi B-tree indeks burada kömək etmir — səbəbini izah edin və işləyən həlli qurub ölçün. (4 bal)
-- İpucu: `CREATE EXTENSION pg_trgm;` + `USING GIN (mehsul_adi gin_trgm_ops)`.

-- B-tree indeks var (26-cı tapşırıq), amma plan yenə Seq Scan-dır.
analyze magaza.satis_log;

explain (analyze, buffers)
select *
from magaza.satis_log
where mehsul_adi like '%hsul 4321%';

create extension if not exists pg_trgm;

create index idx_satis_log_mehsul_adi_trgm on magaza.satis_log using gin (mehsul_adi gin_trgm_ops);

analyze magaza.satis_log;

explain (analyze, buffers)
select *
from magaza.satis_log
where mehsul_adi like '%hsul 4321%';

-- İzah (B-tree niyə kömək etmir): B-tree dəyərləri baş hərfdən sıralayır və axtarışa
-- məhz əvvəldən başlayır. '%hsul 4321%' şablonunda sətrin əvvəli bilinmir, ona görə
-- indeksdə başlanğıc nöqtə yoxdur və plan Seq Scan olur: 21.6 ms.
-- pg_trgm GIN indeksi mətni 3 hərflik parçalara (trigram) bölür və hər parça üçün
-- sətir siyahısı saxlayır, ona görə ortadan axtarış da indekslə gedir: 2.6 ms,
-- təqribən 8 dəfə sürətli.


-- 36-cı tapşırıq
-- İndeksin yazma əməliyyatına qiymətini ölçün: (a) bütün indeksləri silin, 100 000 sətir INSERT edin, vaxtı qeyd edin; (b) 5 indeks qurun, əlavə etdiyiniz sətirləri silin, eyni INSERT-i təkrarlayın. Fərqi faizlə göstərin və bir cümləlik nəticə yazın. (4 bal)

-- (a) satis_log-un bütün indekslərini silirik.
drop index if exists magaza.idx_satis_log_mehsul_adi;
drop index if exists magaza.idx_satis_log_kateqoriya_tarix;
drop index if exists magaza.idx_satis_log_status_legv;
drop index if exists magaza.idx_satis_log_mehsul_adi_boyuk;
drop index if exists magaza.idx_satis_log_seher_tarix;
drop index if exists magaza.idx_satis_log_mebleg_desc;
drop index if exists magaza.idx_satis_log_mebleg_desc_nulls_last;
drop index if exists magaza.idx_satis_log_mehsul_adi_trgm;

-- İndekssiz 100 000 INSERT. Vaxtı EXPLAIN ANALYZE-ın Execution Time sətrindən götürürük.
explain (analyze)
insert into magaza.satis_log (id, musteri_kodu, mehsul_adi, kateqoriya, seher, status, miqdar, mebleg, tarix)
select i,
       (random() * 20000)::int + 1,
       'Mehsul ' || (i % 5000),
       (array ['Texnika', 'Aksesuar', 'Ofis', 'Mebel', 'Kitab'])[(i % 5) + 1],
       (array ['Bakı','Gəncə','Sumqayıt','Şəki','Lənkəran'])[(i % 5) + 1],
       case when i % 97 = 0 then 'legv' else 'tamam' end,
       (random() * 10)::int + 1,
       (random() * 5000 + 10)::numeric(12, 2),
       date '2022-01-01' + (i % 1000)
from generate_series(300001, 400000) as i;

-- (b) 5 indeks qururuq və əlavə etdiyimiz sətirləri silirik.
delete
from magaza.satis_log
where id > 300000;

create index idx_satis_log_mehsul_adi on magaza.satis_log (mehsul_adi);
create index idx_satis_log_kateqoriya_tarix on magaza.satis_log (kateqoriya, tarix);
create index idx_satis_log_seher on magaza.satis_log (seher);
create index idx_satis_log_mebleg on magaza.satis_log (mebleg);
create index idx_satis_log_tarix on magaza.satis_log (tarix);

analyze magaza.satis_log;

-- Eyni INSERT, indi 5 indeks də yenilənir.
explain (analyze)
insert into magaza.satis_log (id, musteri_kodu, mehsul_adi, kateqoriya, seher, status, miqdar, mebleg, tarix)
select i,
       (random() * 20000)::int + 1,
       'Mehsul ' || (i % 5000),
       (array ['Texnika', 'Aksesuar', 'Ofis', 'Mebel', 'Kitab'])[(i % 5) + 1],
       (array ['Bakı','Gəncə','Sumqayıt','Şəki','Lənkəran'])[(i % 5) + 1],
       case when i % 97 = 0 then 'legv' else 'tamam' end,
       (random() * 10)::int + 1,
       (random() * 5000 + 10)::numeric(12, 2),
       date '2022-01-01' + (i % 1000)
from generate_series(300001, 400000) as i;

-- Fərq (%) və nəticə: (a) indekssiz 100 000 INSERT 192.7 ms; (b) 5 indekslə
-- 1163.8 ms. Fərq +504%, yəni təqribən 6 dəfə yavaş.
-- Nəticə: hər indeks oxunuşu sürətləndirir, amma yazılışın qiymətini artırır. Kütləvi
-- yükləmədə indeksləri əvvəlcə silib, yükləmədən sonra qurmaq daha sərfəlidir.


-- 37-ci tapşırıq
-- satis_log-a PRIMARY KEY əlavə edin, sonra ona FK ilə bağlı satis_qeyd cədvəli yaradıb 200 000 sətir doldurun. FK sütununa indeks qurmadan valideyn cədvəldən sətir silin və vaxtı ölçün; sonra indeks qurub təkrarlayın. PostgreSQL FK sütununa avtomatik indeks yaradırmı? (4 bal)
-- İpucu: Valideyn sətri silinəndə baza uşaq cədvəldə istinadları axtarmalıdır — indeks yoxdursa bu, tam skan deməkdir.

alter table magaza.satis_log
    add constraint pk_satis_log primary key (id);

drop table if exists magaza.satis_qeyd;

create table magaza.satis_qeyd
(
    id       int generated always as identity,
    satis_id int,
    qeyd     varchar(50),
    constraint pk_satis_qeyd primary key (id),
    constraint fk_satis_qeyd_satis foreign key (satis_id) references magaza.satis_log (id)
);

insert into magaza.satis_qeyd (satis_id, qeyd)
select i, 'Qeyd ' || i
from generate_series(1, 200000) as i;

analyze magaza.satis_qeyd;

-- FK sütununda indeks yoxdur: uşaq cədvəldə istinad axtarışı tam skandır.
-- Silinən sətrin uşağı yoxdur, ona görə ölçdüyümüz məhz yoxlamanın qiymətidir.
explain (analyze)
delete
from magaza.satis_log
where id = 400000;

-- İndi FK sütununa indeks qururuq.
create index idx_satis_qeyd_satis_id on magaza.satis_qeyd (satis_id);

analyze magaza.satis_qeyd;

explain (analyze)
delete
from magaza.satis_log
where id = 399999;

-- İzah (PostgreSQL FK sütununa avtomatik indeks yaradırmı): xeyr. Avtomatik indeks
-- yalnız PRIMARY KEY və UNIQUE üçün qurulur; xarici açarın öz sütunu indekssiz qalır.
-- Ona görə valideyn sətri silinəndə uşaq cədvəldə istinad axtarışı tam skana çevrilir:
-- planda «Trigger for constraint fk_satis_qeyd_satis: time=8.144 ms».
-- FK sütununa indeks quranda həmin yoxlama 0.131 ms olur, yəni ~60 dəfə sürətlidir.


-- 38-ci tapşırıq
-- `WHERE kateqoriya = 'Ofis' AND seher = 'Bakı'` sorğusunu iki konfiqurasiyada müqayisə edin: (a) iki ayrı bir-sütunlu indeks, (b) bir kompozit indeks (kateqoriya, seher). Planda BitmapAnd görünürmü? Hansı variant daha sürətlidir və niyə? (4 bal)

-- (a) iki ayrı bir-sütunlu indeks (idx_satis_log_seher 36-cı tapşırıqda quruldu).
drop index if exists magaza.idx_satis_log_kateqoriya_tarix;

create index idx_satis_log_kateqoriya on magaza.satis_log (kateqoriya);

analyze magaza.satis_log;

explain (analyze, buffers)
select *
from magaza.satis_log
where kateqoriya = 'Ofis'
  and seher = 'Bakı';

-- (b) bir kompozit indeks.
drop index magaza.idx_satis_log_kateqoriya;
drop index magaza.idx_satis_log_seher;

create index idx_satis_log_kateqoriya_seher on magaza.satis_log (kateqoriya, seher);

analyze magaza.satis_log;

explain (analyze, buffers)
select *
from magaza.satis_log
where kateqoriya = 'Ofis'
  and seher = 'Bakı';

-- İzah (BitmapAnd, hansı daha sürətli və niyə): (a) iki ayrı indeksdə planda BitmapAnd
-- görünür: baza hər indeksdən bitmap qurur (80 000 + 80 000 sətir) və kəsişməni alır,
-- icra 2.598 ms. (b) kompozit indeksdə BitmapAnd yoxdur, tək Bitmap Index Scan hər iki
-- şərti birlikdə tətbiq edir: 0.023 ms, ~100 dəfə sürətlidir.
-- Səbəb: kompozit indeks kəsişməni əvvəlcədən hazır saxlayır, iki ayrı indeks isə
-- əvvəlcə iki böyük bitmap qurub sonra birləşdirməlidir.
-- Qeyd: bu məlumatda kateqoriya və seher eyni düsturla (i % 5) yaradılıb, ona görə
-- 'Ofis' + 'Bakı' cütü heç bir sətirdə yoxdur (rows=0), ona görə müqayisə planın qiymətinə görədir.


-- 39-cu tapşırıq
-- İndeksin ölçüsünü qeyd edin, sonra cədvəlin təxminən 40%-ni UPDATE edin və ölçüyə yenidən baxın. Ölçü niyə artdı? n_dead_tup dəyərini göstərin, REINDEX icra edib fərqi cədvəldə təqdim edin. (4 bal)
-- İpucu: Şişmə (bloat), MVCC və ölü sətirlər. `pg_stat_user_tables`.

-- Təmiz başlanğıc: 36-cı tapşırıqdakı silinmə indeksdə boş yer qoyub, ona görə
-- ölçməyə başlamazdan əvvəl indeksi yenidən qurub cədvəli təmizləyirik.
reindex index magaza.idx_satis_log_mebleg;
vacuum magaza.satis_log;

-- UPDATE-dən əvvəl.
select pg_size_pretty(pg_relation_size('magaza.idx_satis_log_mebleg')) as indeks_olcusu,
       n_dead_tup                                                      as olu_setirler
from pg_stat_user_tables
where schemaname = 'magaza'
  and relname = 'satis_log';

-- Cədvəlin təxminən 40%-i (id % 5 < 2).
update magaza.satis_log
set mebleg = mebleg + 1
where id % 5 < 2;

analyze magaza.satis_log;

select pg_size_pretty(pg_relation_size('magaza.idx_satis_log_mebleg')) as indeks_olcusu,
       n_dead_tup                                                      as olu_setirler
from pg_stat_user_tables
where schemaname = 'magaza'
  and relname = 'satis_log';

reindex index magaza.idx_satis_log_mebleg;

select pg_size_pretty(pg_relation_size('magaza.idx_satis_log_mebleg')) as indeks_olcusu
from pg_stat_user_tables
where schemaname = 'magaza'
  and relname = 'satis_log';

-- İzah (bloat, MVCC, ölü sətirlər): UPDATE-dən əvvəl indeks 8800 kB, n_dead_tup = 0.
-- Cədvəlin ~40%-ni (159 999 sətir) UPDATE-dən sonra indeks 17 MB, n_dead_tup = 159 999.
-- Səbəb MVCC-dir: UPDATE sətri yerində dəyişmir, köhnə versiyanı ölü sətir kimi saxlayır
-- və yeni versiya yazır. İndekslənmiş sütun (mebleg) dəyişdiyi üçün indeksə də yeni giriş
-- düşür, köhnəsi isə dərhal getmir, nəticədə indeks şişir (bloat).
-- REINDEX indeksi sıfırdan qurur və ölçü 8800 kB-a qayıdır.


-- 40-cı tapşırıq
-- Yekun audit sorğusu. Sxeminizdəki hər cədvəl üçün bir sətir: cədvəl adı, təxmini sətir sayı, cədvəl ölçüsü, indeks sayı, indekslərin ümumi ölçüsü, PRIMARY KEY-in olub-olmaması (Var / Yoxdur) və status — PK yoxdursa Problemli, indekslərin ölçüsü cədvəlin 50%-dən çoxdursa Nezaret lazimdir, qalanı Normal. Cədvəl ölçüsünə görə azalan sıra. (4 bal)
-- İpucu: `pg_class` / `pg_stat_user_tables` + alt-sorğular + CASE + `pg_total_relation_size`. Məhdudiyyət, indeks, ölçü və şərti ifadələr — hamısı bir sorğuda.

-- reltuples yalnız ANALYZE-dan sonra dolur.
analyze;

select c.relname                               as cedvel,
       c.reltuples::bigint                     as setir_sayi,
       pg_size_pretty(pg_relation_size(c.oid)) as cedvel_olcusu,
       (select count(*)
        from pg_index i
        where i.indrelid = c.oid)              as indeks_sayi,
       pg_size_pretty(pg_indexes_size(c.oid))  as indekslerin_olcusu,
       case
           when exists (select 1
                        from pg_constraint k
                        where k.conrelid = c.oid
                          and k.contype = 'p')
               then 'Var'
           else 'Yoxdur'
           end                                 as primary_key,
       case
           when not exists (select 1
                            from pg_constraint k
                            where k.conrelid = c.oid
                              and k.contype = 'p')
               then 'Problemli'
           when pg_indexes_size(c.oid) > pg_relation_size(c.oid) * 0.5
               then 'Nezaret lazimdir'
           else 'Normal'
           end                                 as status
from pg_class c
         join pg_namespace n on n.oid = c.relnamespace
where n.nspname = 'magaza'
  and c.relkind = 'r'
order by pg_relation_size(c.oid) desc;

-- Nəticə: 'Problemli' statuslu cədvəl yoxdur, hamısının PRIMARY KEY-i var.
-- satis_log (54 MB cədvəl, 40 MB indeks) və satis_qeyd 'Nezaret lazimdir' statusundadır,
-- yəni indekslər cədvəlin yarısından çox yer tutur. Bu, 36-cı tapşırıqda qurulan 5 indeksin
-- qiymətidir. Kiçik cədvəllər (kateqoriya, mehsul, musteri, sifaris, sifaris_detal) 8 KB-dır
-- və bir neçə indeks saxlayır, ona görə nisbət formal olaraq 50%-i keçir, amma bu həcmdə
-- praktiki mənası yoxdur.
