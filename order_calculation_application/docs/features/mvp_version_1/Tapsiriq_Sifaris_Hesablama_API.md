# Tapşırıq: Sifariş Hesablama API

**Texnologiya:** Java 21, Spring Boot  
**Müddət:** \_\_\_\_\_\_  
**Təhvil tarixi:** \_\_\_\_\_\_

# 1. Məqsəd

Sifarişin yekun məbləğini hesablayan bir POST endpoint yazın. Endirimlər, ƏDV və çatdırılma aşağıdakı qaydalarla hesablanır. Verilənlər bazası yoxdur, bütün məntiq service sinfində olur, nəticə DTO kimi qaytarılır.

Tapşırıq bu bacarıqları yoxlayır: Controller → Service → DTO strukturu, BigDecimal ilə pul hesabı, qaydaların düzgün ardıcıllıqla tətbiqi və girişin yoxlanması.

# 2. Ümumi qaydalar

- Layihədə DB, JPA, Repository istifadə olunmur.
- Bütün hesablama OrderCalculationService sinfində olur. Controller-də heç bir if və ya hesablama olmamalıdır.
- Pul məbləğləri yalnız BigDecimal ilə hesablanır. double və float istifadə etmək qadağandır.
- Hər məbləğ 2 rəqəm dəqiqliyə yuvarlaqlaşdırılır: setScale(2, RoundingMode.HALF_UP).
- Faizlər, limitlər və promo kodlar private static final sabitlər kimi yazılır. Kodun içində çılpaq rəqəm (məsələn, 0.18) olmamalıdır.
- DTO-lar adi sinif kimi yazılır: private sahələr, boş konstruktor, getter və setter. record və Lombok istifadə olunmur.
- **Exception yazmaq lazım deyil.** Xəta hallarında System.out.println(...) ilə konsola mesaj yazmaq kifayətdir (bax: bölmə 7).

# 3. Yaradılmalı fayllar

Bütün siniflər az.iktex.coreplatform.simasignapi paketinin altında olur.

| **Paket** | **Sinif** | **Təyinatı** |
| --- | --- | --- |
| enums | CustomerType | Müştəri tipi: REGULAR, VIP |
| dto | OrderRequest | Sifariş sorğusu |
| dto | OrderItemRequest | Sorğudakı bir məhsul sətri |
| dto | OrderResponse | Hesablamanın nəticəsi |
| dto | OrderItemResponse | Nəticədəki bir məhsul sətri |
| service | OrderCalculationService | Bütün hesablama və yoxlama məntiqi |
| controller | OrderController | Endpoint, sorğunu service-ə ötürür |

# 4. DTO-ların sahələri

## OrderRequest

| **Sahə** | **Tip** | **Təsvir** |
| --- | --- | --- |
| customerType | CustomerType | Mütləqdir. REGULAR və ya VIP |
| promoCode | String | Boş ola bilər. YENI10 və ya ENDIRIM20 |
| deliveryCity | String | Mütləqdir. Çatdırılma şəhəri |
| items | List\<OrderItemRequest> | Mütləqdir, ən azı 1 sətir |

## OrderItemRequest

| **Sahə** | **Tip** | **Təsvir** |
| --- | --- | --- |
| productName | String | Məhsulun adı, boş ola bilməz |
| unitPrice | BigDecimal | Vahid qiymət, 0-dan böyük |
| quantity | Integer | Say, ən azı 1 |

## OrderItemResponse

| **Sahə** | **Tip** | **Təsvir** |
| --- | --- | --- |
| productName | String | Sorğudan gəlir |
| unitPrice | BigDecimal | Sorğudan gəlir |
| quantity | Integer | Sorğudan gəlir |
| lineTotal | BigDecimal | unitPrice × quantity |
| quantityDiscount | BigDecimal | Say endirimi (yoxdursa 0.00) |
| finalLineTotal | BigDecimal | lineTotal − quantityDiscount |

