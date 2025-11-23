-- =====================================================
-- Migración: Agregar campo profile_image_url a Users
-- Fecha: 2024-11-21
-- Descripción: Agrega campo profile_image_url a la tabla users
--              para permitir fotos de perfil para todos los usuarios
-- =====================================================

-- Agregar columna profile_image_url a la tabla users
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_image_url VARCHAR(500) NULL;

-- Nota: Si tu versión de MySQL no soporta "IF NOT EXISTS" en ALTER TABLE,
-- ejecuta esta línea manualmente y omite si da error de columna duplicada:
-- ALTER TABLE users ADD COLUMN profile_image_url VARCHAR(500) NULL;

-- Verificar que la columna se agregó correctamente
-- (Opcional: ejecuta esto para confirmar)
-- DESCRIBE users;

