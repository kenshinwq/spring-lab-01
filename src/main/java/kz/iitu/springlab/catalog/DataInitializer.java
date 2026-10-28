package kz.iitu.springlab.catalog;

import kz.iitu.springlab.web.Genre;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final TagRepository tagRepository;
    private final BookTagRepository bookTagRepository;

    public DataInitializer(AuthorRepository authorRepository,
                           BookRepository bookRepository,
                           TagRepository tagRepository,
                           BookTagRepository bookTagRepository) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
        this.tagRepository = tagRepository;
        this.bookTagRepository = bookTagRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (authorRepository.count() > 0) {
            return;
        }

        // 1. Create Tags
        Tag tagJava = tagRepository.save(new Tag("JAVA"));
        Tag tagBestseller = tagRepository.save(new Tag("BESTSELLER"));
        Tag tagClassic = tagRepository.save(new Tag("CLASSIC"));
        Tag tagDystopia = tagRepository.save(new Tag("DYSTOPIA"));

        // 2. Create Authors
        Author bloch = new Author("Joshua Bloch", "USA");
        Author orwell = new Author("George Orwell", "UK");
        Author lee = new Author("Harper Lee", "USA");
        Author martin = new Author("Robert Martin", "USA");

        authorRepository.save(bloch);
        authorRepository.save(orwell);
        authorRepository.save(lee);
        authorRepository.save(martin);

        // 3. Create Books with Author association
        Book b1 = new Book("Effective Java", Genre.TECHNICAL, 2018);
        Book b2 = new Book("Java Puzzlers", Genre.TECHNICAL, 2005);
        Book b3 = new Book("Java Concurrency in Practice", Genre.TECHNICAL, 2006);
        bloch.addBook(b1);
        bloch.addBook(b2);
        bloch.addBook(b3);

        Book b4 = new Book("1984", Genre.NOVEL, 1949);
        Book b5 = new Book("Animal Farm", Genre.NOVEL, 1945);
        orwell.addBook(b4);
        orwell.addBook(b5);

        Book b6 = new Book("To Kill a Mockingbird", Genre.NOVEL, 1960);
        Book b7 = new Book("Go Set a Watchman", Genre.NOVEL, 2015);
        lee.addBook(b6);
        lee.addBook(b7);

        Book b8 = new Book("Clean Code", Genre.TECHNICAL, 2008);
        martin.addBook(b8);

        bookRepository.save(b1);
        bookRepository.save(b2);
        bookRepository.save(b3);
        bookRepository.save(b4);
        bookRepository.save(b5);
        bookRepository.save(b6);
        bookRepository.save(b7);
        bookRepository.save(b8);

        // 4. Variant 2: Add BookTag associations with addedAt attribute
        bookTagRepository.save(new BookTag(b1, tagJava));
        bookTagRepository.save(new BookTag(b1, tagBestseller));

        bookTagRepository.save(new BookTag(b2, tagJava));
        bookTagRepository.save(new BookTag(b3, tagJava));

        // 1984 carries 2 tags: CLASSIC & DYSTOPIA
        bookTagRepository.save(new BookTag(b4, tagClassic));
        bookTagRepository.save(new BookTag(b4, tagDystopia));

        bookTagRepository.save(new BookTag(b5, tagClassic));
        bookTagRepository.save(new BookTag(b6, tagClassic));
        bookTagRepository.save(new BookTag(b6, tagBestseller));
        bookTagRepository.save(new BookTag(b8, tagBestseller));

        System.out.println(">>> Database seeded: 4 authors, 8 books, 4 tags, BookTag associations created.");
    }
}
