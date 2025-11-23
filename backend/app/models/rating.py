from sqlalchemy import Column, Integer, Text, DateTime, ForeignKey, CheckConstraint
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


class Rating(Base):
    """Modelo de Calificaciones"""
    __tablename__ = "ratings"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    job_id = Column(Integer, ForeignKey("jobs.id", ondelete="CASCADE"), nullable=False, unique=True)
    worker_rating = Column(Integer)  # Calificación del trabajador al cliente (1-5)
    worker_comment = Column(Text)
    client_rating = Column(Integer)  # Calificación del cliente al trabajador (1-5)
    client_comment = Column(Text)
    created_at = Column(DateTime, server_default=func.now())

    # Constraint para validar rango de calificaciones
    __table_args__ = (
        CheckConstraint('worker_rating >= 1 AND worker_rating <= 5', name='check_worker_rating'),
        CheckConstraint('client_rating >= 1 AND client_rating <= 5', name='check_client_rating'),
    )

    # Relaciones
    job = relationship("Job", back_populates="rating")

