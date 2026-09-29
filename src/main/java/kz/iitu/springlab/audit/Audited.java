package kz.iitu.springlab.audit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME) // Обязательно RUNTIME, иначе AOP в рантайме её не увидит
@Documented
public @interface Audited {
    String action();
    boolean logArguments() default false;
}