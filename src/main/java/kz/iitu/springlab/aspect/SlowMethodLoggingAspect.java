package kz.iitu.springlab.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Aspect
@Component
@Order(4)
public class SlowMethodLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(SlowMethodLoggingAspect.class);

    private final long thresholdMs;
    private final List<String> slowCallsLog = new CopyOnWriteArrayList<>();

    public SlowMethodLoggingAspect(@Value("${app.slow-threshold-ms:150}") long thresholdMs) {
        this.thresholdMs = thresholdMs;
    }

    @Around("kz.iitu.springlab.aspect.Pointcuts.serviceOperation()")
    public Object recordSlowMethods(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (duration >= thresholdMs) {
                String record = String.format("Method [%s] exceeded threshold (%d ms): took %d ms",
                        pjp.getSignature().toShortString(), thresholdMs, duration);
                slowCallsLog.add(record);
                log.warn("[SLOW-VARIANT-2] {}", record);
            }
        }
    }

    public List<String> getSlowCallsLog() {
        return List.copyOf(slowCallsLog);
    }
}