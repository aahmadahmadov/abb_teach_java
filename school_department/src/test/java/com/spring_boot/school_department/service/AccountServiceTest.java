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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TokenBlacklistService blacklistService;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    @Test
    void list_roleNull_returnsAllAccounts() {
        List<Account> accounts = List.of(account(2L));
        List<AccountResponse> responses = List.of(response(2L));
        when(accountRepository.findAll()).thenReturn(accounts);
        when(accountMapper.toResponseList(accounts)).thenReturn(responses);

        List<AccountResponse> result = accountService.list(null);

        assertThat(result).isEqualTo(responses);
    }

    @Test
    void list_roleGiven_returnsAccountsWithRole() {
        List<Account> accounts = List.of(account(2L));
        List<AccountResponse> responses = List.of(response(2L));
        when(accountRepository.findAllByRole(Role.MANAGER)).thenReturn(accounts);
        when(accountMapper.toResponseList(accounts)).thenReturn(responses);

        List<AccountResponse> result = accountService.list(Role.MANAGER);

        assertThat(result).isEqualTo(responses);
    }

    @Test
    void changeRole_self_throwsBusinessException() {
        AuthPrincipal admin = admin(1L);

        assertThatThrownBy(() -> accountService.changeRole(admin, 1L, new ChangeRoleRequest(Role.USER)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_SELF_MODIFICATION);
        verify(accountRepository, never()).findById(anyLong());
    }

    @Test
    void changeRole_accountNotFound_throwsNotFound() {
        when(accountRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.changeRole(admin(1L), 2L, new ChangeRoleRequest(Role.USER)))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
        verify(blacklistService, never()).revokeAll(any(), any());
    }

    @Test
    void changeRole_valid_changesRoleAndRevokesTokens() {
        Account account = account(2L);
        AccountResponse response = response(2L);
        when(accountRepository.findById(2L)).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(account)).thenReturn(response);

        AccountResponse result = accountService.changeRole(admin(1L), 2L, new ChangeRoleRequest(Role.MANAGER));

        assertThat(result).isEqualTo(response);
        assertThat(account.getRole()).isEqualTo(Role.MANAGER);
        verify(blacklistService).revokeAll(2L, RevokeReason.ADMIN_REVOKE);
    }

    @Test
    void disable_self_throwsBusinessException() {
        AuthPrincipal admin = admin(1L);

        assertThatThrownBy(() -> accountService.disable(admin, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_SELF_MODIFICATION);
    }

    @Test
    void disable_valid_disablesAccountAndRevokesTokens() {
        Account account = account(2L);
        AccountResponse response = response(2L);
        when(accountRepository.findById(2L)).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(account)).thenReturn(response);

        AccountResponse result = accountService.disable(admin(1L), 2L);

        assertThat(result).isEqualTo(response);
        assertThat(account.isEnabled()).isFalse();
        verify(blacklistService).revokeAll(2L, RevokeReason.ACCOUNT_DISABLED);
    }

    @Test
    void enable_valid_enablesAccount() {
        Account account = account(2L);
        account.setEnabled(false);
        AccountResponse response = response(2L);
        when(accountRepository.findById(2L)).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(account)).thenReturn(response);

        AccountResponse result = accountService.enable(2L);

        assertThat(result).isEqualTo(response);
        assertThat(account.isEnabled()).isTrue();
    }

    @Test
    void revokeTokens_valid_revokesAllTokens() {
        when(accountRepository.findById(2L)).thenReturn(Optional.of(account(2L)));

        accountService.revokeTokens(2L);

        verify(blacklistService).revokeAll(2L, RevokeReason.ADMIN_REVOKE);
    }

    private Account account(Long id) {
        Account account = new Account();
        account.setId(id);
        account.setUsername("user" + id);
        account.setRole(Role.USER);
        return account;
    }

    private AccountResponse response(Long id) {
        return new AccountResponse(id, "user" + id, "user" + id + "@mail.az", Role.USER, true, null);
    }

    private AuthPrincipal admin(Long id) {
        return new AuthPrincipal(id, "admin", Role.ADMIN, "jti-1", "session-1");
    }
}
