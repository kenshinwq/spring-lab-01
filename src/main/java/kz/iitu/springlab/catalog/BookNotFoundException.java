package kz.iitu.springlab.catalog;

public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(long id) {
        super("Book not found with id: " + id);
    }
}
