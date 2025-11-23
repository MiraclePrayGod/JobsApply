from pydantic import BaseModel, validator
from typing import Optional, List
from datetime import datetime
from decimal import Decimal
from app.models.job import JobStatus, PaymentMethod


# Schemas para Job (DTOs)

class JobBase(BaseModel):
    """Schema base para Job"""
    title: str
    description: Optional[str] = None
    service_type: str  # 'Plomería', 'Electricidad', etc.
    payment_method: PaymentMethod
    base_fee: Decimal
    address: str
    latitude: Optional[Decimal] = None
    longitude: Optional[Decimal] = None
    scheduled_at: Optional[datetime] = None


class JobCreate(JobBase):
    """Schema para crear trabajo (cliente crea solicitud)
    
    NOTA: client_id NO se incluye aquí, se pasa como parámetro al servicio
    para evitar que el cliente pueda modificar su propio ID.
    """


class ClientInfo(BaseModel):
    """Información básica del cliente"""
    id: int
    full_name: Optional[str] = None
    phone: Optional[str] = None
    email: str
    profile_image_url: Optional[str] = None  # URL de foto de perfil
    
    class Config:
        from_attributes = True


class WorkerInfo(BaseModel):
    """Información básica del trabajador"""
    id: int
    full_name: Optional[str] = None
    phone: Optional[str] = None
    profile_image_url: Optional[str] = None
    is_verified: bool = False
    
    class Config:
        from_attributes = True


class JobResponse(JobBase):
    """Schema para respuesta de trabajo
    
    NOTA: extras y total_amount tienen defaults para evitar errores si la BD
    tiene valores NULL (aunque en el modelo están con default=0.00).
    """
    id: int
    client_id: int
    worker_id: Optional[int] = None
    status: JobStatus
    extras: Decimal = Decimal("0.00")  # Default para evitar errores con NULL
    total_amount: Decimal = Decimal("0.00")  # Default para evitar errores con NULL
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    created_at: datetime
    updated_at: datetime
    client: Optional[ClientInfo] = None  # Información del cliente
    worker: Optional[WorkerInfo] = None  # Información del trabajador

    class Config:
        from_attributes = True


class JobUpdate(BaseModel):
    """Schema para actualizar trabajo"""
    title: Optional[str] = None
    description: Optional[str] = None
    status: Optional[JobStatus] = None
    extras: Optional[Decimal] = None
    total_amount: Optional[Decimal] = None
    scheduled_at: Optional[datetime] = None


class JobAccept(BaseModel):
    """Schema para aceptar trabajo"""
    worker_id: int


class JobAddExtra(BaseModel):
    """Schema para agregar extra al trabajo"""
    extra_amount: Decimal
    description: Optional[str] = None
    
    @validator('extra_amount')
    def validate_extra_amount(cls, v):
        """Valida que el monto del extra sea mayor que cero"""
        if v <= Decimal("0.00"):
            raise ValueError("El monto del extra debe ser mayor que cero")
        return v

