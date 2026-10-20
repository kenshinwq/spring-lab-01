package kz.iitu.springlab.catalog;

import kz.iitu.springlab.web.Genre;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class BookRepository {

    private final Map<Long, Book> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public BookRepository() { // seed data with genres
        save(new Book(null, "Effective Java", "Joshua Bloch", 2018, Genre.TECHNICAL, java.time.Instant.now().minusSeconds(86400 * 5)));
        save(new Book(null, "Clean Code", "Robert Martin", 2008, Genre.TECHNICAL, java.time.Instant.now().minusSeconds(86400 * 10)));
        save(new Book(null, "Spring in Action", "Craig Walls", 2022, Genre.TECHNICAL, java.time.Instant.now().minusSeconds(86400 * 2)));
        save(new Book(null, "1984", "George Orwell", 1949, Genre.NOVEL, java.time.Instant.now().minusSeconds(86400 * 30)));
        save(new Book(null, "To Kill a Mockingbird", "Harper Lee", 1960, Genre.NOVEL, java.time.Instant.now().minusSeconds(86400 * 20)));
    }

    public List<Book> findAll() {
        return List.copyOf(storage.values());
    }

    public Optional<Book> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Book save(Book book) {
        long id = book.id() != null ? book.id() : sequence.incrementAndGet();
        Genre genre = book.genre() != null ? book.genre() : Genre.OTHER;
        java.time.Instant createdAt = book.createdAt() != null ? book.createdAt() : java.time.Instant.now();
        Book stored = new Book(id, book.title(), book.author(), book.year(), genre, createdAt);
        storage.put(id, stored);
        return stored;
    }

    public boolean deleteById(long id) {
        return storage.remove(id) != null;
    }
}
