from sqlalchemy.orm import Session
from typing import List, Optional
from fastapi import HTTPException, status
from app.models.worker import Worker
from app.schemas.worker import WorkerCreate, WorkerUpdate


class WorkerService:
    """Servicio de trabajadores (equivalente a @Service en Spring Boot)"""
    
    @staticmethod
    def create_worker(db: Session, worker_create: WorkerCreate, user_id: int) -> Worker:
        """Crea un nuevo trabajador
        
        user_id se pasa como parámetro (no viene del DTO) para mayor seguridad.
        is_verified siempre se establece en False (solo manager puede verificarlo).
        """
        import logging
        from sqlalchemy.exc import IntegrityError
        
        logger = logging.getLogger(__name__)
        
        try:
            # Verificar si el user_id ya tiene un trabajador
            existing_worker = db.query(Worker).filter(Worker.user_id == user_id).first()
            if existing_worker:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Este usuario ya tiene un perfil de trabajador"
                )
            
            # Validar campos requeridos
            if not worker_create.full_name or worker_create.full_name.strip() == "":
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="El nombre completo es requerido"
                )
            
            # Crear worker con user_id del parámetro
            # Asegurar que is_verified siempre sea False (seguridad)
            worker_data = worker_create.dict()
            worker_data.pop("user_id", None)  # Remover si existe (no debería)
            worker_data.pop("is_verified", None)  # Remover si existe (seguridad)
            worker_data.pop("verification_photo_url", None)  # No se puede setear al crear
            
            new_worker = Worker(
                **worker_data,
                user_id=user_id,  # Usar el user_id del parámetro
                is_verified=False  # Siempre False al crear (solo manager puede cambiar)
            )
            
            db.add(new_worker)
            db.commit()
            db.refresh(new_worker)
            
            return new_worker
        except HTTPException:
            # Re-lanzar HTTPException
            raise
        except IntegrityError:
            # Condición de carrera: worker duplicado
            db.rollback()
            logger.warning(f"Intento de crear worker duplicado para usuario {user_id}")
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Este usuario ya tiene un perfil de trabajador"
            )
        except Exception as e:
            # Rollback en caso de error
            db.rollback()
            logger.exception(f"Error al crear worker para usuario {user_id}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error interno del servidor"
            )
    
    @staticmethod
    def get_worker_by_id(db: Session, worker_id: int) -> Optional[Worker]:
        """Obtiene un trabajador por ID"""
        return db.query(Worker).filter(Worker.id == worker_id).first()
    
    @staticmethod
    def get_worker_by_user_id(db: Session, user_id: int) -> Optional[Worker]:
        """Obtiene un trabajador por user_id"""
        return db.query(Worker).filter(Worker.user_id == user_id).first()
    
    @staticmethod
    def update_worker(db: Session, worker_id: int, worker_update: WorkerUpdate) -> Worker:
        """Actualiza un trabajador
        
        Protege is_verified: no se puede modificar desde aquí (solo manager puede).
        """
        import logging
        from sqlalchemy.exc import IntegrityError
        
        logger = logging.getLogger(__name__)
        
        try:
            worker = db.query(Worker).filter(Worker.id == worker_id).first()
            
            if not worker:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Trabajador no encontrado"
                )
            
            # Actualizar solo los campos proporcionados
            update_data = worker_update.dict(exclude_unset=True)
            
            # Proteger is_verified: no se puede modificar desde aquí (seguridad)
            update_data.pop("is_verified", None)
            
            # Actualizar campos permitidos
            for field, value in update_data.items():
                setattr(worker, field, value)
            
            db.commit()
            db.refresh(worker)
            
            return worker
        except HTTPException:
            # Re-lanzar HTTPException
            raise
        except IntegrityError:
            # Condición de carrera o constraint violation
            db.rollback()
            logger.warning(f"Error de integridad al actualizar worker {worker_id}")
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Error al actualizar el perfil"
            )
        except Exception as e:
            # Rollback en caso de error
            db.rollback()
            logger.exception(f"Error al actualizar worker {worker_id}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error interno del servidor"
            )
    
    @staticmethod
    def search_workers(
        db: Session,
        service_type: Optional[str] = None,
        district: Optional[str] = None,
        is_available: Optional[bool] = None,
        is_verified: Optional[bool] = None
    ) -> List[Worker]:
        """Busca trabajadores con filtros"""
        query = db.query(Worker)
        
        # Validar y filtrar por service_type solo si no está vacío
        if service_type and service_type.strip():
            # Buscar en el array JSON de services
            # Para MySQL: usar JSON_CONTAINS o LIKE para buscar en el JSON
            # Para compatibilidad, usamos LIKE que funciona en ambos
            service_clean = service_type.strip()
            # Buscar el servicio en el JSON usando LIKE (funciona en MySQL y PostgreSQL)
            # El JSON se almacena como ["Plomería", "Electricidad"], buscamos el texto
            from sqlalchemy import cast, String
            # Usar LIKE para búsqueda simple en el JSON convertido a string
            query = query.filter(
                cast(Worker.services, String).like(f'%"{service_clean}"%')
            )
        
        # Validar y filtrar por district solo si no está vacío
        if district and district.strip():
            query = query.filter(Worker.district == district.strip())
        
        if is_available is not None:
            query = query.filter(Worker.is_available == is_available)
        
        # Filtrar por trabajadores verificados si se especifica
        if is_verified is not None:
            query = query.filter(Worker.is_verified == is_verified)
        
        return query.all()

