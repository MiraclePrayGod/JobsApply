-- Script para crear la base de datos en MySQL (Laragon)
-- Ejecutar este script desde HeidiSQL o phpMyAdmin

CREATE DATABASE IF NOT EXISTS getjob_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Verificar que se creó correctamente
USE getjob_db;
SELECT 'Base de datos getjob_db creada exitosamente' AS mensaje;

