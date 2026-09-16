-- =========================================================
-- AkademiyaOnline - normallasdirilmis sxem (5NF) + datalar
-- PostgreSQL 14+
-- Ahmadov Ahmad
-- =========================================================

-- ===== Kohne cedvelleri silinmesi (asili olanlar evvel) =====
-- Evvelce A2/A4/A5/A6 bendlerinde qurulan numayis cedvelleri:
DROP TABLE IF EXISTS qeydiyyat_2nf;
DROP TABLE IF EXISTS kurs_2nf;
DROP TABLE IF EXISTS telebe_2nf;
DROP TABLE IF EXISTS ders_telebe_muellim;
DROP TABLE IF EXISTS ders_muellim_fenn;
DROP TABLE IF EXISTS muellim_dil_4nf;
DROP TABLE IF EXISTS muellim_fenn_4nf;
DROP TABLE IF EXISTS muellim_bacariq;
DROP TABLE IF EXISTS tedris_plani;
-- Esas sxem:
DROP TABLE IF EXISTS telebe_muellim;
DROP TABLE IF EXISTS muellim_fenn;
DROP TABLE IF EXISTS muellim_filial;
DROP TABLE IF EXISTS kurs_filial;
DROP TABLE IF EXISTS muellim_kurs;
DROP TABLE IF EXISTS qeydiyyat;
DROP TABLE IF EXISTS telebe_telefon;
DROP TABLE IF EXISTS muellim_dil;
DROP TABLE IF EXISTS telebe;
DROP TABLE IF EXISTS muellim;
DROP TABLE IF EXISTS kurs;
DROP TABLE IF EXISTS fenn;
DROP TABLE IF EXISTS otaq;
DROP TABLE IF EXISTS filial;
DROP TABLE IF EXISTS qiymet_shkalasi;
DROP TABLE IF EXISTS kurs_qeydiyyat;


-- =========================================================
-- 1) FILIAL
-- filial_kod -> filial_unvan, seher
-- =========================================================
CREATE TABLE filial
(
    filial_kod   VARCHAR(10) PRIMARY KEY,
    filial_unvan VARCHAR(100) NOT NULL,
    seher        VARCHAR(50)  NOT NULL,
    CONSTRAINT uq_filial_unvan UNIQUE (filial_unvan)
);

-- =========================================================
-- 2) OTAQ
-- otaq_no -> otaq_tutum, filial_kod
-- =========================================================
CREATE TABLE otaq
(
    otaq_no    VARCHAR(10) PRIMARY KEY,
    otaq_tutum INT         NOT NULL,
    filial_kod VARCHAR(10) NOT NULL,
    CONSTRAINT fk_otaq_filial FOREIGN KEY (filial_kod) REFERENCES filial (filial_kod),
    CONSTRAINT ch_otaq_tutum CHECK (otaq_tutum > 0)
);

-- =========================================================
-- 3) FENN
-- fenn_kod -> fenn_ad
-- =========================================================
CREATE TABLE fenn
(
    fenn_kod VARCHAR(10) PRIMARY KEY,
    fenn_ad  VARCHAR(60) NOT NULL,
    CONSTRAINT uq_fenn_ad UNIQUE (fenn_ad)
);

-- =========================================================
-- 4) KURS
-- kurs_kod -> kurs_ad, kurs_saat, qiymet, fenn_kod
-- =========================================================
CREATE TABLE kurs
(
    kurs_kod  VARCHAR(10) PRIMARY KEY,
    kurs_ad   VARCHAR(60)   NOT NULL,
    kurs_saat INT           NOT NULL,
    qiymet    NUMERIC(8, 2) NOT NULL,
    fenn_kod  VARCHAR(10)   NOT NULL,
    CONSTRAINT fk_kurs_fenn FOREIGN KEY (fenn_kod) REFERENCES fenn (fenn_kod),
    CONSTRAINT ch_kurs_saat CHECK (kurs_saat > 0),
    CONSTRAINT ch_kurs_qiymet CHECK (qiymet >= 0)
);

-- =========================================================
-- 5) MUELLIM
-- muellim_id -> muellim_ad, muellim_email, fenn_kod, mentor_id
-- mentor_id -> ozune istinad eden FK (7-ci biznes qaydasi)
-- =========================================================
CREATE TABLE muellim
(
    muellim_id    VARCHAR(10) PRIMARY KEY,
    muellim_ad    VARCHAR(60) NOT NULL,
    muellim_email VARCHAR(80) NOT NULL,
    fenn_kod      VARCHAR(10) NOT NULL,
    mentor_id     VARCHAR(10),
    CONSTRAINT uq_muellim_email UNIQUE (muellim_email),
    CONSTRAINT fk_muellim_fenn FOREIGN KEY (fenn_kod) REFERENCES fenn (fenn_kod),
    CONSTRAINT fk_muellim_mentor FOREIGN KEY (mentor_id) REFERENCES muellim (muellim_id),
    CONSTRAINT ch_muellim_mentor_ozu CHECK (mentor_id <> muellim_id)
);

-- =========================================================
-- 6) MUELLIM_DIL  (4NF - dil ile tedris etdiyi fenn asili deyil)
-- =========================================================
CREATE TABLE muellim_dil
(
    muellim_id VARCHAR(10) NOT NULL,
    dil        VARCHAR(30) NOT NULL,
    CONSTRAINT pk_muellim_dil PRIMARY KEY (muellim_id, dil),
    CONSTRAINT fk_muellim_dil_muellim FOREIGN KEY (muellim_id) REFERENCES muellim (muellim_id)
);

-- =========================================================
-- 7) TELEBE
-- telebe_id -> telebe_ad, dogum_tarixi
-- =========================================================
CREATE TABLE telebe
(
    telebe_id    VARCHAR(10) PRIMARY KEY,
    telebe_ad    VARCHAR(60) NOT NULL,
    dogum_tarixi DATE        NOT NULL,
    CONSTRAINT ch_telebe_dogum CHECK (dogum_tarixi < CURRENT_DATE)
);

-- =========================================================
-- 8) TELEBE_TELEFON  (1NF - cox qiymetli sutun ayrildi)
-- =========================================================
CREATE TABLE telebe_telefon
(
    telebe_id VARCHAR(10) NOT NULL,
    telefon   VARCHAR(20) NOT NULL,
    CONSTRAINT pk_telebe_telefon PRIMARY KEY (telebe_id, telefon),
    CONSTRAINT fk_telefon_telebe FOREIGN KEY (telebe_id) REFERENCES telebe (telebe_id)
);

-- =========================================================
-- 9) QEYDIYYAT
-- (telebe_id, kurs_kod) -> muellim_id, otaq_no, qeyd_tarixi, odenis, imtahan_bali
-- imtahan_bali NULL ola biler (hele imtahan vermeyen telebe)
-- =========================================================
CREATE TABLE qeydiyyat
(
    telebe_id    VARCHAR(10)   NOT NULL,
    kurs_kod     VARCHAR(10)   NOT NULL,
    muellim_id   VARCHAR(10)   NOT NULL,
    otaq_no      VARCHAR(10)   NOT NULL,
    qeyd_tarixi  DATE          NOT NULL,
    odenis       NUMERIC(8, 2) NOT NULL,
    imtahan_bali INT,
    CONSTRAINT pk_qeydiyyat PRIMARY KEY (telebe_id, kurs_kod),
    CONSTRAINT fk_qeyd_telebe FOREIGN KEY (telebe_id) REFERENCES telebe (telebe_id),
    CONSTRAINT fk_qeyd_kurs FOREIGN KEY (kurs_kod) REFERENCES kurs (kurs_kod),
    CONSTRAINT fk_qeyd_muellim FOREIGN KEY (muellim_id) REFERENCES muellim (muellim_id),
    CONSTRAINT fk_qeyd_otaq FOREIGN KEY (otaq_no) REFERENCES otaq (otaq_no),
    CONSTRAINT ch_qeyd_odenis CHECK (odenis >= 0),
    CONSTRAINT ch_qeyd_bal CHECK (imtahan_bali BETWEEN 0 AND 100)
);

-- =========================================================
-- 10) QIYMET_SHKALASI  (herf qiymetleri)
-- =========================================================
CREATE TABLE qiymet_shkalasi
(
    herf    CHAR(1) PRIMARY KEY,
    min_bal INT NOT NULL,
    max_bal INT NOT NULL,
    CONSTRAINT ch_shkala_interval CHECK (min_bal <= max_bal),
    CONSTRAINT ch_shkala_diapazon CHECK (min_bal >= 0 AND max_bal <= 100)
);

-- =========================================================
-- 11) 5NF - tedris_plani-nin uc binar proyeksiyasi
-- =========================================================
CREATE TABLE muellim_kurs
(
    muellim_id VARCHAR(10) NOT NULL,
    kurs_kod   VARCHAR(10) NOT NULL,
    CONSTRAINT pk_muellim_kurs PRIMARY KEY (muellim_id, kurs_kod),
    CONSTRAINT fk_mk_muellim FOREIGN KEY (muellim_id) REFERENCES muellim (muellim_id),
    CONSTRAINT fk_mk_kurs FOREIGN KEY (kurs_kod) REFERENCES kurs (kurs_kod)
);

CREATE TABLE kurs_filial
(
    kurs_kod   VARCHAR(10) NOT NULL,
    filial_kod VARCHAR(10) NOT NULL,
    CONSTRAINT pk_kurs_filial PRIMARY KEY (kurs_kod, filial_kod),
    CONSTRAINT fk_kf_kurs FOREIGN KEY (kurs_kod) REFERENCES kurs (kurs_kod),
    CONSTRAINT fk_kf_filial FOREIGN KEY (filial_kod) REFERENCES filial (filial_kod)
);

CREATE TABLE muellim_filial
(
    muellim_id VARCHAR(10) NOT NULL,
    filial_kod VARCHAR(10) NOT NULL,
    CONSTRAINT pk_muellim_filial PRIMARY KEY (muellim_id, filial_kod),
    CONSTRAINT fk_mf_muellim FOREIGN KEY (muellim_id) REFERENCES muellim (muellim_id),
    CONSTRAINT fk_mf_filial FOREIGN KEY (filial_kod) REFERENCES filial (filial_kod)
);


-- =========================================================
-- DATALARIN DOLDURULMASI
-- =========================================================

-- ----- Filiallar (F-03-un hec bir otagi yoxdur) -----
INSERT INTO filial (filial_kod, filial_unvan, seher)
VALUES ('F-01', 'Bakı, Nizami küç. 12', 'Bakı'),
       ('F-02', 'Gəncə, Atatürk pr. 5', 'Gəncə'),
       ('F-03', 'Sumqayıt, Sülh küç. 3', 'Sumqayıt');

-- ----- Otaqlar -----
INSERT INTO otaq (otaq_no, otaq_tutum, filial_kod)
VALUES ('A-201', 25, 'F-01'),
       ('A-305', 30, 'F-01'),
       ('B-305', 22, 'F-02'),
       ('B-410', 18, 'F-02');

-- ----- Fennler (F-DIZ-in hec bir kursu yoxdur) -----
INSERT INTO fenn (fenn_kod, fenn_ad)
VALUES ('F-SQL', 'Verilənlər Bazaları'),
       ('F-PYT', 'Proqramlaşdırma'),
       ('F-DSC', 'Data Science'),
       ('F-DIZ', 'Dizayn');

-- ----- Kurslar (PYT-305-e hec bir telebe yazilmayib) -----
INSERT INTO kurs (kurs_kod, kurs_ad, kurs_saat, qiymet, fenn_kod)
VALUES ('SQL-101', 'SQL Əsasları', 40, 350, 'F-SQL'),
       ('SQL-202', 'Ətraflı SQL', 50, 450, 'F-SQL'),
       ('PYT-201', 'Python Başlanğıc', 60, 500, 'F-PYT'),
       ('PYT-305', 'Django Web', 70, 600, 'F-PYT'),
       ('DSC-301', 'Data Science', 80, 700, 'F-DSC');

-- ----- Muellimler -----
-- Evvel mentoru olmayanlari yazırıq, sonra mentoru olanları (self FK ucun)
INSERT INTO muellim (muellim_id, muellim_ad, muellim_email, fenn_kod, mentor_id)
VALUES ('M-05', 'Rəşad Quliyev', 'reshad@akademiya.az', 'F-SQL', NULL),
       ('M-13', 'Samir Nəbiyev', 'samir@akademiya.az', 'F-PYT', NULL);

INSERT INTO muellim (muellim_id, muellim_ad, muellim_email, fenn_kod, mentor_id)
VALUES ('M-07', 'Nigar Əliyeva', 'nigar@akademiya.az', 'F-PYT', 'M-05'),
       ('M-09', 'Tural Abbasov', 'tural@akademiya.az', 'F-DSC', 'M-05');

INSERT INTO muellim (muellim_id, muellim_ad, muellim_email, fenn_kod, mentor_id)
VALUES ('M-11', 'Aygün Vəliyeva', 'aygun@akademiya.az', 'F-SQL', 'M-07');

-- ----- Muellim dilleri (M-13-un dil qeydi yoxdur) -----
INSERT INTO muellim_dil (muellim_id, dil)
VALUES ('M-05', 'Azərbaycan'),
       ('M-05', 'İngilis'),
       ('M-05', 'Rus'),
       ('M-07', 'Azərbaycan'),
       ('M-07', 'İngilis'),
       ('M-09', 'Azərbaycan'),
       ('M-11', 'Azərbaycan'),
       ('M-11', 'Rus');

-- ----- Telebeler -----
INSERT INTO telebe (telebe_id, telebe_ad, dogum_tarixi)
VALUES ('T-01', 'Aysel Məmmədova', DATE '2001-04-12'),
       ('T-02', 'Elvin Hüseynov', DATE '1999-11-03'),
       ('T-03', 'Nigar Səfərova', DATE '2003-02-20'),
       ('T-04', 'Kamran İsmayılov', DATE '2000-07-08'),
       ('T-05', 'Leyla Orucova', DATE '2002-09-30');

-- ----- Telefonlar (T-04 ve T-05-in telefonu yoxdur) -----
INSERT INTO telebe_telefon (telebe_id, telefon)
VALUES ('T-01', '055-111-22-33'),
       ('T-01', '070-111-22-33'),
       ('T-02', '051-777-88-99'),
       ('T-03', '070-222-33-44'),
       ('T-03', '055-222-33-44');

-- ----- Qeydiyyatlar (T-04 hec bir kursa yazilmayib) -----
-- T-05-in imtahan bali NULL-dir (hele imtahan vermeyib)
INSERT INTO qeydiyyat (telebe_id, kurs_kod, muellim_id, otaq_no, qeyd_tarixi, odenis, imtahan_bali)
VALUES ('T-01', 'SQL-101', 'M-05', 'A-201', DATE '2025-02-10', 350, 92),
       ('T-01', 'PYT-201', 'M-07', 'A-305', DATE '2025-02-12', 500, 78),
       ('T-02', 'SQL-101', 'M-05', 'A-201', DATE '2025-02-15', 350, 65),
       ('T-02', 'PYT-201', 'M-07', 'B-305', DATE '2025-03-05', 500, 54),
       ('T-03', 'DSC-301', 'M-09', 'B-410', DATE '2025-03-01', 700, 88),
       ('T-03', 'SQL-202', 'M-11', 'B-305', DATE '2025-03-10', 450, 71),
       ('T-05', 'SQL-101', 'M-05', 'A-201', DATE '2025-02-18', 350, NULL);

