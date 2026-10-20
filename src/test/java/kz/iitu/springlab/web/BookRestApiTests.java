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
    @DisplayName("Task 2: GET /api/books returns 200 and JSON array with DTO fields")
    void testListBooks() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].author").exists())
                .andExpect(jsonPath("$[0].year").exists())
                .andExpect(jsonPath("$[0].genre").exists())
                .andExpect(jsonPath("$[0].createdAt").exists());
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
    @DisplayName("Variant 2: GET /api/books?sort=year,desc&genre=NOVEL returns sorted filtered books")
    void testVariant2SortingAndFiltering() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("genre", "NOVEL")
                        .param("sort", "year,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("To Kill a Mockingbird"))
                .andExpect(jsonPath("$[0].year").value(1960))
                .andExpect(jsonPath("$[1].title").value("1984"))
                .andExpect(jsonPath("$[1].year").value(1949));
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
