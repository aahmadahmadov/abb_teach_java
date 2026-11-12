package com.spring_boot.school_department.controller.v1;

import com.spring_boot.school_department.dto.request.CreateAuthorRequest;
import com.spring_boot.school_department.dto.request.UpdateAuthorRequest;
import com.spring_boot.school_department.dto.response.AuthorResponse;
import com.spring_boot.school_department.dto.response.BookResponse;
import com.spring_boot.school_department.service.AuthorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Author", description = "Muellif emeliyyatlari")
@RequestMapping("/api/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

    @Operation(summary = "Muellif yarat", description = "Eyni muellif varsa 409 (ERR-2401)")
    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorResponse create(@Valid @RequestBody CreateAuthorRequest request) {
        return authorService.create(request);
    }

    @Operation(summary = "Muellifi id ile getir", description = "Tapilmasa 404 (ERR-2400)")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AuthorResponse getById(@PathVariable Long id) {
        return authorService.getById(id);
    }

    @Operation(summary = "Butun muellifler", description = "country verilse hemin olkenin muelliflerini qaytarir (query cache)")
    @GetMapping("/")
    @ResponseStatus(HttpStatus.OK)
    public List<AuthorResponse> getAll(@RequestParam(required = false) String country) {
        return authorService.getAll(country);
    }

    @Operation(summary = "Kitabi olmayan muellifler", description = "Hec bir kitabi olmayan muellifler")
    @GetMapping("/without-books")
    @ResponseStatus(HttpStatus.OK)
    public List<AuthorResponse> getWithoutBooks() {
        return authorService.getWithoutBooks();
    }

    @Operation(summary = "Muellifin kitablari", description = "Muellif tapilmasa 404 (ERR-2400)")
    @GetMapping("/{id}/books")
    @ResponseStatus(HttpStatus.OK)
    public List<BookResponse> getAuthorBooks(@PathVariable Long id) {
        return authorService.getAuthorBooks(id);
    }

    @Operation(summary = "Muellifi yenile", description = "Tapilmasa 404 (ERR-2400), eyni muellif varsa 409 (ERR-2401)")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public AuthorResponse update(@PathVariable Long id, @Valid @RequestBody UpdateAuthorRequest request) {
        return authorService.update(id, request);
    }

    @Operation(summary = "Muellifi sil", description = "Tapilmasa 404 (ERR-2400)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        authorService.delete(id);
    }
}
