package com.arka.backend.shared.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI arkaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Arka B2B - Wholesale Distribution Platform API")
                        .version("1.0.0")
                        .description("""
                                ### 🛒 Arka Backend MVP API Documentation
                                Plataforma de distribución mayorista de accesorios para PC en Colombia y Latinoamérica.
                                
                                **Arquitectura**:
                                - Monolito Modular con **Minimal DDD** (*Domain + Infrastructure*) en Java 25.
                                - **Transactional Outbox & Idempotent Consumer** para comunicación reactiva y desacoplada.
                                - Respuestas de error estandarizadas bajo **RFC 9457 ProblemDetail** (`application/problem+json`).
                                
                                **Bounded Contexts**:
                                1. 🏭 **Inventory & Catalog** (HU1, HU2, HU3)
                                2. 📦 **Ordering** (HU4, HU5, HU6)
                                3. 🛒 **Cart & Abandonment** (HU8)
                                4. 🔔 **Notification** (HU6, HU8)
                                5. 📊 **Analytics & Reporting** (HU7, HU3)
                                """)
                        .contact(new Contact()
                                .name("Arka Engineering Team")
                                .email("dev@arka.com")
                                .url("https://github.com/lgfh98/Arka"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://arka.com/terms")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Entorno de Desarrollo Local")
                ))
                .tags(List.of(
                        new Tag().name("Inventory & Catalog").description("Gestión de catálogo de productos, inventario físico, reservas y alertas de abastecimiento"),
                        new Tag().name("Ordering").description("Radicación de órdenes B2B, modificación de pedidos pendientes y transiciones del ciclo de vida"),
                        new Tag().name("Cart & Abandonment").description("Gestión de carritos de compra y detección/recuperación de carritos abandonados"),
                        new Tag().name("Notification").description("Consulta y registro de notificaciones multicanal despachadas por eventos"),
                        new Tag().name("Analytics & Reporting").description("Proyecciones CQRS de ventas semanales y reportes de reposición (JSON / CSV)")
                ));
    }
}
