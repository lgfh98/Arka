package com.arka.backend.cart.infrastructure.web;

import com.arka.backend.cart.domain.model.aggregate.Cart;
import com.arka.backend.cart.domain.port.in.AddCartItemUseCase;
import com.arka.backend.cart.domain.port.in.DetectAbandonedCartsUseCase;
import com.arka.backend.cart.domain.port.in.GetCartQuery;
import com.arka.backend.cart.domain.port.in.RemoveCartItemUseCase;
import com.arka.backend.cart.infrastructure.web.dto.AddCartItemRequest;
import com.arka.backend.cart.infrastructure.web.dto.CartResponse;
import com.arka.backend.cart.infrastructure.web.mapper.CartWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Cart & Abandonment")
@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final AddCartItemUseCase addCartItemUseCase;
    private final RemoveCartItemUseCase removeCartItemUseCase;
    private final DetectAbandonedCartsUseCase detectAbandonedCartsUseCase;
    private final GetCartQuery getCartQuery;
    private final CartWebMapper cartWebMapper;

    @Operation(summary = "Agregar ítem al carrito", description = "Agrega un producto o incrementa cantidad en el carrito activo del cliente")
    @PostMapping("/{customerId}/items")
    public ResponseEntity<CartResponse> addItem(
            @PathVariable UUID customerId,
            @Valid @RequestBody AddCartItemRequest request) {
        Cart cart = addCartItemUseCase.addItem(
                customerId,
                request.productId(),
                request.quantity(),
                request.unitPrice()
        );
        return ResponseEntity.ok(cartWebMapper.toResponse(cart));
    }

    @Operation(summary = "Remover ítem del carrito", description = "Elimina un producto del carrito del cliente")
    @DeleteMapping("/{customerId}/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable UUID customerId,
            @PathVariable UUID productId) {
        Cart cart = removeCartItemUseCase.removeItem(customerId, productId);
        return ResponseEntity.ok(cartWebMapper.toResponse(cart));
    }

    @Operation(summary = "Consultar carrito activo", description = "Obtiene el carrito activo de un cliente con cálculo de total")
    @GetMapping("/{customerId}")
    public ResponseEntity<CartResponse> getCartByCustomerId(@PathVariable UUID customerId) {
        Cart cart = getCartQuery.getByCustomerId(customerId);
        return ResponseEntity.ok(cartWebMapper.toResponse(cart));
    }

    @Operation(summary = "Consultar carritos abandonados", description = "Lista todos los carritos en estado de abandono para recuperación de ventas (HU8)")
    @GetMapping("/abandoned")
    public ResponseEntity<List<CartResponse>> getAbandonedCarts() {
        List<Cart> abandoned = getCartQuery.getAbandonedCarts();
        return ResponseEntity.ok(abandoned.stream().map(cartWebMapper::toResponse).toList());
    }

    @Operation(summary = "Detectar carritos abandonados", description = "Evalúa inactividad y marca carritos abandonados emitiendo eventos vía Outbox (HU8)")
    @PostMapping("/detect-abandoned")
    public ResponseEntity<Map<String, Object>> detectAbandoned(
            @RequestParam(name = "minutes", defaultValue = "120") int minutes) {
        int count = detectAbandonedCartsUseCase.detectAndMarkAbandonedCarts(Duration.ofMinutes(minutes));
        return ResponseEntity.ok(Map.of("abandonedCount", count, "thresholdMinutes", minutes));
    }
}
