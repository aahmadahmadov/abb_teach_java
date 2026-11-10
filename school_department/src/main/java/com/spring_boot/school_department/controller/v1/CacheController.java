package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.response.CacheStatsResponse;
import com.spring_boot.school_department.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheService cacheService;

    @GetMapping("/stats")
    @ResponseStatus(HttpStatus.OK)
    public CacheStatsResponse getStats() {
        return cacheService.getStats();
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void evictAll() {
        cacheService.evictAll();
    }

    @DeleteMapping("/stats")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearStats() {
        cacheService.clearStats();
    }

    @DeleteMapping("/{region}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void evictRegion(@PathVariable String region) {
        cacheService.evictRegion(region);
    }
}
