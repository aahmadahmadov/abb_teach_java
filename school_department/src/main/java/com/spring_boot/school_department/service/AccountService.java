package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.request.ChangeRoleRequest;
import com.spring_boot.school_department.dto.response.AccountResponse;
import com.spring_boot.school_department.entity.Account;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.enums.RevokeReason;
import com.spring_boot.school_department.enums.Role;
import com.spring_boot.school_department.exception.BusinessException;
import com.spring_boot.school_department.exception.NotFoundException;
import com.spring_boot.school_department.mapper.AccountMapper;
import com.spring_boot.school_department.repository.AccountRepository;
import com.spring_boot.school_department.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final TokenBlacklistService blacklistService;
    private final AccountMapper accountMapper;

    public List<AccountResponse> list(Role role) {

        if (role == null) {
            return accountMapper.toResponseList(accountRepository.findAll());
        }

        return accountMapper.toResponseList(accountRepository.findAllByRole(role));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AccountResponse changeRole(AuthPrincipal admin, Long id, ChangeRoleRequest request) {

        checkNotSelf(admin, id);
        Account account = findAccount(id);

        account.setRole(request.role());
        blacklistService.revokeAll(id, RevokeReason.ADMIN_REVOKE);

        return accountMapper.toResponse(account);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AccountResponse disable(AuthPrincipal admin, Long id) {

        checkNotSelf(admin, id);
        Account account = findAccount(id);

        account.setEnabled(false);
        blacklistService.revokeAll(id, RevokeReason.ACCOUNT_DISABLED);

        return accountMapper.toResponse(account);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AccountResponse enable(Long id) {

        Account account = findAccount(id);
        account.setEnabled(true);

        return accountMapper.toResponse(account);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void revokeTokens(Long id) {

        findAccount(id);
        blacklistService.revokeAll(id, RevokeReason.ADMIN_REVOKE);
    }

    private void checkNotSelf(AuthPrincipal admin, Long id) {

        if (admin.accountId().equals(id)) {
            throw new BusinessException(ErrorCode.ACCOUNT_SELF_MODIFICATION);
        }
    }

    private Account findAccount(Long id) {

        return accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND, id));
    }
}