-- ----- Qiymet skalasi -----
INSERT INTO qiymet_shkalasi (herf, min_bal, max_bal)
VALUES ('A', 90, 100),
       ('B', 80, 89),
       ('C', 70, 79),
       ('D', 60, 69),
       ('F', 0, 59);

-- ----- 5NF cedvelleri (A6-daki tedris plani) -----
INSERT INTO muellim_kurs (muellim_id, kurs_kod)
VALUES ('M-05', 'SQL-101'),
       ('M-05', 'SQL-202'),
       ('M-11', 'SQL-101');

INSERT INTO kurs_filial (kurs_kod, filial_kod)
VALUES ('SQL-101', 'F-01'),
       ('SQL-202', 'F-01'),
       ('SQL-101', 'F-02');

INSERT INTO muellim_filial (muellim_id, filial_kod)
VALUES ('M-05', 'F-01'),
       ('M-11', 'F-01'),
       ('M-05', 'F-02');


-- #########################################################
-- #                                                       #
-- #   HİSSƏ A — NORMALİZASİYA (1NF -> 5NF)                #
-- #                                                       #
-- #########################################################


-- ===== Tapşırıq A1 — Birinci Normal Forma (1NF) ===== (6 bal)
-- a) kurs_qeydiyyat cədvəlində 1NF-i pozan bütün sütunları göstərin (ən azı ikisi var) və nə üçün pozduğunu izah edin.
-- İzah:
-- 1) telefonlar — bir xanada birdən çox dəyər saxlanılır (T-01: '055-111-22-33, 070-111-22-33'). Dəyər atomik deyil, ona görə 1NF pozulur.
-- 2) muellim_dilleri — eyni problem (M-05: 'Azərbaycan, İngilis, Rus'). Belə sütunda axtarış (WHERE dil = 'Rus'), sayma və dəyişiklik yalnız mətn üzərində LIKE/SPLIT ilə mümkün olur, bu isə səhvə açıqdır.
-- 3) Cədvəldə PRIMARY KEY yoxdur və eyni sətir təkrarlanır (T-01 iki dəfə yazılıb). Münasibətdə eyni sətirin təkrarlanması da 1NF pozuntusu sayılır.
--
-- Nəticə: atomik olmayan sütunlar məlumatın təkrarlanmasına və INSERT / UPDATE / DELETE anomaliyalarına səbəb olur.

-- b) Cədvəli 1NF-ə gətirin. Alınan cədvəl(lər)i CREATE TABLE ilə yazın.

CREATE TABLE kurs_qeydiyyat
(
    telebe_id     VARCHAR(20)   NOT NULL,
    telebe_ad     VARCHAR(100)  NOT NULL,
    dogum_tarixi  DATE          NOT NULL,
    kurs_kod      VARCHAR(20)   NOT NULL,
    kurs_ad       VARCHAR(100)  NOT NULL,
    kurs_saat     INT           NOT NULL CHECK (kurs_saat > 0),
    qiymet        NUMERIC(8, 2) NOT NULL CHECK (qiymet >= 0),
    fenn_kod      VARCHAR(20)   NOT NULL,
    fenn_ad       VARCHAR(100)  NOT NULL,
    muellim_id    VARCHAR(20)   NOT NULL,
    muellim_ad    VARCHAR(100)  NOT NULL,
    muellim_email VARCHAR(100)  NOT NULL,
    otaq_no       VARCHAR(20)   NOT NULL,
    otaq_tutum    INT CHECK (otaq_tutum > 0),
    filial_kod    VARCHAR(20)   NOT NULL,
    filial_unvan  VARCHAR(100)  NOT NULL,
    seher         VARCHAR(50),
    qeyd_tarixi   DATE          NOT NULL,
    odenis        NUMERIC(8, 2) NOT NULL CHECK (odenis >= 0),
    imtahan_bali  INT CHECK (imtahan_bali BETWEEN 0 AND 100),
    CONSTRAINT pk_kurs_qeydiyyat PRIMARY KEY (telebe_id, kurs_kod)
);

-- telebe_telefon ve muellim_dil cedvellerini yuxarida, DDL bolmesinde (6 ve 8 nomreli cedveller) artiq yaratmisam - bu iki cedvel 1NF-den 5NF-e
-- qeder deyismeden qalir, ona gore burada tekrar yaratmiram.

-- 1NF cedvelini real datayla doldururuq ki, asagidaki anomaliyalar (A1c) ve C3
-- bendindeki muqayise "fikren" yox, real UPDATE/DELETE ile gosterile bilsin.
INSERT INTO kurs_qeydiyyat (telebe_id, telebe_ad, dogum_tarixi, kurs_kod, kurs_ad, kurs_saat, qiymet,
                            fenn_kod, fenn_ad, muellim_id, muellim_ad, muellim_email,
                            otaq_no, otaq_tutum, filial_kod, filial_unvan, seher,
                            qeyd_tarixi, odenis, imtahan_bali)
SELECT t.telebe_id,
       t.telebe_ad,
       t.dogum_tarixi,
       k.kurs_kod,
       k.kurs_ad,
       k.kurs_saat,
       k.qiymet,
       f.fenn_kod,
       f.fenn_ad,
       m.muellim_id,
       m.muellim_ad,
       m.muellim_email,
       o.otaq_no,
       o.otaq_tutum,
       fl.filial_kod,
       fl.filial_unvan,
       fl.seher,
       q.qeyd_tarixi,
       q.odenis,
       q.imtahan_bali
FROM qeydiyyat q
         JOIN telebe t ON q.telebe_id = t.telebe_id
         JOIN kurs k ON q.kurs_kod = k.kurs_kod
         JOIN fenn f ON k.fenn_kod = f.fenn_kod
         JOIN muellim m ON q.muellim_id = m.muellim_id
         JOIN otaq o ON q.otaq_no = o.otaq_no
         JOIN filial fl ON o.filial_kod = fl.filial_kod;

SELECT COUNT(*) AS kurs_qeydiyyat_setir_sayi FROM kurs_qeydiyyat;

-- c) 1NF-dən sonra hələ də qalan INSERT, UPDATE və DELETE anomaliyalarının hər biri üçün bu ssenaridən konkret nümunə verin (məsələn: "DSC-301 kursuna yazılan tək tələbə silinsə, ...").
-- İzah:

-- INSERT anomaliyası: yeni fənn ("F-DIZ" kimi) əlavə etmək istəsək, kurs_qeydiyyat
-- cədvəlində fenn_kod/fenn_ad yalnız bir kursla (kurs_kod ilə) birlikdə mövcud ola bilər.
-- Hələ heç bir kursu olmayan fənni ümumiyyətlə yazmaq mümkün deyil, çünki kurs_kod NOT NULL-dur.
--
-- UPDATE anomaliyası: müəllim M-05-in e-poçtu dəyişəndə bu, onun tədris etdiyi
-- bütün qeydiyyat sətirlərində (T-01/SQL-101, T-02/SQL-101, T-05/SQL-101) ayrı-ayrılıqda
-- yenilənməlidir. Bir sətir unudulsa, eyni müəllimin iki fərqli e-poçtu olduğu görünər
-- (məlumat uyğunsuzluğu).
--
-- DELETE anomaliyası: DSC-301 kursuna yazılan tək tələbənin (T-03) qeydiyyatı silinsə,
-- kurs_ad, kurs_saat, qiymet və fenn_kod/fenn_ad kimi kurs məlumatları da onunla birlikdə
-- həmişəlik itir, çünki bu məlumatlar başqa heç bir sətirdə saxlanmır.

-- d) Müzakirə: PostgreSQL-də telefonlar TEXT[] və ya JSONB istifadə etsək, cədvəl 1NF-də sayılırmı? 3-5 cümlə ilə əsaslandırın və massivin praktikada nə vaxt məqbul olduğunu yazın.
-- İzah:
-- 1) Klassik (Codd/Date) baxışa görə sayılmır: 1NF hər xananın atomik olmasını tələb edir,
--    massiv isə öz içində sıralı, indekslənə bilən elementlər saxlayır - yəni bu, gizlədilmiş
--    təkrarlanan qrupdur (repeating group) və vergüllə yazılmış mətndən prinsipial fərqi yoxdur.
-- 2) Müasir (SQL standartı) baxışa görə sayılır: `TEXT[]` domeni olan bir tipdir, xanada həmin
--    domenin TƏK bir dəyəri durur, ona görə münasibət formal olaraq 1NF-i pozmur.
-- 3) Praktikada fərq atomikliyin tərifində deyil, ödənilən qiymətdədir: massivdə FOREIGN KEY,
--    elementə aid UNIQUE və elementə aid əlavə sütun (məsələn telefon növü, təsdiq tarixi) qurmaq olmur.
-- 4) Ona görə mən bu işdə `telebe_telefon` ayrı cədvəlini seçdim - telefon üzrə axtarış,
--    FK və gələcəkdə "əsas nömrə" kimi atribut əlavə etmək lazım ola bilər.
-- 5) Massiv/JSONB isə o vaxt məqbuldur ki, dəyərlər həmişə bütöv halda oxunub yazılır, ayrıca
--    axtarılmır və heç vaxt başqa cədvəllə əlaqələndirilmir (məsələn: log sətrində etiketlər,
--    sorğunun ham cavabı, konfiqurasiya siyahısı).


-- ===== Tapşırıq A2 — İkinci Normal Forma (2NF) ===== (6 bal)
-- a) 1NF cədvəlinin namizəd açarını müəyyən et və seçimi əsaslandır.
-- İzah: (telebe_id, kurs_kod) kompozit açardır. Nə telebe_id tək başına, nə də kurs_kod tək başına sətri unikal etmir — bir tələbə bir neçə kursa yazıla bilər,
-- bir kursa da bir neçə tələbə yazıla bilər. Yalnız bu ikisi birlikdə hər qeydiyyatı unikal müəyyən edir, ona görə namizəd açar budur.

-- b) Bütün qismən asılılıqları X -> Y formasında siyahıla (ən azı 6 ədəd).
-- İzah:

-- telebe_id -> telebe_ad
-- telebe_id -> dogum_tarixi
-- kurs_kod -> kurs_ad
-- kurs_kod -> kurs_saat
-- kurs_kod -> qiymet
-- kurs_kod -> fenn_kod
-- kurs_kod -> fenn_ad
-- (muellim_id, otaq_no, qeyd_tarixi, odenis, imtahan_bali isə tam açara -
-- (telebe_id, kurs_kod) ikisinə birdən - asılıdır, yəni onlar qismən asılılıq deyil.)

-- c) Cədvəli 2NF-ə parçala; bütün CREATE TABLE-ləri PK + FK ilə yaz.
-- Qeyd: aralıq (2NF) cədvəlləri yekun sxemlə ad toqquşmasına düşməsin deyə "_2nf" şəkilçisi ilə
-- adlandırıram və bu bəndin sonunda silmirəm - A2d-dəki itkisizlik yoxlaması onların üzərində gedir.

CREATE TABLE telebe_2nf
(
    telebe_id    VARCHAR(10) PRIMARY KEY,
    telebe_ad    VARCHAR(60) NOT NULL,
    dogum_tarixi DATE        NOT NULL
);

CREATE TABLE kurs_2nf
(
    kurs_kod  VARCHAR(10) PRIMARY KEY,
    kurs_ad   VARCHAR(60)   NOT NULL,
    kurs_saat INT           NOT NULL CHECK (kurs_saat > 0),
    qiymet    NUMERIC(8, 2) NOT NULL CHECK (qiymet >= 0),
    fenn_kod  VARCHAR(10)   NOT NULL,
    fenn_ad   VARCHAR(60)   NOT NULL
);

CREATE TABLE qeydiyyat_2nf
(
    telebe_id     VARCHAR(10)   NOT NULL,
    kurs_kod      VARCHAR(10)   NOT NULL,
    muellim_id    VARCHAR(10)   NOT NULL,
    muellim_ad    VARCHAR(60)   NOT NULL,
    muellim_email VARCHAR(80)   NOT NULL,
    otaq_no       VARCHAR(10)   NOT NULL,
    otaq_tutum    INT           NOT NULL CHECK (otaq_tutum > 0),
    filial_kod    VARCHAR(10)   NOT NULL,
    filial_unvan  VARCHAR(100)  NOT NULL,
    seher         VARCHAR(50)   NOT NULL,
    qeyd_tarixi   DATE          NOT NULL,
    odenis        NUMERIC(8, 2) NOT NULL CHECK (odenis >= 0),
    imtahan_bali  INT CHECK (imtahan_bali BETWEEN 0 AND 100),
    CONSTRAINT pk_qeydiyyat_2nf PRIMARY KEY (telebe_id, kurs_kod),
    CONSTRAINT fk_q2nf_telebe FOREIGN KEY (telebe_id) REFERENCES telebe_2nf (telebe_id),
    CONSTRAINT fk_q2nf_kurs FOREIGN KEY (kurs_kod) REFERENCES kurs_2nf (kurs_kod)
);

-- (telebe_telefon və muellim_dil 1NF-dən dəyişmədən qalır.)

-- Parçalanmanı real göstərmək üçün A1-dəki kurs_qeydiyyat cədvəlini üç hissəyə bölürük:
INSERT INTO telebe_2nf (telebe_id, telebe_ad, dogum_tarixi)
SELECT DISTINCT telebe_id, telebe_ad, dogum_tarixi
FROM kurs_qeydiyyat;

INSERT INTO kurs_2nf (kurs_kod, kurs_ad, kurs_saat, qiymet, fenn_kod, fenn_ad)
SELECT DISTINCT kurs_kod, kurs_ad, kurs_saat, qiymet, fenn_kod, fenn_ad
FROM kurs_qeydiyyat;

INSERT INTO qeydiyyat_2nf (telebe_id, kurs_kod, muellim_id, muellim_ad, muellim_email,
                           otaq_no, otaq_tutum, filial_kod, filial_unvan, seher,
                           qeyd_tarixi, odenis, imtahan_bali)
SELECT telebe_id, kurs_kod, muellim_id, muellim_ad, muellim_email,
       otaq_no, otaq_tutum, filial_kod, filial_unvan, seher,
       qeyd_tarixi, odenis, imtahan_bali
FROM kurs_qeydiyyat;

SELECT (SELECT COUNT(*) FROM kurs_qeydiyyat) AS nf1_setir,
       (SELECT COUNT(*) FROM telebe_2nf)     AS telebe_2nf_setir,
       (SELECT COUNT(*) FROM kurs_2nf)       AS kurs_2nf_setir,
       (SELECT COUNT(*) FROM qeydiyyat_2nf)  AS qeydiyyat_2nf_setir;

