package com.spring_boot.spring_practice_07_10.repository;

import com.spring_boot.spring_practice_07_10.dto.request.UserInfoRequest;
import com.spring_boot.spring_practice_07_10.entity.UserInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Repository
public class UserRepository {

    private final ConcurrentHashMap<Long, UserInfo> storage = new ConcurrentHashMap<>();

    private final AtomicLong idGenerator = new AtomicLong(0);

    public UserRepository() {
        System.out.println("UserRepository created. Hash: " + storage.hashCode());
    }

    @PostConstruct
    public void init() {

        create(new UserInfoRequest("Ali", "ali@example.com"));
        create(new UserInfoRequest("Veli", "veli@example.com"));
        create(new UserInfoRequest("Aysel", "aysel@example.com"));
        create(new UserInfoRequest("Mehmet", "mehmet@example.com"));
        create(new UserInfoRequest("Zeynep", "zeynep@example.com"));
        create(new UserInfoRequest("Can", "can@example.com"));
        create(new UserInfoRequest("Elif", "elif@example.com"));
        create(new UserInfoRequest("Burak", "burak@example.com"));
        create(new UserInfoRequest("Deniz", "deniz@example.com"));
        create(new UserInfoRequest("Leyla", "leyla@example.com"));

        System.out.println("UserRepository initialized with sample data. Hash: " + storage.hashCode());
    }

    @PreDestroy
    public void destroy() {
        storage.clear();
        System.out.println("UserRepository storage cleared");
    }

    public List<UserInfo> getAllUsers() {
        return new ArrayList<>(storage.values());
    }

    public UserInfo getById(Long id) {
        UserInfo user = storage.get(id);

        if (user == null) {
            throw new NoSuchElementException("User not found with id: " + id);
        }

        return user;
    }

    public UserInfo create(UserInfoRequest user) {

        UserInfo saved = new UserInfo(idGenerator.incrementAndGet(), user.name(), user.email());

        storage.put(saved.getId(), saved);

        return saved;
    }

}
