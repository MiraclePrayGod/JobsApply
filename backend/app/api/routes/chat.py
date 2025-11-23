from fastapi import APIRouter, Depends, WebSocket, WebSocketDisconnect, HTTPException, status
from sqlalchemy.orm import Session
from typing import List, Dict, Optional, Tuple
import json
from app.database import get_db
from app.utils.dependencies import get_current_user
from app.models.user import User
from app.models.job import Job
from app.schemas.message import MessageCreate, MessageResponse
from app.services.chat_service import ChatService
from app.utils.security import decode_access_token

router = APIRouter(prefix="/api/chat", tags=["Chat"])

# Almacenar conexiones WebSocket activas por (job_id, application_id)
# application_id puede ser None para chats generales (trabajo aceptado)
ConnectionKey = Tuple[int, Optional[int]]
active_connections: Dict[ConnectionKey, List[WebSocket]] = {}


def get_user_from_token(token: str, db: Session) -> User:
    """Obtiene el usuario desde el token JWT"""
    payload = decode_access_token(token)
    if payload is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token inválido")
    
    user_id = int(payload.get("sub"))
    user = db.query(User).filter(User.id == user_id).first()
    if user is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Usuario no encontrado")
    
    return user


@router.websocket("/ws/{job_id}")
async def websocket_endpoint(websocket: WebSocket, job_id: int):
    """Endpoint WebSocket para chat en tiempo real"""
    await websocket.accept()
    
    # Obtener token de headers primero (más seguro), luego de query params como fallback
    token = None
    # Intentar obtener de headers Authorization: Bearer <token>
    auth_header = websocket.headers.get("Authorization")
    if auth_header and auth_header.startswith("Bearer "):
        token = auth_header.split("Bearer ")[1]
    # Si no está en headers, intentar query params (compatibilidad hacia atrás)
    if not token:
        token = websocket.query_params.get("token")
    
    if not token:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION, reason="Token no proporcionado")
        return
    
    # Obtener usuario desde token
    from app.database import SessionLocal
    db = SessionLocal()
    try:
        user = get_user_from_token(token, db)
        
        # Verificar que el usuario tiene acceso al job
        job = db.query(Job).filter(Job.id == job_id).first()
        if not job:
            await websocket.close(code=status.WS_1008_POLICY_VIOLATION, reason="Trabajo no encontrado")
            return
        
        # Verificar que el usuario es cliente o trabajador del trabajo
        is_client = job.client_id == user.id
        
        # Verificar si el usuario es trabajador: obtener el worker asociado al user
        from app.services.worker_service import WorkerService
        from app.models.job_application import JobApplication
        query_application_id = websocket.query_params.get("application_id")
        resolved_application_id: Optional[int] = None
        if query_application_id not in (None, "", "null", "-1"):
            try:
                resolved_application_id = int(query_application_id)
            except ValueError:
                resolved_application_id = None
        is_worker = False
        worker = WorkerService.get_worker_by_user_id(db, user.id)
        if worker:
            if job.worker_id is not None and worker.id == job.worker_id:
                is_worker = True
            else:
                # Validar si el trabajador tiene una aplicación a este trabajo
                application_query = db.query(JobApplication).filter(
                    JobApplication.job_id == job_id,
                    JobApplication.worker_id == worker.id
                )
                if resolved_application_id:
                    application_query = application_query.filter(JobApplication.id == resolved_application_id)
                application = application_query.first()
                if application:
                    is_worker = True
                    # Si no se pasó application_id pero encontramos la aplicación, usar su ID
                    if resolved_application_id is None:
                        resolved_application_id = application.id
        
        if not is_client and not is_worker:
            await websocket.close(code=status.WS_1008_POLICY_VIOLATION, reason="No tienes acceso a este chat")
            return
        
        # Agregar conexión a la lista
        connection_key: ConnectionKey = (job_id, resolved_application_id)
        import logging
        logger = logging.getLogger(__name__)
        logger.info(f"🔌🔌🔌 NUEVA CONEXIÓN WEBSOCKET - job_id={job_id}, application_id={resolved_application_id}")
        logger.info(f"🔌🔌🔌 Connection key: {connection_key}")
        
        if connection_key not in active_connections:
            active_connections[connection_key] = []
        active_connections[connection_key].append(websocket)
        
        logger.info(f"✅✅✅ Conexión agregada. Total conexiones para este key: {len(active_connections[connection_key])}")
        logger.info(f"✅✅✅ Total conexiones activas: {sum(len(conns) for conns in active_connections.values())}")
        
        # Enviar mensaje de bienvenida
        await websocket.send_json({
            "type": "connected",
            "message": "Conectado al chat"
        })
        
        # Escuchar mensajes
        try:
            while True:
                data = await websocket.receive_text()
                message_data = json.loads(data)
                
                # Crear mensaje
                message_create = MessageCreate(
                    job_id=job_id,
                    application_id=resolved_application_id,
                    content=message_data.get("content", ""),
                    has_image=message_data.get("has_image", False),
                    image_url=message_data.get("image_url", None)
                )
                
                message = ChatService.create_message(db, message_create, user.id)
                message_response = ChatService.message_to_response(message)
                
                # Enviar mensaje a todos los conectados a este job + application_id
                if connection_key in active_connections:
                    import logging
                    logger = logging.getLogger(__name__)
                    logger.info(
                        f"Enviando mensaje WebSocket a {len(active_connections[connection_key])} "
                        f"conexiones para job {job_id} (application_id={resolved_application_id})"
                    )
                    
                    disconnected = []
                    for conn in active_connections[connection_key]:
                        try:
                            # Usar model_dump con mode='json' para serializar datetime correctamente
                            # Si model_dump no existe (Pydantic v1), usar dict() con conversión manual
                            try:
                                message_dict = message_response.model_dump(mode='json')
                            except AttributeError:
                                # Fallback para Pydantic v1
                                from datetime import datetime
                                message_dict = message_response.dict()
                                # Convertir datetime a string manualmente
                                if 'created_at' in message_dict and isinstance(message_dict['created_at'], datetime):
                                    message_dict['created_at'] = message_dict['created_at'].isoformat()
                            
                            await conn.send_json({
                                "type": "message",
                                "data": message_dict
                            })
                            logger.info(f"Mensaje WebSocket enviado exitosamente")
                        except Exception as e:
                            logger.error(f"Error al enviar mensaje WebSocket: {str(e)}")
                            import traceback
                            logger.error(f"Traceback: {traceback.format_exc()}")
                            disconnected.append(conn)
                    
                    # Remover conexiones desconectadas
                    for conn in disconnected:
                        active_connections[connection_key].remove(conn)
                        logger.warning(f"Conexión WebSocket desconectada removida")
                    if not active_connections[connection_key]:
                        del active_connections[connection_key]
        
        except WebSocketDisconnect:
            # Remover conexión al desconectarse
            if connection_key in active_connections:
                if websocket in active_connections[connection_key]:
                    active_connections[connection_key].remove(websocket)
                if len(active_connections[connection_key]) == 0:
                    del active_connections[connection_key]
    
    finally:
        db.close()


