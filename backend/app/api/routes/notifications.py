"""
Router para notificaciones en tiempo real del dashboard
WebSocket global para recibir notificaciones de nuevos mensajes, trabajos, etc.
"""
from fastapi import APIRouter, WebSocket, WebSocketDisconnect, HTTPException, status
from sqlalchemy.orm import Session
from typing import Dict, List
import logging
from app.database import SessionLocal
from app.utils.security import decode_access_token
from app.models.user import User

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/notifications", tags=["Notifications"])

# Almacenar conexiones WebSocket activas por user_id
# Cada usuario (cliente o trabajador) tiene su propia conexión para notificaciones del dashboard
dashboard_connections: Dict[int, WebSocket] = {}


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


@router.websocket("/ws/dashboard")
async def dashboard_websocket_endpoint(websocket: WebSocket):
    """
    Endpoint WebSocket global para notificaciones del dashboard
    
    Escucha notificaciones de:
    - Nuevos mensajes en cualquier trabajo
    - Nuevas aplicaciones de trabajadores
    - Cambios de estado en trabajos
    - Etc.
    """
    await websocket.accept()
    
    # Obtener token de headers
    token = None
    auth_header = websocket.headers.get("Authorization")
    if auth_header and auth_header.startswith("Bearer "):
        token = auth_header.split("Bearer ")[1]
    
    if not token:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION, reason="Token no proporcionado")
        return
    
    # Obtener usuario desde token
    db = SessionLocal()
    user = None
    try:
        user = get_user_from_token(token, db)
        
        # Si el usuario ya tiene una conexión activa, cerrarla primero
        if user.id in dashboard_connections:
            try:
                old_websocket = dashboard_connections[user.id]
                await old_websocket.close(code=1000, reason="Nueva conexión establecida")
            except Exception as e:
                logger.warning(f"Error al cerrar conexión anterior para user {user.id}: {e}")
        
        # Agregar nueva conexión
        dashboard_connections[user.id] = websocket
        logger.info(f"✅ Conexión WebSocket dashboard establecida para user_id={user.id} ({user.email})")
        logger.info(f"📊 Total conexiones dashboard activas: {len(dashboard_connections)}")
        
        # Enviar mensaje de bienvenida
        await websocket.send_json({
            "type": "connected",
            "message": "Conectado al dashboard",
            "user_id": user.id
        })
        
        # Mantener conexión abierta y escuchar mensajes
        try:
            while True:
                # Recibir mensajes del cliente (ping/pong, etc.)
                data = await websocket.receive_text()
                logger.debug(f"📨 Mensaje recibido del dashboard (user_id={user.id}): {data}")
                
                # Responder a ping con pong
                if data == "ping":
                    await websocket.send_json({"type": "pong"})
        
        except WebSocketDisconnect:
            logger.info(f"🔌 WebSocket dashboard desconectado para user_id={user.id}")
    
    except Exception as e:
        logger.error(f"❌ Error en WebSocket dashboard: {e}")
        try:
            await websocket.close(code=status.WS_1011_INTERNAL_ERROR, reason="Error interno")
        except:
            pass
    
    finally:
        # Remover conexión al desconectarse
        if user and user.id in dashboard_connections:
            if dashboard_connections[user.id] == websocket:
                del dashboard_connections[user.id]
                logger.info(f"🗑️ Conexión dashboard removida para user_id={user.id}")
        db.close()


async def send_dashboard_notification(user_id: int, notification_type: str, data: dict):
    """
    Envía una notificación a un usuario específico a través de su conexión WebSocket del dashboard
    
    Args:
        user_id: ID del usuario que recibirá la notificación
        notification_type: Tipo de notificación (ej: "new_message", "new_application", "job_status_changed")
        data: Datos de la notificación
    """
    if user_id not in dashboard_connections:
        logger.debug(f"⚠️ Usuario {user_id} no tiene conexión WebSocket activa para dashboard")
        return False
    
    websocket = dashboard_connections[user_id]
    try:
        notification = {
            "type": notification_type,
            "data": data
        }
        
        # Intentar serializar datetime si existe
        from datetime import datetime
        if 'created_at' in data and isinstance(data.get('created_at'), datetime):
            data['created_at'] = data['created_at'].isoformat()
        
        await websocket.send_json(notification)
        logger.info(f"✅ Notificación '{notification_type}' enviada a user_id={user_id}")
        return True
    
    except Exception as e:
        logger.error(f"❌ Error al enviar notificación a user_id={user_id}: {e}")
        # Remover conexión si está rota
        if user_id in dashboard_connections and dashboard_connections[user_id] == websocket:
            del dashboard_connections[user_id]
        return False

