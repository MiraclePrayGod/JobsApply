import logging
from sqlalchemy.orm import Session
from sqlalchemy import func
from typing import List
from datetime import datetime
from fastapi import HTTPException, status
from app.models.commission import Commission, CommissionStatus
from app.models.user import UserRole
from app.schemas.commission import CommissionSubmitPayment, CommissionReview

logger = logging.getLogger(__name__)


class CommissionService:
    """Servicio de comisiones (equivalente a @Service en Spring Boot)"""
    
    @staticmethod
    def get_pending_commissions(db: Session, worker_id: int) -> List[Commission]:
        """Obtiene comisiones pendientes de un trabajador"""
        from sqlalchemy.orm import joinedload
        return db.query(Commission).options(
            joinedload(Commission.job)
        ).filter(
            Commission.worker_id == worker_id,
            Commission.status == CommissionStatus.PENDING
        ).all()
    
    @staticmethod
    def get_commission_history(db: Session, worker_id: int) -> List[Commission]:
        """Obtiene historial completo de comisiones de un trabajador"""
        from sqlalchemy.orm import joinedload
        return db.query(Commission).options(
            joinedload(Commission.job)
        ).filter(
            Commission.worker_id == worker_id
        ).order_by(Commission.created_at.desc()).all()
    
    @staticmethod
    def submit_payment(
        db: Session,
        commission_id: int,
        worker_id: int,
        payment_data: CommissionSubmitPayment
    ) -> Commission:
        """Trabajador adjunta código Yape para pagar comisión"""
        from sqlalchemy.exc import IntegrityError
        
        try:
            commission = db.query(Commission).filter(
                Commission.id == commission_id,
                Commission.worker_id == worker_id
            ).first()
            
            if not commission:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Comisión no encontrada"
                )
            
            if commission.status != CommissionStatus.PENDING:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Esta comisión ya fue procesada"
                )
            
            # Actualizar estado a "payment_submitted"
            commission.status = CommissionStatus.PAYMENT_SUBMITTED
            commission.payment_code = payment_data.payment_code
            commission.payment_proof_url = payment_data.payment_proof_url
            commission.submitted_at = datetime.utcnow()  # Más eficiente que consulta a BD
            
            db.commit()
            db.refresh(commission)
            
            return commission
        except HTTPException:
            # Re-lanzar HTTPException
            raise
        except IntegrityError:
            # Condición de carrera o constraint violation
            db.rollback()
            logger.warning(f"Error de integridad al enviar pago de comisión {commission_id}")
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Error al enviar el pago"
            )
        except Exception as e:
            # Rollback en caso de error
            db.rollback()
            logger.exception(f"Error al enviar pago de comisión {commission_id}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error interno del servidor"
            )
    
    @staticmethod
    def approve_payment(
        db: Session,
        commission_id: int,
        manager_id: int,
        review_data: CommissionReview
    ) -> Commission:
        """Manager aprueba el pago de comisión"""
        from sqlalchemy.exc import IntegrityError
        
        try:
            commission = db.query(Commission).filter(
                Commission.id == commission_id
            ).first()
            
            if not commission:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Comisión no encontrada"
                )
            
            if commission.status != CommissionStatus.PAYMENT_SUBMITTED:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Esta comisión no está en estado de revisión"
                )
            
            # Aprobar pago
            commission.status = CommissionStatus.APPROVED
            commission.reviewed_by = manager_id
            commission.reviewed_at = datetime.utcnow()  # Más eficiente que consulta a BD
            if review_data.notes:
                commission.notes = review_data.notes
            
            db.commit()
            db.refresh(commission)
            
            logger.info(f"Comisión {commission_id} aprobada por manager {manager_id}")
            return commission
        except HTTPException:
            # Re-lanzar HTTPException
            raise
        except IntegrityError:
            # Condición de carrera o constraint violation
            db.rollback()
            logger.warning(f"Error de integridad al aprobar comisión {commission_id}")
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Error al aprobar la comisión"
            )
        except Exception as e:
            # Rollback en caso de error
            db.rollback()
            logger.exception(f"Error al aprobar comisión {commission_id}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error interno del servidor"
            )
    
    @staticmethod
    def reject_payment(
        db: Session,
        commission_id: int,
        manager_id: int,
        review_data: CommissionReview
    ) -> Commission:
        """Manager rechaza el pago de comisión
        
        Vuelve el estado a PENDING para que el trabajador pueda corregir y reenviar.
        """
        from sqlalchemy.exc import IntegrityError
        
        try:
            commission = db.query(Commission).filter(
                Commission.id == commission_id
            ).first()
            
            if not commission:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Comisión no encontrada"
                )
            
            if commission.status != CommissionStatus.PAYMENT_SUBMITTED:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Esta comisión no está en estado de revisión"
                )
            
            if not review_data.notes:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Debe proporcionar una razón para rechazar"
                )
            
            # Rechazar y volver a pendiente para que el trabajador pueda corregir
            commission.status = CommissionStatus.PENDING
            commission.reviewed_by = manager_id
            commission.reviewed_at = datetime.utcnow()  # Más eficiente que consulta a BD
            commission.notes = review_data.notes
            commission.payment_code = None
            commission.payment_proof_url = None
            commission.submitted_at = None
            
            db.commit()
            db.refresh(commission)
            
            logger.info(f"Comisión {commission_id} rechazada por manager {manager_id}: {review_data.notes}")
            return commission
        except HTTPException:
            # Re-lanzar HTTPException
            raise
        except IntegrityError:
            # Condición de carrera o constraint violation
            db.rollback()
            logger.warning(f"Error de integridad al rechazar comisión {commission_id}")
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Error al rechazar la comisión"
            )
        except Exception as e:
            # Rollback en caso de error
            db.rollback()
            logger.exception(f"Error al rechazar comisión {commission_id}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error interno del servidor"
            )
    
    @staticmethod
    def get_pending_review_commissions(db: Session) -> List[Commission]:
        """Obtiene comisiones pendientes de revisión (para manager)"""
        return db.query(Commission).filter(
            Commission.status == CommissionStatus.PAYMENT_SUBMITTED
        ).all()

