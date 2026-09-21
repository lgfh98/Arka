package com.arka.backend.inventory.domain.model.aggregate;

import com.arka.backend.inventory.domain.exception.InsufficientStockException;
import com.arka.backend.inventory.domain.exception.InvalidStockQuantityException;
import com.arka.backend.inventory.domain.model.event.LowStockDetectedEvent;
import com.arka.backend.inventory.domain.model.event.StockAdjustedEvent;
import com.arka.backend.inventory.domain.model.event.StockPermanentlyDeductedEvent;
import com.arka.backend.inventory.domain.model.event.StockReservationAdjustedEvent;
import com.arka.backend.inventory.domain.model.event.StockReservationFailedEvent;
import com.arka.backend.inventory.domain.model.event.StockReservationReleasedEvent;
import com.arka.backend.inventory.domain.model.event.StockReservedEvent;
import com.arka.backend.inventory.domain.model.valueobject.InventoryId;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.shared.domain.DomainEvent;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
public class InventoryItem {

    private final InventoryId id;
    private final ProductId productId;
    private int physicalStock;
    private int reservedStock;
    private int minimumThreshold;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public InventoryItem(InventoryId id, ProductId productId, int physicalStock, int reservedStock, int minimumThreshold) {
        validateNonNegativeStock(physicalStock);
        validateNonNegativeReserved(reservedStock);
        validateNonNegativeThreshold(minimumThreshold);
        this.id = Objects.requireNonNull(id, "InventoryId no puede ser nulo");
        this.productId = Objects.requireNonNull(productId, "ProductId no puede ser nulo");
        this.physicalStock = physicalStock;
        this.reservedStock = reservedStock;
        this.minimumThreshold = minimumThreshold;
    }

    public static InventoryItem initialize(InventoryId id, ProductId productId, int initialStock, int minimumThreshold) {
        InventoryItem item = new InventoryItem(id, productId, initialStock, 0, minimumThreshold);
        item.recordEvent(new StockAdjustedEvent(
                UUID.randomUUID(),
                item.getId(),
                item.getProductId(),
                0,
                initialStock,
                "Inicialización de inventario",
                Instant.now()
        ));
        item.checkThreshold();
        return item;
    }

    public void adjustPhysicalStock(int newStock, String reason) {
        validateNonNegativeStock(newStock);
        if (newStock < this.reservedStock) {
            throw new InvalidStockQuantityException("El stock físico no puede ser menor a las unidades ya reservadas (" + this.reservedStock + ")");
        }
        int oldStock = this.physicalStock;
        this.physicalStock = newStock;

        recordEvent(new StockAdjustedEvent(
                UUID.randomUUID(),
                this.id,
                this.productId,
                oldStock,
                newStock,
                reason,
                Instant.now()
        ));
        checkThreshold();
    }

    public void reserveStock(UUID orderId, int quantity) {
        if (quantity <= 0) {
            throw new InvalidStockQuantityException("La cantidad a reservar debe ser mayor a cero");
        }
        int available = getAvailableStock();
        if (available < quantity) {
            recordEvent(new StockReservationFailedEvent(
                    UUID.randomUUID(),
                    this.id,
                    this.productId,
                    orderId,
                    quantity,
                    available,
                    Instant.now()
            ));
            throw new InsufficientStockException("Stock insuficiente para el producto " + productId.value() +
                    ". Disponible: " + available + ", Solicitado: " + quantity + " (INV-03)");
        }

        this.reservedStock += quantity;
        recordEvent(new StockReservedEvent(
                UUID.randomUUID(),
                this.id,
                this.productId,
                orderId,
                quantity,
                Instant.now()
        ));
        checkThreshold();
    }

    public void adjustReservation(UUID orderId, int oldQuantity, int newQuantity) {
        if (newQuantity <= 0) {
            throw new InvalidStockQuantityException("La nueva cantidad debe ser mayor a cero");
        }
        int delta = newQuantity - oldQuantity;
        if (delta > 0) {
            int available = getAvailableStock();
            if (available < delta) {
                throw new InsufficientStockException("Stock insuficiente para incrementar la reserva del producto " +
                        productId.value() + ". Disponible: " + available + ", Requerido adicional: " + delta);
            }
        }
        this.reservedStock += delta;
        recordEvent(new StockReservationAdjustedEvent(
                UUID.randomUUID(),
                this.id,
                this.productId,
                orderId,
                oldQuantity,
                newQuantity,
                delta,
                Instant.now()
        ));
        checkThreshold();
    }

    public void deductPermanent(int quantity) {
        if (quantity <= 0) {
            throw new InvalidStockQuantityException("La cantidad a descontar debe ser mayor a cero");
        }
        if (this.physicalStock < quantity) {
            throw new InvalidStockQuantityException("Stock físico insuficiente para descuento permanente");
        }
        this.physicalStock -= quantity;
        this.reservedStock = Math.max(0, this.reservedStock - quantity);

        recordEvent(new StockPermanentlyDeductedEvent(
                UUID.randomUUID(),
                this.id,
                this.productId,
                quantity,
                Instant.now()
        ));
        checkThreshold();
    }

    public void releaseReservation(int quantity) {
        if (quantity <= 0) {
            throw new InvalidStockQuantityException("La cantidad a liberar debe ser mayor a cero");
        }
        this.reservedStock = Math.max(0, this.reservedStock - quantity);

        recordEvent(new StockReservationReleasedEvent(
                UUID.randomUUID(),
                this.id,
                this.productId,
                quantity,
                Instant.now()
        ));
    }

    public int getAvailableStock() {
        return Math.max(0, this.physicalStock - this.reservedStock);
    }

    public boolean isLowStock() {
        return getAvailableStock() <= this.minimumThreshold;
    }

    private void checkThreshold() {
        if (isLowStock()) {
            recordEvent(new LowStockDetectedEvent(
                    UUID.randomUUID(),
                    this.id,
                    this.productId,
                    getAvailableStock(),
                    this.minimumThreshold,
                    Instant.now()
            ));
        }
    }

    private void validateNonNegativeStock(int stock) {
        if (stock < 0) {
            throw new InvalidStockQuantityException("El stock físico no puede ser negativo: " + stock + " (INV-02)");
        }
    }

    private void validateNonNegativeReserved(int reserved) {
        if (reserved < 0) {
            throw new InvalidStockQuantityException("El stock reservado no puede ser negativo: " + reserved);
        }
    }

    private void validateNonNegativeThreshold(int threshold) {
        if (threshold < 0) {
            throw new InvalidStockQuantityException("El umbral mínimo de stock no puede ser negativo: " + threshold);
        }
    }

    protected void recordEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return Collections.unmodifiableList(events);
    }
}
