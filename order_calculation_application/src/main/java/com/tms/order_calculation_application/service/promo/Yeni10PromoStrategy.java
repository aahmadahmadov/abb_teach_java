package com.tms.order_calculation_application.service.promo;

import com.tms.order_calculation_application.enums.PromoCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class Yeni10PromoStrategy implements PromoStrategy {

    private static final BigDecimal RATE = new BigDecimal("0.10");

    @Override
    public BigDecimal calculate(BigDecimal subtotal, List<String> appliedRules) {
        BigDecimal discount = subtotal.multiply(RATE).setScale(2, RoundingMode.HALF_UP);
        appliedRules.add("YENI10 promo kodu 10%");
        return discount;
    }

    @Override
    public PromoCode getPromoCode() {
        return PromoCode.YENI10;
    }
}