## OrderResponse

| **Sahə** | **Tip** | **Təsvir** |
| --- | --- | --- |
| items | List\<OrderItemResponse> | Hesablanmış sətirlər |
| subtotal | BigDecimal | Sətirlərin cəmi |
| customerDiscount | BigDecimal | VIP endirimi |
| promoDiscount | BigDecimal | Promo kod endirimi |
| totalDiscount | BigDecimal | Ümumi endirim (limitdən sonra) |
| amountAfterDiscount | BigDecimal | subtotal − totalDiscount |
| vat | BigDecimal | ƏDV məbləği |
| deliveryFee | BigDecimal | Çatdırılma haqqı |
| grandTotal | BigDecimal | Yekun məbləğ |
| appliedRules | List\<String> | Tətbiq olunan qaydaların mətnləri |

# 5. Endpoint

POST /sima-sign/api/orders/calculate sorğunu qəbul edir və nəticəni 200 OK ilə qaytarır. Controller metodu bir sətirdən ibarət olmalıdır: return orderCalculationService.calculate(request);

## Sorğu nümunəsi

```json
{
  "customerType": "VIP",
  "promoCode": "YENI10",
  "deliveryCity": "Bakı",
  "items": [
    { "productName": "Qulaqlıq", "unitPrice": 150.00, "quantity": 2 }
  ]
}
```

## Gözlənilən cavab

```json
{
  "items": [
    {
      "productName": "Qulaqlıq",
      "unitPrice": 150.00,
      "quantity": 2,
      "lineTotal": 300.00,
      "quantityDiscount": 0.00,
      "finalLineTotal": 300.00
    }
  ],
  "subtotal": 300.00,
  "customerDiscount": 45.00,
  "promoDiscount": 30.00,
  "totalDiscount": 75.00,
  "amountAfterDiscount": 225.00,
  "vat": 40.50,
  "deliveryFee": 0.00,
  "grandTotal": 265.50,
  "appliedRules": ["VIP endirimi 15%", "YENI10 promo kodu 10%", "Pulsuz çatdırılma"]
}
```

# 6. Hesablama qaydaları

Qaydalar məhz bu ardıcıllıqla tətbiq olunur. Ardıcıllıq dəyişsə, nəticə də dəyişəcək. Bütün məbləğlər AZN-dir.

1. **Sətir məbləği:** lineTotal = unitPrice × quantity.
2. **Say endirimi:** sətirdə quantity ≥ 10 olarsa, həmin sətrə 5% endirim. finalLineTotal = lineTotal − quantityDiscount.
3. **Subtotal:** bütün sətirlərin finalLineTotal cəmi.
4. **Müştəri endirimi:** VIP üçün subtotal-ın 15%-i, REGULAR üçün 0.
5. **Promo kod** (subtotal üzərindən, yalnız bir kod): YENI10 subtotal-ın 10%-i; ENDIRIM20 sabit 20.00 AZN, ancaq subtotal ≥ 100.00 olduqda. Şərt ödənmirsə, endirim 0 olur.
6. **Endirim limiti:** totalDiscount = customerDiscount + promoDiscount, amma subtotal-ın 30%-dən çox ola bilməz. Çox olarsa, 30%-ə endirilir.
7. **Endirimdən sonra:** amountAfterDiscount = subtotal − totalDiscount.
8. **ƏDV:** vat = amountAfterDiscount × 18%.
9. **Çatdırılma:** amountAfterDiscount ≥ 200.00 olarsa pulsuz. Əks halda şəhər “Bakı”, “BAKI” və ya “Baku” olarsa 5.00, digər şəhərlər üçün 10.00.
10. **Yekun:** grandTotal = amountAfterDiscount + vat + deliveryFee.

## Sabitlər

Service sinfində ən azı bu sabitlər olmalıdır:

