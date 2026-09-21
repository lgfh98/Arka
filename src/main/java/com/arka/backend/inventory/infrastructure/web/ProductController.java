package com.arka.backend.inventory.infrastructure.web;

import com.arka.backend.inventory.domain.model.aggregate.Product;
import com.arka.backend.inventory.domain.model.valueobject.ProductId;
import com.arka.backend.inventory.domain.port.in.GetProductQuery;
import com.arka.backend.inventory.domain.port.in.RegisterProductUseCase;
import com.arka.backend.inventory.infrastructure.web.dto.CreateProductRequest;
import com.arka.backend.inventory.infrastructure.web.dto.ProductResponse;
import com.arka.backend.inventory.infrastructure.web.mapper.ProductWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final RegisterProductUseCase registerProductUseCase;
    private final GetProductQuery getProductQuery;
    private final ProductWebMapper productWebMapper;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        var command = new RegisterProductUseCase.RegisterProductCommand(
                request.name(),
                request.description(),
                request.price(),
                request.category(),
                request.attributes(),
                request.initialStock(),
                request.minimumThreshold()
        );

        Product created = registerProductUseCase.registerProduct(command);
        ProductResponse response = productWebMapper.toResponse(created);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId().value())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(
            @RequestParam(name = "category", required = false) String category) {
        List<Product> products = (category != null && !category.isBlank())
                ? getProductQuery.getByCategory(category)
                : getProductQuery.getAll();

        return ResponseEntity.ok(products.stream().map(productWebMapper::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        Product product = getProductQuery.getById(new ProductId(id));
        return ResponseEntity.ok(productWebMapper.toResponse(product));
    }
}
