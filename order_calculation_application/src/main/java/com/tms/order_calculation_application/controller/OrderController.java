package com.tms.order_calculation_application.controller;

import com.tms.order_calculation_application.dto.request.OrderRequest;
import com.tms.order_calculation_application.dto.response.OrderResponse;
import com.tms.order_calculation_application.dto.response.PromoCodeResponse;
import com.tms.order_calculation_application.service.OrderCalculationService;
import com.tms.order_calculation_application.service.promo.PromoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sima-sign/api/orders")
public class OrderController {

    private final OrderCalculationService orderCalculationService;
    private final PromoService promoService;

    public OrderController(OrderCalculationService orderCalculationService, PromoService promoService) {
        this.orderCalculationService = orderCalculationService;
        this.promoService = promoService;
    }

    @PostMapping("/calculate")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse calculate(@RequestBody OrderRequest request) {
        return orderCalculationService.calculate(request);
    }

    @GetMapping("/promo-codes")
    @ResponseStatus(HttpStatus.OK)
    public List<PromoCodeResponse> getPromoCodes() {
        return promoService.getPromoCodes();
    }

}
