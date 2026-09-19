# Ölçmə hesabatı: indekslər (G və H bölmələri)

Ad, Soyad: Ahmad Ahmadov · Qrup: - · Tarix: 2026-09-15

Mühit: PostgreSQL 16 · Cədvəl: `satis_log` (300 000 sətir)
Qeyd: SQL kodu `query.sql` faylındadır. Hər ölçmədən əvvəl `ANALYZE satis_log;` icra olunub.
Ayar: `SET max_parallel_workers_per_gather = 0;`
Qeyd: vaxtlar `EXPLAIN (ANALYZE, BUFFERS)` planının `Execution Time` sətrindəndir; təkrar icrada
onlar bir qədər dəyişir, nisbətlər isə sabit qalır.

---

## G. İndekslər: əsaslar

### 26-cı tapşırıq: `WHERE mehsul_adi = 'Mehsul 4321'`

| Vəziyyət | Plan növü | İcra vaxtı (ms) | shared hit | shared read |
|---|---|---|---|---|
| İndekssiz | Seq Scan | 16.513 | 3258 | 0 |
| İndekslə | Bitmap Index Scan + Bitmap Heap Scan | 0.080 | 60 | 3 |

İzah: indekssiz plan bütün 300 000 sətri oxuyur və filtrdən sonra 60 sətir saxlayır.
`idx_satis_log_mehsul_adi` qurulandan sonra baza əvvəlcə indeksdən 3 blok oxuyur, sonra
yalnız lazım olan 60 cədvəl blokuna gedir. Nəticədə ~200 dəfə sürətlidir.

### 27-ci tapşırıq: kompozit indeks (kateqoriya, tarix)

| Sorğu | Plan növü | İcra vaxtı (ms) | İndeks işlədi? |
|---|---|---|---|
| (a) yalnız kateqoriya | Bitmap Index Scan (cost 663) | 11.661 | Bəli, sol sütun |
| (b) yalnız tarix | Bitmap Index Scan (cost 3266) | 1.330 | Bəli, amma tam indeks skanı ilə |
| (c) kateqoriya + tarix | Bitmap Index Scan (cost 5.02) | 0.249 | Bəli, hər iki sütun |

İzah (sol prefiks qaydası): B-tree indeks açarları soldan sağa sıralayır. (a) və (c)-də
indeksin sol sütunu (kateqoriya) şərtdədir, ona görə axtarış hədəflənmiş olur.
(b)-də sol sütun yoxdur. Planner indeksi yenə seçir, amma onu başdan-sona oxumalı olur:
qiymət 3266, yəni (c)-dəkindən ~650 dəfə baha. Ona görə kompozit indeksdə ən çox
filtrlənən sütun birinci yazılır.

### 28-ci tapşırıq: UNIQUE məhdudiyyət vs UNIQUE INDEX

| Obyekt | pg_constraint-də var? | pg_indexes-də var? | DROP CONSTRAINT nəticəsi |
|---|---|---|---|
| UNIQUE məhdudiyyət (`uq_satis_log_id`) | Bəli (contype = u) | Bəli | Silindi (indeksi də apardı) |
| UNIQUE INDEX (`uq_satis_log_id_musteri`) | Xeyr | Bəli | ERROR: constraint ... does not exist |

İzah: UNIQUE məhdudiyyət qurulanda baza onu icra etmək üçün özü unikal indeks yaradır,
ona görə hər iki kataloqda görünür. Əl ilə yaradılan unikal indeks isə yalnız indeksdir,
`pg_constraint` onu tanımır və yalnız `DROP INDEX` ilə silinir. Fərq: məhdudiyyət məntiqi
qaydadır, indeks onun texniki icrasıdır; xarici açar yalnız məhdudiyyətə istinad edə bilər.

### 29-cu tapşırıq: `WHERE status = 'legv'`

| İndeks | Ölçü | Plan növü | İcra vaxtı (ms) |
|---|---|---|---|
| Tam indeks | 2056 kB | Index Scan | 1.386 |
| Partial indeks (`WHERE status = 'legv'`) | 40 kB | Index Scan | 2.241 |