| **Sabit** | **Dəyər** |
| --- | --- |
| QUANTITY_DISCOUNT_THRESHOLD | 10 |
| QUANTITY_DISCOUNT_RATE | 0.05 |
| VIP_DISCOUNT_RATE | 0.15 |
| YENI10_RATE | 0.10 |
| ENDIRIM20_AMOUNT | 20.00 |
| ENDIRIM20_MIN_SUBTOTAL | 100.00 |
| MAX_DISCOUNT_RATE | 0.30 |
| VAT_RATE | 0.18 |
| FREE_DELIVERY_THRESHOLD | 200.00 |
| BAKU_DELIVERY_FEE | 5.00 |
| OTHER_CITY_DELIVERY_FEE | 10.00 |

## Service metodları

Məntiqi bir böyük metoda yığmayın, bu metodlara bölün:

| **Metod** | **Nə edir** |
| --- | --- |
| public OrderResponse calculate(OrderRequest request) | Əsas metod: yoxlayır, qaydaları ardıcıl çağırır, cavabı doldurur |
| private boolean isValid(OrderRequest request) | Girişi yoxlayır, xəta varsa mesajı çap edib false qaytarır |
| private OrderItemResponse calculateLine(OrderItemRequest item) | Bir sətri və say endirimini hesablayır |
| private BigDecimal calculatePromoDiscount(String code, BigDecimal subtotal) | Promo kod endirimini hesablayır |
| private BigDecimal calculateDeliveryFee(BigDecimal amount, String city) | Çatdırılma haqqını hesablayır |

## appliedRules mətnləri

Hər qayda tətbiq olunanda appliedRules siyahısına aşağıdakı mətn olduğu kimi əlavə olunur:

| **Nə vaxt** | **Mətn** |
| --- | --- |
| Sətirdə say endirimi var | Say endirimi 5%: \<məhsulun adı> |
| Müştəri VIP-dir | VIP endirimi 15% |
| YENI10 tətbiq olundu | YENI10 promo kodu 10% |
| ENDIRIM20 tətbiq olundu | ENDIRIM20 promo kodu 20.00 AZN |
| ENDIRIM20 şərti ödənmədi | ENDIRIM20 tətbiq olunmadı: minimum məbləğ 100.00 AZN |
| Endirim limiti işə düşdü | Endirim limiti tətbiq olundu: maksimum 30% |
| Çatdırılma pulsuzdur | Pulsuz çatdırılma |

# 7. Xəta halları

Exception yazmaq lazım deyil. Yanlış giriş olduqda service System.out.println ilə mesajı konsola yazır və null qaytarır. Bu halda endpoint boş cavab qaytaracaq, bu normaldır.

```java
if (request.getItems() == null || request.getItems().isEmpty()) {
    System.out.println("XƏTA: Sifarişdə ən azı bir məhsul olmalıdır");
    return false;
}
```

| **Hal** | **Konsola yazılacaq mesaj** | **Nəticə** |
| --- | --- | --- |
| items boşdur və ya yoxdur | XƏTA: Sifarişdə ən azı bir məhsul olmalıdır | null |
| productName boşdur | XƏTA: Məhsulun adı boş ola bilməz | null |
| quantity 1-dən kiçikdir | XƏTA: Say 1-dən kiçik ola bilməz | null |
| unitPrice 0 və ya mənfidir | XƏTA: Qiymət 0-dan böyük olmalıdır | null |
| customerType yoxdur | XƏTA: Müştəri tipi mütləqdir | null |
| deliveryCity boşdur | XƏTA: Çatdırılma şəhəri mütləqdir | null |
| Naməlum promo kod | XƏBƏRDARLIQ: Promo kod tapılmadı: SALAM | Hesablama promo kodsuz davam edir |

# 8. Test ssenariləri

Hər ssenarini Postman və ya Swagger ilə yoxlayın. Nəticə cədvəldəki rəqəmlərlə qəpiyinə qədər üst-üstə düşməlidir.

