package com.spring_boot.school_department.dto.request;

import java.math.BigDecimal;

public record CreateBookRequest(
        String title,
        BigDecimal price,
        Integer pageCount
) {
}
