package com.spring_boot.school_department.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Butun xetalar ucun eyni formatda cavab")
public record ErrorResponse(

        @Schema(description = "HTTP status", example = "404")
        int status,

        @Schema(description = "Unikal xeta kodu", example = "ERR-2000")
        String code,

        @Schema(description = "Secilmis dilde xeta mesaji", example = "Student not found: 99999")
        String message,

        @Schema(description = "Validasiya xetalari: sahe -> mesaj (diger xetalarda bos olur)", example = "{\"name\": \"This field is required\"}")
        Map<String, String> errors,

        @Schema(description = "Sorgunun trace id-si, X-Trace-Id header-i ile eynidir", example = "test-12345")
        String traceId,

        @Schema(description = "Xetanin bas verdiyi vaxt", example = "2026-11-03T09:58:08.290767")
        LocalDateTime timestamp
) {
}
