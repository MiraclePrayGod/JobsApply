from datetime import datetime, timedelta
from decimal import Decimal
from sqlalchemy.orm import Session
from fastapi import HTTPException, status
from app.models.subscription import WorkerSubscription, SubscriptionPlan, SubscriptionStatus
from app.models.worker import Worker


class SubscriptionService:
    @staticmethod
    def _get_worker(db: Session, user_id: int) -> Worker:
        from app.services.worker_service import WorkerService
        worker = WorkerService.get_worker_by_user_id(db, user_id)
        if not worker:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="No tienes un perfil de trabajador"
            )
        return worker

    @staticmethod
    def create_subscription(db: Session, user_id: int, plan: SubscriptionPlan, payment_code: str) -> WorkerSubscription:
        worker = SubscriptionService._get_worker(db, user_id)

        now = datetime.utcnow()

        # Definir días y monto
        if plan == SubscriptionPlan.DAILY:
            days = 1
            amount = Decimal("2.00")
        elif plan == SubscriptionPlan.WEEKLY:
            days = 7
            amount = Decimal("12.00")  # 7 días * 2 - 2 (1 día gratis)
        else:
            raise HTTPException(status_code=400, detail="Plan inválido")
        
        # Si ya tiene Plus activo, extender desde la fecha de expiración
        start_base = worker.plus_expires_at if worker.plus_expires_at and worker.plus_expires_at > now else now
        valid_from = start_base
        valid_until = start_base + timedelta(days=days)

        subscription = WorkerSubscription(
            worker_id=worker.id,
            plan=plan,
            days=days,
            amount=amount,
            status=SubscriptionStatus.ACTIVE,  # simulamos pago aprobado
            payment_code=payment_code,
            payment_method="yape",
            valid_from=valid_from,
            valid_until=valid_until,
        )

        db.add(subscription)

        # Actualizar estado Plus del worker
        worker.is_plus_active = True
        worker.plus_expires_at = valid_until

        db.commit()
        db.refresh(subscription)
        db.refresh(worker)

        return subscription

    @staticmethod
    def get_status(db: Session, user_id: int):
        worker = SubscriptionService._get_worker(db, user_id)
        now = datetime.utcnow()
        
        # Verificar si el Plus está activo basándose en la fecha de expiración
        # Solo considerar inactivo si la fecha de expiración existe y ya pasó
        if worker.is_plus_active and worker.plus_expires_at:
            is_active = worker.plus_expires_at > now
            # Solo desactivar si la fecha ya venció
            if not is_active:
                worker.is_plus_active = False
                db.commit()
                db.refresh(worker)
        else:
            # Si no hay fecha de expiración pero el flag está activo, mantenerlo activo
            # (no desactivar automáticamente)
            is_active = worker.is_plus_active

        # Obtener la última suscripción
        last_sub = (
            db.query(WorkerSubscription)
            .filter(WorkerSubscription.worker_id == worker.id)
            .order_by(WorkerSubscription.created_at.desc())
            .first()
        )

        return worker, is_active, last_sub

    @staticmethod
    def get_history(db: Session, user_id: int):
        worker = SubscriptionService._get_worker(db, user_id)
        return (
            db.query(WorkerSubscription)
            .filter(WorkerSubscription.worker_id == worker.id)
            .order_by(WorkerSubscription.created_at.desc())
            .all()
        )

    @staticmethod
    def cancel_subscription(db: Session, user_id: int):
        worker = SubscriptionService._get_worker(db, user_id)
        
        # Desactivar Modo Plus del worker
        worker.is_plus_active = False
        worker.plus_expires_at = None
        
        # Marcar suscripciones activas como canceladas
        now = datetime.utcnow()
        active_subs = (
            db.query(WorkerSubscription)
            .filter(
                WorkerSubscription.worker_id == worker.id,
                WorkerSubscription.status == SubscriptionStatus.ACTIVE,
                WorkerSubscription.valid_until > now
            )
            .all()
        )
        
        for sub in active_subs:
            sub.status = SubscriptionStatus.CANCELLED
        
        db.commit()
        db.refresh(worker)
        
        return {"message": "Suscripción cancelada exitosamente"}

