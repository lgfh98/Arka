package com.arka.backend.inventory.domain.exception;

import com.arka.backend.shared.domain.exception.DomainException;

public class InsufficientStockException extends DomainException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
