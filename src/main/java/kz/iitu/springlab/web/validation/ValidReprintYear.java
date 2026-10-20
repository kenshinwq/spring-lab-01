package kz.iitu.springlab.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidReprintYearValidator.class)
public @interface ValidReprintYear {
    String message() default "{book.reprintYear.invalid}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
