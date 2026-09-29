package kz.iitu.springlab.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;

@Aspect
@Component
@Order(1)
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        String action = audited.action();
        String method = pjp.getSignature().toShortString();
        String args = audited.logArguments() ? " args=" + Arrays.toString(pjp.getArgs()) : "";

        log.info("[AUDIT] start {} | timestamp={} | {}{}", action, Instant.now(), method, args);

        try {
            Object result = pjp.proceed();
            log.info("[AUDIT] {} success | timestamp={}", action, Instant.now());
            return result;
        } catch (Throwable ex) {
            log.warn("[AUDIT] {} failure | timestamp={} | error: {}", action, Instant.now(), ex.getMessage());
            throw ex; // Исключение пробрасываем дальше для контроллера
        }
    }
}