package ru.practicum.comment.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.practicum.comment.dto.GetCommentsAdminDtoParams;

import java.time.LocalDateTime;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, GetCommentsAdminDtoParams> {

    @Override
    public boolean isValid(GetCommentsAdminDtoParams value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        LocalDateTime rangeStart = value.getRangeStart();
        LocalDateTime rangeEnd = value.getRangeEnd();
        if (rangeStart == null || rangeEnd == null) {
            return true;
        }

        return !rangeStart.isAfter(rangeEnd);
    }
}