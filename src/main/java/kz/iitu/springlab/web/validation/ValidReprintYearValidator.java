package kz.iitu.springlab.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import kz.iitu.springlab.web.BookForm;

public class ValidReprintYearValidator implements ConstraintValidator<ValidReprintYear, BookForm> {

    @Override
    public boolean isValid(BookForm form, ConstraintValidatorContext context) {
        if (form == null || form.getReprintYear() == null) {
            return true; // Optional reprint year passes when omitted
        }
        // "A reprint year must not precede the first edition year"
        return form.getReprintYear() >= form.getYear();
    }
}
