package kz.iitu.springlab.catalog;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookTagRepository extends JpaRepository<BookTag, Long> {

    @Override
    @EntityGraph(attributePaths = {"book", "tag"})
    List<BookTag> findAll();

    @EntityGraph(attributePaths = {"book", "tag"})
    List<BookTag> findByBookId(Long bookId);
}
