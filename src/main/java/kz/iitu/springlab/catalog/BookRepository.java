package kz.iitu.springlab.catalog;

import kz.iitu.springlab.web.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Override
    @EntityGraph(attributePaths = "author")
    Optional<Book> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "author")
    List<Book> findAll();

    // Task 3.1: Five derived queries
    @EntityGraph(attributePaths = "author")
    List<Book> findByGenre(Genre genre);

    @EntityGraph(attributePaths = "author")
    List<Book> findByTitleContainingIgnoreCase(String fragment);

    @EntityGraph(attributePaths = "author")
    List<Book> findByYearBetweenOrderByYearDesc(int from, int to);

    @EntityGraph(attributePaths = "author")
    List<Book> findTop3ByGenreOrderByYearDesc(Genre genre);

    long countByAuthorName(String name);

    // Task 3.2: JPQL queries
    @Query("""
           select b from Book b
           left join fetch b.author
           where b.genre = :genre and b.year >= :year
           order by b.title""")
    List<Book> search(@Param("genre") Genre genre, @Param("year") int year);

    @Modifying(clearAutomatically = true)
    @Query("update Book b set b.archived = true where b.year < :year")
    int archiveOlderThan(@Param("year") int year);

    // Task 3.4: N+1 demonstration queries
    @Query("select b from Book b join fetch b.author")
    List<Book> findAllWithAuthor();

    // Plain query to intentionally demonstrate N+1 when lazy loading
    @Query("select b from Book b")
    List<Book> findAllPlain();

    @EntityGraph(attributePaths = "author")
    List<Book> findWithGraphByGenre(Genre genre);

    // Task 4: Pagination and sorting
    @EntityGraph(attributePaths = "author")
    Page<Book> findByGenre(Genre genre, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Slice<Book> findSliceByGenre(Genre genre, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "author")
    Page<Book> findAll(Pageable pageable);

    // Duplicate check
    boolean existsByTitleIgnoreCaseAndAuthorNameIgnoreCase(String title, String authorName);
}
