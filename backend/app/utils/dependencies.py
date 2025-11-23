import logging
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.user import User
from app.utils.security import decode_access_token
from app.config import settings

logger = logging.getLogger(__name__)

# Security scheme para JWT
security = HTTPBearer()


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: Session = Depends(get_db)
) -> User:
    """Dependencia para obtener el usuario actual desde el JWT token
    
    Valida el token JWT, extrae el user_id y busca el usuario en la BD.
    Maneja errores de forma segura sin exponer información sensible.
    """
    token = credentials.credentials
    
    # Logging solo en desarrollo y sin información sensible
    if settings.is_development:
        logger.debug(f"Token recibido (parcial): {token[:10]}...")
        logger.debug(f"ALGORITHM: {settings.ALGORITHM}")
        # NUNCA loguear SECRET_KEY, ni siquiera parcialmente
    
    try:
        payload = decode_access_token(token)
    except Exception as e:
        # Capturar excepciones de decode_access_token
        if settings.is_development:
            logger.debug(f"Error al decodificar token: {type(e).__name__}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token inválido o expirado",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    if payload is None:
        if settings.is_development:
            logger.debug("Token decodificado pero payload es None")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token inválido o expirado",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    user_id_str = payload.get("sub")
    if user_id_str is None:
        if settings.is_development:
            logger.debug("Token válido pero no contiene 'sub'")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token inválido",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    # Convertir user_id de string a int (porque lo guardamos como string en el token)
    try:
        user_id = int(user_id_str)
    except (ValueError, TypeError):
        if settings.is_development:
            logger.debug(f"user_id no es un número válido: {user_id_str}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token inválido",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    user = db.query(User).filter(User.id == user_id).first()
    if user is None:
        if settings.is_development:
            logger.warning(f"Token válido pero usuario {user_id} no encontrado en BD")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Usuario no encontrado",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    return user

