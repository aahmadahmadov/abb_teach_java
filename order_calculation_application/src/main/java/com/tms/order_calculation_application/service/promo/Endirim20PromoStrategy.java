package com.tms.order_calculation_application.service.promo;

import com.tms.order_calculation_application.enums.PromoCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class Endirim20PromoStrategy implements PromoStrategy {

    private static final BigDecimal AMOUNT = new BigDecimal("20.00");
    private static final BigDecimal MIN_SUBTOTAL = new BigDecimal("100.00");

    @Override
    public BigDecimal calculate(BigDecimal subtotal, List<String> appliedRules) {
        if (subtotal.compareTo(MIN_SUBTOTAL) >= 0) {
            appliedRules.add("ENDIRIM20 promo kodu 20.00 AZN");
            return AMOUNT;
        } else {
            appliedRules.add("ENDIRIM20 tətbiq olunmadı: minimum məbləğ 100.00 AZN");
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    @Override
    public PromoCode getPromoCode() {
        return PromoCode.ENDIRIM20;
    }
}