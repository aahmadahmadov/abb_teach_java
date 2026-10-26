package com.spring_boot.school_department.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookResponse(
        Long id,
        String title,
        BigDecimal price,
        @JsonProperty("page_count") Integer pageCount,

        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt,

        List<AuthorResponse> authors
) {
}
