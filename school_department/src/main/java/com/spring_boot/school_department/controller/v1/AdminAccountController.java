package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.request.ChangeRoleRequest;
import com.spring_boot.school_department.dto.response.AccountResponse;
import com.spring_boot.school_department.enums.Role;
import com.spring_boot.school_department.security.AuthPrincipal;
import com.spring_boot.school_department.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Admin Account", description = "Hesablarin idare olunmasi (yalniz ADMIN)")
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AccountService accountService;

    @Operation(summary = "Hesablarin siyahisi", description = "role verilse hemin rola gore filter edir")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<AccountResponse> list(@RequestParam(required = false) Role role) {
        return accountService.list(role);
    }

    @Operation(summary = "Rolu deyis", description = "Hesabin tokenleri legv olunur. Tapilmasa 404 (ERR-2710), oz rolunu deyismek olmaz 409 (ERR-2715), validasiya xetasinda 400 (ERR-1000)")
    @PatchMapping("/{id}/role")
    @ResponseStatus(HttpStatus.OK)
    public AccountResponse changeRole(@AuthenticationPrincipal AuthPrincipal principal,
                                      @PathVariable Long id,
                                      @Valid @RequestBody ChangeRoleRequest request) {
        return accountService.changeRole(principal, id, request);
    }

    @Operation(summary = "Hesabi sondur", description = "Butun tokenler legv olunur. Tapilmasa 404 (ERR-2710), ozunu sondurmek olmaz 409 (ERR-2715)")
    @PatchMapping("/{id}/disable")
    @ResponseStatus(HttpStatus.OK)
    public AccountResponse disable(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        return accountService.disable(principal, id);
    }

    @Operation(summary = "Hesabi aktiv et", description = "Tapilmasa 404 (ERR-2710)")
    @PatchMapping("/{id}/enable")
    @ResponseStatus(HttpStatus.OK)
    public AccountResponse enable(@PathVariable Long id) {
        return accountService.enable(id);
    }

    @Operation(summary = "Tokenleri legv et", description = "Hesabin butun tokenleri legv olunur. Tapilmasa 404 (ERR-2710)")
    @PostMapping("/{id}/revoke-tokens")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeTokens(@PathVariable Long id) {
        accountService.revokeTokens(id);
    }
}
