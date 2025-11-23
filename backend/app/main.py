import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.database import engine, Base
from app.config import settings

# Importar modelos para que SQLAlchemy los reconozca
# Importamos el módulo completo en lugar de modelos individuales
import app.models  # noqa: F401

# Configurar logging
logging.basicConfig(
    level=logging.INFO if settings.is_development else logging.WARNING,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

# Crear aplicación FastAPI (similar a @SpringBootApplication)
app = FastAPI(
    title="ServiFast API",
    description="API para conectar trabajadores con clientes",
    version="1.0.0"
)

# CORS: Configuración según entorno
# IMPORTANTE: allow_origins=["*"] NO es compatible con allow_credentials=True
# En desarrollo: permite orígenes comunes (localhost, emulador Android)
# En producción: usar ALLOWED_ORIGINS específicos desde .env
if settings.is_development:
    # Desarrollo: permite orígenes comunes + cualquier origen (sin credentials)
    # Si necesitas credentials, especifica orígenes exactos
    allowed_origins = settings.ALLOWED_ORIGINS + ["*"]
    allow_credentials = False  # En dev, si usas "*", no puedes usar credentials
    logger.info("CORS configurado para desarrollo - permite todos los orígenes")
else:
    # Producción: solo orígenes específicos
    allowed_origins = settings.ALLOWED_ORIGINS
    allow_credentials = True  # En producción, con orígenes específicos sí puedes
    if not allowed_origins:
        logger.error("ALLOWED_ORIGINS no configurado en producción!")
        allowed_origins = []

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_credentials=allow_credentials,
    allow_methods=["*"],
    allow_headers=["*"],
)


# NOTA: Creación de tablas
# ========================
# NO usar Base.metadata.create_all() en producción.
# 
# Para crear/migrar tablas, usar Alembic (sistema de migraciones):
#   1. Instalar: pip install alembic
#   2. Inicializar: alembic init alembic
#   3. Crear migración: alembic revision --autogenerate -m "Initial migration"
#   4. Aplicar: alembic upgrade head
#
# Ver: https://alembic.sqlalchemy.org/
#
# Para desarrollo rápido (solo primera vez):
#   Descomentar la siguiente línea SOLO la primera vez:
# Base.metadata.create_all(bind=engine)


@app.on_event("startup")
async def startup_event():
    """Evento que se ejecuta al iniciar la aplicación"""
    logger.info(f"ServiFast API iniciando en modo: {settings.ENVIRONMENT}")
    logger.info(f"Base de datos: {settings.MYSQL_DATABASE}@{settings.MYSQL_HOST}")
    
    # Validaciones de seguridad en producción
    if settings.is_production:
        if settings.SECRET_KEY == "dev-secret-key-cambiar-en-produccion":
            logger.error("⚠️  SECRET_KEY no configurado! Usando valor por defecto (INSEGURO)")
            logger.error("⚠️  Configura SECRET_KEY en .env o variable de entorno")
        
        if not settings.ALLOWED_ORIGINS or "*" in settings.ALLOWED_ORIGINS:
            logger.warning("⚠️  CORS permite todos los orígenes en producción (INSEGURO)")
            logger.warning("⚠️  Configura ALLOWED_ORIGINS específicos en .env")
    
    # Aquí puedes agregar lógica de inicialización si es necesario


@app.get("/")
async def root():
    """Endpoint de prueba"""
    return {
        "message": "ServiFast API está funcionando!",
        "version": "1.0.0"
    }


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "ok"}


# Incluir routers (controllers)
from app.api.routes import auth, workers, jobs, commissions, manager, chat, location, subscriptions, notifications

app.include_router(auth.router)
app.include_router(workers.router)
app.include_router(jobs.router)
app.include_router(commissions.router)
app.include_router(manager.router)
app.include_router(chat.router)
app.include_router(location.router)
app.include_router(subscriptions.router)
app.include_router(notifications.router)

