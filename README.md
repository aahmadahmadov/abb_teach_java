# SQL Praktiki Tapşırıq
## «OnlaynMağaza» verilənlər bazasının layihələndirilməsi və analizi

### Tapşırığın məqsədi
Bu tapşırıqda siz real həyatdan götürülmüş, lakin səhv qurulmuş bir cədvəli təhlil edəcək, onu normallaşdıracaq, düzgün məhdudiyyətlərlə (constraint) yeni verilənlər bazası quracaq və üzərində JOIN, CTE və SQL funksiyalarından istifadə edən analitik sorğular yazacaqsınız.

### Ssenari
Kiçik bir onlayn mağaza bütün sifarişlərini Excel-dən köçürülmüş tək bir cədvəldə saxlayır. Bu cədvəldə bir xanada bir neçə dəyər saxlanılır, müştəri və kuryer məlumatları dəfələrlə təkrarlanır. Mağaza rəhbərliyi sizdən bazanı düzgün qurmağı və satış hesabatları hazırlamağı xahiş edir.

**Mövcud cədvəl: kohne_sifarisler**

| sifaris_id | tarix | musteri | email | telefon | seher | mehsullar | kateqoriya | qiymetler | miqdarlar | kuryer |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | 2026-01-05 | Aysel Məmmədova | aysel@mail.az | 0501112233 | Bakı | Noutbuk, Siçan | Elektronika, Aksesuar | 1500, 25 | 1, 2 | Rəşad Əliyev |
| 2 | 2026-01-17 | Tural Həsənov | tural@gmail.com | 0552223344 | Gəncə | Telefon | Elektronika | 900 | 1 | Nicat Quliyev |
| 3 | 2026-02-03 | Aysel Məmmədova | aysel@mail.az | 0501112233 | Bakı | Qulaqlıq, Siçan | Aksesuar, Aksesuar | 80, 25 | 1, 1 | Rəşad Əliyev |
| 4 | 2026-02-20 | Leyla Kərimova | leyla@mail.ru | 0703334455 | Sumqayıt | Planşet | Elektronika | 650 | 2 | Rəşad Əliyev |
| 5 | 2026-03-11 | Tural Həsənov | tural@gmail.com | 0552223344 | Gəncə | Noutbuk, Qulaqlıq | Elektronika, Aksesuar | 1500, 80 | 1, 1 | Nicat Quliyev |

---

## Addım 1. Mövcud cədvəlin təhlili (normallaşdırma problemləri)

**Tapşırıq:** Yuxarıdakı cədvəli diqqətlə nəzərdən keçirin və onun hansı normal forma qaydalarını pozduğunu müəyyən edin.

**Tələblər:**
*   **1NF pozuntusu:** hansı sütunlarda bir xanada bir neçə dəyər saxlanıldığını göstərin və bunun hansı problemlər yaratdığını izah edin.
*   **2NF pozuntusu:** məhsulun qiyməti və kateqoriyası kimi atributların bütün açardan deyil, açarın bir hissəsindən asılı olduğunu nümunə ilə izah edin.
*   **3NF pozuntusu:** tranzitiv asılılıqları tapın (məsələn: sifariş → müştəri → şəhər).
*   **Anomaliyalar:** cədvəldə baş verə biləcək əlavə etmə (insert), yeniləmə (update) və silmə (delete) anomaliyalarına hər biri üçün bir nümunə yazın.

**Təhvil veriləcək:** Qısa yazılı izah (yarım və ya bir səhifə).

---

## Addım 2. Normallaşdırılmış sxemin layihələndirilməsi

**Tapşırıq:** Cədvəli ən azı 3NF səviyyəsinə gətirin və yeni sxemi layihələndirin.

**Tələblər:**
*   Sxemdə ən azı aşağıdakı 7 cədvəl olmalıdır: `seherler`, `musteriler`, `kateqoriyalar`, `mehsullar`, `kuryerler`, `sifarisler`, `sifaris_detallari`.
*   Hər cədvəl üçün sütunların adını, məlumat tipini və açarlarını göstərin.
*   Sifarişlə məhsul arasındakı çox-çoxa (N:M) əlaqəni `sifaris_detallari` cədvəli vasitəsilə həll edin.
*   Sifariş zamanı məhsulun qiymətini `sifaris_detallari` cədvəlində ayrıca saxlayın (qiymət sonradan dəyişə bilər) və bunun səbəbini izah edin.
*   Cədvəllər arasındakı əlaqələri (1:N, N:M) göstərən ER diaqramı çəkin (draw.io, dbdiagram.io və ya əl ilə).