-- d) Parçalanmanın itkisiz (lossless-join) olduğunu izah et: hansı sütunlar üzərindən geri birləşdirmə mümkündür?
-- İzah:
-- Parçalanma itkisizdir, çünki hər iki ortaq sütun digər tərəfdə PRIMARY KEY-dir:
-- telebe_id qeydiyyat_2nf-də FK, telebe_2nf-də isə PK-dır; kurs_kod qeydiyyat_2nf-də FK,
-- kurs_2nf-də isə PK-dır. Heath teoreminə görə ortaq sütun bir tərəfdə açar olduqda birləşmə
-- saxta sətir yaratmır, ona görə geri JOIN dəqiq ilkin cədvəli verir.
-- Geri birləşdirmə telebe_id və kurs_kod sütunları üzərindən gedir:

SELECT q.telebe_id, t.telebe_ad, q.kurs_kod, k.kurs_ad, k.fenn_ad, q.muellim_ad, q.odenis
FROM qeydiyyat_2nf q
         JOIN telebe_2nf t USING (telebe_id)
         JOIN kurs_2nf k USING (kurs_kod)
ORDER BY q.telebe_id, q.kurs_kod;

-- İtkisizliyi sübut edirik: geri birləşmə ilə ilkin 1NF cədvəli arasında
-- hər iki istiqamətdə EXCEPT 0 sətir qaytarmalıdır (nə itən, nə də saxta sətir var).
SELECT 'nf1 - join' AS istiqamet, COUNT(*) AS ferqli_setir
FROM (SELECT telebe_id, telebe_ad, dogum_tarixi, kurs_kod, kurs_ad, kurs_saat, qiymet, fenn_kod, fenn_ad,
             muellim_id, muellim_ad, muellim_email, otaq_no, otaq_tutum, filial_kod, filial_unvan, seher,
             qeyd_tarixi, odenis, imtahan_bali
      FROM kurs_qeydiyyat
      EXCEPT
      SELECT t.telebe_id, t.telebe_ad, t.dogum_tarixi, k.kurs_kod, k.kurs_ad, k.kurs_saat, k.qiymet, k.fenn_kod, k.fenn_ad,
             q.muellim_id, q.muellim_ad, q.muellim_email, q.otaq_no, q.otaq_tutum, q.filial_kod, q.filial_unvan, q.seher,
             q.qeyd_tarixi, q.odenis, q.imtahan_bali
      FROM qeydiyyat_2nf q
               JOIN telebe_2nf t USING (telebe_id)
               JOIN kurs_2nf k USING (kurs_kod)) x
UNION ALL
SELECT 'join - nf1', COUNT(*)
FROM (SELECT t.telebe_id, t.telebe_ad, t.dogum_tarixi, k.kurs_kod, k.kurs_ad, k.kurs_saat, k.qiymet, k.fenn_kod, k.fenn_ad,
             q.muellim_id, q.muellim_ad, q.muellim_email, q.otaq_no, q.otaq_tutum, q.filial_kod, q.filial_unvan, q.seher,
             q.qeyd_tarixi, q.odenis, q.imtahan_bali
      FROM qeydiyyat_2nf q
               JOIN telebe_2nf t USING (telebe_id)
               JOIN kurs_2nf k USING (kurs_kod)
      EXCEPT
      SELECT telebe_id, telebe_ad, dogum_tarixi, kurs_kod, kurs_ad, kurs_saat, qiymet, fenn_kod, fenn_ad,
             muellim_id, muellim_ad, muellim_email, otaq_no, otaq_tutum, filial_kod, filial_unvan, seher,
             qeyd_tarixi, odenis, imtahan_bali
      FROM kurs_qeydiyyat) y;
-- Hər iki sətirdə 0 gəlirsə, parçalanma itkisizdir.

-- ===== Tapşırıq A3 — Üçüncü Normal Forma (3NF) ===== (7 bal)
-- a) 2NF-dən sonra qalan tranzitiv asılılıqları tap.
--    (otaq_no -> filial_kod -> filial_unvan, seher və kurs_kod -> fenn_kod -> fenn_ad zəncirlərini unutma)

-- İzah:
-- kurs_kod -> fenn_kod -> fenn_ad
-- (telebe_id, kurs_kod) -> muellim_id -> muellim_ad
-- (telebe_id, kurs_kod) -> muellim_id -> muellim_email
-- (telebe_id, kurs_kod) -> otaq_no -> otaq_tutum
-- (telebe_id, kurs_kod) -> otaq_no -> filial_kod -> filial_unvan
-- (telebe_id, kurs_kod) -> otaq_no -> filial_kod -> seher

-- b) Hamısını 3NF-ə gətir, yekun CREATE TABLE-ləri yaz.
-- İzah: A3-ün nəticəsi elə yuxarıdakı DDL bölməsindəki cədvəllərdir - fenn, kurs,
-- muellim, otaq, filial, telebe, telebe_telefon, qeydiyyat, muellim_dil. Hər tranzitiv
-- zəncir (kurs_kod->fenn_kod->fenn_ad , otaq_no->filial_kod->filial_unvan/seher ,
-- muellim_id->muellim_ad/email) artıq ayrı cədvələ çıxarılıb, qeydiyyat-da yalnız muellim_id və otaq_no FK kimi qalıb.

-- Yekun 3NF cədvəllərinin və onların məhdudiyyətlərinin real mövcudluğunu yoxlayıram:
SELECT c.relname                                             AS cedvel,
       COUNT(*) FILTER (WHERE con.contype = 'p')             AS pk,
       COUNT(*) FILTER (WHERE con.contype = 'f')             AS fk,
       COUNT(*) FILTER (WHERE con.contype = 'u')             AS uq,
       COUNT(*) FILTER (WHERE con.contype = 'c')             AS chk
FROM pg_class c
         JOIN pg_namespace n ON n.oid = c.relnamespace
         LEFT JOIN pg_constraint con ON con.conrelid = c.oid
WHERE n.nspname = 'public'
  AND c.relkind = 'r'
  AND c.relname IN ('filial', 'otaq', 'fenn', 'kurs', 'muellim', 'muellim_dil',
                    'telebe', 'telebe_telefon', 'qeydiyyat', 'qiymet_shkalasi')
GROUP BY c.relname
ORDER BY c.relname;

-- Tranzitiv asılılıqların artıq qeydiyyat cədvəlində olmadığını da göstərmək olar:
-- qeydiyyat-da nə muellim_ad, nə filial_unvan, nə də fenn_ad sütunu qalıb.
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'qeydiyyat'
ORDER BY ordinal_position;

-- c) "F-01 filialının ünvanı dəyişdi" əməliyyatı 3NF-dən əvvəl və sonra neçə sətri UPDATE edir? Hər iki halı yaz.
-- İzah:
-- 3NF-dən əvvəl: F-01-ə aid bütün qeydiyyat sətirləri yenilənməli olur, çünki filial_unvan hər sətirdə təkrarlanır.
-- Bizim datada bu 4 sətirdir (T-01/SQL-101, T-01/PYT-201, T-02/SQL-101, T-05/SQL-101 - hamısı F-01-in A-201/A-305 otaqlarındadır).
-- Real, dolu cədvəldə (milyon/milyard qeydiyyat olduqda) bu, eyni sayda sətri UPDATE etmək deməkdir.
-- 3NF-dən sonra: yalnız filial cədvəlindəki 1 sətir (F-01) yenilənir, qeydiyyat cədvəlinə heç toxunulmur.

-- d) 3NF-in rəsmi tərifi + "açardan, bütün açardan, yalnız açardan" ifadəsinin hansı hissəsi 1NF / 2NF / 3NF-ə aiddir.
-- İzah:
-- 3NF-in rəsmi tərifi: cədvəl 2NF-dədir və heç bir qeyri-açar sütun başqa bir qeyri-açar sütundan tranzitiv asılı deyil (yəni X->Y->Z zənciri yoxdur).
-- "açardan" -> 1NF (hər sütun açara görə müəyyənləşir),
-- "bütün açardan" -> 2NF (kompozit açarın hər iki hissəsinə ehtiyac var, qismən asılılıq yoxdur),
-- "yalnız açardan" -> 3NF (başqa qeyri-açar sütundan asılılıq yoxdur, tranzitiv yoxdur).


-- ===== Tapşırıq A4 — Boyce-Codd Normal Forma (BCNF) ===== (7 bal)
-- Verilən münasibət: ders(telebe_id, fenn_kod, muellim_id)
--
-- a) Bütün funksional asılılıqları və bütün namizəd açarları yaz.
-- İzah:
-- FD-lər: muellim_id -> fenn_kod (biznes qaydası 3: hər müəllim bir fənn tədris edir);
-- (telebe_id, fenn_kod) -> muellim_id (bir tələbə bir fənni bir müəllimdən öyrənir).
-- Namizəd açarlar: (telebe_id, fenn_kod) və (telebe_id, muellim_id) - hər ikisi qalan
-- sütunu müəyyənləşdirir, tək sütunların heç biri (telebe_id, fenn_kod, muellim_id)
-- tək başına kifayət etmir.

-- b) Bu münasibətin 3NF-də olduğunu, lakin BCNF-də olmadığını sübut et. Hansı asılılıq pozur və niyə?
-- İzah:
-- muellim_id -> fenn_kod asılılığı 2NF/3NF-i pozmur, çünki fenn_kod özü açar sütunudur (prime atribut - (telebe_id, fenn_kod) namizəd açarının hissəsidir),
-- 2NF/3NF isə yalnız qeyri-açar sütunların qismən/tranzitiv asılılığını qadağan edir.
-- BCNF isə Y-in açar olub-olmamasına baxmır - hər X->Y üçün X superkey olmalıdır.
-- muellim_id nə PK, nə namizəd açardır, ona görə muellim_id -> fenn_kod BCNF-i pozur.

-- c) BCNF-ə parçala, CREATE TABLE ilə yaz.
-- İzah: pozan asılılığı (muellim_id -> fenn_kod) ayrıca cədvələ çıxarmaq lazımdır.
-- Bu, "sadə" BCNF parçalanmasıdır - asılılığı qorumur (bax: d bəndi).

CREATE TABLE ders_muellim_fenn
(
    muellim_id VARCHAR(10) PRIMARY KEY,
    fenn_kod   VARCHAR(10) NOT NULL,
    CONSTRAINT fk_dmf_fenn FOREIGN KEY (fenn_kod) REFERENCES fenn (fenn_kod)
);

CREATE TABLE ders_telebe_muellim
(
    telebe_id  VARCHAR(10) NOT NULL,
    muellim_id VARCHAR(10) NOT NULL,
    CONSTRAINT pk_dtm PRIMARY KEY (telebe_id, muellim_id),
    CONSTRAINT fk_dtm_telebe FOREIGN KEY (telebe_id) REFERENCES telebe (telebe_id),
    CONSTRAINT fk_dtm_muellim FOREIGN KEY (muellim_id) REFERENCES ders_muellim_fenn (muellim_id)
);

-- Parçalanmanın işlədiyini göstərmək üçün real datayla doldururuq:
INSERT INTO ders_muellim_fenn (muellim_id, fenn_kod)
SELECT muellim_id, fenn_kod
FROM muellim;

INSERT INTO ders_telebe_muellim (telebe_id, muellim_id)
SELECT DISTINCT telebe_id, muellim_id
FROM qeydiyyat;

-- Geri birləşmə ders(telebe_id, fenn_kod, muellim_id) münasibətini verir:
SELECT tm.telebe_id, mf.fenn_kod, tm.muellim_id
FROM ders_telebe_muellim tm
         JOIN ders_muellim_fenn mf ON tm.muellim_id = mf.muellim_id
ORDER BY tm.telebe_id, mf.fenn_kod;

-- Bu parçalanmanın zəif yeri: asağıdakı INSERT heç bir məhdudiyyəti pozmur,
-- halbuki T-01-in eyni fənni (F-SQL) iki müəllimdən öyrənməsi deməkdir.
INSERT INTO ders_telebe_muellim (telebe_id, muellim_id)
VALUES ('T-01', 'M-11');

-- Nəticə: T-01 üçün F-SQL iki dəfə görünür - itən asılılıq özünü məhz burada göstərir.
SELECT tm.telebe_id, mf.fenn_kod, COUNT(*) AS muellim_sayi
FROM ders_telebe_muellim tm
         JOIN ders_muellim_fenn mf ON tm.muellim_id = mf.muellim_id
GROUP BY tm.telebe_id, mf.fenn_kod
HAVING COUNT(*) > 1;

-- d) Parçalanmadan sonra hansı funksional asılılıq itir? Onu PostgreSQL-də necə qorumaq olar (UNIQUE / kompozit FK / generated column / TRIGGER) — ən azı bir üsul + DDL yaz.
-- İzah:
-- İtən FD: (telebe_id, fenn_kod) -> muellim_id. Yuxarıda göstərdiyim kimi, iki cədvəlin heç
-- birində bu asılılığı yoxlayacaq məhdudiyyət yoxdur - onu ancaq JOIN edib əl ilə görmək olur.
-- Qorumaq üçün fenn_kod-u telebe_muellim-ə geri gətirib kompozit FK + UNIQUE qoyuruq:
-- kompozit FK fenn_kod-un müəllimin ƏSL fənni olmasını təmin edir (yəni sütun uydurula bilməz),
-- UNIQUE (telebe_id, fenn_kod) isə itən FD-nin özünü bərpa edir.
-- Bu cədvəllər Bonus d bəndində test olunur.
--
-- Kompromis (dürüstlüyə görə qeyd edirəm): fenn_kod-u telebe_muellim-ə geri gətirməklə
-- həmin cədvəldə muellim_id -> fenn_kod asılılığı yenidən yaranır, yəni telebe_muellim
-- özü artıq təmiz BCNF-də deyil. Bu, bilərəkdən verilən güzəştdir: BCNF ilə asılılığın
-- qorunması (dependency preservation) bu münasibətdə eyni anda mümkün deyil - klassik
-- nəticəyə görə hər parçalanma hər iki xassəni birdən verə bilmir. Seçim belədir:
--   (1) təmiz BCNF + itən asılılıq (yuxarıdakı ders_* cədvəlləri), və ya
--   (2) asılılığın qorunması + kiçik nəzarət olunan artıqlıq (aşağıdakı variant).
-- İkincisini seçdim, çünki artıqlıq FK ilə bağlandığına görə uyğunsuz dəyər ala bilmir -
-- yəni praktikada təhlükəsizdir. Alternativ olaraq TRIGGER yazmaq olardı, lakin deklarativ
-- məhdudiyyət trigger-dən həm daha sürətli, həm də daha etibarlıdır.

CREATE TABLE muellim_fenn
(
    muellim_id VARCHAR(10) PRIMARY KEY,
    fenn_kod   VARCHAR(10) NOT NULL,
    CONSTRAINT fk_mf_fenn FOREIGN KEY (fenn_kod) REFERENCES fenn (fenn_kod),
    CONSTRAINT uq_muellim_fenn UNIQUE (muellim_id, fenn_kod)
);

