package com.arka.backend.inventory.infrastructure.web;

import com.arka.backend.inventory.domain.model.aggregate.InventoryItem;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.domain.port.in.AdjustStockUseCase;
import com.arka.backend.inventory.domain.port.in.GetInventoryQuery;
import com.arka.backend.inventory.infrastructure.web.dto.AdjustStockRequest;
import com.arka.backend.inventory.infrastructure.web.dto.InventoryResponse;
import com.arka.backend.inventory.infrastructure.web.mapper.InventoryWebMapper;
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

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final AdjustStockUseCase adjustStockUseCase;
    private final GetInventoryQuery getInventoryQuery;
    private final InventoryWebMapper inventoryWebMapper;

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAllInventory() {
        List<InventoryItem> items = getInventoryQuery.getAll();
        return ResponseEntity.ok(items.stream().map(inventoryWebMapper::toResponse).toList());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable UUID productId) {
        InventoryItem item = getInventoryQuery.getByProductId(new ProductId(productId));
        return ResponseEntity.ok(inventoryWebMapper.toResponse(item));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> getLowStockItems() {
        List<InventoryItem> items = getInventoryQuery.getLowStockItems();
        return ResponseEntity.ok(items.stream().map(inventoryWebMapper::toResponse).toList());
    }

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
