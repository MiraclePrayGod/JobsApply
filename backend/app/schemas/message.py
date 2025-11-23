from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class MessageBase(BaseModel):
    """Schema base para mensaje"""
    content: str
    has_image: bool = False
    image_url: Optional[str] = None


class MessageCreate(MessageBase):
    """Schema para crear mensaje"""
    job_id: int
    application_id: Optional[int] = None  # ID de la aplicación (para identificar el chat específico)


class SenderInfo(BaseModel):
    """Información del remitente"""
    id: int
    full_name: Optional[str] = None
    email: str
    
    class Config:
        from_attributes = True


class MessageResponse(MessageBase):
    """Schema para respuesta de mensaje"""
    id: int
    job_id: int
    application_id: Optional[int] = None  # ID de la aplicación
    sender_id: int
    sender: Optional[SenderInfo] = None
    created_at: datetime
    
    class Config:
        from_attributes = True

