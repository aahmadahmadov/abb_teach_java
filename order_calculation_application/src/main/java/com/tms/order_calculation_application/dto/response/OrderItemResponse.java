package com.tms.order_calculation_application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItemResponse {

    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;
    private BigDecimal quantityDiscount;
    private BigDecimal finalLineTotal;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public BigDecimal getQuantityDiscount() {
        return quantityDiscount;
    }

    public void setQuantityDiscount(BigDecimal quantityDiscount) {
        this.quantityDiscount = quantityDiscount;
    }

    public BigDecimal getFinalLineTotal() {
        return finalLineTotal;
    }

    public void setFinalLineTotal(BigDecimal finalLineTotal) {
        this.finalLineTotal = finalLineTotal;
    }
}