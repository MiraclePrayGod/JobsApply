from sqlalchemy import Column, Integer, Numeric, Enum, String, Text, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
import enum
from app.database import Base


class CommissionStatus(str, enum.Enum):
    """Estados de comisión"""
    PENDING = "pending"
    PAYMENT_SUBMITTED = "payment_submitted"
    APPROVED = "approved"
    REJECTED = "rejected"


class Commission(Base):
    """Modelo de Comisión"""
    __tablename__ = "commissions"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    worker_id = Column(Integer, ForeignKey("workers.id", ondelete="CASCADE"), nullable=False)
    job_id = Column(Integer, ForeignKey("jobs.id", ondelete="CASCADE"), nullable=False, unique=True)
    amount = Column(Numeric(10, 2), nullable=False)  # 10% del total_amount
    status = Column(Enum(CommissionStatus), default=CommissionStatus.PENDING)
    payment_code = Column(String(50))  # Código Yape adjuntado por trabajador
    payment_proof_url = Column(String(500))  # Screenshot/comprobante
    submitted_at = Column(DateTime)
    reviewed_by = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    reviewed_at = Column(DateTime)
    notes = Column(Text)  # Notas del manager si rechaza
    created_at = Column(DateTime, server_default=func.now())
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now())

    # Relaciones
    worker = relationship("Worker", back_populates="commissions")
    job = relationship("Job", back_populates="commission")
    reviewer = relationship("User", foreign_keys=[reviewed_by], back_populates="reviewed_commissions")