CREATE TABLE telebe_muellim
(
    telebe_id  VARCHAR(10) NOT NULL,
    muellim_id VARCHAR(10) NOT NULL,
    fenn_kod   VARCHAR(10) NOT NULL,
    CONSTRAINT pk_telebe_muellim PRIMARY KEY (telebe_id, muellim_id),
    CONSTRAINT fk_tm_telebe FOREIGN KEY (telebe_id) REFERENCES telebe (telebe_id),
    CONSTRAINT fk_tm_muellim_fenn FOREIGN KEY (muellim_id, fenn_kod) REFERENCES muellim_fenn (muellim_id, fenn_kod),
    CONSTRAINT uq_telebe_fenn UNIQUE (telebe_id, fenn_kod)
);

INSERT INTO muellim_fenn (muellim_id, fenn_kod)
SELECT muellim_id, fenn_kod
FROM muellim;

INSERT INTO telebe_muellim (telebe_id, muellim_id, fenn_kod)
SELECT DISTINCT q.telebe_id, q.muellim_id, m.fenn_kod
FROM qeydiyyat q
         JOIN muellim m ON q.muellim_id = m.muellim_id;

SELECT * FROM telebe_muellim ORDER BY telebe_id, fenn_kod;


-- ===== Tapşırıq A5 — Dördüncü Normal Forma (4NF) ===== (7 bal)
-- Verilən münasibət: muellim_bacariq(muellim_id, tedris_fenn, bildiyi_dil)
--
-- a) M-05 iki fənn tədris etsəydi və 3 dil bilsəydi neçə sətir olardı? Bütün sətirləri yaz.
-- İzah: 6 sətir (2 fənn x 3 dil - fənn və dil arasında əlaqə olmadığı üçün bütün
-- kombinasiyalar yazılmalıdır). Cədvəli real qurub dolduraraq göstərirəm:

CREATE TABLE muellim_bacariq
(
    muellim_id  VARCHAR(10) NOT NULL,
    tedris_fenn VARCHAR(10) NOT NULL,
    bildiyi_dil VARCHAR(30) NOT NULL,
    CONSTRAINT pk_muellim_bacariq PRIMARY KEY (muellim_id, tedris_fenn, bildiyi_dil),
    CONSTRAINT fk_mb_muellim FOREIGN KEY (muellim_id) REFERENCES muellim (muellim_id),
    CONSTRAINT fk_mb_fenn FOREIGN KEY (tedris_fenn) REFERENCES fenn (fenn_kod)
);

INSERT INTO muellim_bacariq (muellim_id, tedris_fenn, bildiyi_dil)
VALUES ('M-05', 'F-SQL', 'Azərbaycan'),
       ('M-05', 'F-SQL', 'İngilis'),
       ('M-05', 'F-SQL', 'Rus'),
       ('M-05', 'F-PYT', 'Azərbaycan'),
       ('M-05', 'F-PYT', 'İngilis'),
       ('M-05', 'F-PYT', 'Rus');

SELECT * FROM muellim_bacariq ORDER BY tedris_fenn, bildiyi_dil;

SELECT COUNT(*) AS m05_setir_sayi FROM muellim_bacariq WHERE muellim_id = 'M-05';

-- b) Çoxqiymətli asılılıqları X ->-> Y formasında yaz.
-- İzah:
-- muellim_id ->-> tedris_fenn (müəllimin tədris etdiyi fənlər onun bildiyi dillərdən asılı deyil)
-- muellim_id ->-> bildiyi_dil (müəllimin bildiyi dillər onun tədris etdiyi fənlərdən asılı deyil)

-- c) BCNF-də olsa da niyə artıqlıq yaradır? "M-05 alman dilini öyrəndi" neçə yeni sətir? "Yeni fənn" neçə sətir?
-- İzah:
-- BCNF yalnız funksional asılılıqlara baxır, burada isə FD yox, MVD var - ona görə
-- BCNF-i pozan heç nə yoxdur, amma cədvəldə hələ də artıqlıq qalır: fenn və dil
-- əlaqəsiz olduğu üçün, hər yeni dil öyrənəndə onu bütün fənnlər üçün, hər yeni
-- fənn öyrənəndə onu bütün dillər üçün təkrar yazmalı oluruq.
-- Alman dili öyrənsə: 2 yeni sətir (M-05-in 2 fənni var, hər biri ilə cütləşir).
-- Yeni fənn (F-DSC) öyrətsə: 3 yeni sətir (M-05-in 3 dili var, hər biri ilə cütləşir).
-- Bunu real INSERT ilə göstərirəm - bir dil öyrənmək üçün 2 sətir yazmalı oluruq:

INSERT INTO muellim_bacariq (muellim_id, tedris_fenn, bildiyi_dil)
VALUES ('M-05', 'F-SQL', 'Alman'),
       ('M-05', 'F-PYT', 'Alman');

SELECT COUNT(*) AS alman_dilinden_sonra FROM muellim_bacariq WHERE muellim_id = 'M-05';

-- Növbəti bəndin (d) hesablaması 6 sətirlik ilkin vəziyyət üzərində getsin deyə geri qaytarıram:
DELETE FROM muellim_bacariq WHERE bildiyi_dil = 'Alman';

SELECT COUNT(*) AS geri_qaytarildiqdan_sonra FROM muellim_bacariq WHERE muellim_id = 'M-05';

-- d) 4NF-ə parçala, CREATE TABLE yaz və M-05 üçün sətir sayının necə dəyişdiyini göstər (əvvəl -> sonra).
-- İzah: iki müstəqil çoxqiymətli faktı ayrı cədvəllərə bölürük.
-- (Yekun sxemdəki muellim_dil ilə ad toqquşmasın deyə "_4nf" şəkilçisi ilə adlandırıram.)

CREATE TABLE muellim_fenn_4nf
(
    muellim_id  VARCHAR(10) NOT NULL,
    tedris_fenn VARCHAR(10) NOT NULL,
    CONSTRAINT pk_muellim_fenn_4nf PRIMARY KEY (muellim_id, tedris_fenn),
    CONSTRAINT fk_mf4_muellim FOREIGN KEY (muellim_id) REFERENCES muellim (muellim_id),
    CONSTRAINT fk_mf4_fenn FOREIGN KEY (tedris_fenn) REFERENCES fenn (fenn_kod)
);

CREATE TABLE muellim_dil_4nf
(
    muellim_id  VARCHAR(10) NOT NULL,
    bildiyi_dil VARCHAR(30) NOT NULL,
    CONSTRAINT pk_muellim_dil_4nf PRIMARY KEY (muellim_id, bildiyi_dil),
    CONSTRAINT fk_md4_muellim FOREIGN KEY (muellim_id) REFERENCES muellim (muellim_id)
);

INSERT INTO muellim_fenn_4nf (muellim_id, tedris_fenn)
SELECT DISTINCT muellim_id, tedris_fenn FROM muellim_bacariq;

INSERT INTO muellim_dil_4nf (muellim_id, bildiyi_dil)
SELECT DISTINCT muellim_id, bildiyi_dil FROM muellim_bacariq;

-- Sətir sayı: əvvəl 6 -> sonra 2 + 3 = 5
SELECT (SELECT COUNT(*) FROM muellim_bacariq WHERE muellim_id = 'M-05')    AS evvel_4nf,
       (SELECT COUNT(*) FROM muellim_fenn_4nf WHERE muellim_id = 'M-05')   AS sonra_fenn,
       (SELECT COUNT(*) FROM muellim_dil_4nf WHERE muellim_id = 'M-05')    AS sonra_dil,
       (SELECT COUNT(*) FROM muellim_fenn_4nf WHERE muellim_id = 'M-05')
           + (SELECT COUNT(*) FROM muellim_dil_4nf WHERE muellim_id = 'M-05') AS sonra_cemi;

-- İtkisizliyi yoxlayırıq: iki cədvəlin geri birləşməsi ilkin muellim_bacariq-ı verməlidir (0 fərq).
SELECT COUNT(*) AS ferqli_setir
FROM (SELECT muellim_id, tedris_fenn, bildiyi_dil FROM muellim_bacariq
      EXCEPT
      SELECT f.muellim_id, f.tedris_fenn, d.bildiyi_dil
      FROM muellim_fenn_4nf f
               JOIN muellim_dil_4nf d ON f.muellim_id = d.muellim_id) x;

-- M-05 üçün sətir sayı: əvvəl 6 (2 fənn x 3 dil) -> sonra 5 (muellim_fenn_4nf-də 2 + muellim_dil_4nf-də 3).
-- Burda əsil fərq məlumatın çoxluqundadı. Dəyər sayı artdıqca (məs. 5 fənn, 5 dil) əvvəlki 25 sətir olardı, sonrakı isə cəmi 10 - fərq multiplikativdən additivə düşür.

-- ===== Tapşırıq A6 — Beşinci Normal Forma (5NF / PJNF) ===== (5 bal)
-- Verilən münasibət: tedris_plani(muellim_id, kurs_kod, filial_kod)
--
-- a) Cədvəldə heç bir FD və MVD olmadığını qısaca əsaslandır (yəni 4NF-dədir).
-- Müqayisə üçün ilkin (parçalanmamış) cədvəli real qururam:

CREATE TABLE tedris_plani
(
    muellim_id VARCHAR(10) NOT NULL,
    kurs_kod   VARCHAR(10) NOT NULL,
    filial_kod VARCHAR(10) NOT NULL,
    CONSTRAINT pk_tedris_plani PRIMARY KEY (muellim_id, kurs_kod, filial_kod),
    CONSTRAINT fk_tp_muellim FOREIGN KEY (muellim_id) REFERENCES muellim (muellim_id),
    CONSTRAINT fk_tp_kurs FOREIGN KEY (kurs_kod) REFERENCES kurs (kurs_kod),
    CONSTRAINT fk_tp_filial FOREIGN KEY (filial_kod) REFERENCES filial (filial_kod)
);

INSERT INTO tedris_plani (muellim_id, kurs_kod, filial_kod)
VALUES ('M-05', 'SQL-101', 'F-01'),
       ('M-05', 'SQL-202', 'F-01'),
       ('M-11', 'SQL-101', 'F-01'),
       ('M-05', 'SQL-101', 'F-02');

SELECT * FROM tedris_plani ORDER BY muellim_id, kurs_kod, filial_kod;

-- İzah:

-- FD yoxdur: muellim_id kurs_kod-u müəyyənləşdirmir (M-05 iki fərqli kurs tədris edir),
-- kurs_kod filial_kod-u müəyyənləşdirmir (SQL-101 iki fərqli filialda keçirilir), və
-- muellim_id filial_kod-u müəyyənləşdirmir (M-05 iki fərqli filialda işləyir).

-- MVD də yoxdur: əgər muellim_id ->-> kurs_kod olsaydı, M-05-in bütün kurs/filial
-- kombinasiyaları cədvəldə olmalıydı (4 sətir), amma yalnız 3-ü var (SQL-202/F-02 yoxdur) -
-- deməli MVD-yə uyğun gəlmir. Heç bir FD/MVD olmadığı üçün cədvəl artıq BCNF/4NF-dədir.

-- b) Üç binar proyeksiyaya ayır: muellim_kurs, kurs_filial, muellim_filial. Hər üçünün məzmununu yaz.
-- İzah:

-- muellim_kurs (muellim_id, kurs_kod):
-- M-05, SQL-101
-- M-05, SQL-202
-- M-11, SQL-101
--
-- kurs_filial (kurs_kod, filial_kod):
-- SQL-101, F-01
-- SQL-202, F-01
-- SQL-101, F-02
--
-- muellim_filial (muellim_id, filial_kod):
-- M-05, F-01
-- M-11, F-01
-- M-05, F-02

-- c) Üçünü geri birləşdir və nəticənin ilkin cədvəllə eyni olduğunu göstər (NATURAL JOIN və ya USING ilə).

SELECT *
FROM muellim_kurs
         NATURAL JOIN kurs_filial
         NATURAL JOIN muellim_filial;

/**
    M-05,F-02,SQL-101
    M-05,F-01,SQL-101
    M-05,F-01,SQL-202
    M-11,F-01,SQL-101
 */

-- Nəticənin ilkin cədvəllə EYNİ olduğunu sözlə deyil, EXCEPT ilə sübut edirəm:
-- hər iki istiqamətdə 0 fərq olmalıdır (nə itən sətir, nə saxta sətir).
SELECT 'plan - join' AS istiqamet, COUNT(*) AS ferqli_setir
FROM (SELECT muellim_id, kurs_kod, filial_kod FROM tedris_plani
      EXCEPT
      SELECT muellim_id, kurs_kod, filial_kod
      FROM muellim_kurs NATURAL JOIN kurs_filial NATURAL JOIN muellim_filial) a
UNION ALL
SELECT 'join - plan', COUNT(*)
FROM (SELECT muellim_id, kurs_kod, filial_kod
      FROM muellim_kurs NATURAL JOIN kurs_filial NATURAL JOIN muellim_filial
      EXCEPT
      SELECT muellim_id, kurs_kod, filial_kod FROM tedris_plani) b;

-- d) Yalnız iki proyeksiyanı (muellim_kurs |><| kurs_filial) birləşdir.
--    Hansı saxta sətir (spurious tuple) yaranır? Real həyatda nəyi səhv iddia edir və bu niyə 5NF-i zəruri edir?

SELECT *
FROM muellim_kurs
         NATURAL JOIN kurs_filial;

-- Nəticə: 5 sətir - saxta sətir (M-11, SQL-101, F-02):

/**
    SQL-101,M-05,F-02
    SQL-101,M-05,F-01
    SQL-202,M-05,F-01
    SQL-101,M-11,F-02
    SQL-101,M-11,F-01
 */

-- Saxta sətri "gözlə tapmaq" əvəzinə EXCEPT ilə birbaşa çıxarıram:
SELECT muellim_id, kurs_kod, filial_kod
FROM (SELECT muellim_id, kurs_kod, filial_kod FROM muellim_kurs NATURAL JOIN kurs_filial
      EXCEPT
      SELECT muellim_id, kurs_kod, filial_kod FROM tedris_plani) saxta;

-- M-11 F-02-də işləmədiyi üçün bu, "M-11 SQL-101-i F-02-də tədris edir" kimi yanlış fakt iddia edir.
-- Yalnız iki cədvəlin birləşməsi üçüncü faktı (kim harda işləyir) yoxlamadığı üçün belə saxta
-- kombinasiyalar yaranır - buna görə 5NF üç proyeksiyanın hamısını tələb edir.

