from sqlalchemy.orm import Session
from fastapi import HTTPException, status
from app.models.rating import Rating
from app.models.job import Job, JobStatus
from app.schemas.rating import RatingCreate


class RatingService:
    """Servicio de calificaciones"""
    
    @staticmethod
    def rate_job_by_worker(db: Session, job_id: int, worker_id: int, rating_data: RatingCreate) -> Rating:
        """El trabajador califica al cliente"""
        # Verificar que el trabajo existe y pertenece al trabajador
        job = db.query(Job).filter(Job.id == job_id).first()
        
        if not job:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Trabajo no encontrado"
            )
        
        if job.worker_id != worker_id:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="No tienes permiso para calificar este trabajo"
            )
        
        if job.status != JobStatus.COMPLETED:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Solo puedes calificar trabajos completados"
            )
        
        # Buscar si ya existe una calificación para este trabajo
        existing_rating = db.query(Rating).filter(Rating.job_id == job_id).first()
        
        if existing_rating:
            # Actualizar la calificación existente
            existing_rating.worker_rating = rating_data.rating
            existing_rating.worker_comment = rating_data.comment
            db.commit()
            db.refresh(existing_rating)
            return existing_rating
        else:
            # Crear nueva calificación
            new_rating = Rating(
                job_id=job_id,
                worker_rating=rating_data.rating,
                worker_comment=rating_data.comment
            )
            db.add(new_rating)
            db.commit()
            db.refresh(new_rating)
            return new_rating
    
    @staticmethod
    def rate_job_by_client(db: Session, job_id: int, client_id: int, rating_data: RatingCreate) -> Rating:
        """El cliente califica al trabajador"""
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
                detail="No tienes permiso para calificar este trabajo"
            )
        
        if job.status != JobStatus.COMPLETED:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Solo puedes calificar trabajos completados"
            )
        
        # Buscar si ya existe una calificación para este trabajo
        existing_rating = db.query(Rating).filter(Rating.job_id == job_id).first()
        
        if existing_rating:
            # Actualizar la calificación existente
            existing_rating.client_rating = rating_data.rating
            existing_rating.client_comment = rating_data.comment
            db.commit()
            db.refresh(existing_rating)
            return existing_rating
        else:
            # Crear nueva calificación
            new_rating = Rating(
                job_id=job_id,
                client_rating=rating_data.rating,
                client_comment=rating_data.comment
            )
            db.add(new_rating)
            db.commit()
            db.refresh(new_rating)
            return new_rating
    
    @staticmethod
    def get_rating_by_job_id(db: Session, job_id: int) -> Rating:
        """Obtiene la calificación de un trabajo"""
        rating = db.query(Rating).filter(Rating.job_id == job_id).first()
        
        if not rating:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Calificación no encontrada"
            )
        
        return rating

