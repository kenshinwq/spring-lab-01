package kz.iitu.springlab.web;

import jakarta.validation.Valid;
import kz.iitu.springlab.catalog.Author;
import kz.iitu.springlab.catalog.Book;
import kz.iitu.springlab.catalog.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/books")
public class BookPageController {

    private final BookService service;

    public BookPageController(BookService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String author,
                       Model model) {
        model.addAttribute("books", service.findAll(author));
        model.addAttribute("author", author);
        model.addAttribute("title", "Catalogue");
        return "books/list"; // -> templates/books/list.html
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new BookForm());
        model.addAttribute("genres", Genre.values());
        return "books/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") BookForm form,
                         BindingResult binding,          // immediately after the form object!
                         Model model,
                         RedirectAttributes redirect) {

        if (binding.hasErrors()) {
            model.addAttribute("genres", Genre.values());   // repopulate genres in model
            return "books/form";
        }

        Author author = (form.getAuthor() != null && !form.getAuthor().isBlank())
                ? new Author(form.getAuthor().trim(), "Unknown")
                : null;
        Book saved = service.create(new Book(form.getTitle(), author, form.getGenre(), form.getYear()));

        redirect.addFlashAttribute("message",
                "Book «" + saved.getTitle() + "» has been added");
        return "redirect:/books";
    }

    @GetMapping("/{id}")
    public String get(@PathVariable long id, Model model) {
        Book book = service.getById(id);
        model.addAttribute("book", book);
        return "books/detail";
    }
}
