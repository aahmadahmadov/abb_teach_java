package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.response.CacheStatsResponse;
import com.spring_boot.school_department.entity.Author;
import com.spring_boot.school_department.entity.AuthorBook;
import com.spring_boot.school_department.entity.Book;
import com.spring_boot.school_department.entity.Course;
import com.spring_boot.school_department.entity.Department;
import com.spring_boot.school_department.mapper.CacheMapper;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final EntityManagerFactory entityManagerFactory;

    private final CacheMapper cacheMapper;

    public CacheStatsResponse getStats() {
        Statistics statistics = getStatistics();

        Map<String, CacheStatsResponse.RegionStats> regions = new TreeMap<>();

        for (String regionName : statistics.getSecondLevelCacheRegionNames()) {
            CacheRegionStatistics region = statistics.getCacheRegionStatistics(regionName);

            if (region != null) {
                regions.put(regionName, cacheMapper.toRegionStats(region));
            }
        }

        return cacheMapper.toResponse(statistics, regions);
    }

    public void evictAll() {
        entityManagerFactory.getCache().evictAll();
    }

    public void evictRegion(String region) {

        Class<?> entityClass = switch (region) {
            case "departments" -> Department.class;
            case "courses" -> Course.class;
            case "authors" -> Author.class;
            case "books" -> Book.class;
            case "author-books" -> AuthorBook.class;
            default -> throw new IllegalArgumentException("Unknown region: " + region);
        };

        entityManagerFactory.getCache().evict(entityClass);
    }

    public void clearStats() {
        getStatistics().clear();
    }

    private Statistics getStatistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }
}
