-- =====================================================
-- Migración: Agregar campos Plus a Workers
-- Fecha: 2024-11-21
-- Descripción: Agrega campos is_plus_active y plus_expires_at a la tabla workers
--              y crea la tabla worker_subscriptions si no existe
-- =====================================================

-- 1. Agregar columnas Plus a la tabla workers
-- Verificar si las columnas ya existen antes de agregarlas
ALTER TABLE workers
    ADD COLUMN IF NOT EXISTS is_plus_active TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS plus_expires_at DATETIME NULL;

-- Nota: Si tu versión de MySQL no soporta "IF NOT EXISTS" en ALTER TABLE,
-- ejecuta estas líneas manualmente y omite las que den error de columna duplicada:
-- ALTER TABLE workers ADD COLUMN is_plus_active TINYINT(1) NOT NULL DEFAULT 0;
-- ALTER TABLE workers ADD COLUMN plus_expires_at DATETIME NULL;

-- 2. Crear tabla worker_subscriptions si no existe
CREATE TABLE IF NOT EXISTS worker_subscriptions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    worker_id INT NOT NULL,
    plan ENUM('daily', 'weekly') NOT NULL,
    days INT NOT NULL COMMENT '1 o 7 días',
    amount DECIMAL(10, 2) NOT NULL COMMENT '2.00 o 12.00',
    status ENUM('pending', 'active', 'expired', 'cancelled') NOT NULL DEFAULT 'active',
    payment_method VARCHAR(50) DEFAULT 'yape',
    payment_code VARCHAR(50) NULL COMMENT 'código simulado de Yape',
    valid_from DATETIME NOT NULL,
    valid_until DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (worker_id) REFERENCES workers(id) ON DELETE CASCADE,
    INDEX idx_worker_id (worker_id),
    INDEX idx_status (status),
    INDEX idx_valid_until (valid_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Verificar que las columnas se agregaron correctamente
-- (Opcional: ejecuta esto para confirmar)
-- DESCRIBE workers;
-- SHOW TABLES LIKE 'worker_subscriptions';

