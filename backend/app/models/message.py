from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey, Boolean
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base


class Message(Base):
    """Modelo de Mensaje de Chat"""
    __tablename__ = "messages"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    job_id = Column(Integer, ForeignKey("jobs.id", ondelete="CASCADE"), nullable=False)
    application_id = Column(Integer, ForeignKey("job_applications.id", ondelete="CASCADE"), nullable=True)  # Identifica a qué aplicación pertenece el chat
    sender_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    content = Column(Text, nullable=False)
    has_image = Column(Boolean, default=False)  # Flag para indicar si el mensaje tiene imagen
    image_url = Column(String(500), nullable=True)  # URL de la imagen si tiene
    created_at = Column(DateTime, server_default=func.now())

    # Relaciones
    job = relationship("Job", back_populates="messages")
    application = relationship("JobApplication", back_populates="messages")
    sender = relationship("User", foreign_keys=[sender_id])

