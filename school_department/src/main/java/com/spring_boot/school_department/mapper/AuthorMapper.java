package com.spring_boot.school_department.mapper;

import com.spring_boot.school_department.dto.request.CreateAuthorRequest;
import com.spring_boot.school_department.dto.request.UpdateAuthorRequest;
import com.spring_boot.school_department.dto.response.AuthorResponse;
import com.spring_boot.school_department.entity.Author;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuthorMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorBooks", ignore = true)
    Author toEntity(CreateAuthorRequest request);

    AuthorResponse toResponse(Author author);

    List<AuthorResponse> toResponseList(List<Author> authors);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorBooks", ignore = true)
    void updateEntity(UpdateAuthorRequest request, @MappingTarget Author author);
}
