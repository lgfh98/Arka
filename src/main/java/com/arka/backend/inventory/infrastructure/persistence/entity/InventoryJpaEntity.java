package com.arka.backend.inventory.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_items", schema = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    @Column(name = "physical_stock", nullable = false)
    private int physicalStock;

    @Column(name = "reserved_stock", nullable = false)
    private int reservedStock;

    @Column(name = "minimum_threshold", nullable = false)
    private int minimumThreshold;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
