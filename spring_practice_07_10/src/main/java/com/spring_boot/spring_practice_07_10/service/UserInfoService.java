package com.spring_boot.spring_practice_07_10.service;

import com.spring_boot.spring_practice_07_10.dto.request.UserInfoRequest;
import com.spring_boot.spring_practice_07_10.dto.response.UserInfoResponse;
import com.spring_boot.spring_practice_07_10.entity.UserInfo;
import com.spring_boot.spring_practice_07_10.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserInfoService {

    private final UserRepository userRepository;

    public UserInfoService(UserRepository userRepository) {
        this.userRepository = userRepository;
        System.out.println("UserInfoService created. Hash: " + this.hashCode());
    }

    public UserInfoResponse createUser(UserInfoRequest request) {
        return toResponse(userRepository.create(request));
    }

    public UserInfoResponse getUserById(Long id) {
        return toResponse(userRepository.getById(id));
    }

    public List<UserInfoResponse> getAllUsers() {
        return userRepository.getAllUsers().stream()
                .map(this::toResponse)
                .toList();
    }

    private UserInfoResponse toResponse(UserInfo user) {
        return new UserInfoResponse(user.getId(), user.getName(), user.getEmail());
    }

}