İzah (partial indeks nə vaxt məqsədəuyğundur): məlumatın yalnız ~1%-i 'legv'-dir, ona görə
partial indeks 50 dəfə kiçikdir. Sürət hər iki halda eyni səviyyədədir (fərq ölçmə
səs-küyüdür), yəni eyni faydanı dəfələrlə ucuz saxlayırıq. Partial indeks sorğuların
yalnız kiçik və seçici hissəyə baxdığı hallarda məqsədəuyğundur: həm disk yeri, həm də
hər INSERT/UPDATE-in qiyməti azalır.

### 30-cu tapşırıq: `WHERE UPPER(mehsul_adi) = 'MEHSUL 100'`

| Həll | Plan növü | İcra vaxtı (ms) |
|---|---|---|
| 26-cı tapşırığın indeksi ilə | Seq Scan | 57.924 |
| Həll 1: ifadə üzrə indeks `(upper(mehsul_adi))` | Bitmap Index Scan | 0.089 |
| Həll 2: hesablanan sütun `mehsul_adi_boyuk` + adi indeks | Bitmap Index Scan | 0.137 |

İzah: B-tree indeksdə sütunun xam dəyərləri saxlanılır; `upper(mehsul_adi)` isə funksiyanın
nəticəsidir və indeks açarı ilə uyğun gəlmir, ona görə plan Seq Scan-a düşür. İfadə üzrə
indeks məhz həmin ifadəni indeksləyir. Hesablanan sütun isə eyni nəticəni sütun kimi saxlayır.
Əlavə yer tutur, amma sorğu funksiya çağırmır.

---

## H. Çətin və qarışıq tapşırıqlar

### 31-ci tapşırıq: Index Only Scan

| Vəziyyət | Plan növü | Heap Fetches | İcra vaxtı (ms) |
|---|---|---|---|
| VACUUM-dan əvvəl | Index Only Scan (3704 blok) | 60 000 | 12.341 |
| VACUUM-dan sonra | Index Only Scan (234 blok) | 0 | 5.646 |

İzah (Heap Fetches = 0 nə deməkdir): indeksdə sorğunun hər iki sütunu var (`seher` açar,
`tarix` INCLUDE), ona görə plan Index Only Scan ola bilir. Amma baza sətrin görünən olduğunu
da bilməlidir, bunu görünürlük xəritəsindən (visibility map) oxuyur. Xəritə boş olanda hər
sətir üçün cədvələ gedir: Heap Fetches 60 000. `VACUUM` xəritəni yeniləyəndən sonra
Heap Fetches = 0, yəni sorğu tam olaraq indeksdən cavablanır və cədvəl oxunmur.

### 32-ci tapşırıq: indeks üzrə sıralama

| Sorğu | Planda Sort var? | Plan növü | İcra vaxtı (ms) |
|---|---|---|---|
| ORDER BY mebleg DESC, indekssiz | Bəli (top-N heapsort) | Sort + Seq Scan | 37.241 |
| ORDER BY mebleg DESC, `(mebleg DESC)` indeksi ilə | Xeyr | Index Scan + Limit | 0.031 |
| ORDER BY mebleg DESC NULLS LAST, köhnə indekslə | Bəli (top-N heapsort) | Sort + Seq Scan | 35.438 |
| ORDER BY mebleg DESC NULLS LAST, `(mebleg DESC NULLS LAST)` indeksi ilə | Xeyr | Index Scan + Limit | 0.049 |

İzah: indeks sətirləri artıq öz sıralama qaydası ilə saxlayır, ona görə `LIMIT 20` sorğusu
cəmi 20 sətir oxuyur və Sort düyününə ehtiyac qalmır. NULLS LAST isə başqa sıralamadır:
DESC indeksin standartı NULLS FIRST-dür, ona görə köhnə indeks yaramır və Sort geri qayıdır.
Uyğun sıralama ilə ayrıca indeks lazımdır.

### 33-cü tapşırıq: indeks ölçüləri hesabatı

