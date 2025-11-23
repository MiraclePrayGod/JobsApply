"""
Script para crear las tablas en la base de datos MySQL
Ejecutar solo una vez: python init_db.py
"""
from app.database import engine, Base
from app.models import (
    User, Worker, Job, Commission,
    JobEvidence, JobNotes, Rating, JobApplication, Message
)

def init_db():
    """Crea todas las tablas en la base de datos"""
    print("Creando tablas en la base de datos...")
    Base.metadata.create_all(bind=engine)
    print("¡Tablas creadas exitosamente!")

if __name__ == "__main__":
    init_db()

