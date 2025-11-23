import logging
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError
from fastapi import HTTPException, status
from app.models.user import User, UserRole
from app.schemas.user import UserCreate, UserLogin, UserUpdate
from app.utils.security import verify_password, get_password_hash, create_access_token
from app.config import settings

logger = logging.getLogger(__name__)


class AuthService:
    """Servicio de autenticación (equivalente a @Service en Spring Boot)"""
    
    @staticmethod
    def register_user(db: Session, user_create: UserCreate) -> User:
        """Registra un nuevo usuario
        
        Normaliza el email (lowercase y trim) para evitar duplicados por mayúsculas.
        Maneja condición de carrera con IntegrityError de la BD.
        """
        try:
            # Normalizar email: lowercase y trim
            normalized_email = user_create.email.lower().strip()
            
            # Verificar si el email ya existe
            existing_user = db.query(User).filter(User.email == normalized_email).first()
            if existing_user:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="El email ya está registrado"
                )
            
            # Crear nuevo usuario con email normalizado
            hashed_password = get_password_hash(user_create.password)
            new_user = User(
                email=normalized_email,
                password_hash=hashed_password,
                role=user_create.role,
                full_name=user_create.full_name,
                phone=user_create.phone
            )
            
            db.add(new_user)
            db.commit()
            db.refresh(new_user)
            
            return new_user
        except HTTPException:
            # Re-lanzar HTTPException
            raise
        except IntegrityError:
            # Condición de carrera: dos registros simultáneos con el mismo email
            db.rollback()
            logger.warning(f"Intento de registro duplicado para email: {user_create.email}")
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="El email ya está registrado"
            )
        except Exception as e:
            # Rollback en caso de error
            db.rollback()
            logger.exception("Error al registrar usuario")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error interno del servidor"
            )
    
    @staticmethod
    def login_user(db: Session, user_login: UserLogin) -> dict:
        """Autentica un usuario y retorna token JWT
        
        Normaliza el email antes de buscar para evitar problemas de case-sensitivity.
        """
        # Normalizar email: lowercase y trim
        normalized_email = user_login.email.lower().strip()
        
        # Buscar usuario por email normalizado
        user = db.query(User).filter(User.email == normalized_email).first()
        
        if not user:
            # No revelar si el email existe o no (seguridad)
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Email o contraseña incorrectos"
            )
        
        # Verificar contraseña
        if not verify_password(user_login.password, user.password_hash):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Email o contraseña incorrectos"
            )
        
        # Crear token JWT (sub debe ser string según estándar JWT)
        # Opcional: podrías incluir role en el token para autorización rápida
        # pero ir a BD es más seguro si el rol puede cambiar
        access_token = create_access_token(data={"sub": str(user.id)})
        
        return {
            "access_token": access_token,
            "token_type": "bearer",
            "user": {
                "id": user.id,
                "email": user.email,
                "role": user.role.value
            }
        }
    
    @staticmethod
    def update_user(db: Session, user_id: int, user_update: UserUpdate) -> User:
        """Actualiza la información de un usuario
        
        Normaliza el email si se actualiza.
        Maneja errores con rollback para consistencia.
        """
        try:
            user = db.query(User).filter(User.id == user_id).first()
            
            if not user:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Usuario no encontrado"
                )
            
            # Actualizar solo los campos proporcionados
            update_data = user_update.dict(exclude_unset=True)
            
            # Si se actualiza el email, normalizar y verificar que no esté en uso
            if "email" in update_data and update_data["email"] != user.email:
                normalized_email = update_data["email"].lower().strip()
                
                # Verificar duplicado
                existing_user = db.query(User).filter(User.email == normalized_email).first()
                if existing_user:
                    raise HTTPException(
                        status_code=status.HTTP_400_BAD_REQUEST,
                        detail="El email ya está registrado"
                    )
                user.email = normalized_email
            
            # Si se actualiza la contraseña, hashearla
            if "password" in update_data:
                user.password_hash = get_password_hash(update_data["password"])
            
            # Actualizar otros campos
            if "full_name" in update_data:
                user.full_name = update_data["full_name"]
            
            if "phone" in update_data:
                user.phone = update_data["phone"]
            
            db.commit()
            db.refresh(user)
            
            return user
        except HTTPException:
            # Re-lanzar HTTPException
            raise
        except IntegrityError:
            # Condición de carrera: email duplicado
            db.rollback()
            logger.warning(f"Intento de actualizar email duplicado para usuario {user_id}")
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="El email ya está registrado"
            )
        except Exception as e:
            # Rollback en caso de error
            db.rollback()
            logger.exception(f"Error al actualizar usuario {user_id}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error interno del servidor"
            )