| İndeks | Ölçü | Cədvələ nisbəti (%) | Tərif |
|---|---|---|---|
| idx_satis_log_seher_tarix | 9272 kB | 31.2 | btree (seher) INCLUDE (tarix) |
| idx_satis_log_mebleg_desc_nulls_last | 6608 kB | 22.3 | btree (mebleg DESC NULLS LAST) |
| idx_satis_log_mebleg_desc | 6608 kB | 22.3 | btree (mebleg DESC) |
| idx_satis_log_mehsul_adi_boyuk | 2144 kB | 7.2 | btree (mehsul_adi_boyuk) |
| idx_satis_log_mehsul_adi | 2144 kB | 7.2 | btree (mehsul_adi) |
| idx_satis_log_kateqoriya_tarix | 2032 kB | 6.8 | btree (kateqoriya, tarix) |
| idx_satis_log_status_legv | 40 kB | 0.1 | btree (status) WHERE status = 'legv' |

Ən bahalı indeks `idx_satis_log_seher_tarix`-dir: 9272 kB, cədvəlin 31.2%-i.
Səbəb: INCLUDE bəndi ilə `tarix` sütunu da indeksə yazılır, yəni indeks iki sütunun
məlumatını saxlayır. Ən ucuzu partial indeksdir: 40 kB (0.1%).

### 34-cü tapşırıq: istifadə olunmayan indekslər

| İndeks | idx_scan | Qeyd (idx_scan = 0?) |
|---|---|---|
| idx_satis_log_mehsul_adi_boyuk | 0 | Bəli, istifadə olunmur |
| idx_satis_log_kateqoriya_tarix | 1 | İşləyir |
| idx_satis_log_mebleg_desc_nulls_last | 1 | İşləyir |
| idx_satis_log_mehsul_adi | 1 | İşləyir |
| idx_satis_log_seher_tarix | 1 | İşləyir |
| idx_satis_log_status_legv | 1 | İşləyir |
| idx_satis_log_mebleg_desc | 1 | İşləyir |

İcra olunan SELECT-lər:
1. `where mehsul_adi = 'Mehsul 4321'`
2. `where kateqoriya = 'Texnika'`
3. `where status = 'legv'`
4. `select seher, tarix ... where seher = 'Gəncə' limit 10`
5. `select id, mebleg ... order by mebleg desc limit 20`

İzah: heç bir sorğu `mehsul_adi_boyuk` sütununa baxmadı, ona görə onun indeksi 0 skanla qaldı.
Belə indeks yer tutur və hər INSERT/UPDATE-i yavaşladır, faydası isə yoxdur, yəni silinməyə
namizəddir. Qeyd: statistika `pg_stat_reset()`-dən sonrakı dövrü göstərir, ona görə qərar
verməzdən əvvəl real yük altında uzun müddət yığılmalıdır.

### 35-ci tapşırıq: `LIKE '%hsul 4321%'`

| Vəziyyət | Plan növü | İcra vaxtı (ms) |
|---|---|---|
| B-tree indekslə | Seq Scan (indeks işə düşmür) | 21.592 |
| GIN + pg_trgm ilə | Bitmap Index Scan | 2.644 |

İzah (B-tree niyə kömək etmir): B-tree dəyərləri baş hərfdən sıralayır və axtarışa məhz
sətrin əvvəlindən başlayır. `'%hsul 4321%'` şablonunda əvvəl bilinmir, ona görə indeksdə
başlanğıc nöqtə yoxdur. pg_trgm GIN indeksi mətni 3 hərflik parçalara (trigram) bölüb hər
parça üçün sətir siyahısı saxlayır. Ortadan axtarış da indekslə gedir və ~8 dəfə sürətlidir.

### 36-cı tapşırıq: indeksin yazma qiyməti

| Vəziyyət | 100 000 INSERT vaxtı (ms) |
|---|---|
| (a) indekssiz | 192.713 |
| (b) 5 indekslə | 1163.798 |

Fərq (%): +504 (təqribən 6 dəfə yavaş).

Nəticə (bir cümlə): hər indeks oxunuşu sürətləndirir, amma yazılışın qiymətini artırır.
Kütləvi yükləmədə indeksləri əvvəlcə silib, yükləmədən sonra qurmaq daha sərfəlidir.

### 37-ci tapşırıq: FK sütununda indeks

| Vəziyyət | Valideyn sətrin silinmə vaxtı (ms) | Plan növü |
|---|---|---|
| FK sütununda indekssiz | 8.257 (FK trigger: 8.144) | Delete + Index Scan (PK), uşaq cədvəldə tam skan |
| FK sütununda indekslə | 0.173 (FK trigger: 0.131) | Delete + Index Scan (PK), uşaq cədvəldə indeks axtarışı |

