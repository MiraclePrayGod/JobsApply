from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List, Optional
from pydantic import BaseModel, validator
from app.database import get_db
from app.utils.dependencies import get_current_user
from app.models.user import User
from app.schemas.worker import WorkerCreate, WorkerResponse, WorkerUpdate
from app.services.worker_service import WorkerService


class VerificationRequest(BaseModel):
    verification_photo_url: str
    
    @validator('verification_photo_url')
    def validate_verification_photo_url(cls, v):
        """Valida que la URL de verificación no esté vacía"""
        if not v or not v.strip():
            raise ValueError("La URL de la foto de verificación no puede estar vacía")
        # Validar formato básico de URL (debe empezar con http:// o https://)
        if not (v.startswith("http://") or v.startswith("https://")):
            raise ValueError("La URL de verificación debe ser una URL válida (http:// o https://)")
        return v

router = APIRouter(prefix="/api/workers", tags=["Workers"])


@router.post("/register", response_model=WorkerResponse, status_code=status.HTTP_201_CREATED)
async def register_worker(
    worker_create: WorkerCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Registra o actualiza el perfil de trabajador (completa el perfil)"""
    from app.models.user import UserRole
    
    # Verificar que el usuario sea trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden crear un perfil de trabajador"
        )
    
    # Verificar si el usuario ya tiene un perfil de trabajador
    existing_worker = WorkerService.get_worker_by_user_id(db, current_user.id)
    
    if existing_worker:
        # Si ya existe, actualizar el perfil en lugar de crear uno nuevo
        from app.schemas.worker import WorkerUpdate
        worker_update = WorkerUpdate(
            full_name=worker_create.full_name,
            phone=worker_create.phone,
            services=worker_create.services,
            description=worker_create.description,
            district=worker_create.district,
            is_available=worker_create.is_available,
            yape_number=worker_create.yape_number,
            profile_image_url=worker_create.profile_image_url
        )
        return WorkerService.update_worker(db, existing_worker.id, worker_update)
    else:
        # Si no existe, crear nuevo perfil
        # Pasar user_id como parámetro (no confiar en el body)
        return WorkerService.create_worker(db, worker_create, user_id=current_user.id)


@router.get("/me", response_model=WorkerResponse)
async def get_my_profile(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Obtiene el perfil del trabajador actual"""
    from app.models.user import UserRole
    
    # Validar que el usuario sea trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden acceder a su perfil"
        )
    
    worker = WorkerService.get_worker_by_user_id(db, current_user.id)
    
    if not worker:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No tienes un perfil de trabajador"
        )
    
    return worker


@router.put("/me", response_model=WorkerResponse)
async def update_my_profile(
    worker_update: WorkerUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Actualiza el perfil del trabajador actual"""
    from app.models.user import UserRole
    
    # Validar que el usuario sea trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden actualizar su perfil"
        )
    
    worker = WorkerService.get_worker_by_user_id(db, current_user.id)
    
    if not worker:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No tienes un perfil de trabajador"
        )
    
    return WorkerService.update_worker(db, worker.id, worker_update)


@router.get("/{worker_id}", response_model=WorkerResponse)
async def get_worker(worker_id: int, db: Session = Depends(get_db)):
    """Obtiene un trabajador por ID"""
    worker = WorkerService.get_worker_by_id(db, worker_id)
    
    if not worker:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Trabajador no encontrado"
        )
    
    return worker


@router.get("/search/list", response_model=List[WorkerResponse])
async def search_workers(
    service_type: Optional[str] = None,
    district: Optional[str] = None,
    is_available: Optional[bool] = None,
    is_verified: Optional[bool] = None,
    db: Session = Depends(get_db)
):
    """Busca trabajadores con filtros"""
    return WorkerService.search_workers(db, service_type, district, is_available, is_verified)


@router.post("/me/verify", response_model=WorkerResponse)
async def submit_verification(
    verification_request: VerificationRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Envía foto de verificación (DNI, etc.) para verificar cuenta"""
    from app.models.user import UserRole
    
    # Validar que el usuario sea trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden enviar verificación"
        )
    
    worker = WorkerService.get_worker_by_user_id(db, current_user.id)
    
    if not worker:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No tienes un perfil de trabajador"
        )
    
    # Actualizar con la foto de verificación
    # Nota: is_verified se establecerá en True cuando un manager apruebe la verificación
    worker_update = WorkerUpdate(verification_photo_url=verification_request.verification_photo_url)
    return WorkerService.update_worker(db, worker.id, worker_update)