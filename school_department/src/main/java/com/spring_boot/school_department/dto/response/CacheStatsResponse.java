package com.spring_boot.school_department.dto.response;

import java.util.Map;

public record CacheStatsResponse(
        long secondLevelCacheHitCount,
        long secondLevelCacheMissCount,
        long secondLevelCachePutCount,
        long queryCacheHitCount,
        Map<String, RegionStats> regions
) {

    public record RegionStats(long hitCount, long missCount, long putCount) {
    }
}
