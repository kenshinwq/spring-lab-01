package kz.iitu.springlab.web;

import kz.iitu.springlab.aspect.SlowMethodLoggingAspect;
import kz.iitu.springlab.service.CatalogService;
import org.springframework.aop.support.AopUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lab4")
public class CatalogController {

    private final CatalogService catalogService;
    private final SlowMethodLoggingAspect slowAspect;

    public CatalogController(CatalogService catalogService, SlowMethodLoggingAspect slowAspect) {
        this.catalogService = catalogService;
        this.slowAspect = slowAspect;
    }

    @GetMapping("/item/{id}")
    public String getItem(@PathVariable long id) {
        return catalogService.findById(id);
    }

    @GetMapping("/items")
    public List<String> getItems(@RequestParam(defaultValue = "5") int limit) {
        return catalogService.findAll(limit);
    }

    @DeleteMapping("/item/{id}")
    public String removeItem(@PathVariable long id) {
        return catalogService.remove(id);
    }

    // Task 4: Информация о CGLIB-прокси
    @GetMapping("/proxy")
    public Map<String, String> proxyInfo() {
        return Map.of(
                "className", catalogService.getClass().getName(),
                "superClass", catalogService.getClass().getSuperclass().getSimpleName(),
                "isAopProxy", String.valueOf(AopUtils.isAopProxy(catalogService)),
                "isCglib", String.valueOf(AopUtils.isCglibProxy(catalogService))
        );
    }

    // Task 4: Демонстрация self-invocation
    @GetMapping("/remove-twice/{id}")
    public String removeTwice(@PathVariable long id) {
        return catalogService.removeTwice(id);
    }

    // Индивидуальный вариант 2: Вывод лога медленных методов
    @GetMapping("/slow-log")
    public List<String> getSlowLog() {
        return slowAspect.getSlowCallsLog();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}