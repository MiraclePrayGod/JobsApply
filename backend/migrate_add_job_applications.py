"""
Script de migración para agregar el sistema de aplicaciones de trabajadores
Ejecutar: python migrate_add_job_applications.py
"""
from app.database import engine
from sqlalchemy import text

def migrate_add_job_applications():
    """Agrega la tabla job_applications y el campo application_id a messages"""
    with engine.connect() as conn:
        try:
            # 1. Crear tabla job_applications
            print("Creando tabla job_applications...")
            conn.execute(text("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """))
            conn.commit()
            print("[OK] Tabla job_applications creada")
            
            # 2. Verificar si application_id ya existe en messages
            result = conn.execute(text("""
                SELECT COLUMN_NAME 
                FROM INFORMATION_SCHEMA.COLUMNS 
                WHERE TABLE_SCHEMA = DATABASE() 
                AND TABLE_NAME = 'messages' 
                AND COLUMN_NAME = 'application_id'
            """))
            has_application_id = result.fetchone() is not None
            
            if not has_application_id:
                print("Agregando columna application_id a messages...")
                conn.execute(text("""
                    ALTER TABLE messages 
                    ADD COLUMN application_id INT NULL AFTER job_id
                """))
                conn.commit()
                print("[OK] Columna application_id agregada a messages")
                
                # Agregar foreign key
                print("Agregando foreign key application_id...")
                conn.execute(text("""
                    ALTER TABLE messages 
                    ADD FOREIGN KEY (application_id) REFERENCES job_applications(id) ON DELETE CASCADE
                """))
                conn.commit()
                print("[OK] Foreign key application_id agregada")
            else:
                print("[OK] Columna application_id ya existe en messages")
            
            # 3. Crear índices (verificar si ya existen)
            print("Creando índices...")
            
            # Índice para job_applications.job_id
            try:
                conn.execute(text("""
                    CREATE INDEX idx_job_applications_job_id ON job_applications(job_id)
                """))
                conn.commit()
                print("[OK] Indice idx_job_applications_job_id creado")
            except Exception as e:
                if "Duplicate key name" in str(e):
                    print("[OK] Indice idx_job_applications_job_id ya existe")
                else:
                    raise
            
            # Índice para job_applications.worker_id
            try:
                conn.execute(text("""
                    CREATE INDEX idx_job_applications_worker_id ON job_applications(worker_id)
                """))
                conn.commit()
                print("[OK] Indice idx_job_applications_worker_id creado")
            except Exception as e:
                if "Duplicate key name" in str(e):
                    print("[OK] Indice idx_job_applications_worker_id ya existe")
                else:
                    raise
            
            # Índice para messages.application_id
            try:
                conn.execute(text("""
                    CREATE INDEX idx_messages_application_id ON messages(application_id)
                """))
                conn.commit()
                print("[OK] Indice idx_messages_application_id creado")
            except Exception as e:
                if "Duplicate key name" in str(e):
                    print("[OK] Indice idx_messages_application_id ya existe")
                else:
                    raise
            
            print("\n[OK] Migracion completada exitosamente")
            
        except Exception as e:
            print(f"[ERROR] Error en la migracion: {e}")
            conn.rollback()
            raise

if __name__ == "__main__":
    print("Iniciando migración para agregar sistema de aplicaciones de trabajadores...")
    print("=" * 60)
    migrate_add_job_applications()
    print("=" * 60)

