from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db
from app.utils.dependencies import get_current_user
from app.models.user import User
from app.models.job import Job
from pydantic import BaseModel
from typing import Optional

router = APIRouter(prefix="/api", tags=["Location"])


class LocationUpdateRequest(BaseModel):
    latitude: float
    longitude: float
    accuracy: Optional[float] = None
    speed: Optional[float] = None


class LocationUpdateResponse(BaseModel):
    success: bool
    message: Optional[str] = None


@router.post("/location/update")
async def update_location(
    request: LocationUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Actualiza la ubicación del trabajador en tiempo real"""
    from app.models.user import UserRole
    
    # Validar que el usuario sea trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden actualizar su ubicación"
        )
    
    # Validar coordenadas
    if not (-90 <= request.latitude <= 90):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="La latitud debe estar entre -90 y 90"
        )
    
    if not (-180 <= request.longitude <= 180):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="La longitud debe estar entre -180 y 180"
        )
    
    # TODO: Guardar la ubicación en la base de datos o cache/Redis
    # Por ahora solo retornamos éxito
    return LocationUpdateResponse(
        success=True,
        message="Ubicación actualizada correctamente"
    )


@router.post("/jobs/{job_id}/location")
async def update_job_location(
    job_id: int,
    request: LocationUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Actualiza la ubicación del trabajador para un trabajo específico"""
    from app.models.user import UserRole
    from app.models.job import JobStatus
    from app.services.worker_service import WorkerService
    
    # Verificar que el usuario es trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden actualizar su ubicación"
        )
    
    # Validar coordenadas
    if not (-90 <= request.latitude <= 90):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="La latitud debe estar entre -90 y 90"
        )
    
    if not (-180 <= request.longitude <= 180):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="La longitud debe estar entre -180 y 180"
        )
    
    # Obtener el worker_id del usuario actual
    worker = WorkerService.get_worker_by_user_id(db, current_user.id)
    
    if not worker:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No tienes un perfil de trabajador"
        )
    
    # Verificar que el trabajo existe y pertenece al trabajador
    job = db.query(Job).filter(Job.id == job_id).first()
    
    if not job:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Trabajo no encontrado"
        )
    
    # Verificar que el trabajador está asignado al trabajo
    if job.worker_id != worker.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="No estás asignado a este trabajo"
        )
    
    # Validar que el trabajo esté en un estado válido para actualizar ubicación
    # Solo se puede actualizar ubicación cuando el trabajo está en progreso o en ruta
    valid_statuses_for_location = [
        JobStatus.ACCEPTED,
        JobStatus.IN_ROUTE,
        JobStatus.ON_SITE,
        JobStatus.IN_PROGRESS
    ]
    
    if job.status not in valid_statuses_for_location:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"No se puede actualizar la ubicación para un trabajo con estado {job.status.value}"
        )
    
    # TODO: Crear tabla WorkerLocation para almacenar ubicaciones en tiempo real
    # Por ahora solo retornamos éxito, la ubicación se puede almacenar en cache/Redis
    # o en una tabla separada para no sobrescribir la ubicación del cliente (job.latitude/longitude)
    # job.latitude y job.longitude son para la ubicación del cliente, no del trabajador
    db.commit()
    
    return LocationUpdateResponse(
        success=True,
        message=f"Ubicación actualizada para el trabajo {job_id}"
    )

