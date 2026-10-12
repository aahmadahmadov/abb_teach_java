package com.spring_boot.spring_practice_07_10.service;

import com.spring_boot.spring_practice_07_10.component.RecentlyViewedUsers;
import com.spring_boot.spring_practice_07_10.component.RequestContext;
import com.spring_boot.spring_practice_07_10.component.UserAuditLog;
import com.spring_boot.spring_practice_07_10.dto.request.UserInfoRequest;
import com.spring_boot.spring_practice_07_10.dto.response.UserInfoResponse;
import com.spring_boot.spring_practice_07_10.entity.UserInfo;
import com.spring_boot.spring_practice_07_10.repository.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserInfoService {

    private final UserRepository userRepository;
    private final ObjectProvider<UserAuditLog> auditLogProvider;
    private final RequestContext requestContext;
    private final RecentlyViewedUsers recentlyViewedUsers;

    // optional dependency, implementasiya yoxdursa null olur
    private NotificationService notificationService;

    public UserInfoService(
            @Qualifier("mockUserRepository") UserRepository userRepository,
            ObjectProvider<UserAuditLog> auditLogProvider,
            RequestContext requestContext,
            RecentlyViewedUsers recentlyViewedUsers,
            List<UserRepository> allRepositories
    ) {

        this.userRepository = userRepository;
        this.auditLogProvider = auditLogProvider;
        this.requestContext = requestContext;
        this.recentlyViewedUsers = recentlyViewedUsers;

        System.out.println("UserInfoService yaradildi. Hash: " + this.hashCode());
        System.out.println("UserRepository sayi: " + allRepositories.size());
    }

    @Autowired(required = false)
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public UserInfoResponse createUser(UserInfoRequest request) {
        UserAuditLog auditLog = auditLogProvider.getObject();
        auditLog.setAction("CREATE_USER");

        UserInfoResponse response = toResponse(userRepository.create(request));

        if (notificationService != null) {
            notificationService.sendWelcome(request.email());
        }

        return response;
    }

    public UserInfoResponse getUserById(Long id) {
        System.out.println("[Service] requestId=" + requestContext.getRequestId());

        UserInfoResponse response = toResponse(userRepository.getById(id));
        recentlyViewedUsers.add(id);

        return response;
    }

    public List<UserInfoResponse> getAllUsers() {
        return userRepository.getAllUsers().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<UserInfoResponse> getRecentUsers() {
        List<UserInfoResponse> result = new ArrayList<>();

        for (Long id: recentlyViewedUsers.getIds()) {
            result.add(toResponse(userRepository.getById(id)));
        }

        return result;
    }

    private UserInfoResponse toResponse(UserInfo user) {
        return new UserInfoResponse(user.getId(), user.getName(), user.getEmail());
    }

}
