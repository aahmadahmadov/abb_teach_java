package com.tms.order_calculation_application.service.promo;

import com.tms.order_calculation_application.enums.PromoCode;

import java.math.BigDecimal;
import java.util.List;

public interface PromoStrategy {

    BigDecimal calculate(BigDecimal subtotal, List<String> appliedRules);

    PromoCode getPromoCode();

}