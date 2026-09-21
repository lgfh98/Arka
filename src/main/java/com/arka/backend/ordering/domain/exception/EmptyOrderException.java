package com.arka.backend.ordering.domain.exception;

import com.arka.backend.shared.domain.exception.DomainException;

public class EmptyOrderException extends DomainException {
    public EmptyOrderException(String message) {
        super(message);
    }
}
