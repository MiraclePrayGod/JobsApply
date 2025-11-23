from sqlalchemy import Column, Integer, ForeignKey, DateTime, Boolean, UniqueConstraint
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


class JobApplication(Base):
    """Modelo de Aplicación de Trabajador a un Trabajo
    
    Un trabajador solo puede aplicar una vez a cada trabajo.
    La restricción UniqueConstraint garantiza esto a nivel de BD.
    """
    __tablename__ = "job_applications"
    __table_args__ = (
        UniqueConstraint('job_id', 'worker_id', name='uq_job_worker_application'),
    )

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    job_id = Column(Integer, ForeignKey("jobs.id", ondelete="CASCADE"), nullable=False)
    worker_id = Column(Integer, ForeignKey("workers.id", ondelete="CASCADE"), nullable=False)
    is_accepted = Column(Boolean, default=False)  # True cuando el cliente acepta este trabajador
    created_at = Column(DateTime, server_default=func.now())
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now())

    # Relaciones
    job = relationship("Job", back_populates="applications")
    worker = relationship("Worker", back_populates="job_applications")
    messages = relationship("Message", back_populates="application", cascade="all, delete-orphan")