**Təhvil veriləcək:** ER diaqramı (şəkil) və cədvəllərin təsviri.

---

## Addım 3. Cədvəllərin yaradılması və Constraint-lər

**Tapşırıq:** `CREATE TABLE` əmrləri ilə bütün cədvəlləri yaradın və aşağıdakı məhdudiyyətlərin hamısını tətbiq edin.

**Tələblər:**

| Constraint | Harada tətbiq olunmalıdır |
| :--- | :--- |
| **PRIMARY KEY** | Hər cədvəldə. `sifaris_detallari` üçün (`sifaris_id`, `mehsul_id`) kompozit açar. |
| **FOREIGN KEY** | Bütün əlaqələrdə. Müştəri silinəndə sifarişləri silinməsin (`ON DELETE RESTRICT`); sifariş silinəndə detalları da silinsin (`ON DELETE CASCADE`). |
| **UNIQUE** | Müştərinin email və telefon sahələri, məhsulun adı, kateqoriyanın adı. |
| **NOT NULL** | Ad, soyad, qiymət, sifariş tarixi və digər vacib sahələr. |
| **CHECK** | qiymet > 0; miqdar >= 1; email '@' simvolunu içərməlidir; status yalnız 'Gözləmədə', 'Göndərildi', 'Çatdırıldı', 'Ləğv edildi' ola bilər. |
| **DEFAULT** | Sifariş tarixi üçün cari tarix, status üçün 'Gözləmədə'. |

*   Constraint-lərə mənalı adlar verin (məsələn: `chk_mehsul_qiymet`, `fk_sifaris_musteri`).
*   Cədvəllərin yaradılma ardıcıllığına diqqət edin: əvvəlcə istinad edilən (valideyn) cədvəllər yaradılmalıdır.

**Təhvil veriləcək:** `01_create_tables.sql` faylı.

---

## Addım 4. Məlumatların daxil edilməsi və Constraint-lərin yoxlanılması

**Tapşırıq:** Cədvəlləri məlumatlarla doldurun və qurduğunuz məhdudiyyətlərin işlədiyini sübut edin.

**Tələblər:**
*   `kohne_sifarisler` cədvəlindəki bütün məlumatları yeni cədvəllərə `INSERT` edin.
*   Əlavə olaraq ən azı 3 yeni müştəri daxil edin; onlardan biri heç sifariş verməmiş, biri isə telefonu qeyd olunmamış olsun.
*   Ən azı 3 yeni məhsul əlavə edin; onlardan biri heç vaxt satılmamış olsun.
*   Müxtəlif aylara aid ən azı 5 yeni sifariş əlavə edin (fərqli statuslarla).
*   Constraint-ləri yoxlamaq üçün qəsdən ən azı 4 səhv `INSERT` yazın: mənfi qiymət, təkrar email, mövcud olmayan müştəri ID-si, icazə verilməyən status.
*   Hər səhv sorğu üçün alınan xəta mesajını hesabata əlavə edin və hansı constraint-in işlədiyini qeyd edin.

**Təhvil veriləcək:** `02_insert_data.sql` faylı və xəta mesajlarının ekran görüntüləri.

---

## Addım 5. JOIN sorğuları

**Tapşırıq:** Aşağıdakı sualları müxtəlif JOIN növlərindən istifadə edərək cavablandırın.

**Sorğular:**
*   **5.1.** Hər sifariş üçün: sifariş nömrəsi, tarix, müştərinin tam adı, şəhəri, kuryerin adı və sifarişin ümumi məbləği (`INNER JOIN` + `SUM`).
*   **5.2.** Heç vaxt sifariş verməmiş müştəriləri tapın (`LEFT JOIN` + `IS NULL`).
*   **5.3.** Heç vaxt satılmamış məhsulları tapın.
*   **5.4.** Hər kateqoriya üzrə satılan məhsulların ümumi sayı və gəliri (`GROUP BY`).
*   **5.5.** Hər kuryerin çatdırdığı sifarişlərin sayı və ümumi məbləği; ən çox iş görən kuryer birinci göstərilsin.
*   **5.6.** Hər şəhər üzrə müştəri sayı və həmin şəhərdən gələn ümumi gəlir.

**Təhvil veriləcək:** `03_join_queries.sql` faylı və hər sorğunun nəticəsinin ekran görüntüsü.

---

## Addım 6. WITH (CTE) sorğuları

**Tapşırıq:** Mürəkkəb hesabatları WITH (Common Table Expression) ifadələri ilə hazırlayın.

