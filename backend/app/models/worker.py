from sqlalchemy import Column, Integer, String, Boolean, Text, DateTime, ForeignKey, JSON
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


class Worker(Base):
    """Modelo de Trabajador"""
    __tablename__ = "workers"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, unique=True)
    full_name = Column(String(255), nullable=False)
    phone = Column(String(20))
    services = Column(JSON)  # Lista de servicios: ['Plomería', 'Electricidad', etc.]
    description = Column(Text)
    district = Column(String(100))
    is_available = Column(Boolean, nullable=False, default=False, server_default="0")
    yape_number = Column(String(20))
    profile_image_url = Column(String(500))
    is_verified = Column(Boolean, nullable=False, default=False, server_default="0")  # Si la cuenta está verificada (solo manager puede cambiar)
    verification_photo_url = Column(String(500))  # URL de la foto de verificación (DNI, etc.)
    is_plus_active = Column(Boolean, nullable=False, default=False, server_default="0")
    plus_expires_at = Column(DateTime, nullable=True)
    created_at = Column(DateTime, server_default=func.now())
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now())

    # Relaciones
    user = relationship("User", back_populates="worker")
    jobs = relationship("Job", back_populates="worker")
    job_applications = relationship("JobApplication", back_populates="worker", cascade="all, delete-orphan")
    commissions = relationship("Commission", back_populates="worker")
    subscriptions = relationship("WorkerSubscription", back_populates="worker", cascade="all, delete-orphan")

