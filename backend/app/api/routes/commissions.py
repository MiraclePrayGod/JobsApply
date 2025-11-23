from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List
from app.database import get_db
from app.utils.dependencies import get_current_user
from app.models.user import User
from app.schemas.commission import CommissionResponse, CommissionSubmitPayment
from app.services.commission_service import CommissionService
from app.services.worker_service import WorkerService

router = APIRouter(prefix="/api/commissions", tags=["Commissions"])


@router.get("/pending", response_model=List[CommissionResponse])
async def get_pending_commissions(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Obtiene comisiones pendientes del trabajador actual"""
    from app.models.user import UserRole
    
    # Validar que el usuario sea trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden ver sus comisiones"
        )
    
    worker = WorkerService.get_worker_by_user_id(db, current_user.id)
    
    if not worker:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No tienes un perfil de trabajador"
        )
    
    return CommissionService.get_pending_commissions(db, worker.id)


@router.get("/history", response_model=List[CommissionResponse])
async def get_commission_history(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Obtiene historial completo de comisiones del trabajador"""
    from app.models.user import UserRole
    
    # Validar que el usuario sea trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden ver su historial de comisiones"
        )
    
    worker = WorkerService.get_worker_by_user_id(db, current_user.id)
    
    if not worker:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No tienes un perfil de trabajador"
        )
    
    return CommissionService.get_commission_history(db, worker.id)


@router.post("/{commission_id}/submit-payment", response_model=CommissionResponse)
async def submit_payment(
    commission_id: int,
    payment_data: CommissionSubmitPayment,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Trabajador adjunta código Yape para pagar comisión"""
    from app.models.user import UserRole
    
    # Validar que el usuario sea trabajador
    if current_user.role != UserRole.WORKER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Solo los trabajadores pueden enviar pagos de comisión"
        )
    
    worker = WorkerService.get_worker_by_user_id(db, current_user.id)
    
    if not worker:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No tienes un perfil de trabajador"
        )
    
    return CommissionService.submit_payment(db, commission_id, worker.id, payment_data)

