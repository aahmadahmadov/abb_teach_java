package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.response.CacheStatsResponse;
import com.spring_boot.school_department.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Cache", description = "Second-level cache statistikasi ve temizlenmesi")
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheService cacheService;

    @Operation(summary = "Cache statistikasi", description = "Hit, miss, put saylari ve region-lar uzre statistika")
    @GetMapping("/stats")
    @ResponseStatus(HttpStatus.OK)
    public CacheStatsResponse getStats() {
        return cacheService.getStats();
    }

    @Operation(summary = "Butun cache-i temizle", description = "Butun region-lari bosaldir")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void evictAll() {
        cacheService.evictAll();
    }

    @Operation(summary = "Statistikani sifirla", description = "Hit/miss saylarini sifirlayir")
    @DeleteMapping("/stats")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearStats() {
        cacheService.clearStats();
    }

    @Operation(summary = "Bir region-u temizle", description = "Region tapilmasa 404 (ERR-1011)")
    @DeleteMapping("/{region}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void evictRegion(@PathVariable String region) {
        cacheService.evictRegion(region);
    }
}
