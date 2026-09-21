package com.arka.backend.cart.domain.port.in;

import java.time.Duration;

public interface DetectAbandonedCartsUseCase {
    int detectAndMarkAbandonedCarts(Duration inactiveThreshold);
}
