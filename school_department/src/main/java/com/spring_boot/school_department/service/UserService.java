package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.request.CreatePassportRequest;
import com.spring_boot.school_department.dto.request.CreateUserRequest;
import com.spring_boot.school_department.dto.request.UpdateUserRequest;
import com.spring_boot.school_department.dto.response.PassportResponse;
import com.spring_boot.school_department.dto.response.UserResponse;
import com.spring_boot.school_department.entity.Passport;
import com.spring_boot.school_department.entity.User;
import com.spring_boot.school_department.enums.GenderEnum;
import com.spring_boot.school_department.mapper.PassportMapper;
import com.spring_boot.school_department.mapper.UserMapper;
import com.spring_boot.school_department.repository.PassportRepository;
import com.spring_boot.school_department.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final PassportRepository passportRepository;
    private final UserRepository userRepository;

    private final PassportMapper passportMapper;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email already exists: " + request.email());
        }

        User user = userMapper.toEntity(request);
        userRepository.save(user);

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findUser(id);

        if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new IllegalStateException("Email already exists: " + request.email());
        }

        userMapper.updateEntity(request, user);
        return userMapper.toResponse(user);
    }

    public UserResponse getById(Long id) {
        return userMapper.toResponse(findUser(id));
    }

    public UserResponse getByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found with email: " + email));

        return userMapper.toResponse(user);
    }

    public List<UserResponse> getWithoutPassport() {
        List<User> usersWithoutPassport = userRepository.findAllByPassportIsNull();

        return userMapper.toResponseList(usersWithoutPassport);
    }

    public List<UserResponse> findAllByGender(GenderEnum gender) {
        return userMapper.toResponseList(userRepository.findAllByGender(gender));
    }

    public void delete(Long id) {
        // cascade all oldugu ucun ona gore passportda gedir
        userRepository.delete(findUser(id));
    }

    @Transactional
    public PassportResponse addPassport(Long id, CreatePassportRequest request) {
        User user = findUser(id);

        if (user.getPassport() != null) {
            throw new IllegalStateException("User already has a passport: " + id);
        }

        if (passportRepository.existsBySerialNumber(request.serialNumber())) {
            throw new IllegalStateException("Passport serial number already exists: " + request.serialNumber());
        }

        Passport passport = passportMapper.toEntity(request);
        user.setPassport(passport);

        return passportMapper.toResponse(passportRepository.save(passport));
    }

    public PassportResponse getPassportByUserId(Long userId) {
        Passport passport = passportRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Passport not found for user: " + userId));

        return passportMapper.toResponse(passport);
    }

    @Transactional
    public void deletePassportByUserId(Long userId) {
        User user = findUser(userId);

        if (user.getPassport() == null) {
            throw new EntityNotFoundException("Passport not found for user: " + userId);
        }

        user.setPassport(null);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }
}
