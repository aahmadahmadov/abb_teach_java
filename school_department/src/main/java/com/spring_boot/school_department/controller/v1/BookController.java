package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.request.CreateBookRequest;
import com.spring_boot.school_department.dto.request.UpdateBookRequest;
import com.spring_boot.school_department.dto.response.BookResponse;
import com.spring_boot.school_department.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@RequestBody CreateBookRequest request) {
        return bookService.create(request);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BookResponse getById(@PathVariable Long id) {
        return bookService.getById(id);
    }

    @GetMapping("/")
    @ResponseStatus(HttpStatus.OK)
    public List<BookResponse> getAll(@RequestParam(required = false) String title) {
        return bookService.getAll(title);
    }

    @GetMapping("/cheaper-than")
    @ResponseStatus(HttpStatus.OK)
    public List<BookResponse> getCheaperThan(@RequestParam BigDecimal price) {
        return bookService.getCheaperThan(price);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BookResponse update(@PathVariable Long id, @RequestBody UpdateBookRequest request) {
        return bookService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bookService.delete(id);
    }

    @PostMapping("/{bookId}/authors/{authorId}")
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse addAuthor(@PathVariable Long bookId, @PathVariable Long authorId) {
        return bookService.addAuthor(bookId, authorId);
    }

    @DeleteMapping("/{bookId}/authors/{authorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAuthor(@PathVariable Long bookId, @PathVariable Long authorId) {
        bookService.removeAuthor(bookId, authorId);
    }
}
