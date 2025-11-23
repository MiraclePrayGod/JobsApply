from sqlalchemy import Column, Integer, String, Enum, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
import enum
from app.database import Base


class EvidenceType(str, enum.Enum):
    """Tipos de evidencia"""
    BEFORE = "before"
    AFTER = "after"


class JobEvidence(Base):
    """Modelo de Evidencia (Fotos del trabajo)"""
    __tablename__ = "job_evidence"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    job_id = Column(Integer, ForeignKey("jobs.id", ondelete="CASCADE"), nullable=False)
    image_url = Column(String(500), nullable=False)
    type = Column(Enum(EvidenceType), nullable=False)
    created_at = Column(DateTime, server_default=func.now())

    # Relaciones
    job = relationship("Job", back_populates="evidence")

