package org.tubalabs.app.users.password.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class LocalApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public LocalApiErrorResponse validationFailed(MethodArgumentNotValidException exception) {
        return new LocalApiErrorResponse(exception.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage());
    }

    @ExceptionHandler({LocalApiValidationException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public LocalApiErrorResponse badRequest(IllegalArgumentException exception) {
        return new LocalApiErrorResponse(exception.getMessage());
    }

    public record LocalApiErrorResponse(String message) {
    }
}