@router.get("/{job_id}/messages", response_model=List[MessageResponse])
async def get_messages(
    job_id: int,
    application_id: Optional[int] = None,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Obtiene el historial de mensajes de un trabajo (opcionalmente filtrado por aplicación)"""
    messages = ChatService.get_messages_by_job(db, job_id, current_user.id, application_id)
    return [ChatService.message_to_response(msg) for msg in messages]


@router.post("/{job_id}/send", response_model=MessageResponse)
async def send_message(
    job_id: int,
    message_create: MessageCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Envía un mensaje (endpoint REST alternativo)"""
    message_create.job_id = job_id
    message = ChatService.create_message(db, message_create, current_user.id)
    message_response = ChatService.message_to_response(message)
    
    # Enviar a conexiones WebSocket activas (solo las del mismo trabajo + application_id)
    connection_key: ConnectionKey = (job_id, message.application_id)
    
    import logging
    logger = logging.getLogger(__name__)
    logger.info(f"🔍 Buscando conexiones WebSocket para job_id={job_id}, application_id={message.application_id}")
    logger.info(f"🔍 Connection key: {connection_key}")
    logger.info(f"🔍 Conexiones activas totales: {len(active_connections)}")
    logger.info(f"🔍 Keys disponibles: {list(active_connections.keys())}")
    
    if connection_key in active_connections:
        logger.info(
            f"✅ Enviando mensaje a {len(active_connections[connection_key])} conexiones WebSocket "
            f"para job {job_id} (application_id={message.application_id})"
        )
        
        disconnected = []
        for conn in active_connections[connection_key]:
            try:
                # Usar model_dump con mode='json' para serializar datetime correctamente
                # Si model_dump no existe (Pydantic v1), usar dict() con json_encoders
                try:
                    message_dict = message_response.model_dump(mode='json')
                except AttributeError:
                    # Fallback para Pydantic v1
                    from datetime import datetime
                    import json
                    message_dict = message_response.dict()
                    # Convertir datetime a string manualmente
                    if 'created_at' in message_dict and isinstance(message_dict['created_at'], datetime):
                        message_dict['created_at'] = message_dict['created_at'].isoformat()
                
                await conn.send_json({
                    "type": "message",
                    "data": message_dict
                })
                logger.info(f"✅✅✅ Mensaje enviado exitosamente a una conexión WebSocket")
            except Exception as e:
                logger.error(f"❌❌❌ Error al enviar mensaje a WebSocket: {str(e)}")
                logger.error(f"❌❌❌ Tipo de error: {type(e).__name__}")
                import traceback
                logger.error(f"❌❌❌ Traceback: {traceback.format_exc()}")
                disconnected.append(conn)
        
        # Remover conexiones desconectadas
        for conn in disconnected:
            active_connections[connection_key].remove(conn)
            logger.warning(f"⚠️ Conexión WebSocket desconectada removida")
        if not active_connections[connection_key]:
            del active_connections[connection_key]
    else:
        logger.warning(f"⚠️⚠️⚠️ NO HAY CONEXIONES WEBSOCKET ACTIVAS para connection_key={connection_key}")
        logger.warning(f"⚠️⚠️⚠️ Esto significa que ningún cliente está conectado por WebSocket")
    
    # Enviar notificación al dashboard del cliente (si es que el mensaje no fue enviado por el cliente)
    # Esto permite que el cliente vea nuevos mensajes en su dashboard sin necesidad de recargar
    # IMPORTANTE: Hacer esto de forma asíncrona para no bloquear la respuesta
    import asyncio
    from app.api.routes.notifications import send_dashboard_notification
    from app.models.job import Job
    from app.models.worker import Worker
    from sqlalchemy.orm import joinedload
    
    # Obtener el trabajo con relaciones cargadas para incluir info del trabajador
    job = db.query(Job).options(
        joinedload(Job.worker).joinedload(Worker.user)
    ).filter(Job.id == job_id).first()
    
    if job and job.client_id != current_user.id:
        # Preparar datos del trabajador para la notificación
        worker_info = None
        if job.worker and job.worker.user:
            worker_info = {
                "id": job.worker.id,
                "full_name": job.worker.user.full_name,
                "phone": job.worker.user.phone,
                "profile_image_url": job.worker.user.profile_image_url,
                "is_verified": job.worker.is_verified
            }
        
        # Si el mensaje fue enviado por un trabajador, incluir su info
        sender_info = None
        if current_user.role.value == "worker":
            sender_info = {
                "id": current_user.id,
                "full_name": current_user.full_name,
                "phone": current_user.phone,
                "profile_image_url": current_user.profile_image_url
            }
        
        notification_data = {
            "job_id": job_id,
            "application_id": message.application_id,
            "message_id": message.id,
            "sender_id": current_user.id,
            "sender_name": current_user.full_name,
            "sender_info": sender_info,  # Info completa del trabajador que envió el mensaje
            "worker_info": worker_info,  # Info del trabajador asignado al trabajo
            "content": message.content[:100] if message.content else "",  # Primeros 100 caracteres
            "created_at": message.created_at.isoformat() if message.created_at else None
        }
        
        # Enviar notificación de forma asíncrona (no bloquear la respuesta)
        try:
            await send_dashboard_notification(job.client_id, "new_message", notification_data)
            logger.info(f"📬 Notificación de nuevo mensaje enviada al dashboard del cliente (user_id={job.client_id})")
        except Exception as e:
            logger.error(f"❌ Error al enviar notificación al dashboard: {e}")
    
    return message_response

