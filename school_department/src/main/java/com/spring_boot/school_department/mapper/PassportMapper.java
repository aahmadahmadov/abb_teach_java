package com.spring_boot.school_department.mapper;

import com.spring_boot.school_department.dto.request.CreatePassportRequest;
import com.spring_boot.school_department.dto.response.PassportResponse;
import com.spring_boot.school_department.entity.Passport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PassportMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    Passport toEntity(CreatePassportRequest request);

    PassportResponse toResponse(Passport passport);

    List<PassportResponse> toResponseList(List<Passport> passports);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntity(CreatePassportRequest request, @MappingTarget Passport passport);
}
