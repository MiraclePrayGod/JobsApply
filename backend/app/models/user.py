from sqlalchemy import Column, Integer, String, DateTime, Enum
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
import enum
from app.database import Base


class UserRole(str, enum.Enum):
    """Roles de usuario
    
    Valores posibles:
    - "client": Cliente que solicita servicios
    - "worker": Trabajador que ofrece servicios
    - "manager": Administrador que gestiona comisiones
    
    NOTA: Estos valores deben coincidir exactamente con los usados en la app Android.
    Cualquier cambio aquí debe reflejarse en el código de la app móvil.
    """
    CLIENT = "client"
    WORKER = "worker"
    MANAGER = "manager"


class User(Base):
    """Modelo de Usuario (equivalente a @Entity en JPA)"""
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    email = Column(String(255), unique=True, nullable=False, index=True)
    password_hash = Column(String(255), nullable=False)
    role = Column(Enum(UserRole), nullable=False)
    full_name = Column(String(255), nullable=True)  # Nombre completo
    phone = Column(String(20), nullable=True)  # Teléfono de contacto
    profile_image_url = Column(String(500), nullable=True)  # URL de foto de perfil
    created_at = Column(DateTime, server_default=func.now())
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now())

    # Relaciones (como @OneToOne o @OneToMany en JPA)
    worker = relationship("Worker", back_populates="user", uselist=False)
    client_jobs = relationship("Job", foreign_keys="Job.client_id", back_populates="client")
    reviewed_commissions = relationship("Commission", foreign_keys="Commission.reviewed_by", back_populates="reviewer")

