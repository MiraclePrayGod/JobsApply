from sqlalchemy import Column, Integer, String, Text, Enum, Numeric, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
import enum
from decimal import Decimal
from app.database import Base


class JobStatus(str, enum.Enum):
    """Estados de un trabajo"""
    PENDING = "pending"
    ACCEPTED = "accepted"
    IN_ROUTE = "in_route"
    ON_SITE = "on_site"
    IN_PROGRESS = "in_progress"
    COMPLETED = "completed"
    CANCELLED = "cancelled"


class PaymentMethod(str, enum.Enum):
    """Métodos de pago"""
    YAPE = "yape"
    CASH = "cash"


class Job(Base):
    """Modelo de Trabajo"""
    __tablename__ = "jobs"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    client_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    worker_id = Column(Integer, ForeignKey("workers.id", ondelete="SET NULL"), nullable=True)
    title = Column(String(255), nullable=False)
    description = Column(Text)
    service_type = Column(String(50), nullable=False)  # 'Plomería', 'Electricidad', etc.
    status = Column(Enum(JobStatus), default=JobStatus.PENDING)
    payment_method = Column(Enum(PaymentMethod), nullable=False)
    base_fee = Column(Numeric(10, 2), nullable=False)
    extras = Column(Numeric(10, 2), nullable=False, default=Decimal("0.00"), server_default="0.00")
    total_amount = Column(Numeric(10, 2), nullable=False)  # Se calcula al crear: base_fee + extras
    address = Column(String(500), nullable=False)
    latitude = Column(Numeric(10, 8))
    longitude = Column(Numeric(11, 8))
    scheduled_at = Column(DateTime)
    started_at = Column(DateTime)
    completed_at = Column(DateTime)
    created_at = Column(DateTime, server_default=func.now())
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now())

    # Relaciones
    client = relationship("User", foreign_keys=[client_id], back_populates="client_jobs")
    worker = relationship("Worker", back_populates="jobs")
    applications = relationship("JobApplication", back_populates="job", cascade="all, delete-orphan")
    commission = relationship("Commission", back_populates="job", uselist=False)
    evidence = relationship("JobEvidence", back_populates="job", cascade="all, delete-orphan")
    notes = relationship("JobNotes", back_populates="job", uselist=False, cascade="all, delete-orphan")
    rating = relationship("Rating", back_populates="job", uselist=False, cascade="all, delete-orphan")
    messages = relationship("Message", back_populates="job", cascade="all, delete-orphan")

