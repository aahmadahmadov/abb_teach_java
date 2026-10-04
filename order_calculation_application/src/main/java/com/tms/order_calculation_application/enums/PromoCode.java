package com.tms.order_calculation_application.enums;

public enum PromoCode {
    YENI10("YENI10", "Subtotal-ın 10%-i qədər endirim"),
    ENDIRIM20("ENDIRIM20", "20.00 AZN endirim (minimum subtotal 100.00 AZN olmalıdır)");

    private final String code;
    private final String description;

    PromoCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PromoCode fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (PromoCode promo: values()) {
            if (promo.code.equalsIgnoreCase(value)) {
                return promo;
            }
        }

        return null;
    }
}