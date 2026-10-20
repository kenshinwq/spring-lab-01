package kz.iitu.springlab.catalog;

public class BookNotFoundException extends RuntimeException {
    private final long id;

    public BookNotFoundException(long id) {
        super("No book with id " + id);
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
