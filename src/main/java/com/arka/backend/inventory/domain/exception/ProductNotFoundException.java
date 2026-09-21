package com.arka.backend.inventory.domain.exception;

import com.arka.backend.shared.domain.exception.DomainException;

public class ProductNotFoundException extends DomainException {
    public ProductNotFoundException(String message) {
        super(message);
    }
}
