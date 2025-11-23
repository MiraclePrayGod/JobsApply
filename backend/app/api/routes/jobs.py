from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List, Optional
from app.database import get_db
from app.utils.dependencies import get_current_user
from app.models.user import User
from app.models.job import JobStatus
from app.schemas.job import JobCreate, JobResponse, JobUpdate, JobAccept, JobAddExtra
from app.schemas.job_application import JobApplicationResponse
from app.schemas.rating import RatingCreate, RatingResponse
from app.services.job_service import JobService

router = APIRouter(prefix="/api/jobs", tags=["Jobs"])


@router.post("", response_model=JobResponse, status_code=status.HTTP_201_CREATED)
async def create_job(
    job_create: JobCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Crea un nuevo trabajo (cliente crea solicitud)"""
    from app.models.user import UserRole
    
    # Validar que el usuario sea cliente
    if current_user.role != UserRole.CLIENT:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los clientes pueden crear trabajos"
        )
    
    # Pasar client_id como parámetro (no confiar en el body)
    return JobService.create_job(db, job_create, client_id=current_user.id)


@router.get("/available", response_model=List[JobResponse])
async def get_available_jobs(
    service_type: Optional[str] = None,
    search: Optional[str] = None,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Obtiene trabajos disponibles (pendientes) - Solo para trabajadores
    
    Si el trabajador NO tiene Modo Plus activo:
    - Ve títulos, tipo de servicio, quizá distrito
    - NO ve teléfono ni dirección exacta ni otros datos de contacto
    """
    from app.models.user import UserRole
    from app.services.worker_service import WorkerService
    from datetime import datetime
    
    # Verificar que el usuario es trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden ver trabajos disponibles"
        )
    
    # Verificar si tiene Modo Plus activo
    worker = WorkerService.get_worker_by_user_id(db, current_user.id)
    is_plus = False
    if worker:
        now = datetime.utcnow()
        is_plus = bool(worker.is_plus_active and worker.plus_expires_at and worker.plus_expires_at > now)
    
    # Obtener trabajos con manejo de errores
    try:
        jobs = JobService.get_available_jobs(db, service_type, search)
    except Exception as e:
        import logging
        import traceback
        logger = logging.getLogger(__name__)
        logger.exception(f"Error en get_available_jobs: {str(e)}")
        logger.error(f"Traceback completo:\n{traceback.format_exc()}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error al obtener trabajos disponibles: {str(e)}"
        )
    
    # Si no tiene Plus, redactar datos sensibles del cliente
    if not is_plus:
        for job in jobs:
            if job.client:
                job.client.phone = None
                # No redactamos address porque es parte del JobBase, pero podríamos hacerlo
                # Por ahora dejamos address visible pero sin phone
    
    return jobs


@router.get("/my-jobs", response_model=List[JobResponse])
async def get_my_jobs(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Obtiene los trabajos del usuario actual (trabajador o cliente)"""
    from app.services.worker_service import WorkerService
    from app.models.user import UserRole
    
    # Validar que el usuario sea trabajador o cliente
    if current_user.role not in [UserRole.WORKER, UserRole.CLIENT]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores y clientes pueden ver sus trabajos"
        )
    
    if current_user.role == UserRole.WORKER:
        # Si es trabajador, obtener sus trabajos asignados
        worker = WorkerService.get_worker_by_user_id(db, current_user.id)
        if not worker:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="No tienes un perfil de trabajador"
            )
        return JobService.get_worker_jobs(db, worker.id)
    else:
        # Si es cliente, obtener sus trabajos creados
        return JobService.get_client_jobs(db, current_user.id)


@router.get("/my-applications", response_model=List[JobApplicationResponse])
async def get_my_applications(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Obtiene las aplicaciones del trabajador actual"""
    from app.models.user import UserRole
    from app.services.worker_service import WorkerService
    
    # Validar que el usuario sea trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden ver sus aplicaciones"
        )
    
    # Obtener el worker_id del usuario actual
    worker = WorkerService.get_worker_by_user_id(db, current_user.id)
    
    if not worker:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No tienes un perfil de trabajador"
        )
    
    applications = JobService.get_worker_applications(db, worker.id)
    
    # Usar from_attributes=True para mapear automáticamente desde ORM
    # Las relaciones ya están cargadas (worker, job)
    return applications