-- ===== Tapşırıq A7 — Yekun sxem ===== (7 bal)
-- a) A1-A6-nın nəticələrini birləşdirib tam normallaşdırılmış sxemi tək işlək skript kimi yaz (yuxarıdakı DDL bölməsinə bax).
-- İzah: Bax yuxarıdakı DDL bölməsi (filial, otaq, fenn, kurs, muellim, muellim_dil,
-- telebe, telebe_telefon, qeydiyyat, qiymet_shkalasi, muellim_kurs, kurs_filial,
-- muellim_filial) - bütün PK/FK/UNIQUE/CHECK/NOT NULL-larla, düzgün ardıcıllıqla
-- (asılı cədvəllər sonra). Müəllimlərin mentorluq əlaqəsi (7-ci biznes qaydası)
-- muellim.mentor_id-nin öz-özünə istinad edən FK-si ilə təmin olunub.

-- b) Sxemin ER diaqramı + kardinallıqlar (1:1, 1:N, M:N).
-- İzah:
-- Diaqramın qrafik (rəngli) variantı təqdim olunan PDF-də "A7b — ER diaqram" səhifəsindədir.
-- Aşağıda həmin diaqramın mətn içindəki eyni variantı verilib.
-- Oxunuş qaydası: "1" tərəfi valideyn (PK), "N" tərəfi övlad (FK) tərəfdir.
--
--   ┌──────────────────┐                    ┌──────────────────┐
--   │ FILIAL           │                    │ FENN             │
--   │ PK filial_kod    │                    │ PK fenn_kod      │
--   │    filial_unvan  │                    │ UQ fenn_ad       │
--   │    seher         │                    └───┬───────────┬──┘
--   └───┬──────────────┘                      1 │         1 │
--     1 │                                       │           │
--       │ N                                   N │           │ N
--   ┌───┴──────────────┐                ┌────────┴───────┐ ┌─┴────────────────┐
--   │ OTAQ             │                │ KURS           │ │ MUELLIM          │◄──┐
--   │ PK otaq_no       │                │ PK kurs_kod    │ │ PK muellim_id    │   │ 1
--   │    otaq_tutum    │                │    kurs_ad     │ │    muellim_ad    │   │ (mentor,
--   │ FK filial_kod    │                │    kurs_saat   │ │ UQ muellim_email │   │  öz-özünə
--   └───┬──────────────┘                │    qiymet      │ │ FK fenn_kod      │   │  istinad)
--     1 │                               │ FK fenn_kod    │ │ FK mentor_id ────┼───┘ N
--       │                               └───┬────────────┘ └───┬───────────┬──┘
--       │                                 1 │                1 │         1 │
--       │  N                                │ N                │ N         │ N
--   ┌───┴───────────────────────────────────┴──────────────────┴───┐   ┌───┴──────────────┐
--   │ QEYDIYYAT   (telebe ↔ kurs M:N əlaqəsinin körpü cədvəli)      │   │ MUELLIM_DIL      │
--   │ PK telebe_id, kurs_kod                                        │   │ PK muellim_id    │
--   │ FK telebe_id → TELEBE      FK kurs_kod → KURS                 │   │ PK dil           │
--   │ FK muellim_id → MUELLIM    FK otaq_no → OTAQ                  │   └──────────────────┘
--   │    qeyd_tarixi, odenis, imtahan_bali (NULL ola bilər)         │
--   └───┬───────────────────────────────────────────────────────────┘
--       │ N
--     1 │
--   ┌───┴──────────────┐   1        N   ┌──────────────────┐
--   │ TELEBE           ├────────────────┤ TELEBE_TELEFON   │
--   │ PK telebe_id     │                │ PK telebe_id     │
--   │    telebe_ad     │                │ PK telefon       │
--   │    dogum_tarixi  │                └──────────────────┘
--   └──────────────────┘
--
--   5NF üçün üç binar körpü cədvəli (hamısı M:N açır):
--
--   MUELLIM ──1──N┤ MUELLIM_KURS   ├N──1── KURS       (PK muellim_id, kurs_kod)
--   KURS    ──1──N┤ KURS_FILIAL    ├N──1── FILIAL     (PK kurs_kod, filial_kod)
--   MUELLIM ──1──N┤ MUELLIM_FILIAL ├N──1── FILIAL     (PK muellim_id, filial_kod)
--
--   QIYMET_SHKALASI (PK herf, min_bal, max_bal) — heç bir cədvələ FK ilə bağlı deyil,
--   qeydiyyat ilə yalnız non-equi (BETWEEN) JOIN üzərindən əlaqələnir (B9).
--
-- Kardinallıqlar:
--   1:N  →  filial-otaq, fenn-kurs, fenn-muellim, muellim-mentor (rekursiv),
--           telebe-telebe_telefon, muellim-muellim_dil,
--           telebe-qeydiyyat, kurs-qeydiyyat, muellim-qeydiyyat, otaq-qeydiyyat
--   M:N  →  telebe-kurs (qeydiyyat körpüsü ilə), muellim-kurs (muellim_kurs),
--           kurs-filial (kurs_filial), muellim-filial (muellim_filial)
--   1:1  →  bu sxemdə yoxdur.

-- c) Bir cümlə ilə: normallaşdırma nə vaxt ziyanlıdır, denormalizasiya nə vaxt əsaslandırılır?
-- İzah:
-- Normallaşdırma oxuma zamanı çox sayda JOIN tələb edib performansı aşağı saldıqda
-- ziyanlıdır; denormalizasiya isə oxuma sürəti yazma zamanı yaranan az miqdarda
-- artıqlıqdan daha vacib olduqda (məs. hesabat/analitika cədvəllərində) əsaslandırılır.


-- #########################################################
-- #                                                       #
-- #   HİSSƏ B — JOIN NÖVLƏRİ                              #
-- #                                                       #
-- #########################################################


-- ===== Tapşırıq B1 — INNER JOIN ===== (4 bal)
-- Hər qeydiyyat üçün: tələbənin adı, kursun adı, fənnin adı, ödəniş.
-- Nəticəni tələbə adına görə sırala.
SELECT t.telebe_ad, k.kurs_ad, f.fenn_ad, q.odenis
FROM telebe t
         JOIN qeydiyyat q ON t.telebe_id = q.telebe_id
         JOIN kurs k ON q.kurs_kod = k.kurs_kod
         JOIN fenn f ON k.fenn_kod = f.fenn_kod
ORDER BY t.telebe_ad;

-- İzah: nəticədə neçə sətir var və T-04 niyə yoxdur?
-- İzah: 7 sətir (qeydiyyat sayı qədər). T-04 heç bir kursa yazılmadığı üçün
-- qeydiyyat cədvəlində uyğun sətri yoxdur, ona görə INNER JOIN-da görünmür.


-- ===== Tapşırıq B2 — LEFT (OUTER) JOIN ===== (3 bal)
-- (qeyd: mənbə sənəddə bu bənd bəzən "B3" kimi də göstərilib)
--
-- a) Bütün tələbələri telefon nömrələri ilə göstər;
--    nömrəsi olmayan üçün 'Nömrə yoxdur' (COALESCE).
SELECT t.telebe_ad, COALESCE(tt.telefon, 'Nömrə yoxdur') AS telefon
FROM telebe t
         LEFT JOIN telebe_telefon tt ON t.telebe_id = tt.telebe_id
ORDER BY t.telebe_ad;

-- b) Bütün qeydiyyatları imtahan balı ilə göstər;
--    imtahan verməmişlər üçün 'İmtahan verilməyib'.
SELECT t.telebe_ad, k.kurs_ad, COALESCE(q.imtahan_bali::TEXT, 'İmtahan verilməyib') AS imtahan_bali
FROM qeydiyyat q
         JOIN telebe t ON q.telebe_id = t.telebe_id
         JOIN kurs k ON q.kurs_kod = k.kurs_kod
ORDER BY t.telebe_ad;

-- Qeyd: burada telebe/kurs ilə INNER JOIN kifayətdir, çünki imtahan_bali qeydiyyat-ın
-- ÖZ sütunudur və hər qeydiyyatın FK-ları mütləq mövcuddur - yəni INNER JOIN heç bir
-- qeydiyyatı itirmir. Bölmə LEFT JOIN-a aid olduğu üçün eyni sualın "tələbə tərəfindən"
-- variantını da yazıram: burada LEFT JOIN həqiqətən lazımdır, çünki T-04-ün heç bir
-- qeydiyyatı yoxdur və INNER JOIN olsa o tamamilə itərdi.
SELECT t.telebe_ad,
       COALESCE(k.kurs_ad, 'Qeydiyyatı yoxdur')                            AS kurs_ad,
       COALESCE(q.imtahan_bali::TEXT, 'İmtahan verilməyib')                AS imtahan_bali
FROM telebe t
         LEFT JOIN qeydiyyat q ON t.telebe_id = q.telebe_id
         LEFT JOIN kurs k ON q.kurs_kod = k.kurs_kod
ORDER BY t.telebe_ad;

-- c) Tipik səhv: LEFT JOIN-u INNER JOIN-a çevirən səhv nədir?
--    Şərti ON əvəzinə WHERE-ə yaz, hər iki sorğunu müqayisə et.
-- Ssenari: "hər tələbəni göstər, amma yalnız 055 ilə başlayan nömrələrini".
-- Düzgün - süzgəc ON-un içindədir, LEFT JOIN qorunur:
SELECT t.telebe_ad, tt.telefon
FROM telebe t
         LEFT JOIN telebe_telefon tt
                   ON t.telebe_id = tt.telebe_id AND tt.telefon LIKE '055%'
ORDER BY t.telebe_ad;

-- Səhv - EYNİ şərt ON-dan WHERE-ə köçürülüb:
SELECT t.telebe_ad, tt.telefon
FROM telebe t
         LEFT JOIN telebe_telefon tt ON t.telebe_id = tt.telebe_id
WHERE tt.telefon LIKE '055%'
ORDER BY t.telebe_ad;

-- İzah: fərq şərtin NƏ VAXT işləməsindədir. ON şərti birləşmə ANINDA tətbiq olunur -
-- uyğun gəlməyən sətir üçün sağ tərəf sadəcə NULL ilə doldurulur, sol sətir qalır;
-- ona görə birinci sorğu 5 tələbənin hamısını qaytarır (T-02, T-04, T-05 və 070-li
-- nömrələr NULL kimi görünür). WHERE isə birləşmə BİTDİKDƏN sonra işləyir və
-- NULL LIKE '055%' nəticəsi UNKNOWN olduğu üçün o sətirləri atır - nəticədə yalnız
-- 055 ilə başlayan 2 sətir qalır və LEFT JOIN faktiki INNER JOIN-a çevrilir.
-- Qayda: sağ cədvələ aid süzgəc ON-a, sol cədvələ aid süzgəc WHERE-ə yazılır.


-- ===== Tapşırıq B3 — RIGHT (OUTER) JOIN ===== (3 bal)
-- RIGHT JOIN ilə bütün kursları və onlara yazılan tələbələrin sayını çıxar.
-- Tələbəsi olmayan kurs (PYT-305) da 0 ilə görünməlidir.
SELECT k.kurs_ad, COUNT(q.telebe_id) AS telebe_sayi
FROM qeydiyyat q
         RIGHT JOIN kurs k ON q.kurs_kod = k.kurs_kod
GROUP BY k.kurs_ad
ORDER BY k.kurs_ad;

-- Eyni nəticəni LEFT JOIN ilə al:
SELECT k.kurs_ad, COUNT(q.telebe_id) AS telebe_sayi
FROM kurs k
         LEFT JOIN qeydiyyat q ON k.kurs_kod = q.kurs_kod
GROUP BY k.kurs_ad
ORDER BY k.kurs_ad;

-- İzah: praktikada RIGHT JOIN niyə nadir işlədilir?
-- İzah: RIGHT JOIN-u LEFT JOIN-a çevirmək üçün sadəcə iki cədvəlin yerini dəyişmək
-- kifayətdir, nəticə eynidir. Əksər inkişafçılar "əsas" cədvəli həmişə FROM-da solda
-- yazmağa öyrəşdiyi üçün LEFT JOIN daha oxunaqlı sayılır, RIGHT JOIN-a ehtiyac qalmır.


-- ===== Tapşırıq B4 — FULL OUTER JOIN ===== (4 bal)
-- a) fenn və kurs cədvəllərini fenn_kod üzrə FULL OUTER JOIN et.
--    CASE ilə vəziyyət sütunu: 'Uyğun' / 'Kursu olmayan fənn' / 'Fənni olmayan kurs'.
SELECT f.fenn_kod, f.fenn_ad, k.kurs_kod, k.kurs_ad,
       CASE
           WHEN f.fenn_kod IS NOT NULL AND k.kurs_kod IS NOT NULL THEN 'Uyğun'
           WHEN k.kurs_kod IS NULL THEN 'Kursu olmayan fənn'
           ELSE 'Fənni olmayan kurs'
           END AS veziyyet
FROM fenn f
         FULL OUTER JOIN kurs k ON f.fenn_kod = k.fenn_kod;

-- b) Yalnız uyğunsuz sətirləri qaytaran sorğu (FULL OUTER + IS NULL).
SELECT f.fenn_kod, f.fenn_ad, k.kurs_kod, k.kurs_ad
FROM fenn f
         FULL OUTER JOIN kurs k ON f.fenn_kod = k.fenn_kod
WHERE f.fenn_kod IS NULL
   OR k.kurs_kod IS NULL;

-- c) muellim və qeydiyyat cədvəlləri arasında FULL OUTER JOIN.
--    M-13 görünürmü? Niyə "sağ tərəfdə tək qalan" sətir yoxdur? (İpucu: FOREIGN KEY)
SELECT m.muellim_id, m.muellim_ad, q.telebe_id, q.kurs_kod
FROM muellim m
         FULL OUTER JOIN qeydiyyat q ON m.muellim_id = q.muellim_id;

-- İzah: M-13 görünür (NULL qeydiyyat sütunları ilə), çünki heç bir dərsi yoxdur.
-- Sağ tərəfdə tək qalan sətir yoxdur, çünki qeydiyyat.muellim_id FOREIGN KEY ilə
-- muellim-ə bağlıdır - hər qeydiyyat sətrinin mütləq mövcud bir müəllimi olmalıdır,
-- ona görə uyğunsuz (orphan) sağ sətir mümkün deyil.

-- d) Eyni nəticəni LEFT JOIN UNION RIGHT JOIN ilə al.
--    Burada UNION yerinə UNION ALL niyə işləməz?
SELECT m.muellim_id, m.muellim_ad, q.telebe_id, q.kurs_kod
FROM muellim m
         LEFT JOIN qeydiyyat q ON m.muellim_id = q.muellim_id
UNION
SELECT m.muellim_id, m.muellim_ad, q.telebe_id, q.kurs_kod
FROM muellim m
         RIGHT JOIN qeydiyyat q ON m.muellim_id = q.muellim_id;

-- İzah: UNION ALL işləməzdi, çünki hər iki tərəfdə uyğun gələn (əsl JOIN olunan)
-- sətirlər HƏR İKİ sorğuda eyni şəkildə görünür - UNION ALL bunları TƏKRAR yazardı.
-- UNION isə təkrarları avtomatik silir, düzgün nəticəni verir.