**Sorğular:**
*   **6.1.** CTE ilə hər ayın ümumi gəlirini hesablayın, sonra yalnız orta aylıq gəlirdən yüksək olan ayları göstərin.
*   **6.2.** CTE və `RANK()` və ya `ROW_NUMBER()` pəncərə funksiyası ilə hər kateqoriyada ən çox satılan məhsulu tapın.
*   **6.3.** Ardıcıl bir neçə CTE istifadə edərək hər müştərinin ümumi xərcini hesablayın və CASE ilə seqmentlərə bölün: 2000 AZN-dən çox → 'VIP', 500–2000 arası → 'Adi', qalanları → 'Yeni'.
*   **6.4.** CTE ilə hər müştərinin ilk və son sifariş tarixini, həmçinin iki tarix arasındakı gün fərqini tapın.
*   **6.5. (Bonus)** Rekursiv CTE: kateqoriyalar cədvəlinə `ust_kateqoriya_id` sütunu əlavə edin (məsələn: Elektronika → Kompüterlər → Noutbuklar) və `WITH RECURSIVE` ilə kateqoriya ağacını səviyyələri ilə birlikdə çıxarın.

**Təhvil veriləcək:** `04_cte_queries.sql` faylı və nəticələrin ekran görüntüləri.

---

## Addım 7. SQL funksiyaları

**Tapşırıq:** Daxili (built-in) funksiyalardan istifadə edin və öz funksiyanızı yazın.

**Sorğular:**
*   **7.1.** Sətir funksiyaları: müştəri adlarını `UPPER()` ilə, ad və soyadı `CONCAT()` ilə birləşdirilmiş şəkildə, email-in domen hissəsini isə `SUBSTRING()` və `POSITION()` / `CHARINDEX()` ilə ayrıca sütunda göstərin.
*   **7.2.** Tarix funksiyaları: hər sifarişin neçə gün əvvəl verildiyini hesablayın (`CURRENT_DATE`, `DATEDIFF` və ya tarixlərin çıxılması).
*   **7.3.** Aylar üzrə sifariş sayını `EXTRACT(MONTH FROM ...)` və ya `MONTH()` ilə qruplaşdırın.
*   **7.4.** Riyazi funksiyalar: orta sifariş məbləğini `ROUND()` ilə 2 onluq dəqiqliyə yuvarlaqlaşdırın.
*   **7.5.** NULL ilə iş: telefonu olmayan müştərilər üçün `COALESCE()` ilə 'Qeyd olunmayıb' yazın.
*   **7.6.** Öz funksiyanız: `CREATE FUNCTION` ilə müştərinin ID-sini qəbul edib onun ümumi xərcini qaytaran funksiya yazın və onu `SELECT` sorğusunda bütün müştərilər üçün istifadə edin.

**Təhvil veriləcək:** `05_functions.sql` faylı və nəticələrin ekran görüntüləri.

---

## Addım 8. Yekun hesabat

**Tapşırıq:** Görülən işləri bir sənəddə toplayın.

**Hesabatda olmalıdır:**
*   Titul səhifəsi (ad, soyad, qrup, istifadə olunan DBMS).
*   Addım 1-in yazılı təhlili.
*   ER diaqramı.
*   Hər addımın SQL kodu və nəticələrin ekran görüntüləri.
*   Qısa nəticə: normallaşdırma bazaya hansı üstünlüklər qazandırdı? (5–7 cümlə)

### Qiymətləndirmə meyarları

| Addım | Məzmun | Bal |
| :--- | :--- | :--- |
| **1** | Normallaşdırma problemlərinin təhlili | 10 |
| **2** | Normallaşdırılmış sxem və ER diaqramı | 15 |
| **3** | Cədvəllər və constraint-lərin tamlığı | 15 |
| **4** | Məlumatların daxil edilməsi və constraint yoxlaması | 10 |
| **5** | JOIN sorğuları | 15 |
| **6** | CTE sorğuları | 15 |
| **7** | SQL funksiyaları | 15 |
| **8** | Hesabatın tamlığı və səliqəsi | 5 |
| | **Cəmi** | **100** |
| **Bonus** | Rekursiv CTE (6.5) | +5 |

### Təhvil qaydaları
*   Bütün SQL faylları və hesabat bir qovluqda, **Soyad_Ad_Qrup.zip** adı ilə təhvil verilir.
*   SQL kodu səliqəli yazılmalı və hər sorğunun üstündə nə etdiyini izah edən şərh (`-- ...`) olmalıdır.
*   Hər sorğu xətasız işləməlidir; işləməyən sorğu üçün bal verilmir.
*   Tapşırıq fərdi yerinə yetirilir; eyni işlər hər iki tələbə üçün sıfır bal deməkdir.
*   **Son təhvil tarixi:** ____________________
