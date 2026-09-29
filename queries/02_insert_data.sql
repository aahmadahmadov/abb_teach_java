DROP TABLE IF EXISTS kohne_sifarisler;

CREATE TABLE kohne_sifarisler
(
    sifaris_id INT,
    tarix      DATE,
    musteri    VARCHAR(100),
    email      VARCHAR(100),
    telefon    VARCHAR(20),
    seher      VARCHAR(50),
    mehsullar  VARCHAR(200),
    kateqoriya VARCHAR(200),
    qiymetler  VARCHAR(100),
    miqdarlar  VARCHAR(50),
    kuryer     VARCHAR(100)
);

INSERT INTO kohne_sifarisler
VALUES (1, '2026-01-05', 'Aysel Məmmədova', 'aysel@mail.az', '0501112233', 'Bakı', 'Noutbuk, Siçan',
        'Elektronika, Aksesuar', '1500, 25', '1, 2', 'Rəşad Əliyev'),
       (2, '2026-01-17', 'Tural Həsənov', 'tural@gmail.com', '0552223344', 'Gəncə', 'Telefon', 'Elektronika', '900',
        '1', 'Nicat Quliyev'),
       (3, '2026-02-03', 'Aysel Məmmədova', 'aysel@mail.az', '0501112233', 'Bakı', 'Qulaqlıq, Siçan',
        'Aksesuar, Aksesuar', '80, 25', '1, 1', 'Rəşad Əliyev'),
       (4, '2026-02-20', 'Leyla Kərimova', 'leyla@mail.ru', '0703334455', 'Sumqayıt', 'Planşet', 'Elektronika', '650',
        '2', 'Rəşad Əliyev'),
       (5, '2026-03-11', 'Tural Həsənov', 'tural@gmail.com', '0552223344', 'Gəncə', 'Noutbuk, Qulaqlıq',
        'Elektronika, Aksesuar', '1500, 80', '1, 1', 'Nicat Quliyev');

INSERT INTO seherler (ad)
VALUES ('Bakı'),
       ('Gəncə'),
       ('Sumqayıt'),
       ('Lənkəran'),
       ('Mingəçevir');

INSERT INTO kateqoriyalar (ad, ust_kateqoriya_id)
VALUES ('Elektronika', NULL),
       ('Aksesuar', NULL),
       ('Kompüterlər', 1),
       ('Noutbuklar', 3);

INSERT INTO kuryerler (ad, soyad)
VALUES ('Rəşad', 'Əliyev'),
       ('Nicat', 'Quliyev'),
       ('Fuad', 'Səfərov');

INSERT INTO musteriler (ad, soyad, email, telefon, seher_id)
VALUES ('Aysel', 'Məmmədova', 'aysel@mail.az', '0501112233', 1),
       ('Tural', 'Həsənov', 'tural@gmail.com', '0552223344', 2),
       ('Leyla', 'Kərimova', 'leyla@mail.ru', '0703334455', 3),
       ('Kamran', 'Nəbiyev', 'kamran@mail.az', '0775556677', 4), -- sifarisi yoxdu
       ('Səbinə', 'Hüseynova', 'sebine@gmail.com', NULL, 5),     -- telefonu yoxdu
       ('Elvin', 'Rzayev', 'elvin@mail.ru', '0507778899', 1);

INSERT INTO mehsullar (ad, kateqoriya_id, qiymet)
VALUES ('Noutbuk', 1, 1500),
       ('Siçan', 2, 25),
       ('Telefon', 1, 900),
       ('Qulaqlıq', 2, 80),
       ('Planşet', 1, 650),
       ('Monitor', 3, 400),
       ('Klaviatura', 2, 60),
       ('Smart saat', 1, 300);

INSERT INTO sifarisler (tarix, musteri_id, kuryer_id, status)
VALUES ('2026-01-05', 1, 1, 'Çatdırıldı'),
       ('2026-01-17', 2, 2, 'Çatdırıldı'),
       ('2026-02-03', 1, 1, 'Çatdırıldı'),
       ('2026-02-20', 3, 1, 'Çatdırıldı'),
       ('2026-03-11', 2, 2, 'Çatdırıldı'),
       ('2026-04-08', 5, 3, 'Çatdırıldı'),
       ('2026-05-14', 6, 1, 'Göndərildi'),
       ('2026-06-02', 1, 2, 'Gözləmədə'),
       ('2026-07-19', 6, 3, 'Ləğv edildi'),
       ('2026-08-25', 2, 1, 'Çatdırıldı');

INSERT INTO sifaris_detallari (sifaris_id, mehsul_id, miqdar, qiymet)
VALUES (1, 1, 1, 1500),
       (1, 2, 2, 25),
       (2, 3, 1, 900),
       (3, 4, 1, 80),
       (3, 2, 1, 25),
       (4, 5, 2, 650),
       (5, 1, 1, 1500),
       (5, 4, 1, 80),
       (6, 6, 2, 400),
       (6, 7, 1, 60),
       (7, 3, 1, 900),
       (7, 2, 3, 25),
       (8, 5, 1, 650),
       (8, 4, 2, 80),
       (9, 1, 1, 1500),
       (10, 6, 1, 400),
       (10, 7, 2, 60);

-- ERROR: new row for relation "mehsullar" violates check constraint "chk_mehsul_qiymet"
INSERT INTO mehsullar (ad, kateqoriya_id, qiymet)
VALUES ('Test məhsul', 1, -50);

-- ERROR: duplicate key value violates unique constraint "uq_musteri_email"
INSERT INTO musteriler (ad, soyad, email, telefon, seher_id)
VALUES ('Test', 'Müştəri', 'aysel@mail.az', '0999999999', 1);

-- ERROR: insert or update on table "sifarisler" violates foreign key constraint "fk_sifaris_musteri"
INSERT INTO sifarisler (musteri_id, kuryer_id)
VALUES (999, 1);

-- ERROR: new row for relation "sifarisler" violates check constraint "chk_sifaris_status"
INSERT INTO sifarisler (musteri_id, kuryer_id, status)
VALUES (1, 1, 'Yolda');

-- ERROR: update or delete on table "musteriler" violates foreign key constraint "fk_sifaris_musteri" on table "sifarisler"
DELETE
FROM musteriler
WHERE musteri_id = 1;
