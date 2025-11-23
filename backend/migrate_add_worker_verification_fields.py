"""
Script de migración para agregar las columnas is_verified y verification_photo_url a la tabla workers
"""
from app.database import engine, Base
from app.models.worker import Worker
from sqlalchemy import text

def migrate_add_worker_verification_fields():
    """Agrega las columnas is_verified y verification_photo_url a la tabla workers"""
    with engine.connect() as conn:
        try:
            # Verificar si las columnas ya existen
            result = conn.execute(text("""
                SELECT COLUMN_NAME 
                FROM INFORMATION_SCHEMA.COLUMNS 
                WHERE TABLE_SCHEMA = DATABASE() 
                AND TABLE_NAME = 'workers' 
                AND COLUMN_NAME IN ('is_verified', 'verification_photo_url')
            """))
            existing_columns = [row[0] for row in result]
            
            # Agregar is_verified si no existe
            if 'is_verified' not in existing_columns:
                print("Agregando columna is_verified...")
                conn.execute(text("""
                    ALTER TABLE workers 
                    ADD COLUMN is_verified BOOLEAN DEFAULT FALSE 
                    AFTER profile_image_url
                """))
                conn.commit()
                print("✓ Columna is_verified agregada")
            else:
                print("✓ Columna is_verified ya existe")
            
            # Agregar verification_photo_url si no existe
            if 'verification_photo_url' not in existing_columns:
                print("Agregando columna verification_photo_url...")
                conn.execute(text("""
                    ALTER TABLE workers 
                    ADD COLUMN verification_photo_url VARCHAR(500) NULL 
                    AFTER is_verified
                """))
                conn.commit()
                print("✓ Columna verification_photo_url agregada")
            else:
                print("✓ Columna verification_photo_url ya existe")
            
            print("\n✅ Migración completada exitosamente")
            
        except Exception as e:
            print(f"❌ Error en la migración: {e}")
            conn.rollback()
            raise

if __name__ == "__main__":
    print("Iniciando migración para agregar is_verified y verification_photo_url a workers...")
    migrate_add_worker_verification_fields()







