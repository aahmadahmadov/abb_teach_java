package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.request.ChangePasswordRequest;
import com.spring_boot.school_department.dto.request.LoginRequest;
import com.spring_boot.school_department.dto.request.RefreshRequest;
import com.spring_boot.school_department.dto.request.RegisterRequest;
import com.spring_boot.school_department.dto.response.AccountResponse;
import com.spring_boot.school_department.dto.response.AuthResponse;
import com.spring_boot.school_department.dto.response.SessionResponse;
import com.spring_boot.school_department.security.AuthPrincipal;
import com.spring_boot.school_department.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Auth", description = "Qeydiyyat, login, token yenileme ve sessiyalar")
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Qeydiyyat", description = "Hemise USER rolu ile yaradilir. Validasiya xetasinda 400 (ERR-1000), username varsa 409 (ERR-2711), email varsa 409 (ERR-2712)")
    @SecurityRequirements()
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Login", description = "Access ve refresh token qaytarir. Login ve ya sifre sehvdirse 401 (ERR-2701), hesab sondurulubse 403 (ERR-2708)")
    @SecurityRequirements()
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Token yenile", description = "Eyni sessiya ucun yeni cut qaytarir. Muddeti bitibse 401 (ERR-2702), etibarsizdirsa 401 (ERR-2703), legv olunubsa 401 (ERR-2704), access token gonderilibse 401 (ERR-2705), tekrar istifade olunubsa 401 (ERR-2706), hesab sondurulubse 403 (ERR-2708)")
    @SecurityRequirements()
    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @Operation(summary = "Logout", description = "Cari sessiyani legv edir")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal AuthPrincipal principal) {
        authService.logout(principal);
    }

    @Operation(summary = "Butun sessiyalardan cixis", description = "Hesabin butun sessiyalarini legv edir")
    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal AuthPrincipal principal) {
        authService.logoutAll(principal);
    }

    @Operation(summary = "Cari hesab", description = "Daxil olmus hesabin melumatlari")
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public AccountResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        return authService.me(principal);
    }

    @Operation(summary = "Aktiv sessiyalar", description = "Cari sessiya current = true ile qeyd olunur")
    @GetMapping("/sessions")
    @ResponseStatus(HttpStatus.OK)
    public List<SessionResponse> sessions(@AuthenticationPrincipal AuthPrincipal principal) {
        return authService.sessions(principal);
    }

    @Operation(summary = "Sessiyani legv et", description = "Basqa cihazdan cixis. Sessiya tapilmasa ve ya basqasinindirsa 404 (ERR-2716)")
    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSession(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable String sessionId) {
        authService.revokeSession(principal, sessionId);
    }

    @Operation(summary = "Sifreni deyis", description = "Butun tokenler legv olunur. Kohne sifre sehvdirse 400 (ERR-2713), yeni sifre kohne ile eynidirse 400 (ERR-2714), validasiya xetasinda 400 (ERR-1000)")
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthPrincipal principal,
                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal, request);
    }
}
