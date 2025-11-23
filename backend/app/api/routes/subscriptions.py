from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List
from app.database import get_db
from app.utils.dependencies import get_current_user
from app.models.user import User
from app.services.subscription_service import SubscriptionService
from app.schemas.subscription import (
    CreateSubscriptionRequest,
    SubscriptionResponse,
    SubscriptionStatusResponse,
)

router = APIRouter(prefix="/api/subscriptions", tags=["Subscriptions"])


@router.post("/subscribe", response_model=SubscriptionResponse, status_code=status.HTTP_201_CREATED)
async def subscribe(
    body: CreateSubscriptionRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    from app.models.user import UserRole
    if current_user.role != UserRole.WORKER:
        raise HTTPException(status_code=403, detail="Solo los trabajadores pueden suscribirse al Modo Plus")

    sub = SubscriptionService.create_subscription(db, current_user.id, body.plan, body.payment_code)
    return sub


@router.get("/me/status", response_model=SubscriptionStatusResponse)
async def get_my_subscription_status(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    worker, is_active, last_sub = SubscriptionService.get_status(db, current_user.id)
    return SubscriptionStatusResponse(
        is_plus_active=is_active,
        plus_expires_at=worker.plus_expires_at,
        current_plan=last_sub,
    )


@router.get("/me/history", response_model=List[SubscriptionResponse])
async def get_my_subscription_history(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    history = SubscriptionService.get_history(db, current_user.id)
    return history


@router.post("/cancel")
async def cancel_subscription(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    from app.models.user import UserRole
    if current_user.role != UserRole.WORKER:
        raise HTTPException(status_code=403, detail="Solo los trabajadores pueden cancelar suscripciones")
    
    result = SubscriptionService.cancel_subscription(db, current_user.id)
    return result
