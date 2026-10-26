package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.request.CreateAuthorRequest;
import com.spring_boot.school_department.dto.request.UpdateAuthorRequest;
import com.spring_boot.school_department.dto.response.AuthorResponse;
import com.spring_boot.school_department.dto.response.BookResponse;
import com.spring_boot.school_department.service.AuthorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorResponse create(@RequestBody CreateAuthorRequest request) {
        return authorService.create(request);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AuthorResponse getById(@PathVariable Long id) {
        return authorService.getById(id);
    }

    @GetMapping("/")
    @ResponseStatus(HttpStatus.OK)
    public List<AuthorResponse> getAll(@RequestParam(required = false) String country) {
        return authorService.getAll(country);
    }

    @GetMapping("/without-books")
    @ResponseStatus(HttpStatus.OK)
    public List<AuthorResponse> getWithoutBooks() {
        return authorService.getWithoutBooks();
    }

    @GetMapping("/{id}/books")
    @ResponseStatus(HttpStatus.OK)
    public List<BookResponse> getAuthorBooks(@PathVariable Long id) {
        return authorService.getAuthorBooks(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AuthorResponse update(@PathVariable Long id, @RequestBody UpdateAuthorRequest request) {
        return authorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        authorService.delete(id);
    }
}
