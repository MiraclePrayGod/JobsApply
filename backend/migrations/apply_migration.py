#!/usr/bin/env python3
"""
Script para aplicar migraciones manuales a la base de datos.

Uso:
    python apply_migration.py migration_2024_11_21_add_worker_plus_fields.sql

O ejecuta el SQL directamente en MySQL:
    mysql -u usuario -p nombre_bd < migration_2024_11_21_add_worker_plus_fields.sql
"""
import sys
import os
from pathlib import Path

# Agregar el directorio raíz del proyecto al path
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from app.database import engine
from app.models.subscription import WorkerSubscription
from app.database import Base
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def create_worker_subscriptions_table():
    """Crea la tabla worker_subscriptions si no existe"""
    try:
        logger.info("Creando tabla worker_subscriptions...")
        WorkerSubscription.__table__.create(bind=engine, checkfirst=True)
        logger.info("✅ Tabla worker_subscriptions creada o ya existe")
    except Exception as e:
        logger.error(f"❌ Error al crear tabla worker_subscriptions: {e}")
        raise


def apply_migration_from_file(sql_file: str):
    """Aplica un archivo SQL de migración"""
    sql_path = Path(__file__).parent / sql_file
    
    if not sql_path.exists():
        logger.error(f"❌ Archivo no encontrado: {sql_path}")
        return False
    
    logger.info(f"📄 Leyendo migración: {sql_path}")
    
    with open(sql_path, 'r', encoding='utf-8') as f:
        sql_content = f.read()
    
    # Separar comandos SQL (por ';')
    commands = [cmd.strip() for cmd in sql_content.split(';') if cmd.strip() and not cmd.strip().startswith('--')]
    
    with engine.connect() as conn:
        for i, command in enumerate(commands, 1):
            try:
                logger.info(f"Ejecutando comando {i}/{len(commands)}...")
                conn.execute(command)
                conn.commit()
                logger.info(f"✅ Comando {i} ejecutado correctamente")
            except Exception as e:
                logger.error(f"❌ Error en comando {i}: {e}")
                logger.error(f"Comando: {command[:100]}...")
                conn.rollback()
                return False
    
    logger.info("✅ Migración aplicada correctamente")
    return True


if __name__ == "__main__":
    if len(sys.argv) > 1:
        sql_file = sys.argv[1]
        apply_migration_from_file(sql_file)
    else:
        # Por defecto, solo crear la tabla WorkerSubscription
        logger.info("No se especificó archivo SQL. Creando solo tabla worker_subscriptions...")
        create_worker_subscriptions_table()

