from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional
from decimal import Decimal
from app.models.subscription import SubscriptionPlan, SubscriptionStatus


class CreateSubscriptionRequest(BaseModel):
    plan: SubscriptionPlan = Field(..., description="daily o weekly")
    payment_code: str = Field(..., min_length=4, description="Código simulado de Yape")


class SubscriptionResponse(BaseModel):
    id: int
    plan: SubscriptionPlan
    days: int
    amount: Decimal
    status: SubscriptionStatus
    valid_from: datetime
    valid_until: datetime
    created_at: datetime

    class Config:
        from_attributes = True


class SubscriptionStatusResponse(BaseModel):
    is_plus_active: bool
    plus_expires_at: Optional[datetime] = None
    current_plan: Optional[SubscriptionResponse] = None

