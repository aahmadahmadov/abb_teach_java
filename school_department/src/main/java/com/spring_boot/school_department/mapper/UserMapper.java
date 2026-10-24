package com.spring_boot.school_department.mapper;

import com.spring_boot.school_department.dto.request.CreateUserRequest;
import com.spring_boot.school_department.dto.request.UpdateUserRequest;
import com.spring_boot.school_department.dto.response.UserResponse;
import com.spring_boot.school_department.entity.User;
import com.spring_boot.school_department.enums.GenderEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = PassportMapper.class)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    User toEntity(CreateUserRequest request);

    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passport", ignore = true)
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);

    default GenderEnum toGender(Integer code) {
        return GenderEnum.findByDbCode(code);
    }
}
