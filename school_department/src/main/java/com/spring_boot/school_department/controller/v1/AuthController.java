package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.request.RegisterRequest;
import com.spring_boot.school_department.dto.response.AccountResponse;
import com.spring_boot.school_department.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Auth", description = "Qeydiyyat, login, token yenileme ve sessiyalar")
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Qeydiyyat", description = "Hemise USER rolu ile yaradilir. Validasiya xetasinda 400 (ERR-1000), username varsa 409 (ERR-2711), email varsa 409 (ERR-2712)")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }
}