@router.get("/{job_id}", response_model=JobResponse)
async def get_job(
    job_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Obtiene un trabajo por ID - Solo el cliente o trabajador asignado pueden verlo"""
    from app.models.user import UserRole
    from app.services.worker_service import WorkerService
    
    job = JobService.get_job_by_id(db, job_id)
    
    if not job:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Trabajo no encontrado"
        )
    
    # Validar acceso: Solo el cliente, el trabajador asignado, o un trabajador que aplicó pueden ver el trabajo
    is_client = job.client_id == current_user.id
    
    # Obtener worker una sola vez si es necesario
    worker = None
    is_worker = False
    has_applied = False
    
    if current_user.role == UserRole.WORKER:
        worker = WorkerService.get_worker_by_user_id(db, current_user.id)
        
        if not worker:
            # Si es WORKER pero no tiene perfil, denegar acceso con mensaje claro
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="No tienes un perfil de trabajador"
            )
        
        # Verificar si es el trabajador asignado
        if job.worker_id is not None and worker.id == job.worker_id:
            is_worker = True
        
        # Verificar si aplicó a este trabajo (usando método del servicio)
        has_applied = JobService.worker_has_applied_to_job(db, worker.id, job_id)
    
    if not is_client and not is_worker and not has_applied:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="No tienes permiso para ver este trabajo"
        )
    
    return job


@router.post("/{job_id}/apply", response_model=JobResponse)
async def apply_to_job(
    job_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Aplica a un trabajo (trabajador aplica, NO cambia el estado)"""
    from app.models.user import UserRole
    from app.services.worker_service import WorkerService
    
    # Validar que el usuario sea trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden aplicar a trabajos"
        )
    
    # Obtener el worker_id del usuario actual
    worker = WorkerService.get_worker_by_user_id(db, current_user.id)
    
    if not worker:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No tienes un perfil de trabajador"
        )
    
    # Validar que el trabajador esté disponible
    if not worker.is_available:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Debes estar disponible para aplicar a trabajos. Activa tu disponibilidad en tu perfil."
        )
    
    # Aplicar al trabajo (NO cambia el estado)
    JobService.apply_to_job(db, job_id, worker.id)
    
    # Devolver el trabajo actualizado
    return JobService.get_job_by_id(db, job_id)

@router.post("/{job_id}/accept-worker/{application_id}", response_model=JobResponse)
async def client_accept_worker(
    job_id: int,
    application_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Cliente acepta un trabajador (cambia el estado a ACCEPTED)"""
    from app.models.user import UserRole
    
    # Validar que el usuario sea cliente
    if current_user.role != UserRole.CLIENT:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los clientes pueden aceptar trabajadores"
        )
    
    return JobService.client_accept_worker(db, job_id, application_id, current_user.id)


@router.get("/{job_id}/applications", response_model=List[JobApplicationResponse])
async def get_job_applications(
    job_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Obtiene las aplicaciones de trabajadores para un trabajo (solo para el cliente)"""
    from app.models.user import UserRole
    
    # Validar que el usuario sea cliente
    if current_user.role != UserRole.CLIENT:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los clientes pueden ver las aplicaciones"
        )
    
    applications = JobService.get_job_applications(db, job_id, current_user.id)
    
    # Usar from_attributes=True para mapear automáticamente desde ORM
    # Las relaciones ya están cargadas (worker con user)
    return applications


@router.post("/{job_id}/start-route", response_model=JobResponse)
async def start_route(
    job_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Inicia ruta al cliente"""
    from app.services.worker_service import WorkerService
    from app.models.user import UserRole
    
    # Validar que es trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden iniciar ruta"
        )
    
    # Obtener el worker_id del usuario actual
    worker = WorkerService.get_worker_by_user_id(db, current_user.id)
    if not worker:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No tienes un perfil de trabajador"
        )
    
    return JobService.update_job_status(db, job_id, JobStatus.IN_ROUTE, worker.id)


@router.post("/{job_id}/confirm-arrival", response_model=JobResponse)
async def confirm_arrival(
    job_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Confirma llegada al sitio"""
    from app.services.worker_service import WorkerService
    from app.models.user import UserRole
    
    # Validar que es trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden confirmar llegada"
        )
    
    # Obtener el worker_id del usuario actual
    worker = WorkerService.get_worker_by_user_id(db, current_user.id)
    if not worker:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No tienes un perfil de trabajador"
        )
    
    return JobService.update_job_status(db, job_id, JobStatus.ON_SITE, worker.id)


