-- 7.1 Sətir funksiyaları: böyük hərflə ad, birləşmiş tam ad, email domeni
SELECT UPPER(ad)                                        AS ad_boyuk,
       UPPER(soyad)                                     AS soyad_boyuk,
       CONCAT(ad, ' ', soyad)                           AS tam_ad,
       email,
       SUBSTRING(email FROM POSITION('@' IN email) + 1) AS domen
FROM musteriler;

-- 7.2 Hər sifarişin neçə gün əvvəl verildiyi
SELECT sifaris_id,
       tarix,
       CURRENT_DATE - tarix AS nece_gun_evvel
FROM sifarisler
ORDER BY tarix;

-- 7.3 Aylar üzrə sifariş sayı
SELECT EXTRACT(MONTH FROM tarix) AS ay,
       COUNT(*)                  AS sifaris_sayi
FROM sifarisler
GROUP BY EXTRACT(MONTH FROM tarix)
ORDER BY ay;

-- 7.4 Orta sifariş məbləği
SELECT ROUND(AVG(mebleg), 2) AS orta_sifaris_mebleg
FROM (SELECT sifaris_id, SUM(miqdar * qiymet) AS mebleg
      FROM sifaris_detallari
      GROUP BY sifaris_id) t;

-- 7.5 Telefonu olmayanlar üçün 'Qeyd olunmayıb'
SELECT ad,
       soyad,
       COALESCE(telefon, 'Qeyd olunmayıb') AS telefon
FROM musteriler;

-- 7.6 Müştərinin ümumi xərcini qaytaran funksiya
CREATE
    OR REPLACE FUNCTION musteri_xerci(p_musteri_id INT)
    RETURNS NUMERIC AS
$$
SELECT COALESCE(SUM(d.miqdar * d.qiymet), 0)
FROM sifarisler s
         JOIN sifaris_detallari d ON d.sifaris_id = s.sifaris_id
WHERE s.musteri_id = p_musteri_id
  AND s.status <> 'Ləğv edildi';
$$
    LANGUAGE sql;

SELECT musteri_id,
       ad,
       soyad,
       musteri_xerci(musteri_id) AS umumi_xerc
FROM musteriler
ORDER BY umumi_xerc DESC;
