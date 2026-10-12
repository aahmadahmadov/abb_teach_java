package com.spring_boot.spring_practice_07_10.repository;

import com.spring_boot.spring_practice_07_10.dto.request.UserInfoRequest;
import com.spring_boot.spring_practice_07_10.entity.UserInfo;

import java.util.List;

public interface UserRepository {

    List<UserInfo> getAllUsers();

    UserInfo getById(Long id);

    UserInfo create(UserInfoRequest user);

}