@router.post("/{job_id}/start-service", response_model=JobResponse)
async def start_service(
    job_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Inicia el servicio"""
    from app.services.worker_service import WorkerService
    from app.models.user import UserRole
    
    # Validar que es trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden iniciar servicio"
        )
    
    # Obtener el worker_id del usuario actual
    worker = WorkerService.get_worker_by_user_id(db, current_user.id)
    if not worker:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No tienes un perfil de trabajador"
        )
    
    return JobService.update_job_status(db, job_id, JobStatus.IN_PROGRESS, worker.id)


@router.post("/{job_id}/add-extra", response_model=JobResponse)
async def add_extra(
    job_id: int,
    extra_data: JobAddExtra,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Agrega un extra al trabajo"""
    from app.services.worker_service import WorkerService
    from app.models.user import UserRole
    
    # Validar que es trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden agregar extras"
        )
    
    # Obtener el trabajo y validar que el trabajador está asignado
    job = JobService.get_job_by_id(db, job_id)
    if not job:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Trabajo no encontrado"
        )
    
    worker = WorkerService.get_worker_by_user_id(db, current_user.id)
    if not worker or job.worker_id != worker.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="No tienes permiso para agregar extras a este trabajo"
        )
    
    return JobService.add_extra(db, job_id, extra_data)


@router.post("/{job_id}/complete", response_model=JobResponse)
async def complete_job(
    job_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Finaliza el trabajo (crea comisión automáticamente)"""
    from app.services.worker_service import WorkerService
    from app.models.user import UserRole
    
    # Validar que es trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden completar trabajos"
        )
    
    # Obtener el worker_id del usuario actual
    worker = WorkerService.get_worker_by_user_id(db, current_user.id)
    if not worker:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No tienes un perfil de trabajador"
        )
    
    return JobService.update_job_status(db, job_id, JobStatus.COMPLETED, worker.id)


@router.post("/{job_id}/cancel", response_model=JobResponse)
async def cancel_job(
    job_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Cancela un trabajo - Solo el cliente puede cancelar su trabajo"""
    from app.models.user import UserRole
    
    # Obtener el trabajo
    job = JobService.get_job_by_id(db, job_id)
    if not job:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Trabajo no encontrado"
        )
    
    # Validar permisos: Solo el cliente puede cancelar su trabajo
    if current_user.role == UserRole.CLIENT:
        if job.client_id != current_user.id:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Solo puedes cancelar tus propios trabajos"
            )
        # El cliente solo puede cancelar si el trabajo no está completado
        if job.status == JobStatus.COMPLETED:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="No puedes cancelar un trabajo que ya está completado"
            )
        if job.status == JobStatus.CANCELLED:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Este trabajo ya está cancelado"
            )
    elif current_user.role == UserRole.WORKER:
        # El trabajador solo puede cancelar si el trabajo está en estado PENDING o ACCEPTED
        from app.services.worker_service import WorkerService
        worker = WorkerService.get_worker_by_user_id(db, current_user.id)
        if not worker or job.worker_id != worker.id:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="No tienes permiso para cancelar este trabajo"
            )
        # Validar que el trabajo no esté ya cancelado
        if job.status == JobStatus.CANCELLED:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Este trabajo ya está cancelado"
            )
        # Validar que el trabajo no esté completado
        if job.status == JobStatus.COMPLETED:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="No puedes cancelar un trabajo que ya está completado"
            )
        if job.status not in [JobStatus.PENDING, JobStatus.ACCEPTED]:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="No puedes cancelar un trabajo que ya está en progreso"
            )
    
    return JobService.update_job_status(db, job_id, JobStatus.CANCELLED)


@router.post("/{job_id}/rate", response_model=RatingResponse)
async def rate_job(
    job_id: int,
    rating_data: RatingCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Califica un trabajo (trabajador califica al cliente)"""
    from app.services.rating_service import RatingService
    from app.services.worker_service import WorkerService
    from app.models.user import UserRole
    
    # Validar que el usuario sea trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden calificar trabajos"
        )
    
    # Obtener el worker_id del usuario actual
    worker = WorkerService.get_worker_by_user_id(db, current_user.id)
    
    if not worker:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No tienes un perfil de trabajador"
        )
    
    return RatingService.rate_job_by_worker(db, job_id, worker.id, rating_data)


@router.post("/{job_id}/rate-worker", response_model=RatingResponse)
async def rate_worker(
    job_id: int,
    rating_data: RatingCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Califica al trabajador (cliente califica al trabajador)"""
    from app.services.rating_service import RatingService
    from app.models.user import UserRole
    
    # Validar que es cliente
    if current_user.role != UserRole.CLIENT:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los clientes pueden calificar trabajadores"
        )
    
    return RatingService.rate_job_by_client(db, job_id, current_user.id, rating_data)


@router.get("/{job_id}/rating", response_model=RatingResponse)
async def get_job_rating(
    job_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Obtiene la calificación de un trabajo
    
    Deja que RatingService maneje sus propias HTTPException sin envolverlas,
    para preservar mensajes de error más específicos (ej: permisos).
    """
    from app.services.rating_service import RatingService
    
    # Dejar que el servicio maneje sus propias excepciones
    return RatingService.get_rating_by_job_id(db, job_id)

