from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime


class RatingCreate(BaseModel):
    """Schema para crear calificación (trabajador califica al cliente)"""
    rating: int = Field(..., ge=1, le=5, description="Calificación del 1 al 5")
    comment: Optional[str] = None


class RatingResponse(BaseModel):
    """Schema para respuesta de calificación"""
    id: int
    job_id: int
    worker_rating: Optional[int] = None
    worker_comment: Optional[str] = None
    client_rating: Optional[int] = None
    client_comment: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True

