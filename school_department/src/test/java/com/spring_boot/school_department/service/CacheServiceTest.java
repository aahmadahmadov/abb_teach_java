package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.response.CacheStatsResponse;
import com.spring_boot.school_department.entity.Author;
import com.spring_boot.school_department.entity.AuthorBook;
import com.spring_boot.school_department.entity.Book;
import com.spring_boot.school_department.entity.Course;
import com.spring_boot.school_department.entity.Department;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.exception.NotFoundException;
import com.spring_boot.school_department.mapper.CacheMapper;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private EntityManagerFactory entityManagerFactory;

    @Mock
    private CacheMapper cacheMapper;

    @Mock
    private Statistics statistics;

    @Mock
    private Cache cache;

    @Mock
    private CacheRegionStatistics regionStatistics;

    @InjectMocks
    private CacheService cacheService;

    @Test
    void getStats_regionHasStatistics_addsRegionToResponse() {
        CacheStatsResponse.RegionStats regionStats = new CacheStatsResponse.RegionStats(3, 1, 2);
        Map<String, CacheStatsResponse.RegionStats> expectedRegions = Map.of("courses", regionStats);
        CacheStatsResponse response = new CacheStatsResponse(3, 1, 2, 0, expectedRegions);
        mockStatistics();
        when(statistics.getSecondLevelCacheRegionNames()).thenReturn(new String[]{"courses"});
        when(statistics.getCacheRegionStatistics("courses")).thenReturn(regionStatistics);
        when(cacheMapper.toRegionStats(regionStatistics)).thenReturn(regionStats);
        when(cacheMapper.toResponse(statistics, expectedRegions)).thenReturn(response);

        CacheStatsResponse result = cacheService.getStats();

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getStats_regionStatisticsNull_skipsRegion() {
        CacheStatsResponse response = new CacheStatsResponse(0, 0, 0, 0, Map.of());
        mockStatistics();
        when(statistics.getSecondLevelCacheRegionNames()).thenReturn(new String[]{"courses"});
        when(statistics.getCacheRegionStatistics("courses")).thenReturn(null);
        when(cacheMapper.toResponse(statistics, Map.of())).thenReturn(response);

        CacheStatsResponse result = cacheService.getStats();

        assertThat(result).isEqualTo(response);
        verify(cacheMapper, never()).toRegionStats(any());
    }

    @Test
    void evictAll_called_evictsWholeCache() {
        when(entityManagerFactory.getCache()).thenReturn(cache);

        cacheService.evictAll();

        verify(cache).evictAll();
    }

    @Test
    void evictRegion_departments_evictsDepartment() {
        when(entityManagerFactory.getCache()).thenReturn(cache);

        cacheService.evictRegion("departments");

        verify(cache).evict(Department.class);
    }

    @Test
    void evictRegion_courses_evictsCourse() {
        when(entityManagerFactory.getCache()).thenReturn(cache);

        cacheService.evictRegion("courses");

        verify(cache).evict(Course.class);
    }

    @Test
    void evictRegion_authors_evictsAuthor() {
        when(entityManagerFactory.getCache()).thenReturn(cache);

        cacheService.evictRegion("authors");

        verify(cache).evict(Author.class);
    }

    @Test
    void evictRegion_books_evictsBook() {
        when(entityManagerFactory.getCache()).thenReturn(cache);

        cacheService.evictRegion("books");

        verify(cache).evict(Book.class);
    }

    @Test
    void evictRegion_authorBooks_evictsAuthorBook() {
        when(entityManagerFactory.getCache()).thenReturn(cache);

        cacheService.evictRegion("author-books");

        verify(cache).evict(AuthorBook.class);
    }

    @Test
    void evictRegion_unknownRegion_throwsNotFound() {
        assertThatThrownBy(() -> cacheService.evictRegion("students"))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CACHE_REGION_NOT_FOUND);

        verify(entityManagerFactory, never()).getCache();
    }

    @Test
    void clearStats_called_clearsStatistics() {
        mockStatistics();

        cacheService.clearStats();

        verify(statistics).clear();
    }

    private void mockStatistics() {
        // SessionFactory ozu de EntityManagerFactory oldugu ucun @Mock field etmirik, yoxsa @InjectMocks onu inject ede biler
        SessionFactory sessionFactory = mock(SessionFactory.class);
        when(entityManagerFactory.unwrap(SessionFactory.class)).thenReturn(sessionFactory);
        when(sessionFactory.getStatistics()).thenReturn(statistics);
    }
}
