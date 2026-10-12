package com.spring_boot.spring_practice_07_10.controller;

import com.spring_boot.spring_practice_07_10.component.RequestContext;
import com.spring_boot.spring_practice_07_10.dto.request.UserInfoRequest;
import com.spring_boot.spring_practice_07_10.dto.response.UserInfoResponse;
import com.spring_boot.spring_practice_07_10.service.UserInfoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user-info")
public class UserInfoController {

    private final UserInfoService userInfoService;
    private final RequestContext requestContext;

    public UserInfoController(UserInfoService userInfoService, RequestContext requestContext) {
        this.userInfoService = userInfoService;
        this.requestContext = requestContext;
        System.out.println("UserInfoController yaradildi. Hash: " + this.hashCode());
    }

    @PostMapping("/create")
    public UserInfoResponse create(@RequestBody UserInfoRequest request) {
        return userInfoService.createUser(request);
    }

    @GetMapping("/{id}")
    public UserInfoResponse getById(@PathVariable Long id) {

        System.out.println("[Controller] requestId=" + requestContext.getRequestId());
        System.out.println("RequestContext class: " + requestContext.getClass());

        UserInfoResponse response = userInfoService.getUserById(id);

        long duration = System.currentTimeMillis() - requestContext.getStartTime();

        System.out.println("Sorgu " + duration + " ms oldu");

        return response;
    }

    @GetMapping("/all")
    public List<UserInfoResponse> getAll() {
        return userInfoService.getAllUsers();
    }

    @GetMapping("/recent")
    public List<UserInfoResponse> getRecent() {
        return userInfoService.getRecentUsers();
    }

}
