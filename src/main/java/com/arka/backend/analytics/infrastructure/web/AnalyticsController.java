package com.arka.backend.analytics.infrastructure.web;

import com.arka.backend.analytics.infrastructure.persistence.entity.CustomerSalesProjectionJpaEntity;
import com.arka.backend.analytics.infrastructure.persistence.entity.ProductSalesProjectionJpaEntity;
import com.arka.backend.analytics.infrastructure.persistence.entity.ReplenishmentProjectionJpaEntity;
import com.arka.backend.analytics.infrastructure.persistence.repository.CustomerSalesProjectionJpaRepository;
import com.arka.backend.analytics.infrastructure.persistence.repository.ProductSalesProjectionJpaRepository;
import com.arka.backend.analytics.infrastructure.persistence.repository.ReplenishmentProjectionJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "Analytics & Reporting")
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ProductSalesProjectionJpaRepository productSalesRepo;
    private final CustomerSalesProjectionJpaRepository customerSalesRepo;
    private final ReplenishmentProjectionJpaRepository replenishmentRepo;

    @Operation(summary = "Reporte de ventas semanales", description = "Obtiene métricas agregadas de facturación, top productos y clientes en formato JSON o CSV (HU7)")
    @GetMapping("/reports/sales")
    public ResponseEntity<?> getSalesReport(@RequestParam(name = "format", defaultValue = "json") String format) {
        List<ProductSalesProjectionJpaEntity> topProducts = productSalesRepo.findAllByOrderByTotalUnitsSoldDesc();
        List<CustomerSalesProjectionJpaEntity> topCustomers = customerSalesRepo.findAllByOrderByTotalOrdersCountDesc();

        BigDecimal grandTotal = topProducts.stream()
                .map(ProductSalesProjectionJpaEntity::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalUnits = topProducts.stream()
                .mapToInt(ProductSalesProjectionJpaEntity::getTotalUnitsSold)
                .sum();

        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder csv = new StringBuilder("Tipo,Identificador,Metrica1,Metrica2\n");
            csv.append("TOTAL_GENERAL,GLOBAL,").append(totalUnits).append(",").append(grandTotal).append("\n");
            for (var p : topProducts) {
                csv.append("PRODUCTO,").append(p.getProductId()).append(",")
                        .append(p.getTotalUnitsSold()).append(",").append(p.getTotalRevenue()).append("\n");
            }
            for (var c : topCustomers) {
                csv.append("CLIENTE,").append(c.getCustomerId()).append(",")
                        .append(c.getTotalOrdersCount()).append(",").append(c.getTotalSpent()).append("\n");
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_ventas.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv.toString());
        }

        return ResponseEntity.ok(Map.of(
                "totalRevenue", grandTotal,
                "totalUnitsSold", totalUnits,
                "topProducts", topProducts,
                "topCustomers", topCustomers
        ));
    }

    @Operation(summary = "Reporte de productos por abastecer", description = "Lista productos en umbral crítico para compras y logística en JSON o CSV (HU3)")
    @GetMapping("/reports/replenishment")
    public ResponseEntity<?> getReplenishmentReport(@RequestParam(name = "format", defaultValue = "json") String format) {
        List<ReplenishmentProjectionJpaEntity> items = replenishmentRepo.findAllByOrderByCurrentStockAsc();

        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder csv = new StringBuilder("ProductoId,StockActual,UmbralMinimo,FechaDeteccion\n");
            for (var i : items) {
                csv.append(i.getProductId()).append(",")
                        .append(i.getCurrentStock()).append(",")
                        .append(i.getMinimumThreshold()).append(",")
                        .append(i.getUpdatedAt()).append("\n");
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_abastecimiento.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv.toString());
        }

        return ResponseEntity.ok(Map.of(
                "totalProductsToReplenish", items.size(),
                "items", items
        ));
    }
}
