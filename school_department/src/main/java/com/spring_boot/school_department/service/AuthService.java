package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.request.RegisterRequest;
import com.spring_boot.school_department.dto.response.AccountResponse;
import com.spring_boot.school_department.entity.Account;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.enums.Role;
import com.spring_boot.school_department.exception.AlreadyExistsException;
import com.spring_boot.school_department.mapper.AccountMapper;
import com.spring_boot.school_department.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountMapper accountMapper;

    @Transactional(propagation = Propagation.REQUIRED)
    public AccountResponse register(RegisterRequest request) {

        if (accountRepository.existsByUsername(request.username())) {
            throw new AlreadyExistsException(ErrorCode.ACCOUNT_USERNAME_EXISTS, request.username());
        }

        if (accountRepository.existsByEmail(request.email())) {
            throw new AlreadyExistsException(ErrorCode.ACCOUNT_EMAIL_EXISTS, request.email());
        }

        Account account = new Account();
        account.setUsername(request.username());
        account.setEmail(request.email());

        account.setPassword(passwordEncoder.encode(request.password()));
        account.setRole(Role.USER);

        accountRepository.save(account);

        return accountMapper.toResponse(account);
    }
}
