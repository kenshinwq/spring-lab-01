package kz.iitu.springlab.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookRestApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Task 4: GET /api/books returns 200 and Page<BookResponse> with DTO fields and pagination metadata")
    void testListBooks() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].title").exists())
                .andExpect(jsonPath("$.content[0].author").exists())
                .andExpect(jsonPath("$.content[0].year").exists())
                .andExpect(jsonPath("$.content[0].genre").exists())
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(8)))
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    @DisplayName("Task 2: GET /api/books/1 returns 200 and single book")
    void testFindExistingBook() throws Exception {
        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Effective Java"));
    }

    @Test
    @DisplayName("Task 3: GET /api/books/999 returns 404 ProblemDetail")
    void testFindMissingBookReturnsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.iitu.kz/errors/book-not-found"))
                .andExpect(jsonPath("$.title").value("Book not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No book with id 999"));
    }

    @Test
    @DisplayName("Task 2: POST /api/books valid returns 201 Created and Location header")
    void testCreateBook() throws Exception {
        String json = """
                {
                    "title": "Designing Data-Intensive Applications",
                    "author": "Martin Kleppmann",
                    "year": 2017,
                    "genre": "TECHNICAL"
                }
                """;
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/books/\\d+")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Designing Data-Intensive Applications"))
                .andExpect(jsonPath("$.genre").value("TECHNICAL"));
    }

    @Test
    @DisplayName("Task 3: POST /api/books with empty title returns 400 Bad Request ProblemDetail")
    void testCreateBookInvalidReturns400() throws Exception {
        String json = """
                {
                    "title": "",
                    "author": "Author",
                    "year": 2020,
                    "genre": "NOVEL"
                }
                """;
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Task 3: POST duplicate title and author returns 409 Conflict ProblemDetail")
    void testCreateDuplicateBookReturns409() throws Exception {
        String json = """
                {
                    "title": "Effective Java",
                    "author": "Joshua Bloch",
                    "year": 2018,
                    "genre": "TECHNICAL"
                }
                """;
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.iitu.kz/errors/duplicate-title"))
                .andExpect(jsonPath("$.title").value("Effective Java"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Task 2: PUT /api/books/1 updates book and returns 200 OK")
    void testUpdateBook() throws Exception {
        String json = """
                {
                    "title": "Effective Java 3rd Edition",
                    "author": "Joshua Bloch",
                    "year": 2018,
                    "genre": "TECHNICAL"
                }
                """;
        mockMvc.perform(put("/api/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Effective Java 3rd Edition"));
    }

    @Test
    @DisplayName("Task 2 & 3: DELETE /api/books/2 returns 204 first time, 404 second time")
    void testDeleteBookIdempotency() throws Exception {
        // First delete -> 204
        mockMvc.perform(delete("/api/books/2"))
                .andExpect(status().isNoContent());

        // Second delete -> 404
        mockMvc.perform(delete("/api/books/2"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Task 4 & Variant 2: GET /api/books?genre=NOVEL&sort=year,desc returns sorted page of books")
    void testVariant2SortingAndFiltering() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("genre", "NOVEL")
                        .param("sort", "year,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(4)))
                .andExpect(jsonPath("$.content[0].title").value("Go Set a Watchman"))
                .andExpect(jsonPath("$.content[0].year").value(2015))
                .andExpect(jsonPath("$.content[1].title").value("To Kill a Mockingbird"))
                .andExpect(jsonPath("$.content[1].year").value(1960))
                .andExpect(jsonPath("$.content[2].title").value("1984"))
                .andExpect(jsonPath("$.content[2].year").value(1949))
                .andExpect(jsonPath("$.content[3].title").value("Animal Farm"))
                .andExpect(jsonPath("$.content[3].year").value(1945))
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    @DisplayName("Task 4: GET /api/books with pagination ?page=0&size=3 and ?page=1&size=3")
    void testPaginationPages() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalElements").value(8))
                .andExpect(jsonPath("$.totalPages").value(3));

        mockMvc.perform(get("/api/books")
                        .param("page", "1")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    @DisplayName("Task 4: GET /api/books/slice returns Slice without count query")
    void testSliceEndpoint() throws Exception {
        mockMvc.perform(get("/api/books/slice")
                        .param("genre", "NOVEL")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    @DisplayName("Task 3.1: Derived query endpoints (by-title, year-between, top3, author-count)")
    void testDerivedQueries() throws Exception {
        mockMvc.perform(get("/api/books/derived/by-title").param("fragment", "clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Clean Code"));

        mockMvc.perform(get("/api/books/derived/year-between").param("from", "1940").param("to", "1950"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].year").value(1949))
                .andExpect(jsonPath("$[1].year").value(1945));

        mockMvc.perform(get("/api/books/derived/top3").param("genre", "NOVEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title").value("Go Set a Watchman"));

        mockMvc.perform(get("/api/books/derived/author-count").param("name", "George Orwell"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    @DisplayName("Task 3.2: JPQL search and @Modifying archive query")
    void testJpqlSearchAndArchive() throws Exception {
        mockMvc.perform(get("/api/books/search")
                        .param("genre", "TECHNICAL")
                        .param("year", "2007"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Clean Code"))
                .andExpect(jsonPath("$[1].title").value("Effective Java"));

        mockMvc.perform(post("/api/books/archive").param("year", "1950"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedThresholdYear").value(1950))
                .andExpect(jsonPath("$.rowsUpdated").value(2));
    }

    @Test
    @DisplayName("Task 3.4: N+1 demonstration endpoint (/api/books/n-plus-one)")
    void testNPlusOneDemo() throws Exception {
        mockMvc.perform(get("/api/books/n-plus-one").param("fetchJoin", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fetchJoinUsed").value(true))
                .andExpect(jsonPath("$.booksCount").value(8))
                .andExpect(jsonPath("$.items", hasSize(8)));
    }

    @Test
    @DisplayName("Variant 2: BookTag entity with addedAt timestamp column")
    void testVariant2BookTags() throws Exception {
        mockMvc.perform(get("/api/books/variant2/book-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].bookId").exists())
                .andExpect(jsonPath("$[0].bookTitle").exists())
                .andExpect(jsonPath("$[0].tagId").exists())
                .andExpect(jsonPath("$[0].tagName").exists())
                .andExpect(jsonPath("$[0].addedAt").exists());
    }

    @Test
    @DisplayName("Variant 2: GET /api/books?sort=unknownField,desc returns 400 ProblemDetail")
    void testVariant2InvalidSortField() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("sort", "unknownField,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.iitu.kz/errors/invalid-sort-field"))
                .andExpect(jsonPath("$.title").value("Invalid sort field"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.field").value("unknownField"));
    }

    @Test
    @DisplayName("Task 4: GET /v3/api-docs returns 200 and OpenAPI JSON")
    void testOpenApiDoc() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/books']").exists())
                .andExpect(jsonPath("$.components.schemas.BookRequest").exists())
                .andExpect(jsonPath("$.components.schemas.BookRequest.properties.title.maxLength").value(120));
    }
}
