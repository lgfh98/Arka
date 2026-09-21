package com.arka.backend.shared.infrastructure.web;

import com.arka.backend.inventory.domain.exception.InsufficientStockException;
import com.arka.backend.inventory.domain.exception.InventoryNotFoundException;
import com.arka.backend.inventory.domain.exception.InvalidProductDataException;
import com.arka.backend.inventory.domain.exception.InvalidStockQuantityException;
import com.arka.backend.inventory.domain.exception.ProductNotFoundException;
import com.arka.backend.ordering.domain.exception.EmptyOrderException;
import com.arka.backend.ordering.domain.exception.InvalidOrderStateException;
import com.arka.backend.ordering.domain.exception.OrderNotFoundException;
import com.arka.backend.ordering.domain.exception.OrderNotModifiableException;
import com.arka.backend.shared.domain.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ProductNotFoundException.class, InventoryNotFoundException.class, OrderNotFoundException.class})
    public ProblemDetail handleNotFoundException(DomainException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recurso No Encontrado");
        problem.setType(URI.create("https://arka.com/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler({
            InsufficientStockException.class,
            OrderNotModifiableException.class,
            InvalidOrderStateException.class,
            InvalidStockQuantityException.class,
            InvalidProductDataException.class,
            EmptyOrderException.class
    })
    public ProblemDetail handleInvariantViolationException(DomainException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Violación de Invariante de Dominio");
        problem.setType(URI.create("https://arka.com/errors/invariant-violation"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Error de validación en la solicitud");
        problem.setTitle("Solicitud Inválida");
        problem.setType(URI.create("https://arka.com/errors/validation-error"));
        problem.setProperty("timestamp", Instant.now());

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("fieldErrors", errors);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error interno no esperado.");
        problem.setTitle("Error Interno");
        problem.setType(URI.create("https://arka.com/errors/internal-server-error"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("rawMessage", ex.getMessage());
        return problem;
    }
}
