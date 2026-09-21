package com.arka.backend.inventory.domain;

import com.arka.backend.inventory.domain.exception.InsufficientStockException;
import com.arka.backend.inventory.domain.exception.InvalidStockQuantityException;
import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.event.LowStockDetectedEvent;
import com.arka.backend.inventory.domain.model.event.StockPermanentlyDeductedEvent;
import com.arka.backend.inventory.domain.model.event.StockReservationAdjustedEvent;
import com.arka.backend.inventory.domain.model.event.StockReservedEvent;
import com.arka.backend.inventory.domain.model.valueobject.InventoryId;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.shared.domain.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Nivel 1: Pruebas de Dominio Puro - InventoryItem (Mutation-Ready)")
class InventoryItemDomainTest {

    @Test
    @DisplayName("Debe inicializar inventario y emitir eventos correctamente")
    void shouldInitializeInventoryItem() {
        InventoryId id = InventoryId.generate();
        ProductId productId = ProductId.generate();

        InventoryItem item = InventoryItem.initialize(id, productId, 50, 10);

        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getPhysicalStock()).isEqualTo(50);
        assertThat(item.getReservedStock()).isEqualTo(0);
        assertThat(item.getAvailableStock()).isEqualTo(50);
        assertThat(item.isLowStock()).isFalse();

        List<DomainEvent> events = item.pullDomainEvents();
        assertThat(events).isNotEmpty();
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("INV-02: No debe permitir stock físico negativo")
    void shouldRejectNegativePhysicalStock() {
        InventoryId id = InventoryId.generate();
        ProductId productId = ProductId.generate();

        assertThatThrownBy(() -> new InventoryItem(id, productId, -1, 0, 10))
                .isInstanceOf(InvalidStockQuantityException.class)
                .hasMessageContaining("INV-02");

        InventoryItem item = InventoryItem.initialize(id, productId, 10, 5);
        assertThatThrownBy(() -> item.adjustPhysicalStock(-5, "Ajuste erróneo"))
                .isInstanceOf(InvalidStockQuantityException.class)
                .hasMessageContaining("INV-02");
    }

    @ParameterizedTest(name = "Disponible {0}, Solicitado {1} -> Debe permitir reserva: {2}")
    @CsvSource({
            "10, 10, true",   // Límite exacto: disponible == solicitado
            "10, 9, true",    // Dentro del límite
            "10, 11, false",  // Límite exacto: disponible + 1 -> InsufficientStockException
            "1, 1, true",     // Valor frontera mínimo
            "0, 1, false"     // Stock cero
    })
    @DisplayName("INV-03: Boundary testing para reserva anti-sobreventa")
    void shouldValidateStockReservationBoundaries(int physicalStock, int requestedQuantity, boolean shouldSucceed) {
        InventoryItem item = new InventoryItem(InventoryId.generate(), ProductId.generate(), physicalStock, 0, 5);
        UUID orderId = UUID.randomUUID();

        if (shouldSucceed) {
            item.reserveStock(orderId, requestedQuantity);
            assertThat(item.getReservedStock()).isEqualTo(requestedQuantity);
            assertThat(item.getAvailableStock()).isEqualTo(physicalStock - requestedQuantity);

            List<DomainEvent> events = item.pullDomainEvents();
            assertThat(events).anyMatch(e -> e instanceof StockReservedEvent);
        } else {
            assertThatThrownBy(() -> item.reserveStock(orderId, requestedQuantity))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("INV-03");
        }
    }

    @Test
    @DisplayName("Debe ajustar la reserva incrementando o decrementando correctamente")
    void shouldAdjustReservation() {
        InventoryItem item = new InventoryItem(InventoryId.generate(), ProductId.generate(), 50, 10, 5);
        UUID orderId = UUID.randomUUID();

        // Incrementa de 10 a 15 (delta = +5)
        item.adjustReservation(orderId, 10, 15);
        assertThat(item.getReservedStock()).isEqualTo(15);
        assertThat(item.getAvailableStock()).isEqualTo(35);

        // Decrementa de 15 a 8 (delta = -7)
        item.adjustReservation(orderId, 15, 8);
        assertThat(item.getReservedStock()).isEqualTo(8);
        assertThat(item.getAvailableStock()).isEqualTo(42);

        List<DomainEvent> events = item.pullDomainEvents();
        assertThat(events).anyMatch(e -> e instanceof StockReservationAdjustedEvent);
    }

    @Test
    @DisplayName("Debe descontar stock físico permanentemente tras confirmación")
    void shouldDeductStockPermanently() {
        InventoryItem item = new InventoryItem(InventoryId.generate(), ProductId.generate(), 50, 10, 5);

        item.deductPermanent(10);

        assertThat(item.getPhysicalStock()).isEqualTo(40);
        assertThat(item.getReservedStock()).isEqualTo(0);
        assertThat(item.getAvailableStock()).isEqualTo(40);

        List<DomainEvent> events = item.pullDomainEvents();
        assertThat(events).anyMatch(e -> e instanceof StockPermanentlyDeductedEvent);
    }

    @Test
    @DisplayName("Debe liberar reserva de stock ante cancelación")
    void shouldReleaseStockReservation() {
        InventoryItem item = new InventoryItem(InventoryId.generate(), ProductId.generate(), 50, 10, 5);

        item.releaseReservation(10);

        assertThat(item.getPhysicalStock()).isEqualTo(50);
        assertThat(item.getReservedStock()).isEqualTo(0);
        assertThat(item.getAvailableStock()).isEqualTo(50);
    }

    @ParameterizedTest(name = "Stock disponible {0} con umbral {1} -> Alerta activa: {2}")
    @CsvSource({
            "10, 10, true",  // Límite exacto: disponible == umbral -> ALERTA
            "9, 10, true",   // Por debajo -> ALERTA
            "11, 10, false"  // Límite exacto: disponible > umbral -> NO ALERTA
    })
    @DisplayName("INV-08: Boundary testing para detección de stock bajo")
    void shouldTriggerLowStockAlertAtExactBoundaries(int availableStock, int threshold, boolean expectedAlert) {
        InventoryItem item = new InventoryItem(InventoryId.generate(), ProductId.generate(), availableStock, 0, threshold);
        assertThat(item.isLowStock()).isEqualTo(expectedAlert);

        item.adjustPhysicalStock(availableStock, "Verificación de umbral");
        List<DomainEvent> events = item.pullDomainEvents();

        if (expectedAlert) {
            assertThat(events).anyMatch(e -> e instanceof LowStockDetectedEvent);
        }
    }
}
