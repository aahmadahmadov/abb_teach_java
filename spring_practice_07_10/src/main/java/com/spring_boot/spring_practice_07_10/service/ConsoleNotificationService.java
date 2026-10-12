package com.spring_boot.spring_practice_07_10.service;

import org.springframework.stereotype.Service;

@Service
public class ConsoleNotificationService implements NotificationService {

    @Override
    public void sendWelcome(String email) {
        System.out.println("Xoş gəldin, " + email);
    }

}
