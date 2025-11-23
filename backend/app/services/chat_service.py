from sqlalchemy.orm import Session
from typing import List, Optional
from fastapi import HTTPException, status
from app.models.message import Message
from app.models.job import Job
from app.models.user import User
from app.schemas.message import MessageCreate, MessageResponse, SenderInfo
from sqlalchemy.orm import joinedload


class ChatService:
    """Servicio de chat"""
    
    @staticmethod
    def create_message(db: Session, message_create: MessageCreate, sender_id: int) -> Message:
        """Crea un nuevo mensaje"""
        from app.models.job_application import JobApplication
        from app.services.worker_service import WorkerService
        
        # Verificar que el trabajo existe
        job = db.query(Job).filter(Job.id == message_create.job_id).first()
        if not job:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Trabajo no encontrado"
            )
        
        # Si hay application_id, verificar que existe y que el usuario tiene acceso
        application = None
        if message_create.application_id:
            application = db.query(JobApplication).filter(
                JobApplication.id == message_create.application_id,
                JobApplication.job_id == message_create.job_id
            ).first()
            
            if not application:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Aplicación no encontrada"
                )
        
        # Verificar acceso: cliente o trabajador de la aplicación
        is_client = job.client_id == sender_id
        
        is_worker = False
        if application:
            # Verificar que el trabajador de la aplicación es el que envía
            worker = WorkerService.get_worker_by_user_id(db, sender_id)
            is_worker = worker is not None and worker.id == application.worker_id
        elif job.worker_id is not None:
            # Para compatibilidad con trabajos ya aceptados (sin application_id)
            worker = WorkerService.get_worker_by_user_id(db, sender_id)
            is_worker = worker is not None and worker.id == job.worker_id
        
        if not is_client and not is_worker:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="No tienes acceso a este chat"
            )
        
        new_message = Message(
            job_id=message_create.job_id,
            application_id=message_create.application_id,
            sender_id=sender_id,
            content=message_create.content,
            has_image=message_create.has_image,
            image_url=message_create.image_url
        )
        
        db.add(new_message)
        db.commit()
        db.refresh(new_message)
        
        return new_message
    
    @staticmethod
    def get_messages_by_job(db: Session, job_id: int, user_id: int, application_id: Optional[int] = None) -> List[Message]:
        """Obtiene los mensajes de un trabajo (opcionalmente filtrados por aplicación)"""
        from app.models.job_application import JobApplication
        from app.services.worker_service import WorkerService
        
        # Verificar que el trabajo existe
        job = db.query(Job).filter(Job.id == job_id).first()
        if not job:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Trabajo no encontrado"
            )
        
        # Si hay application_id, verificar acceso a esa aplicación
        if application_id:
            application = db.query(JobApplication).filter(
                JobApplication.id == application_id,
                JobApplication.job_id == job_id
            ).first()
            
            if not application:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Aplicación no encontrada"
                )
            
            # Verificar acceso: cliente o trabajador de la aplicación
            is_client = job.client_id == user_id
            worker = WorkerService.get_worker_by_user_id(db, user_id)
            is_worker = worker is not None and worker.id == application.worker_id
            
            if not is_client and not is_worker:
                raise HTTPException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    detail="No tienes acceso a este chat"
                )
            
            # Filtrar mensajes por application_id
            messages = db.query(Message)\
                .options(joinedload(Message.sender))\
                .filter(
                    Message.job_id == job_id,
                    Message.application_id == application_id
                )\
                .order_by(Message.created_at.asc())\
                .all()
        else:
            # Sin application_id: verificar acceso general (para compatibilidad)
            is_client = job.client_id == user_id
            is_worker = False
            if job.worker_id is not None:
                worker = WorkerService.get_worker_by_user_id(db, user_id)
                is_worker = worker is not None and worker.id == job.worker_id
            
            if not is_client and not is_worker:
                raise HTTPException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    detail="No tienes acceso a este chat"
                )
            
            # Mensajes sin application_id (compatibilidad)
            messages = db.query(Message)\
                .options(joinedload(Message.sender))\
                .filter(
                    Message.job_id == job_id,
                    Message.application_id.is_(None)
                )\
                .order_by(Message.created_at.asc())\
                .all()
        
        return messages
    
    @staticmethod
    def message_to_response(message: Message) -> MessageResponse:
        """Convierte un modelo Message a MessageResponse"""
        sender_info = None
        if message.sender:
            sender_info = SenderInfo(
                id=message.sender.id,
                full_name=message.sender.full_name,
                email=message.sender.email
            )
        
        return MessageResponse(
            id=message.id,
            job_id=message.job_id,
            application_id=message.application_id,
            sender_id=message.sender_id,
            content=message.content,
            has_image=message.has_image,
            image_url=message.image_url,
            sender=sender_info,
            created_at=message.created_at
        )

