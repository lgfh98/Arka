package com.arka.backend.ordering.domain.exception;

import com.arka.backend.shared.domain.exception.DomainException;

public class OrderNotModifiableException extends DomainException {
    public OrderNotModifiableException(String message) {
        super(message);
    }
}