-- ===== Tapşırıq B5 — CROSS JOIN ===== (3 bal)
-- a) filial x qiymet_shkalasi Dekart hasili.
--    Sətir sayını sorğudan ƏVVƏL hesabla, sonra yoxla.
-- Gözlənilən sətir sayı: 3 (filial) x 5 (qiymet_shkalasi) = 15
SELECT f.filial_kod, qs.herf
FROM filial f
         CROSS JOIN qiymet_shkalasi qs
ORDER BY f.filial_kod, qs.herf;

SELECT COUNT(*) AS setir_sayi
FROM filial f
         CROSS JOIN qiymet_shkalasi qs;


-- b) CROSS JOIN ilə "fənn x qiymət hərfi" matrisi; hər xanada həmin fənn üzrə
--    həmin qiyməti alan tələbələrin sayı. Sıfır xanalar da görünsün.
--    (İpucu: CROSS JOIN + LEFT JOIN + COUNT)
SELECT f.fenn_ad, qs.herf, COUNT(DISTINCT q.telebe_id) AS telebe_sayi
FROM fenn f
         CROSS JOIN qiymet_shkalasi qs
         LEFT JOIN kurs k ON k.fenn_kod = f.fenn_kod
         LEFT JOIN qeydiyyat q
                   ON q.kurs_kod = k.kurs_kod AND q.imtahan_bali BETWEEN qs.min_bal AND qs.max_bal
GROUP BY f.fenn_ad, qs.herf
ORDER BY f.fenn_ad, qs.herf;
-- Qeyd: DISTINCT vacibdir - tapşırıq "tələbələrin sayı" deyir, bir fənnin isə bir neçə kursu
-- ola bilər. DISTINCT olmasa eyni tələbə F-SQL-in həm SQL-101, həm SQL-202 kursundan eyni
-- hərfi alsaydı iki dəfə sayılardı. Cari datada belə hal yoxdur, amma sorğu düzgün olmalıdır.

-- c) Təsadüfi Dekart partlayışının əsas səbəbi bir cümlə ilə.
-- İzah: iki cədvəl arasında JOIN şərti (ON) unudulur və ya səhv yazılır, nəticədə
-- CROSS JOIN kimi davranan qeyri-adi böyük nəticə yaranır.


-- ===== Tapşırıq B6 — SELF JOIN ===== (3 bal)
-- a) Hər müəllimin adı və mentorunun adı; mentoru olmayanlar üçün 'Mentoru yoxdur'.
SELECT m.muellim_ad, COALESCE(mentor.muellim_ad, 'Mentoru yoxdur') AS mentor_ad
FROM muellim m
         LEFT JOIN muellim mentor ON m.mentor_id = mentor.muellim_id
ORDER BY m.muellim_ad;

-- b) Mentoru ilə eyni fənni tədris edən müəllimləri tap.
SELECT m.muellim_ad, mentor.muellim_ad AS mentor_ad, m.fenn_kod
FROM muellim m
         JOIN muellim mentor ON m.mentor_id = mentor.muellim_id
WHERE m.fenn_kod = mentor.fenn_kod;
-- Nəticə: 0 sətir - bizim datada heç bir müəllim mentoru ilə eyni fənni tədris etmir.

-- c) Eyni kursa yazılmış, fərqli tələbələrdən ibarət cütlüklər.
--    Hər cütlük bir dəfə ((A,B) varsa (B,A) olmasın).
SELECT q1.telebe_id AS telebe_1, q2.telebe_id AS telebe_2, q1.kurs_kod
FROM qeydiyyat q1
         JOIN qeydiyyat q2 ON q1.kurs_kod = q2.kurs_kod AND q1.telebe_id < q2.telebe_id
ORDER BY q1.kurs_kod;

-- d) İzah: SELF JOIN-da alias niyə məcburidir?
-- İzah: eyni cədvəl FROM-da iki dəfə göründüyü üçün, alias olmasa Postgres hansı
-- sütunun hansı "nüsxəyə" aid olduğunu ayırd edə bilmir və "ambiguous" xətası verir.

-- İki səviyyəli zəncir (müəllim -> mentor -> mentorun mentoru):
SELECT m.muellim_ad,
       mentor.muellim_ad          AS mentor_ad,
       mentor_of_mentor.muellim_ad AS mentorun_mentoru
FROM muellim m
         LEFT JOIN muellim mentor ON m.mentor_id = mentor.muellim_id
         LEFT JOIN muellim mentor_of_mentor ON mentor.mentor_id = mentor_of_mentor.muellim_id
ORDER BY m.muellim_ad;


-- ===== Tapşırıq B7 — NATURAL JOIN, USING və ON ===== (10 bal)
-- a) otaq və filial cədvəllərini NATURAL JOIN ilə birləşdir.
--    Birləşmə hansı sütun(lar) üzrə gedir?
SELECT *
FROM otaq
         NATURAL JOIN filial;
-- İzah: filial_kod hər iki cədvəldə eyni adla mövcuddur, NATURAL JOIN avtomatik
-- olaraq bu ortaq sütun üzərindən birləşir.

-- b) Eyni sorğunu JOIN ... USING (filial_kod) ilə yaz.
--    SELECT * ilə yoxla: USING nəticə sütunlarına necə təsir edir?
SELECT *
FROM otaq
         JOIN filial USING (filial_kod);
-- İzah: USING da NATURAL JOIN kimi filial_kod sütununu nəticədə YALNIZ BİR DƏFƏ
-- göstərir, amma fərqli olaraq birləşmə sütununu özün əl ilə seçirsən (bütün ortaq
-- adlı sütunlar avtomatik seçilmir).

-- c) Eyni sorğunu JOIN ... ON ilə yaz və üç variantın sütunlarını müqayisə et.
SELECT *
FROM otaq o
         JOIN filial f ON o.filial_kod = f.filial_kod;
-- İzah: ON versiyasında filial_kod sütunu İKİ DƏFƏ görünür (həm otaq-dan, həm
-- filial-dan), çünki ON heç bir sütunu birləşdirmir/gizlətmir - NATURAL/USING isə
-- ortaq sütunu tək dəfə göstərir.

-- d) Təhlükə ssenarisi: hər iki cədvələ created_at TIMESTAMP əlavə et
--    və NATURAL JOIN-u yenidən icra et. Sonda DROP COLUMN et.
ALTER TABLE otaq
    ADD COLUMN created_at TIMESTAMP;
ALTER TABLE filial
    ADD COLUMN created_at TIMESTAMP;

SELECT *
FROM otaq
         NATURAL JOIN filial;
-- Nəticə: 0 sətir! İndi NATURAL JOIN filial_kod İLƏ YANAŞI created_at sütunu
-- üzərindən də birləşməyə çalışır - hər iki tərəfdə created_at NULL olduğu üçün
-- (NULL = NULL yalan sayılır) heç bir sətir uyğun gəlmir.

ALTER TABLE otaq
    DROP COLUMN created_at;
ALTER TABLE filial
    DROP COLUMN created_at;

-- İzah: nəticə necə dəyişdi, NATURAL JOIN istehsalda niyə tövsiyə edilmir?
-- İzah: cədvələ yeni, hətta əlaqəsiz bir sütun (created_at) əlavə olunanda,
-- NATURAL JOIN-un birləşmə məntiqi səssizcə dəyişdi - inkişafçı bunu görmür,
-- gözlənilməz (bəzən boş) nəticələr yaranır. Ona görə istehsalda ON/USING üstünlük təşkil edir.


-- ===== Tapşırıq B8 — Çoxcədvəlli JOIN zənciri ===== (4 bal)
-- Ən azı 6 cədvəl: tələbə adı, kurs adı, fənn adı, müəllim adı, otaq nömrəsi,
-- otaq tutumu, filial ünvanı, şəhər, ödəniş, imtahan balı.
--
-- a) Yalnız imtahan vermiş tələbələr üçün (tam INNER zənciri).
SELECT t.telebe_ad,
       k.kurs_ad,
       f.fenn_ad,
       m.muellim_ad,
       o.otaq_no,
       o.otaq_tutum,
       fl.filial_unvan,
       fl.seher,
       q.odenis,
       q.imtahan_bali
FROM qeydiyyat q
         JOIN telebe t ON q.telebe_id = t.telebe_id
         JOIN kurs k ON q.kurs_kod = k.kurs_kod
         JOIN fenn f ON k.fenn_kod = f.fenn_kod
         JOIN muellim m ON q.muellim_id = m.muellim_id
         JOIN otaq o ON q.otaq_no = o.otaq_no
         JOIN filial fl ON o.filial_kod = fl.filial_kod
WHERE q.imtahan_bali IS NOT NULL
ORDER BY t.telebe_ad;

-- b) İmtahan verməmiş qeydiyyatlar (T-05) da görünsün.
--    Hansı bənddə hansı JOIN növünü dəyişdin?
SELECT t.telebe_ad,
       k.kurs_ad,
       f.fenn_ad,
       m.muellim_ad,
       o.otaq_no,
       o.otaq_tutum,
       fl.filial_unvan,
       fl.seher,
       q.odenis,
       q.imtahan_bali
FROM qeydiyyat q
         JOIN telebe t ON q.telebe_id = t.telebe_id
         JOIN kurs k ON q.kurs_kod = k.kurs_kod
         JOIN fenn f ON k.fenn_kod = f.fenn_kod
         JOIN muellim m ON q.muellim_id = m.muellim_id
         JOIN otaq o ON q.otaq_no = o.otaq_no
         JOIN filial fl ON o.filial_kod = fl.filial_kod
ORDER BY t.telebe_ad;

-- İzah: heç bir JOIN növünü dəyişmədim - sadəcə WHERE q.imtahan_bali IS NOT NULL
-- şərtini sildim. T-05-in bütün FK-ləri (telebe, kurs, muellim, otaq, filial)
-- mövcud olduğu üçün INNER zəncir onu onsuz da tuturdu, yalnız WHERE gizlədirdi.

-- c) Zəncirdə LEFT JOIN-dan sonra gələn INNER JOIN nə üçün bütün nəticəni
--    "daxili birləşməyə" çevirə bilər? Qəsdən yarat və göstər.
SELECT t.telebe_ad, k.kurs_ad
FROM telebe t
         LEFT JOIN qeydiyyat q ON t.telebe_id = q.telebe_id
         JOIN kurs k ON q.kurs_kod = k.kurs_kod
ORDER BY t.telebe_ad;

-- İzah: T-04 yenə görünmür, çünki onun q.kurs_kod-u NULL-dur, INNER JOIN isə
-- NULL-u heç bir kurs_kod ilə uyğunlaşdırmır - bu, LEFT JOIN-un "hamısını saxla"
-- effektini sıradan çıxarır, bütün zəncir faktiki INNER JOIN kimi davranır.


-- ===== Tapşırıq B9 — Non-equi JOIN ===== (3 bal)
-- a) Hər imtahan nəticəsinin hərf qiymətini tap:
--    qeydiyyat ilə qiymet_shkalasi-nı BETWEEN şərti ilə birləşdir ("=" işlətmə).
SELECT t.telebe_ad, q.kurs_kod, q.imtahan_bali, qs.herf
FROM qeydiyyat q
         JOIN telebe t ON q.telebe_id = t.telebe_id
         JOIN qiymet_shkalasi qs ON q.imtahan_bali BETWEEN qs.min_bal AND qs.max_bal
ORDER BY t.telebe_ad;
-- Nəticə: 6 sətir (T-05 imtahan verməyib, BETWEEN NULL ilə heç bir hərflə uyğunlaşmır).

-- b) Hər hərf üzrə nəticə sayı və orta bal; heç kimin almadığı hərf də 0 ilə.
SELECT qs.herf, COUNT(q.imtahan_bali) AS neticeler_sayi, COALESCE(AVG(q.imtahan_bali), 0) AS orta_bal
FROM qiymet_shkalasi qs
         LEFT JOIN qeydiyyat q ON q.imtahan_bali BETWEEN qs.min_bal AND qs.max_bal
GROUP BY qs.herf
ORDER BY qs.herf;
-- Qeyd: LEFT JOIN qiymet_shkalasi tərəfdən gedir, ona görə heç kimin almadığı hərf də sətir kimi qalır;
-- COUNT(sütun) belə hərf üçün onsuz da 0 verir, AVG isə NULL verərdi - onu COALESCE ilə 0-a çevirdim.

-- c) Özündən böyük bal alan hər tələbə ilə cütlük quran non-equi SELF JOIN;
--    hər nəticə üçün "ondan yuxarıda neçə nəticə var" sütunu.
-- Əvvəlcə cütlüklərin özünü çıxarıram (aqreqasiyasız - hansı nəticə hansından aşağıdır):
SELECT q1.telebe_id  AS asagi_telebe,
       q1.kurs_kod   AS asagi_kurs,
       q1.imtahan_bali AS asagi_bal,
       q2.telebe_id  AS yuxari_telebe,
       q2.kurs_kod   AS yuxari_kurs,
       q2.imtahan_bali AS yuxari_bal
FROM qeydiyyat q1
         JOIN qeydiyyat q2 ON q2.imtahan_bali > q1.imtahan_bali
ORDER BY q1.imtahan_bali DESC, q2.imtahan_bali DESC;

-- Sonra eyni birləşmənin üzərində sayğac sütununu hesablayıram.
-- Burada LEFT JOIN-dur ki, ən yüksək bal (92) da 0 ilə sətir kimi qalsın.
SELECT q1.telebe_id, q1.kurs_kod, q1.imtahan_bali, COUNT(q2.imtahan_bali) AS ondan_yuxari_sayi
FROM qeydiyyat q1
         LEFT JOIN qeydiyyat q2 ON q2.imtahan_bali > q1.imtahan_bali
WHERE q1.imtahan_bali IS NOT NULL
GROUP BY q1.telebe_id, q1.kurs_kod, q1.imtahan_bali
ORDER BY q1.imtahan_bali DESC;


-- ===== Tapşırıq B10 — ANTI JOIN ===== (3 bal)
-- a) Heç bir kursa yazılmamış tələbələr — LEFT JOIN ... WHERE ... IS NULL.
SELECT t.telebe_ad
FROM telebe t
         LEFT JOIN qeydiyyat q ON t.telebe_id = q.telebe_id
WHERE q.telebe_id IS NULL;

-- b) Eyni nəticə — NOT EXISTS.
SELECT t.telebe_ad
FROM telebe t
WHERE NOT EXISTS (SELECT 1 FROM qeydiyyat q WHERE q.telebe_id = t.telebe_id);

-- c) Eyni nəticə — NOT IN. Alt-sorğuya UNION ALL SELECT NULL əlavə et.
--    Nəticə necə dəyişir? Üç-qiymətli məntiq (TRUE/FALSE/UNKNOWN) ilə izah et.
SELECT t.telebe_ad
FROM telebe t
WHERE t.telebe_id NOT IN (SELECT q.telebe_id FROM qeydiyyat q);
-- Nəticə: T-04 (1 sətir) - normal işləyir, çünki qeydiyyat.telebe_id-də NULL yoxdur.

