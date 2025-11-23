"""
Script de migración para agregar las columnas full_name y phone a la tabla users
"""
from app.database import engine, Base
from app.models.user import User
from sqlalchemy import text

def migrate_add_user_fields():
    """Agrega las columnas full_name y phone a la tabla users"""
    with engine.connect() as conn:
        try:
            # Verificar si las columnas ya existen
            result = conn.execute(text("""
                SELECT COLUMN_NAME 
                FROM INFORMATION_SCHEMA.COLUMNS 
                WHERE TABLE_SCHEMA = DATABASE() 
                AND TABLE_NAME = 'users' 
                AND COLUMN_NAME IN ('full_name', 'phone')
            """))
            existing_columns = [row[0] for row in result]
            
            # Agregar full_name si no existe
            if 'full_name' not in existing_columns:
                print("Agregando columna full_name...")
                conn.execute(text("""
                    ALTER TABLE users 
                    ADD COLUMN full_name VARCHAR(255) NULL 
                    AFTER role
                """))
                conn.commit()
                print("✓ Columna full_name agregada")
            else:
                print("✓ Columna full_name ya existe")
            
            # Agregar phone si no existe
            if 'phone' not in existing_columns:
                print("Agregando columna phone...")
                conn.execute(text("""
                    ALTER TABLE users 
                    ADD COLUMN phone VARCHAR(20) NULL 
                    AFTER full_name
                """))
                conn.commit()
                print("✓ Columna phone agregada")
            else:
                print("✓ Columna phone ya existe")
            
            print("\n✅ Migración completada exitosamente")
            
        except Exception as e:
            print(f"❌ Error en la migración: {e}")
            conn.rollback()
            raise

if __name__ == "__main__":
    print("Iniciando migración para agregar full_name y phone a users...")
    migrate_add_user_fields()

