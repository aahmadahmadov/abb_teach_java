-- Temizlik
DROP TABLE IF EXISTS sifaris_detallari, sifarisler, mehsullar, kuryerler,
    musteriler, kateqoriyalar, seherler CASCADE;

-- Seherler
CREATE TABLE seherler
(
    seher_id SERIAL,
    ad       VARCHAR(50) NOT NULL,
    CONSTRAINT pk_seherler PRIMARY KEY (seher_id),
    CONSTRAINT uq_seher_ad UNIQUE (ad)
);

-- Kateqoriyalar
CREATE TABLE kateqoriyalar
(
    kateqoriya_id     SERIAL,
    ad                VARCHAR(50) NOT NULL,
    ust_kateqoriya_id INT,
    CONSTRAINT pk_kateqoriyalar PRIMARY KEY (kateqoriya_id),
    CONSTRAINT uq_kateqoriya_ad UNIQUE (ad),
    CONSTRAINT fk_kateqoriya_ust FOREIGN KEY (ust_kateqoriya_id)
        REFERENCES kateqoriyalar (kateqoriya_id) ON DELETE RESTRICT
);

-- Kuryerlər
CREATE TABLE kuryerler
(
    kuryer_id SERIAL,
    ad        VARCHAR(50) NOT NULL,
    soyad     VARCHAR(50) NOT NULL,
    CONSTRAINT pk_kuryerler PRIMARY KEY (kuryer_id)
);

-- Müştərilər
CREATE TABLE musteriler
(
    musteri_id SERIAL,
    ad         VARCHAR(50)  NOT NULL,
    soyad      VARCHAR(50)  NOT NULL,
    email      VARCHAR(100) NOT NULL,
    telefon    VARCHAR(20),
    seher_id   INT          NOT NULL,
    CONSTRAINT pk_musteriler PRIMARY KEY (musteri_id),
    CONSTRAINT uq_musteri_email UNIQUE (email),
    CONSTRAINT uq_musteri_telefon UNIQUE (telefon),
    CONSTRAINT chk_musteri_email CHECK (email LIKE '%@%'),
    CONSTRAINT fk_musteri_seher FOREIGN KEY (seher_id)
        REFERENCES seherler (seher_id) ON DELETE RESTRICT
);

-- Məhsullar
CREATE TABLE mehsullar
(
    mehsul_id     SERIAL,
    ad            VARCHAR(100)   NOT NULL,
    kateqoriya_id INT            NOT NULL,
    qiymet        NUMERIC(10, 2) NOT NULL,
    CONSTRAINT pk_mehsullar PRIMARY KEY (mehsul_id),
    CONSTRAINT uq_mehsul_ad UNIQUE (ad),
    CONSTRAINT chk_mehsul_qiymet CHECK (qiymet > 0),
    CONSTRAINT fk_mehsul_kateqoriya FOREIGN KEY (kateqoriya_id)
        REFERENCES kateqoriyalar (kateqoriya_id) ON DELETE RESTRICT
);

-- Sifarişlər
CREATE TABLE sifarisler
(
    sifaris_id SERIAL,
    tarix      DATE        NOT NULL DEFAULT CURRENT_DATE,
    musteri_id INT         NOT NULL,
    kuryer_id  INT         NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'Gözləmədə',
    CONSTRAINT pk_sifarisler PRIMARY KEY (sifaris_id),
    CONSTRAINT chk_sifaris_status CHECK (
        status IN ('Gözləmədə', 'Göndərildi', 'Çatdırıldı', 'Ləğv edildi')),
    -- musteri silinende sifaris qalmamalidi, ona gore RESTRICT
    CONSTRAINT fk_sifaris_musteri FOREIGN KEY (musteri_id)
        REFERENCES musteriler (musteri_id) ON DELETE RESTRICT,
    CONSTRAINT fk_sifaris_kuryer FOREIGN KEY (kuryer_id)
        REFERENCES kuryerler (kuryer_id) ON DELETE RESTRICT
);

-- Sifariş detalları
CREATE TABLE sifaris_detallari
(
    sifaris_id INT            NOT NULL,
    mehsul_id  INT            NOT NULL,
    miqdar     INT            NOT NULL,
    qiymet     NUMERIC(10, 2) NOT NULL,
    CONSTRAINT pk_sifaris_detallari PRIMARY KEY (sifaris_id, mehsul_id),
    CONSTRAINT chk_detal_miqdar CHECK (miqdar >= 1),
    CONSTRAINT chk_detal_qiymet CHECK (qiymet > 0),
    CONSTRAINT fk_detal_sifaris FOREIGN KEY (sifaris_id)
        REFERENCES sifarisler (sifaris_id) ON DELETE CASCADE,
    CONSTRAINT fk_detal_mehsul FOREIGN KEY (mehsul_id)
        REFERENCES mehsullar (mehsul_id) ON DELETE RESTRICT
);

CREATE INDEX idx_musteri_seher ON musteriler (seher_id);
CREATE INDEX idx_mehsul_kateqoriya ON mehsullar (kateqoriya_id);
CREATE INDEX idx_sifaris_musteri ON sifarisler (musteri_id);
CREATE INDEX idx_sifaris_kuryer ON sifarisler (kuryer_id);
CREATE INDEX idx_sifaris_tarix ON sifarisler (tarix);
CREATE INDEX idx_detal_mehsul ON sifaris_detallari (mehsul_id);
