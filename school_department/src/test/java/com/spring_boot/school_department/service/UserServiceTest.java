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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private PassportRepository passportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PassportMapper passportMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void create_emailNotExists_savesAndReturnsUser() {
        CreateUserRequest request = new CreateUserRequest("Ali", "Aliyev", "ali@mail.az", GenderEnum.MALE);
        User user = createUser(1L);
        UserResponse response = createUserResponse(1L);
        when(userRepository.existsByEmail("ali@mail.az")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.create(request);

        assertThat(result).isEqualTo(response);
        verify(userRepository).save(user);
    }

    @Test
    void create_emailExists_throwsAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest("Ali", "Aliyev", "ali@mail.az", GenderEnum.MALE);
        when(userRepository.existsByEmail("ali@mail.az")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_EMAIL_EXISTS);

        verify(userRepository, never()).save(any());
    }

    @Test
    void update_validRequest_updatesAndReturnsUser() {
        UpdateUserRequest request = new UpdateUserRequest("Veli", "Veliyev", "veli@mail.az", GenderEnum.MALE);
        User user = createUser(1L);
        UserResponse response = createUserResponse(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("veli@mail.az", 1L)).thenReturn(false);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.update(1L, request);

        assertThat(result).isEqualTo(response);
        verify(userMapper).updateEntity(request, user);
    }

    @Test
    void update_userNotFound_throwsNotFound() {
        UpdateUserRequest request = new UpdateUserRequest("Veli", "Veliyev", "veli@mail.az", GenderEnum.MALE);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(1L, request))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void update_emailUsedByOther_throwsAlreadyExists() {
        UpdateUserRequest request = new UpdateUserRequest("Veli", "Veliyev", "veli@mail.az", GenderEnum.MALE);
        User user = createUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("veli@mail.az", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1L, request))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_EMAIL_USED);

        verify(userMapper, never()).updateEntity(any(), any());
    }

    @Test
    void getById_userExists_returnsUser() {
        User user = createUser(1L);
        UserResponse response = createUserResponse(1L);
        when(userRepository.findByIdWithPassport(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.getById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getById_userNotFound_throwsNotFound() {
        when(userRepository.findByIdWithPassport(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void getByEmail_userExists_returnsUser() {
        User user = createUser(1L);
        UserResponse response = createUserResponse(1L);
        when(userRepository.findByEmail("ali@mail.az")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.getByEmail("ali@mail.az");

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getByEmail_userNotFound_throwsNotFound() {
        when(userRepository.findByEmail("ali@mail.az")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByEmail("ali@mail.az"))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void getWithoutPassport_usersExist_returnsList() {
        User user = createUser(1L);
        List<User> users = List.of(user);
        List<UserResponse> responses = List.of(createUserResponse(1L));
        when(userRepository.findAllByPassportIsNull()).thenReturn(users);
        when(userMapper.toResponseList(users)).thenReturn(responses);

        List<UserResponse> result = userService.getWithoutPassport();

        assertThat(result).isEqualTo(responses);
    }

    @Test
    void findAllByGender_genderNull_returnsAllUsers() {
        List<User> users = List.of(createUser(1L), createUser(2L));
        List<UserResponse> responses = List.of(createUserResponse(1L), createUserResponse(2L));
        when(userRepository.findAllWithPassport()).thenReturn(users);
        when(userMapper.toResponseList(users)).thenReturn(responses);

        List<UserResponse> result = userService.findAllByGender(null);

        assertThat(result).isEqualTo(responses);
        verify(userRepository, never()).findAllByGender(any());
    }

    @Test
    void findAllByGender_genderGiven_returnsFilteredUsers() {
        List<User> users = List.of(createUser(1L));
        List<UserResponse> responses = List.of(createUserResponse(1L));
        when(userRepository.findAllByGender(GenderEnum.FEMALE)).thenReturn(users);
        when(userMapper.toResponseList(users)).thenReturn(responses);

        List<UserResponse> result = userService.findAllByGender(GenderEnum.FEMALE);

        assertThat(result).isEqualTo(responses);
        verify(userRepository, never()).findAllWithPassport();
    }

    @Test
    void delete_userExists_deletesUser() {
        User user = createUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void delete_userNotFound_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(userRepository, never()).delete(any());
    }

    @Test
    void addPassport_validRequest_savesAndReturnsPassport() {
        CreatePassportRequest request = createPassportRequest();
        User user = createUser(1L);
        Passport passport = createPassport();
        PassportResponse response = createPassportResponse();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passportRepository.existsByUserId(1L)).thenReturn(false);
        when(passportRepository.existsBySerialNumber("AA1234567")).thenReturn(false);
        when(passportMapper.toEntity(request)).thenReturn(passport);
        when(passportRepository.save(passport)).thenReturn(passport);
        when(passportMapper.toResponse(passport)).thenReturn(response);

        PassportResponse result = userService.addPassport(1L, request);

        assertThat(result).isEqualTo(response);
        assertThat(user.getPassport()).isEqualTo(passport);
    }

    @Test
    void addPassport_userNotFound_throwsNotFound() {
        CreatePassportRequest request = createPassportRequest();
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.addPassport(1L, request))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void addPassport_userHasPassport_throwsAlreadyExists() {
        CreatePassportRequest request = createPassportRequest();
        User user = createUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passportRepository.existsByUserId(1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.addPassport(1L, request))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_PASSPORT_EXISTS);

        verify(passportRepository, never()).save(any());
    }

    @Test
    void addPassport_serialNumberExists_throwsAlreadyExists() {
        CreatePassportRequest request = createPassportRequest();
        User user = createUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passportRepository.existsByUserId(1L)).thenReturn(false);
        when(passportRepository.existsBySerialNumber("AA1234567")).thenReturn(true);

        assertThatThrownBy(() -> userService.addPassport(1L, request))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PASSPORT_SERIAL_EXISTS);

        verify(passportRepository, never()).save(any());
    }

    @Test
    void getPassportByUserId_passportExists_returnsPassport() {
        Passport passport = createPassport();
        PassportResponse response = createPassportResponse();
        when(passportRepository.findByUserId(1L)).thenReturn(Optional.of(passport));
        when(passportMapper.toResponse(passport)).thenReturn(response);

        PassportResponse result = userService.getPassportByUserId(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getPassportByUserId_passportNotFound_throwsNotFound() {
        when(passportRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getPassportByUserId(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PASSPORT_NOT_FOUND);
    }

    @Test
    void deletePassportByUserId_userHasPassport_removesPassport() {
        User user = createUser(1L);
        user.setPassport(createPassport());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deletePassportByUserId(1L);

        assertThat(user.getPassport()).isNull();
    }

    @Test
    void deletePassportByUserId_userHasNoPassport_throwsNotFound() {
        User user = createUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.deletePassportByUserId(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PASSPORT_NOT_FOUND);
    }

    @Test
    void deletePassportByUserId_userNotFound_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deletePassportByUserId(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    private User createUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Ali");
        user.setSurname("Aliyev");
        user.setEmail("ali@mail.az");
        user.setGender(GenderEnum.MALE);
        return user;
    }

    private UserResponse createUserResponse(Long id) {
        return new UserResponse(id, "Ali", "Aliyev", "ali@mail.az", GenderEnum.MALE, null, null, null);
    }

    private CreatePassportRequest createPassportRequest() {
        return new CreatePassportRequest("AA1234567", LocalDate.of(2020, 1, 1), "Azerbaijan");
    }

    private Passport createPassport() {
        return new Passport(1L, "AA1234567", LocalDate.of(2020, 1, 1), "Azerbaijan", null);
    }

    private PassportResponse createPassportResponse() {
        return new PassportResponse(1L, "AA1234567", LocalDate.of(2020, 1, 1), "Azerbaijan", null, null);
    }
}
