import logging
from datetime import datetime, timedelta
from typing import Optional
from jose import JWTError, jwt
import bcrypt
from app.config import settings

logger = logging.getLogger(__name__)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verifica si la contraseña coincide con el hash"""
    try:
        # bcrypt espera bytes, no strings
        password_bytes = plain_password.encode('utf-8')
        hash_bytes = hashed_password.encode('utf-8')
        return bcrypt.checkpw(password_bytes, hash_bytes)
    except Exception:
        return False


def get_password_hash(password: str) -> str:
    """Genera hash de la contraseña usando bcrypt"""
    # bcrypt espera bytes, no strings
    password_bytes = password.encode('utf-8')
    # Generar salt y hash
    salt = bcrypt.gensalt()
    hashed = bcrypt.hashpw(password_bytes, salt)
    # Devolver como string
    return hashed.decode('utf-8')


def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    """Crea un JWT token"""
    to_encode = data.copy()
    
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    
    to_encode.update({"exp": int(expire.timestamp())})  # Convertir a timestamp Unix
    encoded_jwt = jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)
    return encoded_jwt


def decode_access_token(token: str) -> Optional[dict]:
    """Decodifica y valida un JWT token
    
    Retorna el payload si el token es válido, None si hay error.
    En desarrollo loguea errores, en producción solo retorna None.
    """
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
        
        if settings.is_development:
            logger.debug("Token decodificado exitosamente")
        
        return payload
    except JWTError as e:
        # JWTError incluye: ExpiredSignatureError, InvalidTokenError, etc.
        if settings.is_development:
            logger.debug(f"JWT Error: {type(e).__name__} - {str(e)}")
        return None
    except Exception as e:
        # Capturar cualquier otro error inesperado
        if settings.is_development:
            logger.exception("Error inesperado al decodificar token")
        else:
            logger.error(f"Error decodificando token: {type(e).__name__}")
        return None

