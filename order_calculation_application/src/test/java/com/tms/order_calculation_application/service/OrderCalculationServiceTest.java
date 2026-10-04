package com.tms.order_calculation_application.service;

import com.tms.order_calculation_application.dto.request.OrderItemRequest;
import com.tms.order_calculation_application.dto.request.OrderRequest;
import com.tms.order_calculation_application.dto.response.OrderResponse;
import com.tms.order_calculation_application.enums.CustomerType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderCalculationServiceTest {

    @Autowired
    private OrderCalculationService service;

    private OrderItemRequest item(String name, String price, int qty) {
        OrderItemRequest i = new OrderItemRequest();
        i.setProductName(name);
        i.setUnitPrice(new BigDecimal(price));
        i.setQuantity(qty);
        return i;
    }

    @Test
    void scenario1_regular_noPromo_baku() {
        OrderRequest req = new OrderRequest();
        req.setCustomerType(CustomerType.REGULAR);
        req.setDeliveryCity("Bakı");
        req.setItems(List.of(item("Məhsul", "20.00", 2)));

        OrderResponse res = service.calculate(req);

        assertEquals(new BigDecimal("40.00"), res.getSubtotal());
        assertEquals(new BigDecimal("0.00"), res.getTotalDiscount());
        assertEquals(new BigDecimal("7.20"), res.getVat());
        assertEquals(new BigDecimal("5.00"), res.getDeliveryFee());
        assertEquals(new BigDecimal("52.20"), res.getGrandTotal());
    }

    @Test
    void scenario2_quantityDiscount() {
        OrderRequest req = new OrderRequest();
        req.setCustomerType(CustomerType.REGULAR);
        req.setDeliveryCity("Gəncə");
        req.setItems(List.of(
                item("A", "3.00", 10),
                item("B", "50.00", 1)
        ));

        OrderResponse res = service.calculate(req);

        assertEquals(new BigDecimal("78.50"), res.getSubtotal());
        assertEquals(new BigDecimal("0.00"), res.getTotalDiscount());
        assertEquals(new BigDecimal("14.13"), res.getVat());
        assertEquals(new BigDecimal("10.00"), res.getDeliveryFee());
        assertEquals(new BigDecimal("102.63"), res.getGrandTotal());
    }

    @Test
    void scenario3_vip_yeni10_freeDelivery() {
        OrderRequest req = new OrderRequest();
        req.setCustomerType(CustomerType.VIP);
        req.setPromoCode("YENI10");
        req.setDeliveryCity("Bakı");
        req.setItems(List.of(item("Qulaqlıq", "150.00", 2)));

        OrderResponse res = service.calculate(req);

        assertEquals(new BigDecimal("300.00"), res.getSubtotal());
        assertEquals(new BigDecimal("75.00"), res.getTotalDiscount());
        assertEquals(new BigDecimal("40.50"), res.getVat());
        assertEquals(new BigDecimal("0.00"), res.getDeliveryFee());
        assertEquals(new BigDecimal("265.50"), res.getGrandTotal());
    }

    @Test
    void scenario4_vip_endirim20_limit() {
        OrderRequest req = new OrderRequest();
        req.setCustomerType(CustomerType.VIP);
        req.setPromoCode("ENDIRIM20");
        req.setDeliveryCity("Bakı");
        req.setItems(List.of(item("Məhsul", "25.00", 4)));

        OrderResponse res = service.calculate(req);

        assertEquals(new BigDecimal("100.00"), res.getSubtotal());
        assertEquals(new BigDecimal("30.00"), res.getTotalDiscount()); // limit işə düşdü
        assertEquals(new BigDecimal("12.60"), res.getVat());
        assertEquals(new BigDecimal("5.00"), res.getDeliveryFee());
        assertEquals(new BigDecimal("87.60"), res.getGrandTotal());
    }

    @Test
    void scenario5_endirim20_minNotMet() {
        OrderRequest req = new OrderRequest();
        req.setCustomerType(CustomerType.REGULAR);
        req.setPromoCode("ENDIRIM20");
        req.setDeliveryCity("Bakı");
        req.setItems(List.of(item("Məhsul", "30.00", 3)));

        OrderResponse res = service.calculate(req);

        assertEquals(new BigDecimal("90.00"), res.getSubtotal());
        assertEquals(new BigDecimal("0.00"), res.getTotalDiscount());
        assertEquals(new BigDecimal("16.20"), res.getVat());
        assertEquals(new BigDecimal("5.00"), res.getDeliveryFee());
        assertEquals(new BigDecimal("111.20"), res.getGrandTotal());
    }

    @Test
    void scenario6_yeni10_otherCity() {
        OrderRequest req = new OrderRequest();
        req.setCustomerType(CustomerType.REGULAR);
        req.setPromoCode("YENI10");
        req.setDeliveryCity("Sumqayıt");
        req.setItems(List.of(item("Məhsul", "220.00", 1)));

        OrderResponse res = service.calculate(req);

        assertEquals(new BigDecimal("220.00"), res.getSubtotal());
        assertEquals(new BigDecimal("22.00"), res.getTotalDiscount());
        assertEquals(new BigDecimal("35.64"), res.getVat());
        assertEquals(new BigDecimal("10.00"), res.getDeliveryFee());
        assertEquals(new BigDecimal("243.64"), res.getGrandTotal());
    }

    @Test
    void scenario7_unknownPromo() {
        OrderRequest req = new OrderRequest();
        req.setCustomerType(CustomerType.REGULAR);
        req.setPromoCode("SALAM");
        req.setDeliveryCity("Bakı");
        req.setItems(List.of(item("Məhsul", "10.00", 1)));

        OrderResponse res = service.calculate(req);

        assertEquals(new BigDecimal("10.00"), res.getSubtotal());
        assertEquals(new BigDecimal("0.00"), res.getTotalDiscount());
        assertEquals(new BigDecimal("1.80"), res.getVat());
        assertEquals(new BigDecimal("5.00"), res.getDeliveryFee());
        assertEquals(new BigDecimal("16.80"), res.getGrandTotal());
    }

    @Test
    void scenario8_invalidQuantity_returnsNull() {
        OrderRequest req = new OrderRequest();
        req.setCustomerType(CustomerType.REGULAR);
        req.setDeliveryCity("Bakı");
        req.setItems(List.of(item("Məhsul", "10.00", 0)));

        OrderResponse res = service.calculate(req);

        assertNull(res);
    }
}