package com.arka.backend.inventory.domain.exception;

import com.arka.backend.shared.domain.exception.DomainException;

public class InventoryNotFoundException extends DomainException {
    public InventoryNotFoundException(String message) {
        super(message);
    }
}
