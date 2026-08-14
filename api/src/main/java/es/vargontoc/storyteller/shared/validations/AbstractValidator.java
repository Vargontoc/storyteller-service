package es.vargontoc.storyteller.shared.validations;

import java.util.List;

import es.vargontoc.storyteller.shared.exceptions.ValidationException;

public abstract class AbstractValidator<T> implements IValidator<T> {
    
    protected void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + " must not be null");
        }
    }

    protected void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " must not be blank");
        }
    }

    protected void requireMaxLength(String value, int max, String fieldName) {
        if (value != null && value.length() > max) {
            throw new ValidationException(
                fieldName + " must not exceed " + max + " characters"
            );
        }
    }

    protected void requirePositive(Number value, String fieldName) {
        if (value == null || value.doubleValue() <= 0) {
            throw new ValidationException(fieldName + " must be a positive number");
        }
    }

    protected void requireMin(int value, int min, String fieldName) {
        if (value < min) {
            throw new ValidationException(fieldName + " must be at least " + min);
        }
    }

    protected void requireMax(int value, int max, String fieldName) {
        if (value > max) {
            throw new ValidationException(fieldName + " must be at most " + max);
        }
    }


    protected void requiredItems(List value, String fieldName) {
        if(value == null || value.isEmpty())
            throw new ValidationException(fieldName + " must have items");
    }
}
