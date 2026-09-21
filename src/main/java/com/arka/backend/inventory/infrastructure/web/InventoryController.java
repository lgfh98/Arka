package com.arka.backend.inventory.infrastructure.web;

import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.domain.port.in.AdjustStockUseCase;
import com.arka.backend.inventory.domain.port.in.GetInventoryQuery;
import com.arka.backend.inventory.infrastructure.web.dto.AdjustStockRequest;
import com.arka.backend.inventory.infrastructure.web.dto.InventoryResponse;
import com.arka.backend.inventory.infrastructure.web.mapper.InventoryWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Inventory & Catalog")
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final AdjustStockUseCase adjustStockUseCase;
    private final GetInventoryQuery getInventoryQuery;
    private final InventoryWebMapper inventoryWebMapper;

    @Operation(summary = "Consultar inventario global", description = "Lista todos los ítems de inventario con stock físico y reservado")
    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAllInventory() {
        List<InventoryItem> items = getInventoryQuery.getAll();
        return ResponseEntity.ok(items.stream().map(inventoryWebMapper::toResponse).toList());
    }

    @Operation(summary = "Consultar inventario por producto", description = "Obtiene stock físico, reservado y disponible de un producto")
    @ApiResponse(responseCode = "200", description = "Inventario obtenido")
    @ApiResponse(responseCode = "404", description = "Inventario no encontrado")
    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable UUID productId) {
        InventoryItem item = getInventoryQuery.getByProductId(new ProductId(productId));
        return ResponseEntity.ok(inventoryWebMapper.toResponse(item));
    }

    @Operation(summary = "Consultar productos con stock crítico", description = "Lista productos cuyo stock disponible es menor o igual al umbral mínimo (HU3)")
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> getLowStockItems() {
        List<InventoryItem> items = getInventoryQuery.getLowStockItems();
        return ResponseEntity.ok(items.stream().map(inventoryWebMapper::toResponse).toList());
    }

    @Operation(summary = "Ajustar stock de producto", description = "Modifica manualmente el stock físico con auditoría e invariante anti-negativos (HU2)")
    @ApiResponse(responseCode = "200", description = "Stock ajustado exitosamente")
    @ApiResponse(responseCode = "422", description = "Invariante violada (INV-02: Stock negativo)")
    @PutMapping("/product/{productId}/stock")
    public ResponseEntity<InventoryResponse> adjustStock(
            @PathVariable UUID productId,
            @Valid @RequestBody AdjustStockRequest request) {
        var command = new AdjustStockUseCase.AdjustStockCommand(
                new ProductId(productId),
                request.newStock(),
                request.reason()
        );
        InventoryItem updated = adjustStockUseCase.adjustStock(command);
        return ResponseEntity.ok(inventoryWebMapper.toResponse(updated));
    }
}
