package com.arka.backend.inventory.domain.exception;

import com.arka.backend.shared.domain.exception.DomainException;

public class InvalidProductDataException extends DomainException {
    public InvalidProductDataException(String message) {
        super(message);
    }
}
