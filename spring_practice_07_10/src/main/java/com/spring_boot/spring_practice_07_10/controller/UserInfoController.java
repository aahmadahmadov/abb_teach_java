package com.spring_boot.spring_practice_07_10.controller;

import com.spring_boot.spring_practice_07_10.dto.request.UserInfoRequest;
import com.spring_boot.spring_practice_07_10.dto.response.UserInfoResponse;
import com.spring_boot.spring_practice_07_10.service.UserInfoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user-info")
public class UserInfoController {

    private final UserInfoService userInfoService;

    public UserInfoController(UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
        System.out.println("UserInfoController created. Hash: : " + this.hashCode());
    }

    @PostMapping("/create")
    public UserInfoResponse create(@RequestBody UserInfoRequest request) {
        return userInfoService.createUser(request);
    }

    @GetMapping("/{id}")
    public UserInfoResponse getById(@PathVariable Long id) {
        return userInfoService.getUserById(id);
    }

    @GetMapping("/all")
    public List<UserInfoResponse> getAll() {
        return userInfoService.getAllUsers();
    }


}
