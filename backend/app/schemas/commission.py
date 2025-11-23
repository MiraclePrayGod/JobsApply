from pydantic import BaseModel
from typing import Optional
from datetime import datetime
from decimal import Decimal
from app.models.commission import CommissionStatus


# Schemas para Commission (DTOs)

class CommissionBase(BaseModel):
    """Schema base para Commission"""
    amount: Decimal
    status: CommissionStatus


class JobInfo(BaseModel):
    """Información básica del trabajo"""
    id: int
    title: str
    payment_method: str  # Se convertirá automáticamente del Enum

    class Config:
        from_attributes = True


class CommissionResponse(CommissionBase):
    """Schema para respuesta de comisión"""
    id: int
    worker_id: int
    job_id: int
    payment_code: Optional[str] = None
    payment_proof_url: Optional[str] = None
    submitted_at: Optional[datetime] = None
    reviewed_by: Optional[int] = None
    reviewed_at: Optional[datetime] = None
    notes: Optional[str] = None
    created_at: datetime
    updated_at: datetime
    job: Optional[JobInfo] = None  # Información del trabajo

    class Config:
        from_attributes = True


class CommissionSubmitPayment(BaseModel):
    """Schema para adjuntar código Yape"""
    payment_code: str
    payment_proof_url: Optional[str] = None


class CommissionReview(BaseModel):
    """Schema para que manager apruebe/rechace pago"""
    notes: Optional[str] = None

