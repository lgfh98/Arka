-- =========================================================================
-- ARKA DATABASE SCHEMAS DEFINITION (DDL)
-- Aislamiento Físico por Bounded Contexts y Kernel de Resiliencia
-- =========================================================================

CREATE SCHEMA IF NOT EXISTS inventory;
CREATE SCHEMA IF NOT EXISTS ordering;
CREATE SCHEMA IF NOT EXISTS cart;
CREATE SCHEMA IF NOT EXISTS notification;
CREATE SCHEMA IF NOT EXISTS analytics;
CREATE SCHEMA IF NOT EXISTS shared;
