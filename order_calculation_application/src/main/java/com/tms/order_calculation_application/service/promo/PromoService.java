package com.tms.order_calculation_application.service.promo;

import com.tms.order_calculation_application.dto.response.PromoCodeResponse;
import com.tms.order_calculation_application.enums.PromoCode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PromoService {

    public List<PromoCodeResponse> getPromoCodes() {

        List<PromoCodeResponse> list = new ArrayList<>();

        for (PromoCode promo: PromoCode.values()) {
            list.add(new PromoCodeResponse(promo.getCode(), promo.getDescription()));
        }

        return list;
    }

}
