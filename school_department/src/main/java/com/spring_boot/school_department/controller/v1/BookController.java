package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.request.CreateBookRequest;
import com.spring_boot.school_department.dto.request.UpdateBookRequest;
import com.spring_boot.school_department.dto.response.BookResponse;
import com.spring_boot.school_department.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@Tag(name = "Book", description = "Kitab emeliyyatlari")
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @Operation(summary = "Kitab yarat", description = "Eyni adda kitab varsa 409 (ERR-2501)")
    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@Valid @RequestBody CreateBookRequest request) {
        return bookService.create(request);
    }

    @Operation(summary = "Kitabi id ile getir", description = "Muellifleri ile birlikde qaytarir. Tapilmasa 404 (ERR-2500)")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BookResponse getById(@PathVariable Long id) {
        return bookService.getById(id);
    }

    @Operation(summary = "Butun kitablar", description = "title verilse adinda hemin soz olan kitablari qaytarir")
    @GetMapping("/")
    @ResponseStatus(HttpStatus.OK)
    public List<BookResponse> getAll(@RequestParam(required = false) String title) {
        return bookService.getAll(title);
    }

    @Operation(summary = "Qiymeti verilenden ucuz kitablar", description = "price gonderilmese 400 (ERR-1004), yanlis tipde olsa 400 (ERR-1003)")
    @GetMapping("/cheaper-than")
    @ResponseStatus(HttpStatus.OK)
    public List<BookResponse> getCheaperThan(@RequestParam BigDecimal price) {
        return bookService.getCheaperThan(price);
    }

    @Operation(summary = "Kitabi yenile", description = "Tapilmasa 404 (ERR-2500), eyni adda kitab varsa 409 (ERR-2501)")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BookResponse update(@PathVariable Long id, @Valid @RequestBody UpdateBookRequest request) {
        return bookService.update(id, request);
    }

    @Operation(summary = "Kitabi sil", description = "Tapilmasa 404 (ERR-2500)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bookService.delete(id);
    }

    @Operation(summary = "Kitaba muellif bagla", description = "Kitab tapilmasa 404 (ERR-2500), muellif tapilmasa 404 (ERR-2400), artiq baglidirsa 409 (ERR-2502)")
    @PostMapping("/{bookId}/authors/{authorId}")
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse addAuthor(@PathVariable Long bookId, @PathVariable Long authorId) {
        return bookService.addAuthor(bookId, authorId);
    }

    @Operation(summary = "Kitabdan muellifi ayir", description = "Bagli deyilse 404 (ERR-2503)")
    @DeleteMapping("/{bookId}/authors/{authorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAuthor(@PathVariable Long bookId, @PathVariable Long authorId) {
        bookService.removeAuthor(bookId, authorId);
    }
}
