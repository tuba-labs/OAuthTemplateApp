package org.tubalabs.app.users.identity.password.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.tubalabs.app.users.identity.password.api.dtos.LocalApiErrorResponseDto;

@RestControllerAdvice
public class LocalApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public LocalApiErrorResponseDto validationFailed(MethodArgumentNotValidException exception) {
        return new LocalApiErrorResponseDto(exception.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage());
    }

    @ExceptionHandler({LocalApiValidationException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public LocalApiErrorResponseDto badRequest(IllegalArgumentException exception) {
        return new LocalApiErrorResponseDto(exception.getMessage());
    }
}
