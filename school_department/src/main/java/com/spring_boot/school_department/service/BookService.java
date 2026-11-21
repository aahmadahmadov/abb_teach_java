package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.request.CreateBookRequest;
import com.spring_boot.school_department.dto.request.UpdateBookRequest;
import com.spring_boot.school_department.dto.response.BookResponse;
import com.spring_boot.school_department.entity.Author;
import com.spring_boot.school_department.entity.Book;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.exception.AlreadyExistsException;
import com.spring_boot.school_department.exception.NotFoundException;
import com.spring_boot.school_department.mapper.BookMapper;
import com.spring_boot.school_department.repository.AuthorBookRepository;
import com.spring_boot.school_department.repository.AuthorRepository;
import com.spring_boot.school_department.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    @Transactional(propagation = Propagation.REQUIRED)
    public BookResponse create(CreateBookRequest request) {

        if (bookRepository.existsByTitle(request.title())) {
            throw new AlreadyExistsException(ErrorCode.BOOK_ALREADY_EXISTS, request.title());
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

    @Transactional(propagation = Propagation.REQUIRED)
    public BookResponse update(Long id, UpdateBookRequest request) {
        Book book = findBook(id);

        if (bookRepository.existsByTitleAndIdNot(request.title(), id)) {
            throw new AlreadyExistsException(ErrorCode.BOOK_ALREADY_EXISTS, request.title());
        }

        bookMapper.updateEntity(request, book);

        return bookMapper.toResponse(book);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(Long id) {
        bookRepository.delete(findBook(id));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public BookResponse addAuthor(Long bookId, Long authorId) {
        Book book = findBookWithAuthors(bookId);
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.AUTHOR_NOT_FOUND, authorId));

        if (authorBookRepository.existsByAuthorIdAndBookId(authorId, bookId)) {
            throw new AlreadyExistsException(ErrorCode.BOOK_AUTHOR_ALREADY_LINKED, authorId, bookId);
        }

        book.addAuthor(author);

        return bookMapper.toDetailResponse(book);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void removeAuthor(Long bookId, Long authorId) {

        if (authorBookRepository.deleteByAuthorIdAndBookId(authorId, bookId) == 0) {
            throw new NotFoundException(ErrorCode.BOOK_AUTHOR_NOT_LINKED, authorId, bookId);
        }
    }

    private Book findBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOOK_NOT_FOUND, id));
    }

    private Book findBookWithAuthors(Long id) {
        return bookRepository.findByIdWithAuthors(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOOK_NOT_FOUND, id));
    }
}
