"""
Migración: Agregar campo profile_image_url a Users
Ejecutar: python migrate_add_user_profile_image.py
"""
import sys
import os
from sqlalchemy import create_engine, text, inspect
from app.config import settings

# Configurar encoding UTF-8 para Windows
if sys.platform == 'win32':
    os.system('chcp 65001 >nul 2>&1')
    sys.stdout.reconfigure(encoding='utf-8') if hasattr(sys.stdout, 'reconfigure') else None

def migrate_add_user_profile_image():
    """Agrega el campo profile_image_url a la tabla users"""
    engine = create_engine(settings.database_url)
    
    with engine.connect() as conn:
        try:
            inspector = inspect(engine)
            user_columns = [col['name'] for col in inspector.get_columns('users')]
            
            # Agregar profile_image_url si no existe
            if 'profile_image_url' not in user_columns:
                conn.execute(text("ALTER TABLE users ADD COLUMN profile_image_url VARCHAR(500) NULL"))
                conn.commit()
                print("  ✓ Columna profile_image_url agregada a users")
            else:
                print("  ℹ Columna profile_image_url ya existe en users")
                
        except Exception as e:
            conn.rollback()
            if "Duplicate column name" in str(e) or "already exists" in str(e).lower():
                print(f"  ℹ Columna profile_image_url ya existe (ignorando error)")
            else:
                print(f"  ❌ Error: {e}")
                raise

if __name__ == "__main__":
    print("="*60)
    print("MIGRACIÓN: Agregar profile_image_url a Users")
    print("="*60)
    migrate_add_user_profile_image()
    print("="*60)
    print("✅ Migración completada")

