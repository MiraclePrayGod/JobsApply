from sqlalchemy import Column, Integer, Numeric, Enum, String, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from decimal import Decimal
import enum
from app.database import Base


class SubscriptionPlan(str, enum.Enum):
    DAILY = "daily"   # 1 día
    WEEKLY = "weekly" # 7 días (1 gratis)


class SubscriptionStatus(str, enum.Enum):
    PENDING = "pending"
    ACTIVE = "active"
    EXPIRED = "expired"
    CANCELLED = "cancelled"


class WorkerSubscription(Base):
    __tablename__ = "worker_subscriptions"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    worker_id = Column(Integer, ForeignKey("workers.id", ondelete="CASCADE"), nullable=False)
    plan = Column(Enum(SubscriptionPlan), nullable=False)
    days = Column(Integer, nullable=False)  # 1 o 7
    amount = Column(Numeric(10, 2), nullable=False)  # 2.00 o 12.00
    status = Column(Enum(SubscriptionStatus), default=SubscriptionStatus.ACTIVE, nullable=False)
    payment_method = Column(String(50), default="yape")
    payment_code = Column(String(50))  # código simulado de Yape
    valid_from = Column(DateTime, nullable=False)
    valid_until = Column(DateTime, nullable=False)
    created_at = Column(DateTime, server_default=func.now())
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now())

    worker = relationship("Worker", back_populates="subscriptions")

