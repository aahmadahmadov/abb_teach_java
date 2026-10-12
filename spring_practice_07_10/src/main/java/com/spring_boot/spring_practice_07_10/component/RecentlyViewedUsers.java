package com.spring_boot.spring_practice_07_10.component;

import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.List;

@Getter
@Component
@SessionScope
public class RecentlyViewedUsers {

    private final List<Long> ids = new ArrayList<>();

    public void add(Long id) {

        ids.remove(id);
        ids.add(0, id);

        if (ids.size() > 5) {
            ids.remove(ids.size() - 1);
        }

    }

    @PreDestroy
    public void destroy() {
        System.out.println("RecentlyViewedUsers silindi, session bitdi");
    }

}
