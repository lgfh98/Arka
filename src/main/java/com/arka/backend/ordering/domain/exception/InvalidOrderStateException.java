package com.arka.backend.ordering.domain.exception;

import com.arka.backend.shared.domain.exception.DomainException;

public class InvalidOrderStateException extends DomainException {
    public InvalidOrderStateException(String message) {
        super(message);
    }
}
