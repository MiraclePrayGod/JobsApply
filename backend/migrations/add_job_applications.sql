-- Migración: Agregar sistema de aplicaciones de trabajadores
-- Fecha: 2025-01-12

-- 1. Crear tabla job_applications
CREATE TABLE IF NOT EXISTS job_applications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    job_id INT NOT NULL,
    worker_id INT NOT NULL,
    is_accepted BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    FOREIGN KEY (worker_id) REFERENCES workers(id) ON DELETE CASCADE,
    UNIQUE KEY unique_application (job_id, worker_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Agregar campo application_id a la tabla messages
ALTER TABLE messages 
ADD COLUMN application_id INT NULL AFTER job_id,
ADD FOREIGN KEY (application_id) REFERENCES job_applications(id) ON DELETE CASCADE;

-- 3. Crear índices para mejorar rendimiento
CREATE INDEX idx_job_applications_job_id ON job_applications(job_id);
CREATE INDEX idx_job_applications_worker_id ON job_applications(worker_id);
CREATE INDEX idx_messages_application_id ON messages(application_id);

