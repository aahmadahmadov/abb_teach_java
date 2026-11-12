package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.request.CreatePassportRequest;
import com.spring_boot.school_department.dto.request.CreateUserRequest;
import com.spring_boot.school_department.dto.request.UpdateUserRequest;
import com.spring_boot.school_department.dto.response.PassportResponse;
import com.spring_boot.school_department.dto.response.UserResponse;
import com.spring_boot.school_department.enums.GenderEnum;
import com.spring_boot.school_department.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "User", description = "Istifadeci ve pasport emeliyyatlari")
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Istifadeci yarat", description = "Email artiq varsa 409 (ERR-2601), validasiya xetasinda 400 (ERR-1000)")
    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @Operation(summary = "Istifadecini id ile getir", description = "Tapilmasa 404 (ERR-2600)")
    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @Operation(summary = "Istifadecini email ile getir", description = "Tapilmasa 404 (ERR-2600)")
    @GetMapping("/by-email")
    public UserResponse getByEmail(@RequestParam String email) {
        return userService.getByEmail(email);
    }

    @Operation(summary = "Istifadeciler", description = "gender verilse hemin cinsden olanlari qaytarir, yanlis deyerde 400 (ERR-1003)")
    @GetMapping("/")
    public List<UserResponse> getByGender(@RequestParam(required = false) GenderEnum gender) {
        return userService.findAllByGender(gender);
    }

    @Operation(summary = "Pasportu olmayan istifadeciler", description = "Pasportu olmayan istifadeciler")
    @GetMapping("/without-passport")
    public List<UserResponse> getWithoutPassport() {
        return userService.getWithoutPassport();
    }

    @Operation(summary = "Istifadecini yenile", description = "Tapilmasa 404 (ERR-2600), email basqasindadirsa 409 (ERR-2602)")
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @Operation(summary = "Istifadecini sil", description = "Pasportu ile birlikde silinir. Tapilmasa 404 (ERR-2600)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    @Operation(summary = "Istifadeciye pasport elave et", description = "Istifadeci tapilmasa 404 (ERR-2600), pasportu varsa 409 (ERR-2603), seriya nomresi varsa 409 (ERR-2605)")
    @PostMapping("/{id}/passport")
    @ResponseStatus(HttpStatus.CREATED)
    public PassportResponse addPassport(@PathVariable Long id, @Valid @RequestBody CreatePassportRequest request) {
        return userService.addPassport(id, request);
    }

    @Operation(summary = "Istifadecinin pasportu", description = "Tapilmasa 404 (ERR-2604)")
    @GetMapping("/{id}/passport")
    public PassportResponse getPassport(@PathVariable Long id) {
        return userService.getPassportByUserId(id);
    }

    @Operation(summary = "Istifadecinin pasportunu sil", description = "Istifadeci tapilmasa 404 (ERR-2600), pasport yoxdursa 404 (ERR-2604)")
    @DeleteMapping("/{id}/passport")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePassport(@PathVariable Long id) {
        userService.deletePassportByUserId(id);
    }
}