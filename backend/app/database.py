from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from app.config import settings

# Crear engine de SQLAlchemy
# echo=True solo en desarrollo para ver queries SQL en consola
engine = create_engine(
    settings.database_url,
    pool_pre_ping=True,  # Verifica conexiones antes de usarlas
    pool_recycle=300,    # Recicla conexiones cada 5 minutos
    echo=settings.is_development  # Ver queries SQL solo en desarrollo
)

# SessionLocal: clase para crear sesiones de BD
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# Base: clase base para todos los modelos
Base = declarative_base()


# Dependencia para obtener sesión de BD (similar a @Autowired en Spring)
def get_db():
    """Dependencia para inyectar sesión de BD en endpoints"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

