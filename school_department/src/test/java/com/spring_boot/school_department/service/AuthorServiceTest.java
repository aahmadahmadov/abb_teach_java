package com.spring_boot.school_department.service;

import com.spring_boot.school_department.dto.request.CreateAuthorRequest;
import com.spring_boot.school_department.dto.request.UpdateAuthorRequest;
import com.spring_boot.school_department.dto.response.AuthorResponse;
import com.spring_boot.school_department.dto.response.BookResponse;
import com.spring_boot.school_department.entity.Author;
import com.spring_boot.school_department.entity.Book;
import com.spring_boot.school_department.enums.ErrorCode;
import com.spring_boot.school_department.exception.AlreadyExistsException;
import com.spring_boot.school_department.exception.NotFoundException;
import com.spring_boot.school_department.mapper.AuthorMapper;
import com.spring_boot.school_department.mapper.BookMapper;
import com.spring_boot.school_department.repository.AuthorRepository;
import com.spring_boot.school_department.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorMapper authorMapper;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private AuthorService authorService;

    @Test
    void create_newAuthor_savesAndReturnsResponse() {
        CreateAuthorRequest request = new CreateAuthorRequest("Nizami", "Ganjavi", "Azerbaijan");
        Author author = newAuthor(1L);
        AuthorResponse response = newResponse(1L);
        when(authorRepository.existsByNameAndSurname("Nizami", "Ganjavi")).thenReturn(false);
        when(authorMapper.toEntity(request)).thenReturn(author);
        when(authorMapper.toResponse(author)).thenReturn(response);

        AuthorResponse result = authorService.create(request);

        assertThat(result).isEqualTo(response);
        verify(authorRepository).save(author);
    }

    @Test
    void create_authorExists_throwsAlreadyExists() {
        CreateAuthorRequest request = new CreateAuthorRequest("Nizami", "Ganjavi", "Azerbaijan");
        when(authorRepository.existsByNameAndSurname("Nizami", "Ganjavi")).thenReturn(true);

        assertThatThrownBy(() -> authorService.create(request))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTHOR_ALREADY_EXISTS);

        verify(authorRepository, never()).save(any());
    }

    @Test
    void getById_authorExists_returnsResponse() {
        Author author = newAuthor(1L);
        AuthorResponse response = newResponse(1L);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorMapper.toResponse(author)).thenReturn(response);

        AuthorResponse result = authorService.getById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getById_authorNotFound_throwsNotFound() {
        when(authorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.getById(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTHOR_NOT_FOUND);
    }

    @Test
    void getAll_countryNull_returnsAllOrderedBySurname() {
        List<Author> authors = List.of(newAuthor(1L));
        List<AuthorResponse> responses = List.of(newResponse(1L));
        when(authorRepository.findAllOrderBySurname()).thenReturn(authors);
        when(authorMapper.toResponseList(authors)).thenReturn(responses);

        List<AuthorResponse> result = authorService.getAll(null);

        assertThat(result).isEqualTo(responses);
        verify(authorRepository, never()).findAllByCountry(any());
    }

    @Test
    void getAll_countryGiven_returnsAuthorsOfCountry() {
        List<Author> authors = List.of(newAuthor(1L));
        List<AuthorResponse> responses = List.of(newResponse(1L));
        when(authorRepository.findAllByCountry("Azerbaijan")).thenReturn(authors);
        when(authorMapper.toResponseList(authors)).thenReturn(responses);

        List<AuthorResponse> result = authorService.getAll("Azerbaijan");

        assertThat(result).isEqualTo(responses);
        verify(authorRepository, never()).findAllOrderBySurname();
    }

    @Test
    void getWithoutBooks_called_returnsAuthorsWithoutBooks() {
        List<Author> authors = List.of(newAuthor(1L));
        List<AuthorResponse> responses = List.of(newResponse(1L));
        when(authorRepository.findAllWithoutBooks()).thenReturn(authors);
        when(authorMapper.toResponseList(authors)).thenReturn(responses);

        List<AuthorResponse> result = authorService.getWithoutBooks();

        assertThat(result).isEqualTo(responses);
    }

    @Test
    void getAuthorBooks_authorExists_returnsBooks() {
        List<Book> books = List.of(new Book());
        List<BookResponse> responses = List.of(new BookResponse(1L, "Xamse", null, 300, null, null, List.of()));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(newAuthor(1L)));
        when(bookRepository.findAllByAuthorBooksAuthorId(1L)).thenReturn(books);
        when(bookMapper.toResponseList(books)).thenReturn(responses);

        List<BookResponse> result = authorService.getAuthorBooks(1L);

        assertThat(result).isEqualTo(responses);
    }

    @Test
    void getAuthorBooks_authorNotFound_throwsNotFound() {
        when(authorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.getAuthorBooks(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTHOR_NOT_FOUND);

        verify(bookRepository, never()).findAllByAuthorBooksAuthorId(any());
    }

    @Test
    void update_validRequest_updatesAndReturnsResponse() {
        UpdateAuthorRequest request = new UpdateAuthorRequest("Fuzuli", "Baghdadi", "Iraq");
        Author author = newAuthor(1L);
        AuthorResponse response = newResponse(1L);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorRepository.existsByNameAndSurnameAndIdNot("Fuzuli", "Baghdadi", 1L)).thenReturn(false);
        when(authorMapper.toResponse(author)).thenReturn(response);

        AuthorResponse result = authorService.update(1L, request);

        assertThat(result).isEqualTo(response);
        verify(authorMapper).updateEntity(request, author);
    }

    @Test
    void update_authorNotFound_throwsNotFound() {
        UpdateAuthorRequest request = new UpdateAuthorRequest("Fuzuli", "Baghdadi", "Iraq");
        when(authorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.update(1L, request))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTHOR_NOT_FOUND);
    }

    @Test
    void update_nameTakenByOtherAuthor_throwsAlreadyExists() {
        UpdateAuthorRequest request = new UpdateAuthorRequest("Fuzuli", "Baghdadi", "Iraq");
        Author author = newAuthor(1L);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorRepository.existsByNameAndSurnameAndIdNot("Fuzuli", "Baghdadi", 1L)).thenReturn(true);

        assertThatThrownBy(() -> authorService.update(1L, request))
                .isInstanceOf(AlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTHOR_ALREADY_EXISTS);

        verify(authorMapper, never()).updateEntity(request, author);
    }

    @Test
    void delete_authorExists_deletesAuthor() {
        Author author = newAuthor(1L);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));

        authorService.delete(1L);

        verify(authorRepository).delete(author);
    }

    @Test
    void delete_authorNotFound_throwsNotFound() {
        when(authorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.delete(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTHOR_NOT_FOUND);

        verify(authorRepository, never()).delete(any());
    }

    private Author newAuthor(Long id) {
        Author author = new Author();
        author.setId(id);
        author.setName("Nizami");
        author.setSurname("Ganjavi");
        author.setCountry("Azerbaijan");
        return author;
    }

    private AuthorResponse newResponse(Long id) {
        return new AuthorResponse(id, "Nizami", "Ganjavi", "Azerbaijan", null, null);
    }
}
