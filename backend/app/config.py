from pydantic_settings import BaseSettings
from typing import List


class Settings(BaseSettings):
    # Database Configuration
    # Defaults para Laragon (desarrollo local)
    # Puedes sobrescribir estos valores con un archivo .env
    MYSQL_HOST: str = "localhost"
    MYSQL_PORT: int = 3306
    MYSQL_USER: str = "root"
    MYSQL_PASSWORD: str = ""  # Laragon por defecto no tiene contraseña, o la que hayas configurado
    MYSQL_DATABASE: str = "getjob_db"
    
    # JWT Configuration
    # IMPORTANTE: En producción, SECRET_KEY DEBE estar en .env o variable de entorno
    # En desarrollo tiene un valor por defecto, pero en producción es obligatorio
    SECRET_KEY: str = "dev-secret-key-cambiar-en-produccion"  # Solo para desarrollo
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    
    # Environment
    ENVIRONMENT: str = "development"  # development | production
    
    # CORS Configuration
    # En desarrollo: permite localhost y emulador Android
    # En producción: especificar dominios exactos
    ALLOWED_ORIGINS: List[str] = [
        "http://localhost:3000",
        "http://localhost:8000",
        "http://127.0.0.1:8000",
        "http://10.0.2.2:8000",  # Emulador Android
    ]
    
    class Config:
        env_file = ".env"
        case_sensitive = True
    
    @property
    def is_development(self) -> bool:
        """Verifica si estamos en entorno de desarrollo"""
        return self.ENVIRONMENT.lower() == "development"
    
    @property
    def is_production(self) -> bool:
        """Verifica si estamos en entorno de producción"""
        return self.ENVIRONMENT.lower() == "production"
    
    @property
    def database_url(self) -> str:
        """Construye la URL de conexión a la base de datos"""
        # Construye la URL desde variables individuales
        # Si no hay contraseña, no incluirla en la URL
        if self.MYSQL_PASSWORD:
            return f"mysql+pymysql://{self.MYSQL_USER}:{self.MYSQL_PASSWORD}@{self.MYSQL_HOST}:{self.MYSQL_PORT}/{self.MYSQL_DATABASE}"
        else:
            return f"mysql+pymysql://{self.MYSQL_USER}@{self.MYSQL_HOST}:{self.MYSQL_PORT}/{self.MYSQL_DATABASE}"


# Instancia global de configuración
settings = Settings()

