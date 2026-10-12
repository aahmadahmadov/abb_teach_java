package com.spring_boot.spring_practice_07_10.repository;

import com.spring_boot.spring_practice_07_10.component.RequestContext;
import com.spring_boot.spring_practice_07_10.dto.request.UserInfoRequest;
import com.spring_boot.spring_practice_07_10.entity.UserInfo;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MockUserRepository implements UserRepository {

    private final RequestContext requestContext;

    public MockUserRepository(RequestContext requestContext) {
        this.requestContext = requestContext;
        System.out.println("MockUserRepository yaradildi. Hash: " + this.hashCode());
    }

    @Override
    public List<UserInfo> getAllUsers() {
        return List.of(
                new UserInfo(1L, "Mock User", "mock1@example.com"),
                new UserInfo(2L, "Mock User", "mock2@example.com")
        );
    }

    @Override
    public UserInfo getById(Long id) {
        System.out.println("[Repository] requestId=" + requestContext.getRequestId());
        return new UserInfo(id, "Mock User", "mock@example.com");
    }

    @Override
    public UserInfo create(UserInfoRequest user) {
        return new UserInfo(1L, "Mock User", user.email());
    }

}
