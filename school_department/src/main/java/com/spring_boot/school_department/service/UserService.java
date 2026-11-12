package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.request.CreatePassportRequest;
import com.spring_boot.school_department.dto.request.CreateUserRequest;
import com.spring_boot.school_department.dto.request.UpdateUserRequest;
import com.spring_boot.school_department.dto.response.PassportResponse;
import com.spring_boot.school_department.dto.response.UserResponse;
import com.spring_boot.school_department.entity.Passport;
import com.spring_boot.school_department.entity.User;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.enums.GenderEnum;
import com.spring_boot.school_department.exception.AlreadyExistsException;
import com.spring_boot.school_department.exception.NotFoundException;
import com.spring_boot.school_department.mapper.PassportMapper;
import com.spring_boot.school_department.mapper.UserMapper;
import com.spring_boot.school_department.repository.PassportRepository;
import com.spring_boot.school_department.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    @Transactional(propagation = Propagation.REQUIRED)
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AlreadyExistsException(ErrorCode.USER_EMAIL_EXISTS, request.email());
        }

        User user = userMapper.toEntity(request);
        userRepository.save(user);

        return userMapper.toResponse(user);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findUser(id);

        if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new AlreadyExistsException(ErrorCode.USER_EMAIL_USED, request.email());
        }

        userMapper.updateEntity(request, user);
        return userMapper.toResponse(user);
    }

    public UserResponse getById(Long id) {
        User user = userRepository.findByIdWithPassport(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, id));

        return userMapper.toResponse(user);
    }

    public UserResponse getByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, email));

        return userMapper.toResponse(user);
    }

    public List<UserResponse> getWithoutPassport() {
        List<User> usersWithoutPassport = userRepository.findAllByPassportIsNull();

        return userMapper.toResponseList(usersWithoutPassport);
    }

    public List<UserResponse> findAllByGender(GenderEnum gender) {
        if (gender == null) {
            return userMapper.toResponseList(userRepository.findAllWithPassport());
        }

        return userMapper.toResponseList(userRepository.findAllByGender(gender));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(Long id) {
        // cascade all oldugu ucun ona gore passportda gedir
        userRepository.delete(findUser(id));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public PassportResponse addPassport(Long id, CreatePassportRequest request) {
        User user = findUser(id);

        if (passportRepository.existsByUserId(id)) {
            throw new AlreadyExistsException(ErrorCode.USER_PASSPORT_EXISTS, id);
        }

        if (passportRepository.existsBySerialNumber(request.serialNumber())) {
            throw new AlreadyExistsException(ErrorCode.PASSPORT_SERIAL_EXISTS, request.serialNumber());
        }

        Passport passport = passportMapper.toEntity(request);
        user.setPassport(passport);

        return passportMapper.toResponse(passportRepository.save(passport));
    }

    public PassportResponse getPassportByUserId(Long userId) {
        Passport passport = passportRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PASSPORT_NOT_FOUND, userId));

        return passportMapper.toResponse(passport);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deletePassportByUserId(Long userId) {
        User user = findUser(userId);

        if (user.getPassport() == null) {
            throw new NotFoundException(ErrorCode.PASSPORT_NOT_FOUND, userId);
        }

        user.setPassport(null);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, id));
    }
}
