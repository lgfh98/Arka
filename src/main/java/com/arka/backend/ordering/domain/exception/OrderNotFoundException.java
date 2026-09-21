package com.arka.backend.ordering.domain.exception;

import com.arka.backend.shared.domain.exception.DomainException;

public class OrderNotFoundException extends DomainException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
