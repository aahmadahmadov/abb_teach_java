package com.spring_boot.school_department.mapper;

import com.spring_boot.school_department.dto.response.CacheStatsResponse;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.mapstruct.Mapper;

import java.util.Map;

@Mapper(componentModel = "spring")
public interface CacheMapper {

    CacheStatsResponse toResponse(Statistics statistics, Map<String, CacheStatsResponse.RegionStats> regions);

    CacheStatsResponse.RegionStats toRegionStats(CacheRegionStatistics region);
}