PostgreSQL FK sütununa avtomatik indeks yaradırmı? Xeyr. Avtomatik indeks yalnız
PRIMARY KEY və UNIQUE üçün qurulur; xarici açarın öz sütunu indekssiz qalır. Ona görə
valideyn sətri silinəndə uşaq cədvəldə istinad axtarışı tam skana çevrilir. Planda bu,
`Trigger for constraint fk_satis_qeyd_satis` sətrində görünür (~60 dəfə fərq).

### 38-ci tapşırıq: iki ayrı indeks vs kompozit indeks

| Konfiqurasiya | Plan növü | BitmapAnd var? | İcra vaxtı (ms) |
|---|---|---|---|
| (a) iki bir-sütunlu indeks | Bitmap Heap Scan + BitmapAnd | Bəli | 2.598 |
| (b) kompozit (kateqoriya, seher) | Bitmap Heap Scan + tək Bitmap Index Scan | Xeyr | 0.023 |

Hansı daha sürətlidir və niyə: kompozit indeks (~100 dəfə). İki ayrı indeksdə baza
əvvəlcə hər indeksdən bitmap qurur (80 000 + 80 000 sətir), sonra kəsişməni alır.
BitmapAnd məhz budur. Kompozit indeks isə kəsişməni əvvəlcədən hazır saxlayır, tək
axtarışla hər iki şərti tətbiq edir.
Qeyd: bu məlumatda `kateqoriya` və `seher` eyni düsturla (`i % 5`) yaradılıb, ona görə
'Ofis' + 'Bakı' cütü heç bir sətirdə yoxdur (rows = 0), ona görə müqayisə planın qiymətinə görədir.

### 39-cu tapşırıq: şişmə (bloat) və REINDEX

| Mərhələ | İndeks ölçüsü | n_dead_tup |
|---|---|---|
| UPDATE-dən əvvəl (REINDEX + VACUUM sonrası) | 8800 kB | 0 (təkrar icrada 0-2) |
| UPDATE-dən sonra (~40%, 159 999 sətir) | 17 MB | 159 999 |
| REINDEX-dən sonra | 8800 kB | - |

İzah (bloat, MVCC, ölü sətirlər): MVCC-də UPDATE sətri yerində dəyişmir: köhnə versiya ölü
sətir kimi qalır, yenisi ayrıca yazılır. İndekslənmiş sütun (`mebleg`) dəyişdiyi üçün
indeksə də yeni giriş düşür, köhnə giriş dərhal getmir. Nəticədə indeks təxminən iki dəfə
şişir. `REINDEX` indeksi sıfırdan qurur və ölçü ilkin həddə qayıdır.

### 40-cı tapşırıq: yekun audit

| Cədvəl | Sətir sayı (təxmini) | Cədvəl ölçüsü | İndeks sayı | İndekslərin ölçüsü | PK | Status |
|---|---|---|---|---|---|---|
| satis_log | 399 998 | 54 MB | 5 | 40 MB | Var | Nezaret lazimdir |
| satis_qeyd | 200 000 | 10192 kB | 2 | 8816 kB | Var | Nezaret lazimdir |
| sifaris_detal | 1 | 8192 bytes | 1 | 16 kB | Var | Nezaret lazimdir |
| kateqoriya | 1 | 8192 bytes | 1 | 16 kB | Var | Nezaret lazimdir |
| mehsul | 7 | 8192 bytes | 3 | 48 kB | Var | Nezaret lazimdir |
| musteri | 8 | 8192 bytes | 3 | 48 kB | Var | Nezaret lazimdir |
| sifaris | 2 | 8192 bytes | 1 | 16 kB | Var | Nezaret lazimdir |

Nəticə: 'Problemli' statuslu cədvəl yoxdur, hamısının PRIMARY KEY-i var. `satis_log`-da
indekslər cədvəlin 74%-ni tutur (36-cı tapşırıqdakı 5 indeksin qiyməti), ona görə status
'Nezaret lazimdir'. Kiçik cədvəllər cəmi 8 KB-dır və bir neçə indeks saxlayır, ona görə
nisbət formal olaraq 50%-i keçir, amma bu həcmdə praktiki mənası yoxdur.
