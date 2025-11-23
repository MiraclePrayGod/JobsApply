import logging
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db
from app.utils.dependencies import get_current_user
from app.models.user import User
from app.schemas.user import (
    UserCreate, UserLogin, UserResponse, UserUpdate, TokenResponse
)
from app.services.auth_service import AuthService

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/auth", tags=["Authentication"])


@router.post("/register", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
async def register(user_create: UserCreate, db: Session = Depends(get_db)):
    """Registra un nuevo usuario"""
    try:
        return AuthService.register_user(db, user_create)
    except HTTPException:
        # Re-lanzar HTTPException (errores 400, 401, etc.)
        raise
    except Exception as e:
        # Loggear error completo para debugging, pero no exponer detalles al cliente
        logger.exception("Error en /api/auth/register")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error interno del servidor"
        )


@router.post("/login", response_model=TokenResponse)
async def login(user_login: UserLogin, db: Session = Depends(get_db)):
    """Login de usuario - retorna JWT token y información del usuario"""
    try:
        return AuthService.login_user(db, user_login)
    except HTTPException:
        # Re-lanzar HTTPException (errores 401, etc.)
        raise
    except Exception as e:
        # Loggear error completo para debugging, pero no exponer detalles al cliente
        logger.exception("Error en /api/auth/login")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error interno del servidor"
        )


@router.get("/me", response_model=UserResponse)
async def get_current_user_info(
    current_user: User = Depends(get_current_user)
):
    """Obtiene la información del usuario actual"""
    return current_user


@router.put("/me", response_model=UserResponse)
async def update_current_user_info(
    user_update: UserUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Actualiza la información del usuario actual
    
    NOTA: Si se actualiza password, se hashea automáticamente.
    Considera usar un endpoint separado /me/password para cambio de contraseña.
    """
    try:
        return AuthService.update_user(db, current_user.id, user_update)
    except HTTPException:
        # Re-lanzar HTTPException (errores 400, 404, etc.)
        raise
    except Exception as e:
        # Loggear error completo para debugging, pero no exponer detalles al cliente
        logger.exception("Error en /api/auth/me (PUT)")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error interno del servidor"
        )
