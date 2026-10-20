package kz.iitu.springlab.catalog;

import kz.iitu.springlab.web.Genre;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    public List<Book> findAll(String author) {
        return findAll(author, null, null);
    }

    public List<Book> findAll(String author, Genre genre, String sort) {
        List<Book> list = new ArrayList<>(repository.findAll());

        if (author != null && !author.isBlank()) {
            String authorLower = author.toLowerCase();
            list = list.stream()
                    .filter(b -> b.author() != null && b.author().toLowerCase().contains(authorLower))
                    .toList();
        }

        if (genre != null) {
            list = list.stream()
                    .filter(b -> b.genre() == genre)
                    .toList();
        }

        if (sort != null && !sort.isBlank()) {
            list = sortBooks(list, sort);
        }

        return list;
    }

    private List<Book> sortBooks(List<Book> books, String sortParam) {
        String[] parts = sortParam.split(",");
        String field = parts[0].trim().toLowerCase();
        boolean desc = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc");

        Comparator<Book> comparator = switch (field) {
            case "id" -> Comparator.comparing(Book::id, Comparator.nullsLast(Comparator.naturalOrder()));
            case "title" -> Comparator.comparing(b -> b.title().toLowerCase(), Comparator.nullsLast(Comparator.naturalOrder()));
            case "author" -> Comparator.comparing(b -> b.author().toLowerCase(), Comparator.nullsLast(Comparator.naturalOrder()));
            case "year" -> Comparator.comparingInt(Book::year);
            case "genre" -> Comparator.comparing(b -> b.genre() != null ? b.genre().name() : "", Comparator.naturalOrder());
            case "createdat" -> Comparator.comparing(Book::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> throw new InvalidSortFieldException(parts[0].trim());
        };

        if (desc) {
            comparator = comparator.reversed();
        }

        return books.stream().sorted(comparator).toList();
    }

    public Optional<Book> findById(long id) {
        return repository.findById(id);
    }

    public Book getById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    public Book create(Book book) {
        boolean duplicate = repository.findAll().stream()
                .anyMatch(b -> b.title() != null && b.title().equalsIgnoreCase(book.title().trim())
                            && b.author() != null && b.author().equalsIgnoreCase(book.author().trim()));
        if (duplicate) {
            throw new DuplicateTitleException(book.title());
        }
        return repository.save(book);
    }

    public Book update(long id, Book book) {
        if (repository.findById(id).isEmpty()) {
            throw new BookNotFoundException(id);
        }
        Book updated = new Book(id, book.title(), book.author(), book.year(), book.genre(), book.createdAt());
        return repository.save(updated);
    }

    public Optional<Book> replace(long id, Book book) {
        if (repository.findById(id).isEmpty()) {
            return Optional.empty();
        }
        Book updated = new Book(id, book.title(), book.author(), book.year(), book.genre(), book.createdAt());
        return Optional.of(repository.save(updated));
    }

    public boolean delete(long id) {
        if (!repository.deleteById(id)) {
            throw new BookNotFoundException(id);
        }
        return true;
    }

    public List<Book> findByYear(int year) {
        return repository.findAll().stream()
                .filter(b -> b.year() == year)
                .toList();
    }
}
