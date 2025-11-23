from pydantic import BaseModel, EmailStr
from typing import Optional
from datetime import datetime
from app.models.user import UserRole


# Schema para respuesta de login
class TokenResponse(BaseModel):
    """Schema para respuesta de login con token JWT"""
    access_token: str
    token_type: str = "bearer"
    user: dict  # Información básica del usuario (id, email, role)


# Schemas para User (DTOs)

class UserBase(BaseModel):
    """Schema base para User"""
    email: EmailStr
    role: UserRole
    full_name: Optional[str] = None
    phone: Optional[str] = None
    profile_image_url: Optional[str] = None  # URL de foto de perfil


class UserCreate(UserBase):
    """Schema para crear usuario (registro)"""
    password: str


class UserLogin(BaseModel):
    """Schema para login"""
    email: EmailStr
    password: str


class UserResponse(UserBase):
    """Schema para respuesta de usuario (sin password)"""
    id: int
    # full_name y phone ya están en UserBase, no hace falta redeclararlos
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True  # Permite crear desde ORM (SQLAlchemy)


class UserUpdate(BaseModel):
    """Schema para actualizar usuario
    
    NOTA: Si se actualiza password, se hashea automáticamente en AuthService.
    Considera crear un endpoint separado /me/password para cambio de contraseña
    con validación de contraseña actual.
    """
    email: Optional[EmailStr] = None
    password: Optional[str] = None  # Se hashea automáticamente en AuthService
    full_name: Optional[str] = None
    phone: Optional[str] = None
    profile_image_url: Optional[str] = None  # URL de foto de perfil

