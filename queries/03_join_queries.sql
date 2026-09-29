-- 5.1 Hər sifarişin nömrəsi, tarixi, müştəri, şəhər, kuryer və ümumi məbləği
SELECT s.sifaris_id,
       s.tarix,
       m.ad || ' ' || m.soyad   AS musteri,
       sh.ad                    AS seher,
       k.ad || ' ' || k.soyad   AS kuryer,
       SUM(d.miqdar * d.qiymet) AS umumi_mebleg
FROM sifarisler s
         INNER JOIN musteriler m ON m.musteri_id = s.musteri_id
         INNER JOIN seherler sh ON sh.seher_id = m.seher_id
         INNER JOIN kuryerler k ON k.kuryer_id = s.kuryer_id
         INNER JOIN sifaris_detallari d ON d.sifaris_id = s.sifaris_id
GROUP BY s.sifaris_id, s.tarix, m.ad, m.soyad, sh.ad, k.ad, k.soyad
ORDER BY s.sifaris_id;

-- 5.2 Heç vaxt sifariş verməmiş müştərilər
SELECT m.musteri_id, m.ad, m.soyad, m.email
FROM musteriler m
         LEFT JOIN sifarisler s ON s.musteri_id = m.musteri_id
WHERE s.sifaris_id IS NULL;

-- 5.3 Heç vaxt satılmamış məhsullar
SELECT p.mehsul_id, p.ad, p.qiymet
FROM mehsullar p
         LEFT JOIN sifaris_detallari d ON d.mehsul_id = p.mehsul_id
WHERE d.sifaris_id IS NULL;

-- 5.4 Hər kateqoriya üzrə satılan məhsulların sayı və gəliri
SELECT k.ad                     AS kateqoriya,
       SUM(d.miqdar)            AS satilan_say,
       SUM(d.miqdar * d.qiymet) AS gelir
FROM kateqoriyalar k
         INNER JOIN mehsullar p ON p.kateqoriya_id = k.kateqoriya_id
         INNER JOIN sifaris_detallari d ON d.mehsul_id = p.mehsul_id
         INNER JOIN sifarisler s ON s.sifaris_id = d.sifaris_id
WHERE s.status <> 'Ləğv edildi'
GROUP BY k.ad
ORDER BY gelir DESC;

-- 5.5 Hər kuryerin çatdırdığı sifarişlərin sayı və məbləği
SELECT k.ad || ' ' || k.soyad       AS kuryer,
       COUNT(DISTINCT s.sifaris_id) AS catdirilan_say,
       SUM(d.miqdar * d.qiymet)     AS umumi_mebleg
FROM kuryerler k
         INNER JOIN sifarisler s ON s.kuryer_id = k.kuryer_id
         INNER JOIN sifaris_detallari d ON d.sifaris_id = s.sifaris_id
WHERE s.status = 'Çatdırıldı'
GROUP BY k.kuryer_id, k.ad, k.soyad
ORDER BY catdirilan_say DESC, umumi_mebleg DESC;

-- 5.6 Hər şəhər üzrə müştəri sayı və həmin şəhərdən gələn gəlir
SELECT sh.ad                                 AS seher,
       COUNT(DISTINCT m.musteri_id)          AS musteri_sayi,
       COALESCE(SUM(d.miqdar * d.qiymet), 0) AS gelir
FROM seherler sh
         LEFT JOIN musteriler m ON m.seher_id = sh.seher_id
         LEFT JOIN sifarisler s ON s.musteri_id = m.musteri_id
    AND s.status <> 'Ləğv edildi'
         LEFT JOIN sifaris_detallari d ON d.sifaris_id = s.sifaris_id
GROUP BY sh.ad
ORDER BY gelir DESC;
