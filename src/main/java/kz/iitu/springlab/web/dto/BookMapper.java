package kz.iitu.springlab.web.dto;

import kz.iitu.springlab.catalog.Author;
import kz.iitu.springlab.catalog.Book;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookMapper {

    public Book toDomain(BookRequest request) {
        Author author = null;
        if (request.author() != null && !request.author().isBlank()) {
            author = new Author(request.author().trim(), "Unknown");
        }
        return new Book(request.title(), author, request.genre(), request.year());
    }

    public BookResponse toResponse(Book book) {
        String authorName = book.getAuthor() != null ? book.getAuthor().getName() : "";
        return new BookResponse(book.getId(), book.getTitle(), authorName,
                book.getYear(), book.getGenre(), book.getCreatedAt());
    }

    public List<BookResponse> toResponseList(List<Book> books) {
        return books.stream().map(this::toResponse).toList();
    }
}
