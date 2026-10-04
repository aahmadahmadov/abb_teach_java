package com.tms.order_calculation_application.service;

import com.tms.order_calculation_application.dto.request.OrderItemRequest;
import com.tms.order_calculation_application.dto.request.OrderRequest;
import com.tms.order_calculation_application.dto.response.OrderItemResponse;
import com.tms.order_calculation_application.dto.response.OrderResponse;
import com.tms.order_calculation_application.enums.CustomerType;
import com.tms.order_calculation_application.enums.PromoCode;
import com.tms.order_calculation_application.service.promo.Endirim20PromoStrategy;
import com.tms.order_calculation_application.service.promo.PromoStrategy;
import com.tms.order_calculation_application.service.promo.Yeni10PromoStrategy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderCalculationService {

    private static final int QUANTITY_DISCOUNT_THRESHOLD = 10;
    private static final BigDecimal QUANTITY_DISCOUNT_RATE = new BigDecimal("0.05");
    private static final BigDecimal VIP_DISCOUNT_RATE = new BigDecimal("0.15");
    private static final BigDecimal MAX_DISCOUNT_RATE = new BigDecimal("0.30");
    private static final BigDecimal VAT_RATE = new BigDecimal("0.18");
    private static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("200.00");
    private static final BigDecimal BAKU_DELIVERY_FEE = new BigDecimal("5.00");
    private static final BigDecimal OTHER_CITY_DELIVERY_FEE = new BigDecimal("10.00");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final List<String> bakuDeliviryCases = new ArrayList<>(List.of("Bakı", "BAKI", "Baku"));

    private final Map<PromoCode, PromoStrategy> promoStrategies;

    public OrderCalculationService(List<PromoStrategy> strategies) {
        this.promoStrategies = strategies.stream()
                .collect(Collectors.toMap(PromoStrategy::getPromoCode, s -> s));
    }

    public OrderResponse calculate(OrderRequest request) {

        if (!isValid(request)) {
            return null;
        }

        List<String> appliedRules = new ArrayList<>();
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        BigDecimal subtotal = ZERO;

        for (OrderItemRequest item: request.getItems()) {

            OrderItemResponse line = calculateLine(item);
            itemResponses.add(line);

            subtotal = subtotal.add(line.getFinalLineTotal());

            if (line.getQuantityDiscount().compareTo(ZERO) > 0) {
                appliedRules.add("Say endirimi 5%: " + item.getProductName());
            }
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal customerDiscount = ZERO;

        if (request.getCustomerType().equals(CustomerType.VIP)) {
            customerDiscount = subtotal.multiply(VIP_DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
            appliedRules.add("VIP endirimi 15%");
        }

        BigDecimal promoDiscount = calculatePromoDiscount(request.getPromoCode(), subtotal, appliedRules);

        BigDecimal totalDiscount = customerDiscount.add(promoDiscount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxDiscount = subtotal.multiply(MAX_DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);

        if (totalDiscount.compareTo(maxDiscount) > 0) {
            totalDiscount = maxDiscount;
            appliedRules.add("Endirim limiti tətbiq olundu: maksimum 30%");
        }

        BigDecimal amountAfterDiscount = subtotal.subtract(totalDiscount).setScale(2, RoundingMode.HALF_UP);

        BigDecimal vat = amountAfterDiscount.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);

        BigDecimal deliveryFee = calculateDeliveryFee(amountAfterDiscount, request.getDeliveryCity(), appliedRules);

        BigDecimal grandTotal = amountAfterDiscount.add(vat).add(deliveryFee).setScale(2, RoundingMode.HALF_UP);

        return OrderResponse.builder()
                .items(itemResponses)
                .subtotal(subtotal)
                .customerDiscount(customerDiscount)
                .promoDiscount(promoDiscount)
                .totalDiscount(totalDiscount)
                .amountAfterDiscount(amountAfterDiscount)
                .vat(vat)
                .deliveryFee(deliveryFee)
                .grandTotal(grandTotal)
                .appliedRules(appliedRules)
                .build();
    }

    private boolean isValid(OrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            System.out.println("XƏTA: Sifarişdə ən azı bir məhsul olmalıdır");
            return false;
        }

        if (request.getCustomerType() == null) {
            System.out.println("XƏTA: Müştəri tipi mütləqdir");
            return false;
        }

        if (request.getDeliveryCity() == null || request.getDeliveryCity().isBlank()) {
            System.out.println("XƏTA: Çatdırılma şəhəri mütləqdir");
            return false;
        }

        for (OrderItemRequest item: request.getItems()) {
            if (item.getProductName() == null || item.getProductName().isBlank()) {
                System.out.println("XƏTA: Məhsulun adı boş ola bilməz");
                return false;
            }

            if (item.getQuantity() == null || item.getQuantity() < 1) {
                System.out.println("XƏTA: Say 1-dən kiçik ola bilməz");
                return false;
            }

            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("XƏTA: Qiymət 0-dan böyük olmalıdır");
                return false;
            }
        }

        return true;
    }

    private OrderItemResponse calculateLine(OrderItemRequest item) {

        BigDecimal lineTotal = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal quantityDiscount = ZERO;

        if (item.getQuantity() >= QUANTITY_DISCOUNT_THRESHOLD) {
            quantityDiscount = lineTotal.multiply(QUANTITY_DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
        }

        return OrderItemResponse.builder()
                .productName(item.getProductName())
                .unitPrice(item.getUnitPrice().setScale(2, RoundingMode.HALF_UP))
                .quantity(item.getQuantity())
                .lineTotal(lineTotal)
                .quantityDiscount(quantityDiscount)
                .finalLineTotal(
                        lineTotal.subtract(quantityDiscount).setScale(2, RoundingMode.HALF_UP)
                )
                .build();
    }

    private BigDecimal calculatePromoDiscount(String code, BigDecimal subtotal, List<String> appliedRules) {
        PromoCode promoCode = PromoCode.fromString(code);

        if (promoCode == null) {
            if (code != null && !code.isBlank()) {
                System.out.println("XƏBƏRDARLIQ: Promo kod tapılmadı: " + code);
            }
            return ZERO;
        }

        PromoStrategy strategy = promoStrategies.get(promoCode);

        if (strategy != null) {
            return strategy.calculate(subtotal, appliedRules);
        }

        return ZERO;
    }

    private BigDecimal calculateDeliveryFee(BigDecimal amountAfterDiscount, String city, List<String> appliedRules) {

        if (amountAfterDiscount.compareTo(FREE_DELIVERY_THRESHOLD) >= 0) {
            appliedRules.add("Pulsuz çatdırılma");
            return ZERO;
        }

        if (city != null && bakuDeliviryCases.contains(city)) {
            return BAKU_DELIVERY_FEE;
        }

        return OTHER_CITY_DELIVERY_FEE;
    }
}