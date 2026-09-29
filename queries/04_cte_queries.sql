-- 6.1 Aylıq gəlir, sonra yalnız orta aylıq gəlirdən yüksək olan aylar
WITH ayliq AS (SELECT DATE_TRUNC('month', s.tarix)::date AS ay, SUM(d.miqdar * d.qiymet) AS gelir
               FROM sifarisler s
                        JOIN sifaris_detallari d ON d.sifaris_id = s.sifaris_id
               WHERE s.status <> 'Ləğv edildi'
               GROUP BY DATE_TRUNC('month', s.tarix)),
     orta AS (SELECT AVG(gelir) AS orta_gelir
              FROM ayliq)
SELECT a.ay, a.gelir, ROUND(o.orta_gelir, 2) AS orta_aylıq_gelir
FROM ayliq a
         CROSS JOIN orta o
WHERE a.gelir > o.orta_gelir
ORDER BY a.ay;

-- 6.2 Hər kateqoriyada ən çox satılan məhsul
WITH satis AS (SELECT k.ad          AS kateqoriya,
                      p.ad          AS mehsul,
                      SUM(d.miqdar) AS satilan_say
               FROM sifaris_detallari d
                        JOIN sifarisler s ON s.sifaris_id = d.sifaris_id
                        JOIN mehsullar p ON p.mehsul_id = d.mehsul_id
                        JOIN kateqoriyalar k ON k.kateqoriya_id = p.kateqoriya_id
               WHERE s.status <> 'Ləğv edildi'
               GROUP BY k.ad, p.ad),
     sirali AS (SELECT kateqoriya,
                       mehsul,
                       satilan_say,
                       RANK() OVER (PARTITION BY kateqoriya ORDER BY satilan_say DESC) AS sira
                FROM satis)
SELECT kateqoriya, mehsul, satilan_say
FROM sirali
WHERE sira = 1;

-- 6.3 Müştərinin ümumi xərci və seqmenti
WITH xerc AS (SELECT m.musteri_id,
                     m.ad,
                     m.soyad,
                     COALESCE(SUM(d.miqdar * d.qiymet), 0) AS umumi_xerc
              FROM musteriler m
                       LEFT JOIN sifarisler s ON s.musteri_id = m.musteri_id
                  AND s.status <> 'Ləğv edildi'
                       LEFT JOIN sifaris_detallari d ON d.sifaris_id = s.sifaris_id
              GROUP BY m.musteri_id, m.ad, m.soyad),
     bolunme AS (SELECT *,
                        CASE
                            WHEN umumi_xerc > 2000 THEN 'VIP'
                            WHEN umumi_xerc >= 500 THEN 'Adi'
                            ELSE 'Yeni'
                            END AS seqment
                 FROM xerc)
SELECT musteri_id, ad, soyad, umumi_xerc, seqment
FROM bolunme
ORDER BY umumi_xerc DESC;

-- 6.4 Hər müştərinin ilk və son sifariş tarixi, aralarındakı gün fərqi
WITH tarixler AS (SELECT musteri_id,
                         MIN(tarix) AS ilk_sifaris,
                         MAX(tarix) AS son_sifaris
                  FROM sifarisler
                  GROUP BY musteri_id)
SELECT m.ad,
       m.soyad,
       t.ilk_sifaris,
       t.son_sifaris,
       t.son_sifaris - t.ilk_sifaris AS gun_ferqi
FROM tarixler t
         JOIN musteriler m ON m.musteri_id = t.musteri_id
ORDER BY m.musteri_id;

-- 6.5 Rekursiv CTE ilə kateqoriya ağacı
WITH RECURSIVE agac AS (SELECT kateqoriya_id, ad, 1 AS seviyye, ad::text AS yol
                        FROM kateqoriyalar
                        WHERE ust_kateqoriya_id IS NULL

                        UNION ALL

                        SELECT k.kateqoriya_id, k.ad, a.seviyye + 1, a.yol || ' -> ' || k.ad
                        FROM kateqoriyalar k
                                 JOIN agac a ON k.ust_kateqoriya_id = a.kateqoriya_id)
SELECT seviyye, ad, yol
FROM agac
ORDER BY yol;
