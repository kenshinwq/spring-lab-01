package kz.iitu.springlab.catalog;

import kz.iitu.springlab.web.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository repository;
    private final AuthorRepository authorRepository;

    public BookService(BookRepository repository, AuthorRepository authorRepository) {
        this.repository = repository;
        this.authorRepository = authorRepository;
    }

    public Page<Book> find(Genre genre, Pageable pageable) {
        if (genre != null) {
            return repository.findByGenre(genre, pageable);
        }
        return repository.findAll(pageable);
    }

    public Slice<Book> findSlice(Genre genre, Pageable pageable) {
        return repository.findSliceByGenre(genre, pageable);
    }

    public List<Book> findAll() {
        return repository.findAll();
    }

    public List<Book> findAll(String author) {
        if (author != null && !author.isBlank()) {
            return repository.findAll().stream()
                    .filter(b -> b.getAuthor() != null && b.getAuthor().getName().toLowerCase().contains(author.toLowerCase()))
                    .toList();
        }
        return repository.findAll();
    }

    public Optional<Book> findById(long id) {
        return repository.findById(id);
    }

    public Book getById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional
    public Book create(Book book) {
        String authorName = book.getAuthor() != null ? book.getAuthor().getName().trim() : "";
        if (!authorName.isBlank() && repository.existsByTitleIgnoreCaseAndAuthorNameIgnoreCase(book.getTitle().trim(), authorName)) {
            throw new DuplicateTitleException(book.getTitle());
        }

        if (book.getAuthor() != null && !authorName.isBlank()) {
            Author author = authorRepository.findByName(authorName)
                    .orElseGet(() -> authorRepository.save(new Author(authorName, book.getAuthor().getCountry())));
            book.setAuthor(author);
        }

        return repository.save(book);
    }

    @Transactional
    public Book update(long id, Book incoming) {
        Book existing = repository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));

        existing.setTitle(incoming.getTitle());
        existing.setYear(incoming.getYear());
        existing.setGenre(incoming.getGenre());

        if (incoming.getAuthor() != null && !incoming.getAuthor().getName().isBlank()) {
            Author author = authorRepository.findByName(incoming.getAuthor().getName().trim())
                    .orElseGet(() -> authorRepository.save(new Author(incoming.getAuthor().getName().trim(), "Unknown")));
            existing.setAuthor(author);
        }

        return repository.save(existing);
    }

    @Transactional
    public boolean delete(long id) {
        if (!repository.existsById(id)) {
            throw new BookNotFoundException(id);
        }
        repository.deleteById(id);
        return true;
    }

    // Task 3.1: Five derived queries
    public List<Book> findByGenre(Genre genre) {
        return repository.findByGenre(genre);
    }

    public List<Book> findByTitleContaining(String fragment) {
        return repository.findByTitleContainingIgnoreCase(fragment);
    }

    public List<Book> findByYearBetween(int from, int to) {
        return repository.findByYearBetweenOrderByYearDesc(from, to);
    }

    public List<Book> findTop3(Genre genre) {
        return repository.findTop3ByGenreOrderByYearDesc(genre);
    }

    public long countByAuthorName(String name) {
        return repository.countByAuthorName(name);
    }

    // Task 3.2: JPQL queries
    public List<Book> search(Genre genre, int year) {
        return repository.search(genre, year);
    }

    @Transactional
    public int archiveOlderThan(int year) {
        return repository.archiveOlderThan(year);
    }

    // Task 3.4: N+1 demonstration
    public List<Book> findAllWithAuthorFetchJoin() {
        return repository.findAllWithAuthor();
    }

    public List<Book> findWithGraphByGenre(Genre genre) {
        return repository.findWithGraphByGenre(genre);
    }
}
