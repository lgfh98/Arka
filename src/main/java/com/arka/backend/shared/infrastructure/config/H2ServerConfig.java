package com.arka.backend.shared.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

/**
 * Configuración para habilitar el servidor TCP de H2 en desarrollo local.
 * Permite que herramientas externas como DataGrip o DBeaver se conecten a la base de datos
 * en memoria (mem:arkadb) mientras la aplicación backend está en ejecución.
 *
 * JDBC URL de conexión externa: jdbc:h2:tcp://localhost:9092/mem:arkadb
 */
@Slf4j
@Configuration
public class H2ServerConfig {

    @Bean(destroyMethod = "stop")
    @ConditionalOnProperty(prefix = "spring.h2.tcp", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Server h2TcpServer(@Value("${spring.h2.tcp.port:9092}") String port) {
        try {
            Server server = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", port);
            server.start();
            log.info("H2 TCP Server iniciado exitosamente en el puerto {} para conexiones externas (DataGrip/DBeaver)", port);
            return server;
        } catch (SQLException e) {
            log.warn("H2 TCP Server no se inició en el puerto {} (posiblemente ya está en ejecución en otro contexto o proceso): {}", port, e.getMessage());
            return null;
        }
    }
}
