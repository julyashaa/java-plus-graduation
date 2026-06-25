package ru.practicum.comment.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DateRangeValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {

    String message() default "Дата начала не может быть позже даты конца";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
