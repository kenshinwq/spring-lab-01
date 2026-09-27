package kz.iitu.springlab.web;

import kz.iitu.springlab.config.AppProperties;
import kz.iitu.springlab.config.EnvironmentBanner;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/lab3")
public class Lab3Controller {

    private final AppProperties props;
    private final EnvironmentBanner banner;
    private final Environment environment;

    public Lab3Controller(AppProperties props,
                          EnvironmentBanner banner,
                          Environment environment) {
        this.props = props;
        this.banner = banner;
        this.environment = environment;
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("owner", props.owner());
        response.put("group", props.group());
        response.put("mailFrom", props.mail().from());
        response.put("mailRetryCount", props.mail().retryCount());
        response.put("mailTimeout", props.mail().timeout().toString());
        response.put("mailEnabled", props.mail().enabled());

        // Поля индивидуального задания (Вариант 2)
        response.put("paginationDefaultSize", props.pagination().defaultSize());
        response.put("paginationMaxSize", props.pagination().maxSize());

        response.put("serverPort", environment.getProperty("server.port", "8080"));
        response.put("activeProfiles", Arrays.asList(environment.getActiveProfiles()));
        response.put("banner", banner.describe());

        return response;
    }
}