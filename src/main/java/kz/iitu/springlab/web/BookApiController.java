package kz.iitu.springlab.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.iitu.springlab.catalog.Book;
import kz.iitu.springlab.catalog.BookNotFoundException;
import kz.iitu.springlab.catalog.BookService;
import kz.iitu.springlab.catalog.BookTag;
import kz.iitu.springlab.catalog.BookTagRepository;
import kz.iitu.springlab.web.dto.BookMapper;
import kz.iitu.springlab.web.dto.BookRequest;
import kz.iitu.springlab.web.dto.BookResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Tag(name = "Books", description = "The catalogue of books")
@RestController
@RequestMapping("/api/books")
public class BookApiController {

    private final BookService service;
    private final BookMapper mapper;
    private final BookTagRepository bookTagRepository;

    public BookApiController(BookService service, BookMapper mapper, BookTagRepository bookTagRepository) {
        this.service = service;
        this.mapper = mapper;
        this.bookTagRepository = bookTagRepository;
    }

    @Operation(summary = "Task 4.1: Get paged and sorted list of books")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page retrieved successfully")
    })
    @GetMapping
    public Page<BookResponse> list(
            @Parameter(description = "Filter by genre", example = "NOVEL")
            @RequestParam(required = false) Genre genre,
            @PageableDefault(size = 5, sort = "title") Pageable pageable) {
        return service.find(genre, pageable).map(mapper::toResponse);
    }

    @Operation(summary = "Task 4.3: Get a Slice of books (compares SQL count with Page)")
    @GetMapping("/slice")
    public Slice<BookResponse> listSlice(
            @Parameter(description = "Filter by genre", example = "NOVEL")
            @RequestParam(required = false) Genre genre,
            @PageableDefault(size = 5, sort = "title") Pageable pageable) {
        return service.findSlice(genre, pageable).map(mapper::toResponse);
    }

    @Operation(summary = "Find a book by its identifier")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The book was found"),
        @ApiResponse(responseCode = "404", description = "No book with this id",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public BookResponse find(
            @Parameter(description = "Identifier of the book", example = "1")
            @PathVariable long id) {
        return mapper.toResponse(service.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id)));
    }

    @Operation(summary = "Create a new book")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "The book was created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed on the request body",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "A book with this title and author already exists",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<BookResponse> create(
            @Valid @RequestBody BookRequest request) {
        Book saved = service.create(mapper.toDomain(request));
        return ResponseEntity
                .created(URI.create("/api/books/" + saved.getId()))
                .body(mapper.toResponse(saved));
    }

    @Operation(summary = "Update an existing book")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The book was updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed on the request body",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "No book with this id",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    public BookResponse update(
            @Parameter(description = "Identifier of the book to update", example = "1")
            @PathVariable long id,
            @Valid @RequestBody BookRequest request) {
        return mapper.toResponse(service.update(id, mapper.toDomain(request)));
    }

    @Operation(summary = "Delete a book by its identifier")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "The book was deleted"),
        @ApiResponse(responseCode = "404", description = "No book with this id",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identifier of the book to delete", example = "1")
            @PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Task 3.1: Derived query demonstration endpoints
    @Operation(summary = "Task 3.1: Derived query - findByTitleContainingIgnoreCase")
    @GetMapping("/derived/by-title")
    public List<BookResponse> findByTitle(@RequestParam String fragment) {
        return mapper.toResponseList(service.findByTitleContaining(fragment));
    }

    @Operation(summary = "Task 3.1: Derived query - findByYearBetweenOrderByYearDesc")
    @GetMapping("/derived/year-between")
    public List<BookResponse> findByYearBetween(@RequestParam int from, @RequestParam int to) {
        return mapper.toResponseList(service.findByYearBetween(from, to));
    }

    @Operation(summary = "Task 3.1: Derived query - findTop3ByGenreOrderByYearDesc")
    @GetMapping("/derived/top3")
    public List<BookResponse> findTop3(@RequestParam Genre genre) {
        return mapper.toResponseList(service.findTop3(genre));
    }

    @Operation(summary = "Task 3.1: Derived query - countByAuthorName")
    @GetMapping("/derived/author-count")
    public Map<String, Object> countByAuthor(@RequestParam String name) {
        return Map.of("author", name, "count", service.countByAuthorName(name));
    }

    // Task 3.2: JPQL search and bulk update
    @Operation(summary = "Task 3.2: JPQL query with named parameters")
    @GetMapping("/search")
    public List<BookResponse> search(
            @RequestParam Genre genre,
            @RequestParam(defaultValue = "1900") int year) {
        return mapper.toResponseList(service.search(genre, year));
    }

    @Operation(summary = "Task 3.2: Bulk update using @Modifying and @Query")
    @PostMapping("/archive")
    public Map<String, Object> archiveOlderThan(@RequestParam int year) {
        int count = service.archiveOlderThan(year);
        return Map.of("archivedThresholdYear", year, "rowsUpdated", count);
    }

    // Task 3.4: N+1 Demonstration
    @Operation(summary = "Task 3.4: N+1 query comparison (with or without JOIN FETCH)")
    @GetMapping("/n-plus-one")
    public Map<String, Object> nPlusOneDemo(@RequestParam(defaultValue = "false") boolean fetchJoin) {
        List<Book> list = fetchJoin ? service.findAllWithAuthorFetchJoin() : service.findAll();
        // Access author on each book to trigger lazy loading if not fetch joined
        List<String> details = list.stream()
                .map(b -> b.getTitle() + " by " + (b.getAuthor() != null ? b.getAuthor().getName() : "None"))
                .toList();
        return Map.of(
                "fetchJoinUsed", fetchJoin,
                "booksCount", list.size(),
                "items", details
        );
    }

    // Variant 2: ManyToMany with attributes demonstration
    @Operation(summary = "Variant 2: BookTag associations with addedAt timestamp attribute")
    @GetMapping("/variant2/book-tags")
    public List<Map<String, Object>> getBookTags() {
        return bookTagRepository.findAll().stream()
                .map(bt -> Map.<String, Object>of(
                        "id", bt.getId(),
                        "bookId", bt.getBook().getId(),
                        "bookTitle", bt.getBook().getTitle(),
                        "tagId", bt.getTag().getId(),
                        "tagName", bt.getTag().getName(),
                        "addedAt", bt.getAddedAt().toString()
                ))
                .toList();
    }
}
