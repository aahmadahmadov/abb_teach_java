# Ölçmə Hesabatı — İndekslər (G və H bölmələri)

Ad, Soyad: ______________________________ Qrup: ____________ Tarix: ____________

Mühit: PostgreSQL 15+ · Cədvəl: `satis_log` (300 000 sətir)
Qeyd: SQL kodu `query.sql` faylındadır. Hər ölçmədən əvvəl `ANALYZE satis_log;` icra olunub.
Ayar: `SET max_parallel_workers_per_gather = 0;`

---

## G. İndekslər — əsaslar

### 26-cı tapşırıq — `WHERE mehsul_adi = 'Mehsul 4321'`

| Vəziyyət | Plan növü | İcra vaxtı (ms) | shared hit | shared read |
|---|---|---|---|---|
| İndekssiz |  |  |  |  |
| İndekslə |  |  |  |  |

İzah:

### 27-ci tapşırıq — kompozit indeks (kateqoriya, tarix)

| Sorğu | Plan növü | İcra vaxtı (ms) | İndeks işlədi? |
|---|---|---|---|
| (a) yalnız kateqoriya |  |  |  |
| (b) yalnız tarix |  |  |  |
| (c) kateqoriya + tarix |  |  |  |

İzah (sol prefiks qaydası):

### 28-ci tapşırıq — UNIQUE məhdudiyyət vs UNIQUE INDEX

| Obyekt | pg_constraint-də var? | pg_indexes-də var? | DROP CONSTRAINT nəticəsi |
|---|---|---|---|
| UNIQUE məhdudiyyət |  |  |  |
| UNIQUE INDEX |  |  |  |

İzah:

### 29-cu tapşırıq — `WHERE status = 'legv'`

| İndeks | Ölçü | Plan növü | İcra vaxtı (ms) |
|---|---|---|---|
| Tam indeks |  |  |  |
| Partial indeks |  |  |  |

İzah (partial indeks nə vaxt məqsədəuyğundur):

### 30-cu tapşırıq — `WHERE UPPER(mehsul_adi) = 'MEHSUL 100'`

| Həll | Plan növü | İcra vaxtı (ms) |
|---|---|---|
| 26-cı tapşırığın indeksi ilə |  |  |
| Həll 1: |  |  |
| Həll 2: |  |  |

İzah:

---

## H. Çətin və qarışıq tapşırıqlar

### 31-ci tapşırıq — Index Only Scan

| Vəziyyət | Plan növü | Heap Fetches | İcra vaxtı (ms) |
|---|---|---|---|
| VACUUM-dan əvvəl |  |  |  |
| VACUUM-dan sonra |  |  |  |

İzah (Heap Fetches = 0 nə deməkdir):

### 32-ci tapşırıq — indeks üzrə sıralama

| Sorğu | Planda Sort var? | Plan növü | İcra vaxtı (ms) |
|---|---|---|---|
| ORDER BY mebleg DESC |  |  |  |
| ORDER BY mebleg DESC NULLS LAST |  |  |  |

İzah:

### 33-cü tapşırıq — indeks ölçüləri hesabatı

| İndeks | Ölçü | Cədvələ nisbəti (%) | Tərif |
|---|---|---|---|
|  |  |  |  |

Ən «bahalı» indeks:

### 34-cü tapşırıq — istifadə olunmayan indekslər

| İndeks | idx_scan | Qeyd (idx_scan = 0?) |
|---|---|---|
|  |  |  |

İcra olunan SELECT-lər:

İzah:

### 35-ci tapşırıq — `LIKE '%hsul 4321%'`

| Vəziyyət | Plan növü | İcra vaxtı (ms) |
|---|---|---|
| B-tree indekslə |  |  |
| GIN + pg_trgm ilə |  |  |

İzah (B-tree niyə kömək etmir):

### 36-cı tapşırıq — indeksin yazma qiyməti

| Vəziyyət | 100 000 INSERT vaxtı (ms) |
|---|---|
| (a) indekssiz |  |
| (b) 5 indekslə |  |

Fərq (%):

Nəticə (bir cümlə):

### 37-ci tapşırıq — FK sütununda indeks

| Vəziyyət | Valideyn sətrin silinmə vaxtı (ms) | Plan növü |
|---|---|---|
| FK sütununda indekssiz |  |  |
| FK sütununda indekslə |  |  |

PostgreSQL FK sütununa avtomatik indeks yaradırmı?

### 38-ci tapşırıq — iki ayrı indeks vs kompozit indeks

| Konfiqurasiya | Plan növü | BitmapAnd var? | İcra vaxtı (ms) |
|---|---|---|---|
| (a) iki bir-sütunlu indeks |  |  |  |
| (b) kompozit (kateqoriya, seher) |  |  |  |

Hansı daha sürətlidir və niyə:

### 39-cu tapşırıq — şişmə (bloat) və REINDEX

| Mərhələ | İndeks ölçüsü | n_dead_tup |
|---|---|---|
| UPDATE-dən əvvəl |  |  |
| UPDATE-dən sonra (~40%) |  |  |
| REINDEX-dən sonra |  |  |

İzah (bloat, MVCC, ölü sətirlər):

### 40-cı tapşırıq — yekun audit

| Cədvəl | Sətir sayı (təxmini) | Cədvəl ölçüsü | İndeks sayı | İndekslərin ölçüsü | PK | Status |
|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |

Nəticə:
