import logging
from sqlalchemy.orm import Session
from sqlalchemy import func
from typing import List, Optional
from fastapi import HTTPException, status
from decimal import Decimal
from app.models.job import Job, JobStatus
from app.models.commission import Commission, CommissionStatus
from app.schemas.job import JobCreate, JobUpdate, JobAddExtra

logger = logging.getLogger(__name__)


class JobService:
    """Servicio de trabajos (equivalente a @Service en Spring Boot)"""
    
    @staticmethod
    def create_job(db: Session, job_create: JobCreate, client_id: int) -> Job:
        """Crea un nuevo trabajo con validaciones
        
        client_id se pasa como parámetro (no viene del DTO) para mayor seguridad.
        """
        # Validar que title no esté vacío
        if not job_create.title or not job_create.title.strip():
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="El título del trabajo es requerido"
            )
        
        # Validar que address no esté vacío
        if not job_create.address or not job_create.address.strip():
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="La dirección es requerida"
            )
        
        # Validar que base_fee sea mayor que cero
        if job_create.base_fee <= Decimal("0.00"):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="La tarifa base debe ser mayor que cero"
            )
        
        # Validar que service_type no esté vacío
        if not job_create.service_type or not job_create.service_type.strip():
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="El tipo de servicio es requerido"
            )
        
        # Calcular total_amount inicial (base_fee sin extras)
        # client_id se pasa como parámetro, no desde job_create
        job_data = job_create.dict()
        job_data.pop("client_id", None)  # Remover si existe (no debería)
        
        new_job = Job(
            **job_data,
            client_id=client_id,  # Usar el client_id del parámetro
            status=JobStatus.PENDING,
            extras=Decimal("0.00"),
            total_amount=job_create.base_fee
        )
        
        db.add(new_job)
        db.commit()
        db.refresh(new_job)
        
        return new_job
    
    @staticmethod
    def get_job_by_id(db: Session, job_id: int) -> Optional[Job]:
        """Obtiene un trabajo por ID con información del cliente y trabajador"""
        from sqlalchemy.orm import joinedload
        return db.query(Job).options(
            joinedload(Job.client),
            joinedload(Job.worker)
        ).filter(Job.id == job_id).first()
    
    @staticmethod
    def get_available_jobs(db: Session, service_type: Optional[str] = None, search_query: Optional[str] = None) -> List[Job]:
        """Obtiene trabajos disponibles (pendientes, NO aceptados)"""
        from sqlalchemy import or_
        
        query = db.query(Job).filter(
            Job.status == JobStatus.PENDING  # Solo trabajos pendientes (no aceptados)
            # Nota: Ahora múltiples trabajadores pueden aplicar, así que no filtramos por worker_id
        )
        
        # Validar y filtrar por service_type solo si no está vacío
        if service_type and service_type.strip():
            query = query.filter(Job.service_type == service_type.strip())
        
        # Validar y filtrar por search_query solo si no está vacío
        if search_query and search_query.strip():
            search_term = f"%{search_query.strip().lower()}%"
            # Usar func.lower() + like() para compatibilidad con MySQL (ilike solo funciona en PostgreSQL)
            query = query.filter(
                or_(
                    func.lower(Job.title).like(search_term),
                    func.lower(Job.description).like(search_term),
                    func.lower(Job.address).like(search_term),
                    func.lower(Job.service_type).like(search_term)
                )
            )
        
        return query.order_by(Job.created_at.desc()).all()  # Más recientes primero
    
    @staticmethod
    def get_worker_jobs(db: Session, worker_id: int) -> List[Job]:
        """Obtiene los trabajos activos de un trabajador (excluye completados y cancelados)"""
        from sqlalchemy import case
        
        # Solo trabajos activos: ACCEPTED, IN_ROUTE, ON_SITE, IN_PROGRESS
        # Excluir COMPLETED y CANCELLED
        active_statuses = [
            JobStatus.ACCEPTED,
            JobStatus.IN_ROUTE,
            JobStatus.ON_SITE,
            JobStatus.IN_PROGRESS
        ]
        
        # Ordenar por prioridad de estado (estados más avanzados primero)
        # IN_PROGRESS (1) > ON_SITE (2) > IN_ROUTE (3) > ACCEPTED (4)
        status_priority = case(
            (Job.status == JobStatus.IN_PROGRESS, 1),
            (Job.status == JobStatus.ON_SITE, 2),
            (Job.status == JobStatus.IN_ROUTE, 3),
            (Job.status == JobStatus.ACCEPTED, 4),
            else_=999
        )
        
        return db.query(Job).filter(
            Job.worker_id == worker_id,
            Job.status.in_(active_statuses)
        ).order_by(
            status_priority.asc(),  # Prioridad menor = más importante
            Job.created_at.desc()  # Más recientes primero
        ).all()
    
    @staticmethod
    def get_client_jobs(db: Session, client_id: int) -> List[Job]:
        """Obtiene los trabajos de un cliente ordenados por relevancia (activos primero, luego por fecha)"""
        from sqlalchemy import case
        
        # Priorizar trabajos activos sobre completados/cancelados
        # Estados activos: PENDING, ACCEPTED, IN_ROUTE, ON_SITE, IN_PROGRESS
        # Estados finales: COMPLETED, CANCELLED
        active_statuses = [
            JobStatus.PENDING,
            JobStatus.ACCEPTED,
            JobStatus.IN_ROUTE,
            JobStatus.ON_SITE,
            JobStatus.IN_PROGRESS
        ]
        
        # Ordenar por prioridad de estado (activos primero)
        # Estados activos tienen prioridad 1, finales tienen prioridad 2
        status_priority = case(
            (Job.status.in_(active_statuses), 1),
            else_=2
        )
        
        return db.query(Job).filter(
            Job.client_id == client_id
        ).order_by(
            status_priority.asc(),  # Activos primero
            Job.created_at.desc()  # Más recientes primero
        ).all()
    
    @staticmethod
    def apply_to_job(db: Session, job_id: int, worker_id: int):
        """Aplica a un trabajo (trabajador aplica, pero NO cambia el estado)
        
        Valida que:
        - El trabajo exista y esté en estado PENDING
        - El trabajador no haya aplicado ya
        - El trabajo no tenga ya un trabajador asignado
        - El trabajador tenga Modo Plus activo
        """
        from app.models.job_application import JobApplication
        from app.models.worker import Worker
        from sqlalchemy.exc import IntegrityError
        from datetime import datetime
        
        try:
            job = db.query(Job).filter(Job.id == job_id).first()
            
            if not job:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Trabajo no encontrado"
                )
            
            # Validar que el trabajo esté en estado PENDING
            if job.status != JobStatus.PENDING:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Este trabajo ya no está disponible"
                )
            
            # Validar que el trabajo no tenga ya un trabajador asignado
            if job.worker_id is not None:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Este trabajo ya tiene un trabajador asignado"
                )
            
            # Obtener worker y validar Modo Plus
            worker = db.query(Worker).filter(Worker.id == worker_id).first()
            if not worker:
                raise HTTPException(status_code=404, detail="No tienes un perfil de trabajador")

            now = datetime.utcnow()
            if not (worker.is_plus_active and worker.plus_expires_at and worker.plus_expires_at > now):
                raise HTTPException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    detail="Necesitas un plan Modo Plus activo para aplicar a trabajos"
                )
            
            # Validar que el trabajador no haya aplicado ya
            existing_application = db.query(JobApplication).filter(
                JobApplication.job_id == job_id,
                JobApplication.worker_id == worker_id
            ).first()
            
            if existing_application:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Ya has aplicado a este trabajo"
                )
            
            # Crear aplicación (NO cambia el estado del trabajo)
            application = JobApplication(
                job_id=job_id,
                worker_id=worker_id,
                is_accepted=False
            )
            
            db.add(application)
            db.commit()
            db.refresh(application)
            
            return application
        except HTTPException:
            # Re-lanzar HTTPException
            raise
        except IntegrityError:
            # Condición de carrera: aplicación duplicada
            db.rollback()
            logger.warning(f"Intento de aplicación duplicada: trabajo {job_id}, trabajador {worker_id}")
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Ya has aplicado a este trabajo"
            )
        except Exception as e:
            # Rollback en caso de error
            db.rollback()
            logger.exception(f"Error al aplicar a trabajo {job_id}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error interno del servidor"
            )
    
    @staticmethod
    def client_accept_worker(db: Session, job_id: int, application_id: int, client_id: int) -> Job:
        """Cliente acepta un trabajador (cambia el estado a ACCEPTED)
        
        Valida que:
        - El trabajo pertenezca al cliente
        - El trabajo esté en estado PENDING
        - El trabajo no tenga ya un trabajador asignado
        - La aplicación pertenezca al trabajo
        - El trabajador no esté ya asignado a otro trabajo activo
        """
        from app.models.job_application import JobApplication
        from sqlalchemy.exc import IntegrityError
        import logging
        
        logger = logging.getLogger(__name__)
        
        try:
            job = db.query(Job).filter(Job.id == job_id).first()
            
            if not job:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Trabajo no encontrado"
                )
            
            # Validar que el cliente sea el dueño del trabajo
            if job.client_id != client_id:
                raise HTTPException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    detail="No tienes permiso para aceptar trabajadores en este trabajo"
                )
            
            # Validar que el trabajo esté en estado PENDING
            if job.status != JobStatus.PENDING:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Este trabajo ya no está disponible"
                )
            
            # Validar que el trabajo no tenga ya un trabajador asignado
            if job.worker_id is not None:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Este trabajo ya tiene un trabajador asignado"
                )
            
            # Obtener la aplicación y validar que pertenezca al trabajo
            application = db.query(JobApplication).filter(
                JobApplication.id == application_id,
                JobApplication.job_id == job_id
            ).first()
            
            if not application:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Aplicación no encontrada o no pertenece a este trabajo"
                )
            
            # Validar que el trabajador no esté ya asignado a otro trabajo activo
            # (opcional: podrías permitir múltiples trabajos simultáneos)
            active_jobs = db.query(Job).filter(
                Job.worker_id == application.worker_id,
                Job.status.in_([
                    JobStatus.ACCEPTED,
                    JobStatus.IN_ROUTE,
                    JobStatus.ON_SITE,
                    JobStatus.IN_PROGRESS
                ])
            ).first()
            
            if active_jobs:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Este trabajador ya tiene un trabajo activo"
                )
            
            # Marcar aplicación como aceptada
            application.is_accepted = True
            
            # Asignar trabajador al trabajo y cambiar estado
            job.worker_id = application.worker_id
            job.status = JobStatus.ACCEPTED
            
            db.commit()
            db.refresh(job)
            
            return job
        except HTTPException:
            # Re-lanzar HTTPException
            raise
        except IntegrityError:
            # Condición de carrera: dos aceptaciones simultáneas
            db.rollback()
            logger.warning(f"Intento de aceptar trabajador duplicado para trabajo {job_id}")
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Este trabajo ya tiene un trabajador asignado"
            )
        except Exception as e:
            # Rollback en caso de error
            db.rollback()
            logger.exception(f"Error al aceptar trabajador para trabajo {job_id}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error interno del servidor"
            )
    
    @staticmethod
    def get_job_applications(db: Session, job_id: int, client_id: int) -> List:
        """Obtiene las aplicaciones de trabajadores para un trabajo (solo para el cliente)"""
        from app.models.job_application import JobApplication
        from sqlalchemy.orm import joinedload
        
        # Verificar que el trabajo existe y pertenece al cliente
        job = db.query(Job).filter(Job.id == job_id).first()
        if not job:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Trabajo no encontrado"
            )
        
        if job.client_id != client_id:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="No tienes permiso para ver las aplicaciones de este trabajo"
            )
        
        # Obtener todas las aplicaciones con información del trabajador
        from app.models.worker import Worker
        applications = db.query(JobApplication).options(
            joinedload(JobApplication.worker).joinedload(Worker.user)
        ).filter(JobApplication.job_id == job_id).order_by(JobApplication.created_at.desc()).all()
        
        return applications
    
    @staticmethod
    def get_worker_applications(db: Session, worker_id: int) -> List:
        """Obtiene las aplicaciones de un trabajador con relaciones cargadas"""
        from app.models.job_application import JobApplication
        from sqlalchemy.orm import joinedload
        
        # Obtener todas las aplicaciones con información del trabajo y worker
        applications = db.query(JobApplication).options(
            joinedload(JobApplication.job).joinedload(Job.client),
            joinedload(JobApplication.worker)
        ).filter(JobApplication.worker_id == worker_id).order_by(JobApplication.created_at.desc()).all()
        
        return applications
    
    @staticmethod
    def worker_has_applied_to_job(db: Session, worker_id: int, job_id: int) -> bool:
        """Verifica si un trabajador ha aplicado a un trabajo"""
        from app.models.job_application import JobApplication
        
        application = db.query(JobApplication).filter(
            JobApplication.job_id == job_id,
            JobApplication.worker_id == worker_id
        ).first()
        
        return application is not None
    
    @staticmethod
    def update_job_status(db: Session, job_id: int, new_status: JobStatus, worker_id: Optional[int] = None) -> Job:
        """Actualiza el estado de un trabajo con validación de transiciones y trabajador
        
        Valida transiciones de estado y permisos del trabajador.
        Crea comisión automáticamente al completar el trabajo.
        """
        import logging
        from sqlalchemy.exc import IntegrityError
        
        logger = logging.getLogger(__name__)
        
        try:
            job = db.query(Job).filter(Job.id == job_id).first()
            
            if not job:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Trabajo no encontrado"
                )
            
            # Validar que el trabajo no esté cancelado (no se puede modificar un trabajo cancelado)
            if job.status == JobStatus.CANCELLED:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="No se puede modificar un trabajo cancelado"
                )
            
            # Validar que el trabajo no esté completado (excepto si se está cancelando)
            if job.status == JobStatus.COMPLETED and new_status != JobStatus.CANCELLED:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="No se puede modificar un trabajo completado"
                )
            
            # Validar que el trabajador asignado sea el que hace la acción (si se proporciona worker_id)
            if worker_id is not None:
                if job.worker_id != worker_id:
                    raise HTTPException(
                        status_code=status.HTTP_403_FORBIDDEN,
                        detail="No tienes permiso para modificar este trabajo"
                    )
            
            # Validar transiciones de estado válidas
            valid_transitions = {
                JobStatus.PENDING: [JobStatus.ACCEPTED, JobStatus.CANCELLED],
                JobStatus.ACCEPTED: [JobStatus.IN_ROUTE, JobStatus.CANCELLED],
                JobStatus.IN_ROUTE: [JobStatus.ON_SITE, JobStatus.CANCELLED],
                JobStatus.ON_SITE: [JobStatus.IN_PROGRESS, JobStatus.CANCELLED],
                JobStatus.IN_PROGRESS: [JobStatus.COMPLETED, JobStatus.CANCELLED],
                JobStatus.COMPLETED: [],  # Estado final, no se puede cambiar
                JobStatus.CANCELLED: []  # Estado final, no se puede cambiar
            }
            
            current_status = job.status
            if new_status not in valid_transitions.get(current_status, []):
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=f"No se puede cambiar de {current_status.value} a {new_status.value}. Transición inválida."
                )
            
            # Actualizar estado
            job.status = new_status
            
            # Actualizar timestamps según el estado
            # Usar datetime.utcnow() en lugar de consulta a BD (más eficiente)
            from datetime import datetime
            if new_status == JobStatus.IN_PROGRESS:
                job.started_at = datetime.utcnow()
            elif new_status == JobStatus.COMPLETED:
                job.completed_at = datetime.utcnow()
                # Crear comisión automáticamente
                JobService._create_commission(db, job)
            
            db.commit()
            db.refresh(job)
            
            return job
        except HTTPException:
            # Re-lanzar HTTPException
            raise
        except IntegrityError:
            # Condición de carrera o constraint violation
            db.rollback()
            logger.warning(f"Error de integridad al actualizar estado del trabajo {job_id}")
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Error al actualizar el estado del trabajo"
            )
        except Exception as e:
            # Rollback en caso de error
            db.rollback()
            logger.exception(f"Error al actualizar estado del trabajo {job_id}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error interno del servidor"
            )
    
    @staticmethod
    def add_extra(db: Session, job_id: int, extra_data: JobAddExtra) -> Job:
        """Agrega un extra al trabajo"""
        job = db.query(Job).filter(Job.id == job_id).first()
        
        if not job:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Trabajo no encontrado"
            )
        
        # Validar que el trabajo tenga un trabajador asignado
        if job.worker_id is None:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="El trabajo debe tener un trabajador asignado para agregar extras"
            )
        
        # Validar que el trabajo esté en un estado válido para agregar extras
        # Solo se pueden agregar extras cuando el trabajo está en progreso o estados anteriores
        valid_statuses_for_extras = [
            JobStatus.ACCEPTED,
            JobStatus.IN_ROUTE,
            JobStatus.ON_SITE,
            JobStatus.IN_PROGRESS
        ]
        
        if job.status not in valid_statuses_for_extras:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"No se pueden agregar extras a un trabajo con estado {job.status.value}"
            )
        
        # Validar que el monto del extra sea mayor que cero (doble verificación)
        if extra_data.extra_amount <= Decimal("0.00"):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="El monto del extra debe ser mayor que cero"
            )
        
        # Actualizar extras y recalcular total_amount
        # Proteger contra None (por si hay registros antiguos)
        current_extras = job.extras if job.extras is not None else Decimal("0.00")
        job.extras = current_extras + extra_data.extra_amount
        # Recalcular total_amount para mantener consistencia
        job.total_amount = job.base_fee + job.extras
        
        db.commit()
        db.refresh(job)
        
        return job
    
    @staticmethod
    def _create_commission(db: Session, job: Job) -> Commission:
        """Crea una comisión automáticamente cuando se completa un trabajo
        
        Nuevo modelo de negocio: no usamos comisiones por trabajo
        """
        # Nuevo modelo de negocio: no usamos comisiones por trabajo
        return None