SELECT t.telebe_ad
FROM telebe t
WHERE t.telebe_id NOT IN (SELECT q.telebe_id FROM qeydiyyat q UNION ALL SELECT NULL);
-- Nəticə: 0 sətir!

-- İzah: "x NOT IN (a, b, NULL)" daxildə "x <> a AND x <> b AND x <> NULL" kimi
-- açılır. x <> NULL nəticəsi həmişə UNKNOWN-dur (nə TRUE, nə FALSE). AND zənciri
-- bir dəfə UNKNOWN olsa bütün ifadə UNKNOWN olur, WHERE isə yalnız TRUE sətirləri
-- saxlayır - UNKNOWN sətirlər atılır, ona görə heç bir tələbə qaytarılmır.

-- d) Heç bir kursu olmayan fənni və heç bir otağı olmayan filialı tap.
SELECT f.fenn_ad
FROM fenn f
WHERE NOT EXISTS (SELECT 1 FROM kurs k WHERE k.fenn_kod = f.fenn_kod);

SELECT fl.filial_unvan
FROM filial fl
WHERE NOT EXISTS (SELECT 1 FROM otaq o WHERE o.filial_kod = fl.filial_kod);


-- ===== Tapşırıq B11 — SEMI JOIN ===== (4 bal)
-- a) Ən azı bir kursa yazılmış tələbələr — EXISTS ilə;
--    eyni nəticəni IN və INNER JOIN + DISTINCT ilə də al.
SELECT t.telebe_ad
FROM telebe t
WHERE EXISTS (SELECT 1 FROM qeydiyyat q WHERE q.telebe_id = t.telebe_id);

SELECT t.telebe_ad
FROM telebe t
WHERE t.telebe_id IN (SELECT q.telebe_id FROM qeydiyyat q);

SELECT DISTINCT t.telebe_ad
FROM telebe t
         JOIN qeydiyyat q ON t.telebe_id = q.telebe_id;

-- b) Üç variantı müqayisə et: INNER JOIN-da DISTINCT niyə lazımdır və
--    hansı halda DISTINCT nəticəni səhv edə bilər (məsələn SUM ilə)?
-- İzah: INNER JOIN + DISTINCT-də DISTINCT lazımdır, çünki bir tələbə bir neçə
-- kursa yazılmışsa (T-01, T-02 kimi), JOIN onu bir neçə dəfə təkrarlayır - DISTINCT
-- təkrarları silir. Amma DISTINCT aqreqat funksiyalarla (SUM kimi) birlikdə səhv
-- nəticə verə bilər: hər tələbənin ümumi ödənişini SUM(odenis) ilə hesablasaq,
-- DISTINCT tələbənin fərqli kurslara görə ayrı-ayrı ödədiyi məbləğləri "eyni sətir"
-- kimi silə bilər, real cəm səhv çıxar. EXISTS/IN heç bir sətri təkrarlamadığı üçün
-- bu problemi yaratmır.

-- c) Qiyməti 400-dən yuxarı olan ən azı bir kursa yazılmış tələbələr — EXISTS.
SELECT t.telebe_ad
FROM telebe t
WHERE EXISTS (SELECT 1
              FROM qeydiyyat q
                       JOIN kurs k ON q.kurs_kod = k.kurs_kod
              WHERE q.telebe_id = t.telebe_id
                AND k.qiymet > 400);


-- ===== Tapşırıq B12 — LATERAL JOIN ===== (4 bal)
-- a) LATERAL ilə hər kurs üzrə ən yüksək 2 bal alan tələbə.
--    Nəticəsi olmayan kurslar da görünsün (LEFT JOIN LATERAL ... ON true).
SELECT k.kurs_ad, top2.telebe_id, top2.imtahan_bali
FROM kurs k
         LEFT JOIN LATERAL (
    SELECT q.telebe_id, q.imtahan_bali
    FROM qeydiyyat q
    WHERE q.kurs_kod = k.kurs_kod
      AND q.imtahan_bali IS NOT NULL
    ORDER BY q.imtahan_bali DESC
    LIMIT 2
    ) top2 ON TRUE
ORDER BY k.kurs_ad;

-- b) Hər filial üzrə ən son qeydiyyat (qeyd_tarixi üzrə) — LATERAL ilə.
SELECT fl.filial_kod, son.telebe_id, son.kurs_kod, son.qeyd_tarixi
FROM filial fl
         LEFT JOIN LATERAL (
    SELECT q.telebe_id, q.kurs_kod, q.qeyd_tarixi
    FROM qeydiyyat q
             JOIN otaq o ON q.otaq_no = o.otaq_no
    WHERE o.filial_kod = fl.filial_kod
    ORDER BY q.qeyd_tarixi DESC
    LIMIT 1
    ) son ON TRUE
ORDER BY fl.filial_kod;

-- c) (a) bəndinin eyni nəticəsi — ROW_NUMBER() OVER (PARTITION BY ...) ilə.
SELECT kurs_ad, telebe_id, imtahan_bali
FROM (SELECT k.kurs_kod,
             k.kurs_ad,
             q.telebe_id,
             q.imtahan_bali,
             ROW_NUMBER() OVER (PARTITION BY k.kurs_kod ORDER BY q.imtahan_bali DESC) AS sira
      FROM kurs k
               LEFT JOIN qeydiyyat q ON q.kurs_kod = k.kurs_kod AND q.imtahan_bali IS NOT NULL) sub
WHERE sira <= 2
ORDER BY kurs_ad;

-- İzah: iki yanaşmanın müqayisəsi; LATERAL olmadan alt-sorğu xarici cədvəlin
--       sütununa niyə müraciət edə bilmir?
-- İzah: hər iki yanaşma eyni nəticəni verir. LATERAL hər xarici sətir üçün alt-
-- sorğunu ayrıca icra edir (mürəkkəb, "hər qrup üçün N nəticə" tipli sorğularda
-- rahatdır), ROW_NUMBER isə bütün datanı bir dəfəyə pəncərə funksiyası ilə emal
-- edir (adətən daha performanslıdır). Adi (LATERAL olmayan) alt-sorğu FROM-dan
-- əvvəl, xarici sətirdən asılı olmadan bir dəfə planlaşdırılır, ona görə xarici
-- cədvəlin sütununa (k.kurs_kod kimi) müraciət edə bilmir - LATERAL isə "hər xarici
-- sətir üçün yenidən qiymətləndirilə bilən" alt-sorğuya icazə verir.


-- ===== Tapşırıq B13 — JOIN + aqreqasiya ===== (4 bal)
-- a) Hər filial üzrə: otaq sayı, unikal tələbə sayı, keçirilən kurs sayı,
--    ümumi gəlir. F-03 da 0 ilə görünməlidir.
SELECT fl.filial_kod,
       COUNT(DISTINCT o.otaq_no)      AS otaq_sayi,
       COUNT(DISTINCT q.telebe_id)    AS telebe_sayi,
       COUNT(DISTINCT q.kurs_kod)     AS kurs_sayi,
       COALESCE(SUM(q.odenis), 0)     AS umumi_gelir
FROM filial fl
         LEFT JOIN otaq o ON o.filial_kod = fl.filial_kod
         LEFT JOIN qeydiyyat q ON q.otaq_no = o.otaq_no
GROUP BY fl.filial_kod
ORDER BY fl.filial_kod;

-- b) Yalnız ümumi gəliri 1000-dən çox olan filiallar (HAVING).
SELECT fl.filial_kod, COALESCE(SUM(q.odenis), 0) AS umumi_gelir
FROM filial fl
         LEFT JOIN otaq o ON o.filial_kod = fl.filial_kod
         LEFT JOIN qeydiyyat q ON q.otaq_no = o.otaq_no
GROUP BY fl.filial_kod
HAVING COALESCE(SUM(q.odenis), 0) > 1000
ORDER BY fl.filial_kod;

-- c) Tipik səhv: telebe LEFT JOIN qeydiyyat-dan sonra COUNT(*) ilə
--    COUNT(qeydiyyat.kurs_kod) fərqli nəticə verir. Hər ikisini icra et,
--    T-04 üçün fərqi göstər.
SELECT t.telebe_ad, COUNT(*) AS say_ulduz, COUNT(q.kurs_kod) AS say_sutun
FROM telebe t
         LEFT JOIN qeydiyyat q ON t.telebe_id = q.telebe_id
GROUP BY t.telebe_ad
ORDER BY t.telebe_ad;

-- İzah: T-04 üçün COUNT(*)=1 (LEFT JOIN nəticəsində uyğun qeydiyyat olmasa da
-- 1 sətir yaranır), COUNT(q.kurs_kod)=0 (q.kurs_kod NULL-dur, COUNT(sütun) NULL
-- dəyərləri saymır). Fərq: COUNT(*) sətirləri sayır, COUNT(sütun) yalnız həmin
-- sütunda NULL OLMAYAN dəyərləri sayır.


-- #########################################################
-- #                                                       #
-- #   HİSSƏ C — İKİ MÖVZUNUN BİRLƏŞDİYİ YER               #
-- #                                                       #
-- #########################################################


-- ===== Tapşırıq C1 — İlkin cədvəli geri qurmaq =====
-- Yalnız JOIN-lardan istifadə edərək kurs_qeydiyyat cədvəlini olduğu kimi
-- geri düzəlt — bütün sütunlarla, o cümlədən vergüllə birləşdirilmiş
-- telefonlar və muellim_dilleri sütunları ilə (STRING_AGG).
SELECT t.telebe_id,
       t.telebe_ad,
       t.dogum_tarixi,
       tel.telefonlar,
       q.kurs_kod,
       k.kurs_ad,
       k.kurs_saat,
       k.qiymet,
       k.fenn_kod,
       f.fenn_ad,
       m.muellim_id,
       m.muellim_ad,
       m.muellim_email,
       dil.muellim_dilleri,
       o.otaq_no,
       o.otaq_tutum,
       fl.filial_kod,
       fl.filial_unvan,
       fl.seher,
       q.qeyd_tarixi,
       q.odenis,
       q.imtahan_bali
FROM qeydiyyat q
         JOIN telebe t ON q.telebe_id = t.telebe_id
         JOIN kurs k ON q.kurs_kod = k.kurs_kod
         JOIN fenn f ON k.fenn_kod = f.fenn_kod
         JOIN muellim m ON q.muellim_id = m.muellim_id
         JOIN otaq o ON q.otaq_no = o.otaq_no
         JOIN filial fl ON o.filial_kod = fl.filial_kod
         LEFT JOIN (SELECT telebe_id, STRING_AGG(telefon, ', ') AS telefonlar
                    FROM telebe_telefon
                    GROUP BY telebe_id) tel ON tel.telebe_id = t.telebe_id
         LEFT JOIN (SELECT muellim_id, STRING_AGG(dil, ', ') AS muellim_dilleri
                    FROM muellim_dil
                    GROUP BY muellim_id) dil ON dil.muellim_id = m.muellim_id
ORDER BY t.telebe_id, q.kurs_kod;

-- Neçə cədvəli birləşdirməli oldun?
-- İzah: 9 (qeydiyyat, telebe, kurs, fenn, muellim, otaq, filial - 7 əsas cədvəl -
-- üstəgəl telebe_telefon və muellim_dil-in STRING_AGG ilə aqreqasiya edilmiş alt-sorğuları).

-- T-05 (telefonsuz) və imtahan verməmiş qeydiyyat itməməlidir —
-- hansı JOIN növünü seçdin və niyə?
-- İzah: telebe/kurs/fenn/muellim/otaq/filial arasında INNER JOIN kifayətdir, çünki
-- hamısı FK ilə təmin olunub və heç biri boş ola bilməz. telebe_telefon və
-- muellim_dil ilə isə LEFT JOIN seçdim, çünki T-05-in telefonu yoxdur - INNER
-- JOIN olsaydı bu sətir tamamən itərdi. imtahan_bali qeydiyyat-ın öz sütunu
-- olduğu üçün NULL kimi avtomatik görünür, ayrıca JOIN tələb etmir.

-- Bir cümlə ilə: normallaşdırma nəyi ucuzlaşdırdı, nəyi bahalaşdırdı?
-- İzah: Normallaşdırma yazma əməliyyatlarını və məlumat düzgünlüyünü ucuzlaşdırdı,
-- amma oxuma zamanı bu qədər çox JOIN etməyi tələb edərək sorğu mürəkkəbliyini bahalaşdırdı.


-- ===== Tapşırıq C2 — 5NF cədvəllərinin birləşdirilməsi =====
-- a) A6-dakı üç binar cədvəli birləşdirib tam tədris planını çıxar;
--    müəllimin adını və filialın ünvanını da əlavə et (5 cədvəl).
SELECT mk.muellim_id, m.muellim_ad, mk.kurs_kod, kf.filial_kod, fl.filial_unvan
FROM muellim_kurs mk
         JOIN kurs_filial kf ON mk.kurs_kod = kf.kurs_kod
         JOIN muellim_filial mf ON mk.muellim_id = mf.muellim_id AND kf.filial_kod = mf.filial_kod
         JOIN muellim m ON mk.muellim_id = m.muellim_id
         JOIN filial fl ON kf.filial_kod = fl.filial_kod
ORDER BY mk.muellim_id, mk.kurs_kod;

-- b) Yalnız iki cədvəli birləşdirən variant; hansı sətir artıq gəlir?
SELECT mk.muellim_id, mk.kurs_kod, kf.filial_kod
FROM muellim_kurs mk
         JOIN kurs_filial kf ON mk.kurs_kod = kf.kurs_kod;
-- Nəticə: 5 sətir - əlavə gələn (M-11, SQL-101, F-02) A6-d-də göstərdiyimiz eyni saxta sətirdir.

-- İzah: bu sətir real həyatda nəyi səhv iddia edir?
-- İzah: "M-11 SQL-101-i F-02 filialında tədris edir" - halbuki M-11 F-02-də
-- ümumiyyətlə işləmir (muellim_filial cədvəlinə görə). Üçüncü proyeksiya
-- (muellim_filial) yoxlanmadığı üçün bu yanlış kombinasiya süzülməyib qalır.


-- ===== Tapşırıq C3 — Anomaliyaların praktik nümayişi =====
-- Hər əməliyyatı normallaşdırılmamış cədvəldə (fikrən) və öz sxemində
-- (real UPDATE / DELETE / INSERT ilə) yerinə yetir, təsirlənən sətir
-- sayını müqayisə et.
--
-- Qeyd: A1b-də qurduğum kurs_qeydiyyat (1NF, hələ normallaşmamış) cədvəli real datayla
-- doludur, ona görə hər iki tərəfi "fikrən" yox, real icra ilə müqayisə edirəm.
-- psql hər əməliyyatdan sonra təsirlənən sətir sayını özü çap edir (UPDATE n / DELETE n).

