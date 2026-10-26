package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.request.CreateAuthorRequest;
import com.spring_boot.school_department.dto.request.UpdateAuthorRequest;
import com.spring_boot.school_department.dto.response.AuthorResponse;
import com.spring_boot.school_department.dto.response.BookResponse;
import com.spring_boot.school_department.entity.Author;
import com.spring_boot.school_department.mapper.AuthorMapper;
import com.spring_boot.school_department.mapper.BookMapper;
import com.spring_boot.school_department.repository.AuthorRepository;
import com.spring_boot.school_department.repository.BookRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    private final AuthorMapper authorMapper;
    private final BookMapper bookMapper;

    @Transactional
    public AuthorResponse create(CreateAuthorRequest request) {

        if (authorRepository.existsByNameAndSurname(request.name(), request.surname())) {
            throw new IllegalStateException("Author already exists: " + request.name() + " " + request.surname());
        }

        Author author = authorMapper.toEntity(request);
        authorRepository.save(author);

        return authorMapper.toResponse(author);
    }

    public AuthorResponse getById(Long id) {
        return authorMapper.toResponse(findAuthor(id));
    }

    public List<AuthorResponse> getAll(String country) {

        if (country == null) {
            return authorMapper.toResponseList(authorRepository.findAllOrderBySurname());
        }

        return authorMapper.toResponseList(authorRepository.findAllByCountry(country));
    }

    public List<AuthorResponse> getWithoutBooks() {
        return authorMapper.toResponseList(authorRepository.findAllWithoutBooks());
    }

    public List<BookResponse> getAuthorBooks(Long id) {
        findAuthor(id);

        return bookMapper.toResponseList(bookRepository.findAllByAuthorBooksAuthorId(id));
    }

    @Transactional
    public AuthorResponse update(Long id, UpdateAuthorRequest request) {
        Author author = findAuthor(id);

        if (authorRepository.existsByNameAndSurnameAndIdNot(request.name(), request.surname(), id)) {
            throw new IllegalStateException("Author already exists: " + request.name() + " " + request.surname());
        }

        authorMapper.updateEntity(request, author);

        return authorMapper.toResponse(author);
    }

    @Transactional
    public void delete(Long id) {
        authorRepository.delete(findAuthor(id));
    }

    private Author findAuthor(Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Author not found: " + id));
    }
}
