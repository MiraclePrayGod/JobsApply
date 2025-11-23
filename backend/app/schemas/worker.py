from pydantic import BaseModel, field_validator
from typing import Optional, List
from datetime import datetime
import json


# Schemas para Worker (DTOs)

class WorkerBase(BaseModel):
    """Schema base para Worker
    
    NOTA: is_verified y verification_photo_url NO están aquí por seguridad.
    El cliente no puede modificar is_verified (solo manager puede).
    verification_photo_url se envía en endpoint separado /me/verify.
    """
    full_name: str
    phone: Optional[str] = None
    services: Optional[List[str]] = None  # ['Plomería', 'Electricidad', etc.]
    description: Optional[str] = None
    district: Optional[str] = None
    is_available: bool = False
    yape_number: Optional[str] = None
    profile_image_url: Optional[str] = None


class WorkerCreate(WorkerBase):
    """Schema para crear trabajador (registro completo)
    
    NOTA: user_id NO se incluye aquí, se pasa como parámetro al servicio
    para evitar que el cliente pueda modificar su propio ID.
    """


class WorkerResponse(WorkerBase):
    """Schema para respuesta de trabajador
    
    Incluye is_verified y verification_photo_url solo para lectura.
    Incluye is_plus_active y plus_expires_at para mostrar estado de Modo Plus.
    """
    id: int
    user_id: int
    is_verified: bool = False  # Solo lectura, no se puede modificar desde cliente
    verification_photo_url: Optional[str] = None  # Solo lectura
    is_plus_active: bool = False  # Estado de Modo Plus
    plus_expires_at: Optional[datetime] = None  # Fecha de expiración de Modo Plus
    created_at: datetime
    updated_at: datetime

    @field_validator('services', mode='before')
    @classmethod
    def parse_services(cls, v):
        """Parsea services si viene como string JSON (compatibilidad con MySQL)"""
        if v is None:
            return None
        if isinstance(v, str):
            try:
                # Si es un string JSON, parsearlo a lista
                parsed = json.loads(v)
                if isinstance(parsed, list):
                    return parsed
                return v
            except (json.JSONDecodeError, TypeError):
                # Si no se puede parsear, retornar el valor original
                return v
        # Si ya es una lista u otro tipo, retornarlo tal cual
        return v

    class Config:
        from_attributes = True


class WorkerUpdate(BaseModel):
    """Schema para actualizar trabajador
    
    NOTA: is_verified NO se puede modificar desde aquí (solo manager).
    verification_photo_url se actualiza en endpoint separado /me/verify.
    """
    full_name: Optional[str] = None
    phone: Optional[str] = None
    services: Optional[List[str]] = None
    description: Optional[str] = None
    district: Optional[str] = None
    is_available: Optional[bool] = None
    yape_number: Optional[str] = None
    profile_image_url: Optional[str] = None
    # verification_photo_url se actualiza en /me/verify, no aquí

