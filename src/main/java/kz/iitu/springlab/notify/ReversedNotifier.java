package kz.iitu.springlab.notify;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("reversed")
@Order(3)
public class ReversedNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(ReversedNotifier.class);

    @PostConstruct
    public void init() {
        log.info("CUSTOM >> ReversedNotifier initialized with @Order(3)");
    }

    @Override
    public String send(String message) {
        if (message == null) {
            return "";
        }
        String reversed = new StringBuilder(message).reverse().toString();
        log.info("REVERSED >> {}", reversed);
        return "reversed: " + reversed;
    }

    @Override
    public String channel() {
        return "reversed";
    }
}