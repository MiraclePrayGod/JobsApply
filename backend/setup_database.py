"""
Script maestro para configurar completamente la base de datos
Ejecuta todas las migraciones y seeders en el orden correcto
Ejecutar: python setup_database.py
"""
import sys
import os
# Configurar encoding UTF-8 para Windows
if sys.platform == 'win32':
    os.system('chcp 65001 >nul 2>&1')
    sys.stdout.reconfigure(encoding='utf-8') if hasattr(sys.stdout, 'reconfigure') else None

from app.database import engine, Base, SessionLocal
from app.models import (
    User, Worker, Job, Commission,
    JobEvidence, JobNotes, Rating, JobApplication, Message, WorkerSubscription
)
from sqlalchemy import text, inspect

def check_database_exists():
    """Verifica si la base de datos existe y está accesible"""
    try:
        with engine.connect() as conn:
            result = conn.execute(text("SELECT DATABASE()"))
            db_name = result.scalar()
            if db_name:
                print(f"[OK] Conectado a la base de datos: {db_name}")
                return True
            else:
                print("[ERROR] No hay base de datos seleccionada")
                return False
    except Exception as e:
        print(f"[ERROR] Error al conectar con la base de datos: {e}")
        print("\n[TIP] Asegurate de que:")
        print("   1. Laragon esté ejecutándose")
        print("   2. MySQL esté activo en Laragon")
        print("   3. La base de datos 'getjob_db' esté creada")
        print("   4. El archivo .env tenga la configuración correcta")
        return False

def create_tables():
    """Crea todas las tablas en la base de datos"""
    print("\n" + "="*60)
    print("PASO 1: Creando tablas...")
    print("="*60)
    try:
        Base.metadata.create_all(bind=engine)
        print("[OK] Tablas creadas exitosamente")
        return True
    except Exception as e:
        print(f"[ERROR] Error al crear tablas: {e}")
        return False

def migrate_add_worker_plus_fields():
    """Agrega campos is_plus_active y plus_expires_at a workers y crea tabla worker_subscriptions"""
    with engine.connect() as conn:
        try:
            # Verificar si las columnas ya existen
            inspector = inspect(engine)
            worker_columns = [col['name'] for col in inspector.get_columns('workers')]
            
            # Agregar is_plus_active si no existe
            if 'is_plus_active' not in worker_columns:
                conn.execute(text("ALTER TABLE workers ADD COLUMN is_plus_active TINYINT(1) NOT NULL DEFAULT 0"))
                conn.commit()
                print("  ✓ Columna is_plus_active agregada")
            else:
                print("  ℹ Columna is_plus_active ya existe")
            
            # Agregar plus_expires_at si no existe
            if 'plus_expires_at' not in worker_columns:
                conn.execute(text("ALTER TABLE workers ADD COLUMN plus_expires_at DATETIME NULL"))
                conn.commit()
                print("  ✓ Columna plus_expires_at agregada")
            else:
                print("  ℹ Columna plus_expires_at ya existe")
            
            # Crear tabla worker_subscriptions si no existe
            tables = inspector.get_table_names()
            if 'worker_subscriptions' not in tables:
                conn.execute(text("""
                    CREATE TABLE worker_subscriptions (
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
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """))
                conn.commit()
                print("  ✓ Tabla worker_subscriptions creada")
            else:
                print("  ℹ Tabla worker_subscriptions ya existe")
                
        except Exception as e:
            conn.rollback()
            # Si el error es porque ya existe, no es crítico
            if "Duplicate column name" in str(e) or "already exists" in str(e).lower():
                print(f"  ℹ {str(e)}")
            else:
                raise

