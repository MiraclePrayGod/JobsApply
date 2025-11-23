# ServiFast Backend

Backend desarrollado con FastAPI + MySQL

## Estructura del Proyecto

```
backend/
├── app/
│   ├── __init__.py
│   ├── main.py              # Punto de entrada (similar a Application.java)
│   ├── config.py            # Configuración (application.properties)
│   ├── database.py           # Conexión a BD (DataSource)
│   │
│   ├── models/              # Entidades (JPA Entities)
│   │   └── __init__.py
│   │
│   ├── schemas/             # DTOs (Pydantic)
│   │   └── __init__.py
│   │
│   ├── services/            # Lógica de negocio (Services)
│   │   └── __init__.py
│   │
│   ├── api/                 # Controllers
│   │   ├── __init__.py
│   │   └── routes/          # Endpoints (REST Controllers)
│   │       └── __init__.py
│   │
│   └── utils/               # Utilidades
│       └── __init__.py
│
├── requirements.txt
├── .env
└── README.md
```

## Instalación

1. Crear entorno virtual:
```bash
python -m venv venv
```

2. Activar entorno virtual:
```bash
# Windows
venv\Scripts\activate

# Mac/Linux
source venv/bin/activate
```

3. Instalar dependencias:
```bash
pip install -r requirements.txt
```

4. Configurar base de datos MySQL en Laragon:
   - Asegúrate de que Laragon esté ejecutándose y MySQL esté activo
   - Abre HeidiSQL o phpMyAdmin desde Laragon
   - Crea una base de datos llamada `getjob_db` (o la que configures en `.env`)
   - Por defecto, Laragon usa:
     - Host: `localhost`
     - Puerto: `3306`
     - Usuario: `root`
     - Contraseña: (vacía por defecto)

5. Configurar variables de entorno:
   - El archivo `.env` ya está creado con valores por defecto para Laragon
   - Si tu MySQL tiene contraseña, edita `backend/.env` y actualiza `MYSQL_PASSWORD`

6. Crear la base de datos en MySQL:
   - Abre **HeidiSQL** o **phpMyAdmin** desde Laragon
   - Ejecuta este comando SQL o usa el script `migrations/create_database.sql`:
   ```sql
   CREATE DATABASE IF NOT EXISTS getjob_db 
   CHARACTER SET utf8mb4 
   COLLATE utf8mb4_unicode_ci;
   ```

7. Inicializar la base de datos (RECOMENDADO - ejecuta todo automáticamente):
```bash
# Opción 1: Script maestro (recomendado - hace todo automáticamente)
python setup_database.py
```

   O si prefieres hacerlo paso a paso:
```bash
# Opción 2: Paso a paso
# 1. Crear las tablas
python init_db.py

# 2. Ejecutar migraciones (si es necesario)
python migrate_add_user_fields.py
python migrate_add_worker_verification_fields.py
python migrate_add_job_applications.py

# 3. (Opcional) Poblar con datos de prueba
python seed_data.py
```

8. Ejecutar servidor:
```bash
uvicorn app.main:app --reload
```

El servidor estará en: http://localhost:8000

## Documentación API

- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

