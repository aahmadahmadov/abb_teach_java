package com.spring_boot.spring_practice_07_10.repository;

import com.spring_boot.spring_practice_07_10.component.RequestContext;
import com.spring_boot.spring_practice_07_10.dto.request.UserInfoRequest;
import com.spring_boot.spring_practice_07_10.entity.UserInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryUserRepository implements UserRepository {

    // Singelton bean eyni vaxda bir nece thread istifade edir, ona gore thread-safe tyep istifade edilmelidi.
    private final Map<Long, UserInfo> storage = new ConcurrentHashMap<>();

    private final AtomicLong idGenerator = new AtomicLong(0);

    private final RequestContext requestContext;

    public InMemoryUserRepository(RequestContext requestContext) {
        this.requestContext = requestContext;
        System.out.println("InMemoryUserRepository yaradildi. Hash: " + this.hashCode());
    }

    @PostConstruct
    public void init() {
        create(new UserInfoRequest("Ali", "ali@example.com"));
        create(new UserInfoRequest("Veli", "veli@example.com"));
        create(new UserInfoRequest("Aysel", "aysel@example.com"));

        System.out.println("UserRepository hazirdir, 3 user yuklendi");
    }

    @PreDestroy
    public void destroy() {
        storage.clear();

        System.out.println("UserRepository storage temizlendi");
    }

    @Override
    public List<UserInfo> getAllUsers() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public UserInfo getById(Long id) {
        System.out.println("[Repository] requestId=" + requestContext.getRequestId());

        UserInfo user = storage.get(id);

        if (user == null) {
            throw new NoSuchElementException("User not found with id: " + id);
        }

        return user;
    }

    @Override
    public UserInfo create(UserInfoRequest user) {
        UserInfo saved = new UserInfo(idGenerator.incrementAndGet(), user.name(), user.email());

        storage.put(saved.getId(), saved);

        return saved;
    }

}
