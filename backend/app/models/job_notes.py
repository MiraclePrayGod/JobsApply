from sqlalchemy import Column, Integer, Text, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


class JobNotes(Base):
    """Modelo de Notas del Trabajo"""
    __tablename__ = "job_notes"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    job_id = Column(Integer, ForeignKey("jobs.id", ondelete="CASCADE"), nullable=False, unique=True)
    description = Column(Text, nullable=False)  # Describe lo realizado
    materials_used = Column(Text)  # Materiales utilizados (opcional)
    created_at = Column(DateTime, server_default=func.now())

    # Relaciones
    job = relationship("Job", back_populates="notes")

