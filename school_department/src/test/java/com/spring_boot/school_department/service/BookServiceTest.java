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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private AuthorBookRepository authorBookRepository;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookService bookService;

    @Test
    void create_titleIsFree_savesAndReturnsResponse() {
        CreateBookRequest request = new CreateBookRequest("Java", new BigDecimal("10.00"), 200);
        Book book = book();
        BookResponse response = response();
        when(bookRepository.existsByTitle("Java")).thenReturn(false);
        when(bookMapper.toEntity(request)).thenReturn(book);
        when(bookMapper.toResponse(book)).thenReturn(response);

        BookResponse result = bookService.create(request);

        assertThat(result).isEqualTo(response);
        verify(bookRepository).save(book);
    }

    @Test
    void create_titleExists_throwsAlreadyExists() {
        CreateBookRequest request = new CreateBookRequest("Java", new BigDecimal("10.00"), 200);
        when(bookRepository.existsByTitle("Java")).thenReturn(true);

        assertThatThrownBy(() -> bookService.create(request))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_ALREADY_EXISTS);

        verify(bookRepository, never()).save(any());
    }

    @Test
    void getById_bookExists_returnsDetailResponse() {
        Book book = book();
        BookResponse response = response();
        when(bookRepository.findByIdWithAuthors(1L)).thenReturn(Optional.of(book));
        when(bookMapper.toDetailResponse(book)).thenReturn(response);

        BookResponse result = bookService.getById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getById_bookNotFound_throwsNotFound() {
        when(bookRepository.findByIdWithAuthors(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getById(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    void getAll_titleIsNull_returnsAllBooks() {
        List<Book> books = List.of(book());
        List<BookResponse> responses = List.of(response());
        when(bookRepository.findAllOrderByTitle()).thenReturn(books);
        when(bookMapper.toResponseList(books)).thenReturn(responses);

        List<BookResponse> result = bookService.getAll(null);

        assertThat(result).isEqualTo(responses);
        verify(bookRepository, never()).findAllByTitleContainingIgnoreCase(any());
    }

    @Test
    void getAll_titleGiven_returnsMatchingBooks() {
        List<Book> books = List.of(book());
        List<BookResponse> responses = List.of(response());
        when(bookRepository.findAllByTitleContainingIgnoreCase("jav")).thenReturn(books);
        when(bookMapper.toResponseList(books)).thenReturn(responses);

        List<BookResponse> result = bookService.getAll("jav");

        assertThat(result).isEqualTo(responses);
        verify(bookRepository, never()).findAllOrderByTitle();
    }

    @Test
    void getCheaperThan_priceGiven_returnsResponseList() {
        BigDecimal price = new BigDecimal("15.00");
        List<Book> books = List.of(book());
        List<BookResponse> responses = List.of(response());
        when(bookRepository.findAllByPriceLessThanEqualOrderByPriceAsc(price)).thenReturn(books);
        when(bookMapper.toResponseList(books)).thenReturn(responses);

        List<BookResponse> result = bookService.getCheaperThan(price);

        assertThat(result).isEqualTo(responses);
    }

    @Test
    void update_titleIsFree_updatesAndReturnsResponse() {
        UpdateBookRequest request = new UpdateBookRequest("Spring", new BigDecimal("12.00"), 300);
        Book book = book();
        BookResponse response = response();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.existsByTitleAndIdNot("Spring", 1L)).thenReturn(false);
        when(bookMapper.toResponse(book)).thenReturn(response);

        BookResponse result = bookService.update(1L, request);

        assertThat(result).isEqualTo(response);
        verify(bookMapper).updateEntity(request, book);
    }

    @Test
    void update_titleUsed_throwsAlreadyExists() {
        UpdateBookRequest request = new UpdateBookRequest("Spring", new BigDecimal("12.00"), 300);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book()));
        when(bookRepository.existsByTitleAndIdNot("Spring", 1L)).thenReturn(true);

        assertThatThrownBy(() -> bookService.update(1L, request))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_ALREADY_EXISTS);

        verify(bookMapper, never()).updateEntity(any(), any());
    }

    @Test
    void delete_bookExists_deletesBook() {
        Book book = book();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        bookService.delete(1L);

        verify(bookRepository).delete(book);
    }

    @Test
    void delete_bookNotFound_throwsNotFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.delete(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_NOT_FOUND);

        verify(bookRepository, never()).delete(any());
    }

    @Test
    void addAuthor_notLinked_addsAuthorAndReturnsDetailResponse() {
        Book book = book();
        Author author = author();
        BookResponse response = response();
        when(bookRepository.findByIdWithAuthors(1L)).thenReturn(Optional.of(book));
        when(authorRepository.findById(2L)).thenReturn(Optional.of(author));
        when(authorBookRepository.existsByAuthorIdAndBookId(2L, 1L)).thenReturn(false);
        when(bookMapper.toDetailResponse(book)).thenReturn(response);

        BookResponse result = bookService.addAuthor(1L, 2L);

        assertThat(result).isEqualTo(response);
        assertThat(book.getAuthorBooks()).hasSize(1);
        assertThat(author.getAuthorBooks()).hasSize(1);
    }

    @Test
    void addAuthor_alreadyLinked_throwsAlreadyExists() {
        Book book = book();
        when(bookRepository.findByIdWithAuthors(1L)).thenReturn(Optional.of(book));
        when(authorRepository.findById(2L)).thenReturn(Optional.of(author()));
        when(authorBookRepository.existsByAuthorIdAndBookId(2L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> bookService.addAuthor(1L, 2L))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_AUTHOR_ALREADY_LINKED);

        assertThat(book.getAuthorBooks()).isEmpty();
    }

    @Test
    void addAuthor_authorNotFound_throwsNotFound() {
        when(bookRepository.findByIdWithAuthors(1L)).thenReturn(Optional.of(book()));
        when(authorRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.addAuthor(1L, 2L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTHOR_NOT_FOUND);
    }

    @Test
    void removeAuthor_linked_deletesLink() {
        when(authorBookRepository.deleteByAuthorIdAndBookId(2L, 1L)).thenReturn(1);

        assertThatCode(() -> bookService.removeAuthor(1L, 2L)).doesNotThrowAnyException();

        verify(authorBookRepository).deleteByAuthorIdAndBookId(2L, 1L);
    }

    @Test
    void removeAuthor_notLinked_throwsNotFound() {
        when(authorBookRepository.deleteByAuthorIdAndBookId(2L, 1L)).thenReturn(0);

        assertThatThrownBy(() -> bookService.removeAuthor(1L, 2L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_AUTHOR_NOT_LINKED);
    }

    private Book book() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Java");
        book.setPrice(new BigDecimal("10.00"));
        book.setPageCount(200);
        return book;
    }

    private Author author() {
        Author author = new Author();
        author.setId(2L);
        author.setName("Cingiz");
        author.setSurname("Abdullayev");
        return author;
    }

    private BookResponse response() {
        return new BookResponse(1L, "Java", new BigDecimal("10.00"), 200, null, null, null);
    }
}
