package kz.iitu.springlab.catalog;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "book_tag")
public class BookTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt = Instant.now();

    protected BookTag() { }

    public BookTag(Book book, Tag tag) {
        this.book = book;
        this.tag = tag;
        this.addedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Instant addedAt) {
        this.addedAt = addedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookTag bookTag)) return false;
        return Objects.equals(book, bookTag.book) && Objects.equals(tag, bookTag.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(book, tag);
    }

    @Override
    public String toString() {
        return "BookTag{" +
                "id=" + id +
                ", tag=" + (tag != null ? tag.getName() : null) +
                ", addedAt=" + addedAt +
                '}';
    }
}
