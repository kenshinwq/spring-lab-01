package kz.iitu.springlab.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookValidationAndExceptionTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Task 1: GET /books/new displays the empty form and genres in model")
    void testGetNewForm() throws Exception {
        mockMvc.perform(get("/books/new").locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attributeExists("genres"))
                .andExpect(content().string(containsString("New book")));
    }

    @Test
    @DisplayName("Task 2: Submitting empty form fails validation and retains typed values")
    void testSubmitEmptyFormFailsValidation() throws Exception {
        mockMvc.perform(post("/books")
                        .locale(Locale.ENGLISH)
                        .param("title", "")
                        .param("author", "")
                        .param("year", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeHasFieldErrors("form", "title", "author", "year", "genre"))
                .andExpect(model().attributeExists("genres"));
    }

    @Test
    @DisplayName("Task 2: Submitting year 1200 and no genre reports both errors in English")
    void testSubmitYear1200AndNoGenre() throws Exception {
        mockMvc.perform(post("/books")
                        .locale(Locale.ENGLISH)
                        .param("title", "Clean Code")
                        .param("author", "Robert Martin")
                        .param("year", "1200"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeHasFieldErrors("form", "year", "genre"))
                .andExpect(content().string(containsString("The year must be between 1450 and 2100")))
                .andExpect(content().string(containsString("Choose a genre")));
    }

    @Test
    @DisplayName("Task 4.1 & 4.2: Custom constraint @NoDigits rejects author with numbers (e.g. Bloch 2018)")
    void testCustomConstraintNoDigits() throws Exception {
        mockMvc.perform(post("/books")
                        .locale(Locale.ENGLISH)
                        .param("title", "Effective Java")
                        .param("author", "Bloch 2018")
                        .param("year", "2018")
                        .param("genre", "SCIENCE"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeHasFieldErrors("form", "author"))
                .andExpect(content().string(containsString("The author name must not contain digits")));
    }

    @Test
    @DisplayName("Variant 2: Class-level constraint @ValidReprintYear rejects reprintYear preceding first edition year")
    void testVariant2ClassLevelConstraint() throws Exception {
        mockMvc.perform(post("/books")
                        .locale(Locale.ENGLISH)
                        .param("title", "Domain-Driven Design")
                        .param("author", "Eric Evans")
                        .param("year", "2020")
                        .param("reprintYear", "2015")
                        .param("genre", "SCIENCE"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().hasErrors())
                .andExpect(content().string(containsString("Reprint year must not precede the first edition year")));
    }

    @Test
    @DisplayName("Task 2.4: Valid submission redirects with flash message (Post/Redirect/Get)")
    void testValidSubmissionRedirects() throws Exception {
        mockMvc.perform(post("/books")
                        .locale(Locale.ENGLISH)
                        .param("title", "Refactoring")
                        .param("author", "Martin Fowler")
                        .param("year", "2019")
                        .param("reprintYear", "2023")
                        .param("genre", "SCIENCE")
                        .param("available", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"))
                .andExpect(flash().attribute("message", "Book «Refactoring» has been added"));
    }

    @Test
    @DisplayName("Task 3: Switching language to Russian (?lang=ru) translates page and error messages")
    void testLanguageSwitchingToRussian() throws Exception {
        mockMvc.perform(get("/books/new")
                        .param("lang", "ru")
                        .locale(Locale.forLanguageTag("ru")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Новая книга")))
                .andExpect(content().string(containsString("Название")))
                .andExpect(content().string(containsString("Автор")))
                .andExpect(content().string(containsString("Год")));

        mockMvc.perform(post("/books")
                        .param("lang", "ru")
                        .locale(Locale.forLanguageTag("ru"))
                        .param("title", "")
                        .param("author", "Bloch 2018")
                        .param("year", "1200"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(content().string(containsString("Название не должно быть пустым")))
                .andExpect(content().string(containsString("Имя автора не должно содержать цифр")))
                .andExpect(content().string(containsString("Год должен быть в диапазоне 1450–2100")));
    }

    @Test
    @DisplayName("Task 4.3 & 4.4: GET /books/999 in browser triggers PageExceptionHandler returning HTML 404")
    void testPageExceptionHandler() throws Exception {
        mockMvc.perform(get("/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/not-found"))
                .andExpect(model().attribute("message", "No book with id 999"))
                .andExpect(content().string(containsString("404 - Not Found")))
                .andExpect(content().string(containsString("No book with id 999")));
    }

    @Test
    @DisplayName("Task 4.3 & 4.4: GET /api/books/999 triggers ApiExceptionHandler returning ProblemDetail JSON 404")
    void testApiExceptionHandler() throws Exception {
        mockMvc.perform(get("/api/books/999").accept(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title", is("Book not found")))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.detail", is("No book with id 999")));
    }
}
