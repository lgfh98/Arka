package com.arka.backend.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Nivel 5: Pruebas de Aceptación E2E - Flujos de Negocio B2B Arka (BDD Nativo)")
class ArkaOrderAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PRODUCT_1 = "11111111-1111-1111-1111-111111111111";
    private static final String PRODUCT_2 = "22222222-2222-2222-2222-222222222222";

    @Nested
    @DisplayName("Criterio de Aceptación: Ciclo Completo de Orden de Compra B2B (HU4, HU5, HU6)")
    class OrderLifecycleScenarios {

        @Test
        @DisplayName("GIVEN un cliente mayorista WHEN radica, modifica, confirma y despacha orden THEN el flujo progresa exitosamente")
        void shouldCompleteHappyPathOrderLifecycle() throws Exception {
            UUID customerId = UUID.randomUUID();

            // 1. Radicar orden de compra en estado PENDIENTE (HU4)
            String createOrderPayload = String.format("""
                    {
                        "customerId": "%s",
                        "items": [
                            { "productId": "%s", "quantity": 2, "unitPrice": 120.00 },
                            { "productId": "%s", "quantity": 1, "unitPrice": 250.00 }
                        ]
                    }
                    """, customerId, PRODUCT_1, PRODUCT_2);

            MvcResult createResult = mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createOrderPayload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.totalAmount").value(490.00))
                    .andExpect(jsonPath("$.items", hasSize(2)))
                    .andReturn();

            String orderId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                    .get("id").asText();

            // 2. Modificar orden mientras sigue en estado PENDIENTE (HU5)
            String modifyOrderPayload = String.format("""
                    {
                        "items": [
                            { "productId": "%s", "quantity": 3, "unitPrice": 120.00 }
                        ]
                    }
                    """, PRODUCT_1);

            mockMvc.perform(put("/api/orders/" + orderId + "/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(modifyOrderPayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.totalAmount").value(360.00))
                    .andExpect(jsonPath("$.items", hasSize(1)));

            // 3. Confirmar la orden (HU6)
            mockMvc.perform(post("/api/orders/" + orderId + "/confirm"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));

            // 4. Despachar la orden (HU6)
            mockMvc.perform(post("/api/orders/" + orderId + "/dispatch"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_DISPATCH"));

            // 5. Entregar la orden (HU6)
            mockMvc.perform(post("/api/orders/" + orderId + "/deliver"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DELIVERED"));

            // 6. Comprobar que se hayan despachado notificaciones al cliente
            mockMvc.perform(get("/api/notifications?recipient=" + customerId + "@arka-client.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(1))));
        }
    }

    @Nested
    @DisplayName("Criterio de Aceptación: Violación de Invariantes y RFC 9457 ProblemDetail")
    class InvariantViolationScenarios {

        @Test
        @DisplayName("INV-04: Intentar modificar una orden ya CONFIRMADA debe retornar 422 Unprocessable Entity")
        void shouldRejectModificationOnConfirmedOrder() throws Exception {
            UUID customerId = UUID.randomUUID();
            String createOrderPayload = String.format("""
                    {
                        "customerId": "%s",
                        "items": [ { "productId": "%s", "quantity": 1, "unitPrice": 120.00 } ]
                    }
                    """, customerId, PRODUCT_1);

            MvcResult result = mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createOrderPayload))
                    .andExpect(status().isCreated())
                    .andReturn();

            String orderId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("id").asText();

            // Confirmar la orden
            mockMvc.perform(post("/api/orders/" + orderId + "/confirm"))
                    .andExpect(status().isOk());

            // Intentar modificar
            String modifyPayload = String.format("""
                    {
                        "items": [ { "productId": "%s", "quantity": 5, "unitPrice": 120.00 } ]
                    }
                    """, PRODUCT_1);

            mockMvc.perform(put("/api/orders/" + orderId + "/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(modifyPayload))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.title").value("Violación de Invariante de Dominio"))
                    .andExpect(jsonPath("$.detail", containsString("INV-04")));
        }

        @Test
        @DisplayName("INV-06: Intentar crear una orden sin productos debe retornar 400 Bad Request")
        void shouldRejectEmptyOrder() throws Exception {
            String emptyOrderPayload = """
                    {
                        "customerId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                        "items": []
                    }
                    """;

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(emptyOrderPayload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Solicitud Inválida"));
        }

        @Test
        @DisplayName("Swagger OpenAPI: Debe exponer la documentación en /v3/api-docs y Swagger UI")
        void shouldExposeOpenApiDocs() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.openapi").exists())
                    .andExpect(jsonPath("$.info.title").value("Arka B2B - Wholesale Distribution Platform API"))
                    .andExpect(jsonPath("$.paths['/api/orders']").exists())
                    .andExpect(jsonPath("$.paths['/api/products']").exists())
                    .andExpect(jsonPath("$.paths['/api/inventory']").exists())
                    .andExpect(jsonPath("$.paths['/api/carts/{customerId}/items']").exists())
                    .andExpect(jsonPath("$.paths['/api/notifications']").exists())
                    .andExpect(jsonPath("$.paths['/api/analytics/reports/sales']").exists());
        }
    }
}
