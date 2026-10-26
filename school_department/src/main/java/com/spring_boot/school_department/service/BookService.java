package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.request.CreateBookRequest;
import com.spring_boot.school_department.dto.request.UpdateBookRequest;
import com.spring_boot.school_department.dto.response.BookResponse;
import com.spring_boot.school_department.entity.Author;
import com.spring_boot.school_department.entity.Book;
import com.spring_boot.school_department.mapper.BookMapper;
import com.spring_boot.school_department.repository.AuthorBookRepository;
import com.spring_boot.school_department.repository.AuthorRepository;
import com.spring_boot.school_department.repository.BookRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final AuthorBookRepository authorBookRepository;

    private final BookMapper bookMapper;

    @Transactional
    public BookResponse create(CreateBookRequest request) {

        if (bookRepository.existsByTitle(request.title())) {
            throw new IllegalStateException("Book with title already exists: " + request.title());
        }

        Book book = bookMapper.toEntity(request);
        bookRepository.save(book);

        return bookMapper.toResponse(book);
    }

    public BookResponse getById(Long id) {
        return bookMapper.toDetailResponse(findBookWithAuthors(id));
    }

    public List<BookResponse> getAll(String title) {

        if (title == null) {
            return bookMapper.toResponseList(bookRepository.findAllOrderByTitle());
        }

        return bookMapper.toResponseList(bookRepository.findAllByTitleContainingIgnoreCase(title));
    }

    public List<BookResponse> getCheaperThan(BigDecimal price) {
        return bookMapper.toResponseList(bookRepository.findAllByPriceLessThanEqualOrderByPriceAsc(price));
    }

    @Transactional
    public BookResponse update(Long id, UpdateBookRequest request) {
        Book book = findBook(id);

        if (bookRepository.existsByTitleAndIdNot(request.title(), id)) {
            throw new IllegalStateException("Book with title already exists: " + request.title());
        }

        bookMapper.updateEntity(request, book);

        return bookMapper.toResponse(book);
    }

    @Transactional
    public void delete(Long id) {
        bookRepository.delete(findBook(id));
    }

    @Transactional
    public BookResponse addAuthor(Long bookId, Long authorId) {
        Book book = findBookWithAuthors(bookId);
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new EntityNotFoundException("Author not found: " + authorId));

        if (authorBookRepository.existsByAuthorIdAndBookId(authorId, bookId)) {
            throw new IllegalStateException("Author " + authorId + " is already linked to book " + bookId);
        }

        book.addAuthor(author);

        return bookMapper.toDetailResponse(book);
    }

    @Transactional
    public void removeAuthor(Long bookId, Long authorId) {

        if (authorBookRepository.deleteByAuthorIdAndBookId(authorId, bookId) == 0) {
            throw new EntityNotFoundException("Author " + authorId + " is not linked to book " + bookId);
        }
    }

    private Book findBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found: " + id));
    }

    private Book findBookWithAuthors(Long id) {
        return bookRepository.findByIdWithAuthors(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found: " + id));
    }
}
