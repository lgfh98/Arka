package com.arka.backend.inventory.domain.exception;

import com.arka.backend.shared.domain.exception.DomainException;

public class InvalidStockQuantityException extends DomainException {
    public InvalidStockQuantityException(String message) {
        super(message);
    }
}
