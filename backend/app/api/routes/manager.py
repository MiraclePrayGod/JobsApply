from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List
from app.database import get_db
from app.utils.dependencies import get_current_user
from app.models.user import User, UserRole
from app.schemas.commission import CommissionResponse, CommissionReview
from app.services.commission_service import CommissionService

router = APIRouter(prefix="/api/manager", tags=["Manager"])


def verify_manager(current_user: User):
    """Verifica que el usuario sea manager"""
    if current_user.role != UserRole.MANAGER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="No tienes permisos de manager"
        )


@router.get("/commissions/pending-review", response_model=List[CommissionResponse])
async def get_pending_review_commissions(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Obtiene comisiones pendientes de revisión (solo manager)"""
    verify_manager(current_user)
    return CommissionService.get_pending_review_commissions(db)


@router.post("/commissions/{commission_id}/approve", response_model=CommissionResponse)
async def approve_payment(
    commission_id: int,
    review_data: CommissionReview,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Manager aprueba el pago de comisión"""
    verify_manager(current_user)
    return CommissionService.approve_payment(db, commission_id, current_user.id, review_data)


@router.post("/commissions/{commission_id}/reject", response_model=CommissionResponse)
async def reject_payment(
    commission_id: int,
    review_data: CommissionReview,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Manager rechaza el pago de comisión"""
    verify_manager(current_user)
    return CommissionService.reject_payment(db, commission_id, current_user.id, review_data)

