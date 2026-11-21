package com.spring_boot.school_department.security;

import com.spring_boot.school_department.entity.Account;
import com.spring_boot.school_department.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String login) {
        Account account = accountRepository.findByUsernameOrEmail(login, login)
                .orElseThrow(() -> new UsernameNotFoundException(login));

        return User.withUsername(account.getUsername())
                .password(account.getPassword())
                .roles(account.getRole().name())
                .disabled(!account.isEnabled())
                .build();
    }
}