| **#** | **Sorğu** | **subtotal** | **endirim** | **vat** | **çatdırılma** | **grandTotal** |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | REGULAR, promo yox, Bakı; 20.00 × 2 | 40.00 | 0.00 | 7.20 | 5.00 | 52.20 |
| 2 | REGULAR, promo yox, Gəncə; 3.00 × 10 və 50.00 × 1 | 78.50 | 0.00 | 14.13 | 10.00 | 102.63 |
| 3 | VIP, YENI10, Bakı; 150.00 × 2 | 300.00 | 75.00 | 40.50 | 0.00 | 265.50 |
| 4 | VIP, ENDIRIM20, Bakı; 25.00 × 4 | 100.00 | 30.00 | 12.60 | 5.00 | 87.60 |
| 5 | REGULAR, ENDIRIM20, Bakı; 30.00 × 3 | 90.00 | 0.00 | 16.20 | 5.00 | 111.20 |
| 6 | REGULAR, YENI10, Sumqayıt; 220.00 × 1 | 220.00 | 22.00 | 35.64 | 10.00 | 243.64 |
| 7 | REGULAR, SALAM, Bakı; 10.00 × 1 | 10.00 | 0.00 | 1.80 | 5.00 | 16.80 |
| 8 | REGULAR, promo yox, Bakı; 10.00 × 0 | — | — | — | — | boş cavab |

Ssenarilərin yoxladığı əsas məqamlar:

- **2:** say endirimi, 30.00 sətri 28.50 olur.
- **4:** endirim limiti, 15.00 + 20.00 = 35.00 əvəzinə 30.00 tətbiq olunur.
- **5:** ENDIRIM20 minimum məbləğ şərti ödənmir, endirim tətbiq olunmur.
- **6:** subtotal 220 olsa da, endirimdən sonra 198.00 qalır, ona görə çatdırılma pulsuz deyil.
- **7:** konsolda XƏBƏRDARLIQ: Promo kod tapılmadı: SALAM görünür, hesablama davam edir.
- **8:** konsolda XƏTA: Say 1-dən kiçik ola bilməz görünür, cavab boşdur.

# 9. İş ardıcıllığı

Tövsiyə olunan addımlar:

1. CustomerType enum-unu və 4 DTO-nu yazın.
2. OrderController-i yazın, service hələlik boş new OrderResponse() qaytarsın. Postman ilə endpoint-in işlədiyini yoxlayın.
3. Service-də sabitləri və isValid metodunu yazın, 8-ci ssenarini yoxlayın.
4. Qaydaları bir-bir əlavə edin (1-dən 10-a qədər). Hər qaydadan sonra uyğun ssenarini yoxlayın.
5. appliedRules mətnlərini əlavə edin və bütün 8 ssenarini yenidən yoxlayın.

# 10. Təhvil və qiymətləndirmə

Təhvil: GitHub repo linki və ya layihənin zip faylı. README-də 8 ssenarinin nəticəsinin ekran görüntüsü olmalıdır.

| **Meyar** | **Bal** |
| --- | --- |
| 1–7 ssenarilərin nəticəsi düzgündür | 40 |
| Xəta halları konsola düzgün yazılır (ssenari 7 və 8) | 10 |
| Struktur: controller sadədir, məntiq service-dədir, metodlara bölünüb | 20 |
| BigDecimal və yuvarlaqlaşdırma düzgündür | 15 |
| Kod oxunaqlıdır: sabitlər, enum, aydın adlar | 15 |
| **Cəmi** | **100** |

## Bonus (+10 bal)

- Service üçün JUnit testləri yazın, 8 ssenarinin hamısını əhatə etsin.
- Promo kodları Strategy pattern ilə yazın: hər kod ayrıca sinif, PromoStrategy interfeysini implement etsin.
- GET /api/orders/promo-codes endpoint-i mövcud promo kodları və şərtlərini qaytarsın.
