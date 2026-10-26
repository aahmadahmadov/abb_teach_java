package com.spring_boot.school_department.mapper;

import com.spring_boot.school_department.dto.request.CreateBookRequest;
import com.spring_boot.school_department.dto.request.UpdateBookRequest;
import com.spring_boot.school_department.dto.response.AuthorResponse;
import com.spring_boot.school_department.dto.response.BookResponse;
import com.spring_boot.school_department.entity.AuthorBook;
import com.spring_boot.school_department.entity.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorBooks", ignore = true)
    Book toEntity(CreateBookRequest request);

    @Mapping(target = "authors", ignore = true)
    BookResponse toResponse(Book book);

    @Named("withAuthors")
    @Mapping(target = "authors", source = "authorBooks")
    BookResponse toDetailResponse(Book book);

    List<BookResponse> toResponseList(List<Book> books);

    @Mapping(target = ".", source = "author")
    AuthorResponse toAuthorResponse(AuthorBook authorBook);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorBooks", ignore = true)
    void updateEntity(UpdateBookRequest request, @MappingTarget Book book);
}
