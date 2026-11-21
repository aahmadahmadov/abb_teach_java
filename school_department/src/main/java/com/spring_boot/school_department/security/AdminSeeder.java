package com.spring_boot.school_department.security;

import com.spring_boot.school_department.entity.Account;
import com.spring_boot.school_department.enums.Role;
import com.spring_boot.school_department.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin.username}")
    private String username;

    @Value("${app.security.admin.email}")
    private String email;

    @Value("${app.security.admin.password}")
    private String password;

    @Override
    public void run(String... args) {

        if (accountRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        Account admin = new Account();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        accountRepository.save(admin);

        log.info("First ADMIN account created: {}", username);
    }
}