-- a) Müəllim M-05-in e-poçtu dəyişdi.
-- Normallaşdırılmamış cədvəldə:
UPDATE kurs_qeydiyyat
SET muellim_email = 'reshad.quliyev@akademiya.az'
WHERE muellim_id = 'M-05';
-- Nəticə: UPDATE 3 — M-05-in olduğu hər sətir ayrıca yenilənir
-- (T-01/SQL-101, T-02/SQL-101, T-05/SQL-101).

-- Normallaşdırılmış sxemdə:
UPDATE muellim
SET muellim_email = 'reshad.quliyev@akademiya.az'
WHERE muellim_id = 'M-05';
-- Nəticə: UPDATE 1 — e-poçt yalnız bir yerdə saxlanılır.

-- Fərq: 3 sətir → 1 sətir. Real bazada bu fərq qeydiyyat sayı qədər böyüyür və
-- bir sətir unudulsa eyni müəllimin iki fərqli e-poçtu yaranır (UPDATE anomaliyası).

-- b) Yeni fənn ("Kibertəhlükəsizlik") əlavə olunur — hələ kursu və tələbəsi yoxdur.
--    Normallaşdırılmamış cədvəldə bunu ümumiyyətlə yazmaq olurmu?
-- Normallaşdırılmış sxemdə problemsizdir:
INSERT INTO fenn (fenn_kod, fenn_ad)
VALUES ('F-SEC', 'Kibertəhlükəsizlik');
-- Nəticə: INSERT 0 1 — fenn cədvəli kursdan asılı deyil.

-- Normallaşdırılmamış cədvəldə cəhd edirik. Fənndən başqa heç nə bilmirik,
-- ona görə qalan sütunları NULL qoymalı oluruq:
INSERT INTO kurs_qeydiyyat (fenn_kod, fenn_ad)
VALUES ('F-SEC', 'Kibertəhlükəsizlik');
-- GÖZLƏNİLƏN NƏTİCƏ: XƏTA — null value in column "telebe_id" ... violates not-null constraint.
-- Yəni fənni yazmaq üçün uydurma tələbə və uydurma kurs da yazmalıyıq. Bu, məhz
-- INSERT anomaliyasıdır: müstəqil bir faktı yalnız başqa faktla birlikdə saxlaya bilirik.

-- c) DSC-301 kursuna yazılan tək tələbənin (T-03) qeydiyyatı silinir.
--    Normallaşdırılmamış cədvəldə hansı məlumat həmişəlik itir?
-- Silmədən əvvəl DSC-301 haqqında nə bilirik:
SELECT DISTINCT kurs_kod, kurs_ad, kurs_saat, qiymet, fenn_kod, fenn_ad
FROM kurs_qeydiyyat
WHERE kurs_kod = 'DSC-301';

-- Normallaşdırılmamış cədvəldə silirik:
DELETE
FROM kurs_qeydiyyat
WHERE telebe_id = 'T-03'
  AND kurs_kod = 'DSC-301';
-- Nəticə: DELETE 1

-- Silmədən sonra DSC-301 haqqında nə qaldı:
SELECT DISTINCT kurs_kod, kurs_ad, kurs_saat, qiymet, fenn_kod, fenn_ad
FROM kurs_qeydiyyat
WHERE kurs_kod = 'DSC-301';
-- Nəticə: 0 sətir — kursun adı, saatı, qiyməti və aid olduğu fənn həmişəlik itdi.

-- Normallaşdırılmış sxemdə eyni əməliyyat:
DELETE
FROM qeydiyyat
WHERE telebe_id = 'T-03'
  AND kurs_kod = 'DSC-301';
-- Nəticə: DELETE 1

-- Kurs və fənn məlumatı öz cədvəllərində toxunulmadan qalır:
SELECT k.kurs_kod, k.kurs_ad, k.kurs_saat, k.qiymet, f.fenn_kod, f.fenn_ad
FROM kurs k
         JOIN fenn f ON k.fenn_kod = f.fenn_kod
WHERE k.kurs_kod = 'DSC-301';
-- Nəticə: 1 sətir — heç nə itmədi (DELETE anomaliyası aradan qalxdı).

-- Nəticə: bu üç hal hansı anomaliyalara (UPDATE / INSERT / DELETE) uyğundur?
-- İzah: a) UPDATE anomaliyası, b) INSERT anomaliyası, c) DELETE anomaliyası.


-- #########################################################
-- #                                                       #
-- #   BONUS (+10 bal)                                     #
-- #                                                       #
-- #########################################################


-- ===== Bonus a — EXPLAIN (ANALYZE, BUFFERS) =====
-- C1 sorğusu üçün icra et; birləşmə alqoritmlərini (Nested Loop, Hash Join, Merge Join) sadala və seçimin səbəbini izah et.
-- Qeyd: aşağıdakı sorğu C1-in EYNİSİDİR (bütün 22 sütun və 9 cədvəl ilə), yalnız qarşısına
-- EXPLAIN (ANALYZE, BUFFERS) əlavə olunub.
EXPLAIN (ANALYZE, BUFFERS)
SELECT t.telebe_id,
       t.telebe_ad,
       t.dogum_tarixi,
       tel.telefonlar,
       q.kurs_kod,
       k.kurs_ad,
       k.kurs_saat,
       k.qiymet,
       k.fenn_kod,
       f.fenn_ad,
       m.muellim_id,
       m.muellim_ad,
       m.muellim_email,
       dil.muellim_dilleri,
       o.otaq_no,
       o.otaq_tutum,
       fl.filial_kod,
       fl.filial_unvan,
       fl.seher,
       q.qeyd_tarixi,
       q.odenis,
       q.imtahan_bali
FROM qeydiyyat q
         JOIN telebe t ON q.telebe_id = t.telebe_id
         JOIN kurs k ON q.kurs_kod = k.kurs_kod
         JOIN fenn f ON k.fenn_kod = f.fenn_kod
         JOIN muellim m ON q.muellim_id = m.muellim_id
         JOIN otaq o ON q.otaq_no = o.otaq_no
         JOIN filial fl ON o.filial_kod = fl.filial_kod
         LEFT JOIN (SELECT telebe_id, STRING_AGG(telefon, ', ') AS telefonlar
                    FROM telebe_telefon
                    GROUP BY telebe_id) tel ON tel.telebe_id = t.telebe_id
         LEFT JOIN (SELECT muellim_id, STRING_AGG(dil, ', ') AS muellim_dilleri
                    FROM muellim_dil
                    GROUP BY muellim_id) dil ON dil.muellim_id = m.muellim_id
ORDER BY t.telebe_id, q.kurs_kod;

-- İzah: planda seçilən birləşmə alqoritmləri (yuxarıdakı çıxışdan oxunub):
--   * Hash Join      — 6 ədəd (qeydiyyat↔telebe, ↔kurs, kurs↔fenn, ↔muellim, ↔otaq, otaq↔filial)
--   * Hash Left Join — 2 ədəd (telefon və dil üzrə STRING_AGG alt-sorğuları ilə)
--   * Nested Loop    — 0 ədəd
--   * Merge Join     — 0 ədəd
-- Yəni planlaşdırıcı BÜTÜN birləşmələr üçün hash əsaslı alqoritm seçib.
-- Əlavə düyünlər: bütün 9 cədvəl üçün Seq Scan, iki STRING_AGG üçün HashAggregate,
-- sonda ORDER BY üçün bir Sort. Ümumi cost 202.91, Execution Time 0.130 ms.
--
-- Seçimin səbəbi:
-- 1) Bütün cədvəllər bir səhifəyə sığır (Buffers: shared hit=9), ona görə Seq Scan indeksdən
--    ucuzdur - indeks oxumaq əlavə mərhələdir, cədvəli birbaşa oxumaq isə bir bloka baxmaqdır.
-- 2) Birləşmə şərtlərinin hamısı bərabərlikdir (=). Hash Join yalnız bərabərlik şərti ilə
--    işləyir, ona görə burada namizəddir; B9-dakı BETWEEN şərti olsaydı Hash Join mümkün olmazdı.
-- 3) Kiçik tərəf (telebe, kurs, muellim, otaq, filial, fenn - hər biri 3-5 sətir) yaddaşda
--    hash cədvəli kimi qurulur (Memory Usage: 9kB) və böyük tərəf bir dəfə skan edilir.
--    Bu, O(N+M)-dir; Nested Loop isə indekssiz O(N*M) olardı.
-- 4) Merge Join seçilmədi, çünki heç bir cədvəl birləşmə sütunu üzrə sıralı deyil -
--    onu seçmək üçün planlaşdırıcı hər tərəfi ayrıca Sort etməli olardı (bax: Bonus b).


-- ===== Bonus b — enable_hashjoin = off =====
-- Planı yenidən al, nəyin dəyişdiyini yaz, sonda 'on' qaytar.
SET enable_hashjoin = off;

EXPLAIN (ANALYZE, BUFFERS)
SELECT t.telebe_id,
       t.telebe_ad,
       t.dogum_tarixi,
       tel.telefonlar,
       q.kurs_kod,
       k.kurs_ad,
       k.kurs_saat,
       k.qiymet,
       k.fenn_kod,
       f.fenn_ad,
       m.muellim_id,
       m.muellim_ad,
       m.muellim_email,
       dil.muellim_dilleri,
       o.otaq_no,
       o.otaq_tutum,
       fl.filial_kod,
       fl.filial_unvan,
       fl.seher,
       q.qeyd_tarixi,
       q.odenis,
       q.imtahan_bali
FROM qeydiyyat q
         JOIN telebe t ON q.telebe_id = t.telebe_id
         JOIN kurs k ON q.kurs_kod = k.kurs_kod
         JOIN fenn f ON k.fenn_kod = f.fenn_kod
         JOIN muellim m ON q.muellim_id = m.muellim_id
         JOIN otaq o ON q.otaq_no = o.otaq_no
         JOIN filial fl ON o.filial_kod = fl.filial_kod
         LEFT JOIN (SELECT telebe_id, STRING_AGG(telefon, ', ') AS telefonlar
                    FROM telebe_telefon
                    GROUP BY telebe_id) tel ON tel.telebe_id = t.telebe_id
         LEFT JOIN (SELECT muellim_id, STRING_AGG(dil, ', ') AS muellim_dilleri
                    FROM muellim_dil
                    GROUP BY muellim_id) dil ON dil.muellim_id = m.muellim_id
ORDER BY t.telebe_id, q.kurs_kod;

SET enable_hashjoin = on;

-- İzah: nə dəyişdi (iki planın müqayisəsi):
--   * 6 Hash Join      → 6 Merge Join
--   * 2 Hash Left Join → 2 Merge Left Join
--   * Hash / HashAggregate düyünlərinin əvəzinə hər birləşmə tərəfi üçün ayrıca Sort düyünü
--     əlavə olundu (Sort Method: quicksort Memory: 25kB).
--   * Ümumi cost: 202.91 → 426.97 (təxminən 2.1 dəfə baha).
--   * Execution Time: 0.130 ms → 0.166 ms.
-- Səbəb: Merge Join hər iki tərəfin birləşmə sütunu üzrə SIRALI olmasını tələb edir.
-- Cədvəllərdə bu sütunlar üzrə indeks olmadığı üçün sıralamanı planlaşdırıcı özü etməlidir,
-- bu da plana 9-a yaxın əlavə Sort gətirir - xərcin artması məhz buradandır.
-- Cədvəllər cəmi 3-7 sətirlik olduğu üçün real icra vaxtındakı fərq mikrosaniyələrlə ölçülür,
-- lakin planlaşdırıcının qiymətləndirdiyi xərc (cost) fərqi aydın görünür: hash söndürülməsəydi
-- o, heç vaxt bu planı seçməzdi.
-- Sonda parametri geri qaytarıram ki, növbəti sorğular normal planla işləsin.


-- ===== Bonus c — İndeks =====
-- qeydiyyat cədvəlinin muellim_id sütunu üzərində indeks yarat,
-- planın dəyişib-dəyişmədiyini yoxla.
CREATE INDEX idx_qeydiyyat_muellim ON qeydiyyat (muellim_id);

EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM qeydiyyat
WHERE muellim_id = 'M-05';

-- İzah: bu ölçüdə cədvəldə indeks niyə istifadə olunmaya bilər?
-- İzah: qeydiyyat cədvəlində cəmi 7 sətir olduğu üçün planlaşdırıcı böyük
-- ehtimalla Seq Scan seçəcək - indeksi oxumaq (əlavə I/O mərhələsi) kiçik
-- cədvəldə bütün cədvəli birbaşa oxumaqdan daha baha başa gəlir.


-- ===== Bonus d — TRIGGER testi =====
-- A4-də itən funksional asılılığı qorumaq üçün yazdığın mexanizmi test et:
-- qaydanı pozan bir INSERT yaz və xətanın alındığını göstər.
-- A4d-dəki mexanizm (kompozit FK + UNIQUE) həmin bənddə real yaradılıb və doldurulub.
-- Burada onu iki fərqli pozuntu ilə test edirəm.
-- Cari vəziyyət:
SELECT * FROM telebe_muellim ORDER BY telebe_id, fenn_kod;

-- TEST 1 — itən FD-ni pozan INSERT:
-- T-01 artıq F-SQL-i M-05-dən öyrənir; indi eyni fənni M-11-dən də öyrənmək istəyir.
-- Bu, (telebe_id, fenn_kod) -> muellim_id asılılığını pozur.
INSERT INTO telebe_muellim (telebe_id, muellim_id, fenn_kod)
VALUES ('T-01', 'M-11', 'F-SQL');
-- GÖZLƏNİLƏN NƏTİCƏ: XƏTA — duplicate key value violates unique constraint
-- "uq_telebe_fenn", DETAIL: Key (telebe_id, fenn_kod)=(T-01, F-SQL) already exists.

-- TEST 2 — kompozit FK-nı pozan INSERT:
-- M-11 əslində F-SQL tədris edir; onu F-PYT müəllimi kimi yazmağa çalışırıq.
-- Kompozit FK (muellim_id, fenn_kod) belə uydurma cütlüyü qəbul etmir.
INSERT INTO telebe_muellim (telebe_id, muellim_id, fenn_kod)
VALUES ('T-04', 'M-11', 'F-PYT');
-- GÖZLƏNİLƏN NƏTİCƏ: XƏTA — insert or update on table "telebe_muellim" violates
-- foreign key constraint "fk_tm_muellim_fenn".

-- TEST 3 — qaydaya uyğun INSERT işləməlidir (mexanizm düzgün sətri bloklamır):
INSERT INTO telebe_muellim (telebe_id, muellim_id, fenn_kod)
VALUES ('T-04', 'M-13', 'F-PYT');
-- Gözlənilən nəticə: INSERT 0 1

SELECT * FROM telebe_muellim ORDER BY telebe_id, fenn_kod;

-- İzah: UNIQUE (telebe_id, fenn_kod) A4-də itən funksional asılılığı bərpa edir,
-- kompozit FK isə fenn_kod sütununun müəllimin ƏSL fənni olmasını təmin edir.
-- İkisi birlikdə TRIGGER yazmadan, deklarativ yolla asılılığı qoruyur.
