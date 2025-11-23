from pydantic import BaseModel
from typing import Optional
from datetime import datetime
from app.schemas.job import WorkerInfo


class JobApplicationResponse(BaseModel):
    """Schema para respuesta de aplicación de trabajador"""
    id: int
    job_id: int
    worker_id: int
    is_accepted: bool
    created_at: datetime
    updated_at: datetime
    worker: Optional[WorkerInfo] = None  # Información del trabajador
    
    class Config:
        from_attributes = True