def run_migrations():
    """Ejecuta todas las migraciones necesarias"""
    print("\n" + "="*60)
    print("PASO 2: Ejecutando migraciones...")
    print("="*60)
    
    migrations = [
        ("migrate_add_user_fields", "Agregando campos full_name y phone a users"),
        ("migrate_add_worker_verification_fields", "Agregando campos de verificacion a workers"),
        ("migrate_add_job_applications", "Agregando sistema de aplicaciones de trabajadores"),
        ("migrate_add_worker_plus_fields", "Agregando campos Plus a workers y tabla subscriptions"),
        ("migrate_add_user_profile_image", "Agregando campo profile_image_url a users")
    ]
    
    success = True
    for migration_name, description in migrations:
        try:
            print(f"\n[MIGRACION] {description}...")
            # Importar y ejecutar la migración dinámicamente
            if migration_name == "migrate_add_user_fields":
                from migrate_add_user_fields import migrate_add_user_fields
                migrate_add_user_fields()
            elif migration_name == "migrate_add_worker_verification_fields":
                from migrate_add_worker_verification_fields import migrate_add_worker_verification_fields
                migrate_add_worker_verification_fields()
            elif migration_name == "migrate_add_job_applications":
                from migrate_add_job_applications import migrate_add_job_applications
                migrate_add_job_applications()
            elif migration_name == "migrate_add_worker_plus_fields":
                migrate_add_worker_plus_fields()
            elif migration_name == "migrate_add_user_profile_image":
                from migrate_add_user_profile_image import migrate_add_user_profile_image
                migrate_add_user_profile_image()
            print(f"[OK] {description} - Completado")
        except Exception as e:
            if "ya existe" in str(e).lower() or "already exists" in str(e).lower() or "Duplicate" in str(e):
                print(f"[INFO] {description} - Ya estaba aplicada")
            else:
                print(f"[ERROR] Error en {description}: {e}")
                import traceback
                traceback.print_exc()
                success = False
    
    return success

def seed_data():
    """Pobla la base de datos con datos de prueba"""
    print("\n" + "="*60)
    print("PASO 3: Poblando base de datos con datos de prueba...")
    print("="*60)
    try:
        from seed_data import seed_data as run_seed_data
        run_seed_data()
        return True
    except Exception as e:
        print(f"[ERROR] Error al poblar datos: {e}")
        import traceback
        traceback.print_exc()
        return False

def show_summary():
    """Muestra un resumen de lo que se creó"""
    print("\n" + "="*60)
    print("RESUMEN DE LA BASE DE DATOS")
    print("="*60)
    
    db = SessionLocal()
    try:
        print(f"Usuarios: {db.query(User).count()}")
        print(f"Trabajadores: {db.query(Worker).count()}")
        print(f"Trabajos: {db.query(Job).count()}")
        print(f"Aplicaciones: {db.query(JobApplication).count()}")
        print(f"Comisiones: {db.query(Commission).count()}")
        print(f"Ratings: {db.query(Rating).count()}")
        print(f"Mensajes: {db.query(Message).count()}")
    except Exception as e:
        print(f"[WARNING] Error al obtener resumen: {e}")
    finally:
        db.close()

def main():
    """Función principal que ejecuta todo el proceso"""
    print("INICIANDO CONFIGURACION DE BASE DE DATOS")
    print("="*60)
    
    # Verificar conexión
    if not check_database_exists():
        sys.exit(1)
    
    # Crear tablas
    if not create_tables():
        print("\n[ERROR] No se pudieron crear las tablas. Abortando...")
        sys.exit(1)
    
    # Ejecutar migraciones
    run_migrations()
    
    # Preguntar si quiere poblar con datos
    print("\n" + "="*60)
    respuesta = input("Deseas poblar la base de datos con datos de prueba? (s/n): ").lower().strip()
    
    if respuesta in ['s', 'si', 'si', 'y', 'yes']:
        if not seed_data():
            print("\n[WARNING] Hubo errores al poblar datos, pero las tablas estan creadas")
    else:
        print("[INFO] Saltando poblacion de datos")
    
    # Mostrar resumen
    show_summary()
    
    print("\n" + "="*60)
    print("[OK] CONFIGURACION COMPLETADA")
    print("="*60)
    print("\nCREDENCIALES DE PRUEBA (si ejecutaste seed_data):")
    print("   Cliente: cliente1@test.com / password123")
    print("   Trabajador: trabajador1@test.com / password123")
    print("   Manager: manager@test.com / password123")
    print("\nAhora puedes ejecutar el servidor:")
    print("   uvicorn app.main:app --reload")

if __name__ == "__main__":
    main()

