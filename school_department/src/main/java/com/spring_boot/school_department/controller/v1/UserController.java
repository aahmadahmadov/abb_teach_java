package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.request.CreatePassportRequest;
import com.spring_boot.school_department.dto.request.CreateUserRequest;
import com.spring_boot.school_department.dto.request.UpdateUserRequest;
import com.spring_boot.school_department.dto.response.PassportResponse;
import com.spring_boot.school_department.dto.response.UserResponse;
import com.spring_boot.school_department.enums.GenderEnum;
import com.spring_boot.school_department.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @GetMapping("/by-email")
    public UserResponse getByEmail(@RequestParam String email) {
        return userService.getByEmail(email);
    }

    @GetMapping
    public List<UserResponse> getByGender(@RequestParam(required = false) GenderEnum gender) {
        return userService.findAllByGender(gender);
    }

    @GetMapping("/without-passport")
    public List<UserResponse> getWithoutPassport() {
        return userService.getWithoutPassport();
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    @PostMapping("/{id}/passport")
    @ResponseStatus(HttpStatus.CREATED)
    public PassportResponse addPassport(@PathVariable Long id, @RequestBody CreatePassportRequest request) {
        return userService.addPassport(id, request);
    }

    @GetMapping("/{id}/passport")
    public PassportResponse getPassport(@PathVariable Long id) {
        return userService.getPassportByUserId(id);
    }

    @DeleteMapping("/{id}/passport")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePassport(@PathVariable Long id) {
        userService.deletePassportByUserId(id);
    }
}