package kz.iitu.springlab.notify;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NotificationService {

    private final Notifier primary;              // Выбирается EmailNotifier благодаря аннотации @Primary
    private final Notifier console;              // Выбирается явно через @Qualifier("console")
    private final List<Notifier> all;            // Все реализации Notifier, упорядоченные по @Order
    private final Map<String, Notifier> byName;  // Все реализации в виде Map, где ключ — имя бина

    public NotificationService(Notifier primary,
                               @Qualifier("console") Notifier console,
                               List<Notifier> all,
                               Map<String, Notifier> byName) {
        this.primary = primary;
        this.console = console;
        this.all = all;
        this.byName = byName;
    }

    public String viaPrimary(String message) {
        return primary.send(message);
    }

    public String viaConsole(String message) {
        return console.send(message);
    }

    public List<String> viaAll(String message) {
        return all.stream().map(n -> n.send(message)).toList();
    }

    public Set<String> names() {
        return byName.keySet();
    }
}